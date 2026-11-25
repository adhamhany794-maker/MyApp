package com.example.assistant

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.example.models.AppLanguage
import com.example.models.ExceptionType
import com.example.models.RuleException
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiAssistantEngine"
private const val GEMINI_TIMEOUT_MS = 45_000L

class GeminiAssistantEngine(
    private val context: Context? = null,
    private val fallbackEngine: CompanionAssistantEngine = LocalDemoAssistantEngine(),
    private val connectivityChecker: (() -> Boolean)? = null,
    private val modelInvoker: (suspend (String) -> String?)? = null,
    private val modelByNameInvoker: (suspend (modelName: String, prompt: String) -> String?)? = null,
    private val retryDelayMs: Long = HIGH_DEMAND_RETRY_DELAY_MS
) : CompanionAssistantEngine {

    private val routineResponseSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "status" to Schema.enumeration(
                    values = listOf("ok", "needs_clarification"),
                    description = "Use ok when all required routine fields are known; needs_clarification if any detail is missing or the request is not about creating a routine."
                ),
                "followUpQuestion" to Schema.string(
                    description = "One short follow-up question in the user's language when status is needs_clarification; null when status is ok.",
                    nullable = true
                ),
                "name" to Schema.string(
                    description = "Short descriptive routine name in the user's language."
                ),
                "trigger" to Schema.obj(
                    properties = mapOf(
                        "type" to Schema.enumeration(
                            values = listOf("time_range", "days", "place_enter", "place_exit"),
                            description = "Trigger category: time_range, days, place_enter, or place_exit."
                        ),
                        "startTime" to Schema.string(
                            description = "24-hour HH:mm start time (e.g. 20:00) for time_range.",
                            nullable = true
                        ),
                        "endTime" to Schema.string(
                            description = "24-hour HH:mm end time (e.g. 22:00) for time_range.",
                            nullable = true
                        ),
                        "days" to Schema.array(
                            items = Schema.enumeration(
                                values = listOf(
                                    "EVERY_DAY", "WEEKDAYS", "WEEKENDS",
                                    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
                                    "FRIDAY", "SATURDAY", "SUNDAY"
                                )
                            ),
                            description = "Applicable days for the routine."
                        ),
                        "place" to Schema.enumeration(
                            values = listOf("HOME", "UNIVERSITY", "WORKPLACE", "LIBRARY"),
                            description = "Place name for place_enter or place_exit.",
                            nullable = true
                        )
                    ),
                    optionalProperties = listOf("startTime", "endTime", "place")
                ),
                "conditions" to Schema.array(
                    items = Schema.enumeration(
                        values = listOf(
                            "AUTOMATION_ENABLED",
                            "DAY_OF_WEEK",
                            "TIME_RANGE",
                            "ACTIVE_PROFILE",
                            "SIMULATED_LOCATION",
                            "SIMULATED_ACTIVITY",
                            "FOCUS_SESSION_ACTIVE"
                        )
                    ),
                    description = "Optional extra conditions; usually [\"AUTOMATION_ENABLED\"]."
                ),
                "action" to Schema.obj(
                    properties = mapOf(
                        "mode" to Schema.enumeration(
                            values = listOf("sound", "vibrate", "silent", "focus"),
                            description = "Phone mode when routine activates."
                        ),
                        "focusMinutes" to Schema.integer(
                            description = "Duration in minutes (5-240) when mode is focus.",
                            nullable = true
                        )
                    ),
                    optionalProperties = listOf("focusMinutes")
                ),
                "exceptions" to Schema.array(
                    items = Schema.enumeration(
                        values = listOf("EMERGENCY_CONTACTS", "PRIORITY_CONTACTS")
                    ),
                    description = "Always include EMERGENCY_CONTACTS."
                )
            ),
            optionalProperties = listOf("followUpQuestion")
        )
    }

    private val primaryModel by lazy { createGenerativeModel(PRIMARY_MODEL_NAME) }
    private val fallbackModel by lazy { createGenerativeModel(FALLBACK_MODEL_NAME) }

    private fun createGenerativeModel(modelName: String) =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = routineResponseSchema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(SYSTEM_INSTRUCTION)
            }
        )

    override suspend fun processPrompt(
        prompt: String,
        state: AssistantStateSnapshot
    ): AssistantReply {
        val isArabic = state.language == AppLanguage.ARABIC || prompt.any { it in '\u0600'..'\u06FF' }

        if (!isNetworkAvailable()) {
            Log.w(TAG, "No internet connection detected; using offline LocalDemoAssistantEngine.")
            return runOfflineFallback(prompt, state, isArabic, isOffline = true)
        }

        val fullPrompt = buildModelPrompt(prompt, state, isArabic)

        return try {
            val rawJson = withTimeout(GEMINI_TIMEOUT_MS) {
                try {
                    invokeWithHighDemandRetry(PRIMARY_MODEL_NAME, fullPrompt)
                } catch (e: TimeoutCancellationException) {
                    throw e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (isTransientHighDemandError(e)) {
                        Log.w(
                            TAG,
                            "Primary model $PRIMARY_MODEL_NAME unavailable after $MAX_HIGH_DEMAND_RETRIES retries; falling back to $FALLBACK_MODEL_NAME",
                            e
                        )
                        invokeWithHighDemandRetry(FALLBACK_MODEL_NAME, fullPrompt)
                    } else {
                        throw e
                    }
                }
            }.orEmpty()

            when (val validated = RoutineJsonValidator.parseAndValidate(
                rawJson = rawJson,
                originalPrompt = prompt,
                existingRoutines = state.routines,
                isArabic = isArabic
            )) {
                is RoutineJsonValidationResult.Valid -> {
                    AssistantReply(
                        text = validated.summaryMessage,
                        proposal = validated.proposal
                    )
                }
                is RoutineJsonValidationResult.NeedsClarification -> {
                    AssistantReply(text = validated.question)
                }
            }
        } catch (e: TimeoutCancellationException) {
            val errorDetail = formatErrorDetails(e)
            Log.e(TAG, "Gemini request timed out: $errorDetail", e)
            runOfflineFallback(prompt, state, isArabic, isOffline = false, debugError = errorDetail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorDetail = formatErrorDetails(e)
            val debugToken = readPersistedAppCheckDebugToken()
            Log.e(
                TAG,
                "Gemini call failed: $errorDetail" +
                    (if (debugToken != null) " | AppCheckDebugToken=$debugToken" else ""),
                e
            )
            runOfflineFallback(
                prompt = prompt,
                state = state,
                isArabic = isArabic,
                isOffline = false,
                debugError = if (debugToken != null) "$errorDetail\nApp Check debug token: $debugToken" else errorDetail
            )
        }
    }

    private suspend fun runOfflineFallback(
        prompt: String,
        state: AssistantStateSnapshot,
        isArabic: Boolean,
        isOffline: Boolean,
        debugError: String? = null
    ): AssistantReply {
        val baseNotice = if (isArabic) {
            if (isOffline) "لا يوجد اتصال بالإنترنت — تم استخدام المساعد المحلي دون اتصال."
            else "تعذّر الوصول إلى Gemini الآن — تم استخدام المساعد المحلي."
        } else {
            if (isOffline) "No internet connection — using offline local assistant."
            else "Gemini is unavailable right now — using offline local assistant."
        }
        val notice = if (BuildConfig.DEBUG && !debugError.isNullOrBlank()) {
            "$baseNotice\n[Debug] $debugError"
        } else {
            baseNotice
        }

        val rawFallback = fallbackEngine.processPrompt(prompt, state)

        val rawProposal = rawFallback.proposal
        if (rawProposal != null) {
            // Enforce Rule 6 (emergency contacts always excluded) and Rule 5 (conflict check)
            val enforcedExceptions = buildList {
                add(RuleException(ExceptionType.EMERGENCY_CONTACTS, "Emergency contacts always allowed"))
                rawProposal.routine.exceptions
                    .filter { it.type != ExceptionType.EMERGENCY_CONTACTS }
                    .forEach { add(it) }
            }
            val safeRoutine = rawProposal.routine.copy(exceptions = enforcedExceptions)
            val conflictWarning = RoutineConflictChecker.findConflictWarning(
                proposed = safeRoutine,
                existingRoutines = state.routines,
                isArabic = isArabic
            )
            val updatedExceptionSummary = if (isArabic) {
                if (rawProposal.exceptionSummary.contains("طوارئ")) rawProposal.exceptionSummary
                else "جهات الطوارئ تصلك دائماً"
            } else {
                if (rawProposal.exceptionSummary.contains("Emergency", ignoreCase = true)) rawProposal.exceptionSummary
                else "Emergency contacts always allowed"
            }
            val textWithConflict = if (conflictWarning != null && !rawFallback.text.contains(conflictWarning)) {
                "${rawFallback.text}\n\n$conflictWarning"
            } else {
                rawFallback.text
            }
            return AssistantReply(
                text = textWithConflict,
                proposal = rawProposal.copy(
                    routine = safeRoutine,
                    exceptionSummary = updatedExceptionSummary,
                    conflictWarning = conflictWarning
                ),
                focusMinutesToStart = null, // Rule 8: never take direct actions on phone
                noticeText = notice,
                debugErrorLog = debugError
            )
        }

        // Rule 8: The assistant only creates routines and must not execute direct actions
        val safeText = if (rawFallback.focusMinutesToStart != null) {
            if (isArabic) {
                "يمكنني فقط إنشاء روتينات للموافقة عليها. اكتب متى تريد تشغيل روتين التركيز (مثلاً: \"روتين تركيز 45 دقيقة كل مساء الساعة 7\") لأجهّز لك البطاقة."
            } else {
                "I can only create routines for you to approve. Tell me when the focus routine should run (for example: \"Start a 45m focus routine every evening at 7 PM\")."
            }
        } else {
            rawFallback.text
        }

        return AssistantReply(
            text = safeText,
            proposal = null,
            focusMinutesToStart = null,
            noticeText = notice,
            debugErrorLog = debugError
        )
    }

    private suspend fun invokeWithHighDemandRetry(modelName: String, fullPrompt: String): String? {
        for (attempt in 0..MAX_HIGH_DEMAND_RETRIES) {
            try {
                return invokeSingleModel(modelName, fullPrompt)
            } catch (e: TimeoutCancellationException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isTransientHighDemandError(e) && attempt < MAX_HIGH_DEMAND_RETRIES) {
                    Log.w(
                        TAG,
                        "Model $modelName hit 503/high-demand (attempt ${attempt + 1}/${MAX_HIGH_DEMAND_RETRIES + 1}); retrying in ${retryDelayMs}ms",
                        e
                    )
                    if (retryDelayMs > 0L) delay(retryDelayMs)
                } else {
                    throw e
                }
            }
        }
        return null
    }

    private suspend fun invokeSingleModel(modelName: String, fullPrompt: String): String? {
        if (modelByNameInvoker != null) {
            return modelByNameInvoker.invoke(modelName, fullPrompt)
        }
        if (modelInvoker != null) {
            return modelInvoker.invoke(fullPrompt)
        }
        val activeModel = if (modelName == FALLBACK_MODEL_NAME) fallbackModel else primaryModel
        return activeModel.generateContent(fullPrompt).text
    }

    internal fun isTransientHighDemandError(throwable: Throwable): Boolean {
        val details = generateSequence(throwable) { it.cause }
            .take(5)
            .joinToString(" | ") { "${it.javaClass.name}: ${it.message.orEmpty()}" }
            .lowercase()
        return details.contains("503") ||
            details.contains("high demand") ||
            details.contains("overloaded") ||
            details.contains("service unavailable") ||
            details.contains("temporarily unavailable") ||
            details.contains("serviceunavailable")
    }

    private fun formatErrorDetails(throwable: Throwable): String {
        val chain = generateSequence(throwable) { it.cause }
            .take(4)
            .map { err ->
                val name = err.javaClass.simpleName.ifBlank { "Exception" }
                val msg = err.localizedMessage ?: err.message ?: "No message"
                "$name: $msg"
            }
            .toList()
        return chain.joinToString(" -> ")
    }

    private fun readPersistedAppCheckDebugToken(): String? {
        if (!BuildConfig.DEBUG) return null
        val appContext = context ?: return null
        return runCatching {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val prefs = appContext.getSharedPreferences(
                "com.google.firebase.appcheck.debug.store.$persistenceKey",
                Context.MODE_PRIVATE
            )
            prefs.getString("com.google.firebase.appcheck.debug.DEBUG_SECRET", null)
        }.getOrNull()
    }

    private fun isNetworkAvailable(): Boolean {
        if (connectivityChecker != null) return connectivityChecker.invoke()
        val appContext = context ?: return true
        return runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    private fun buildModelPrompt(
        prompt: String,
        state: AssistantStateSnapshot,
        isArabic: Boolean
    ): String {
        val existingSummary = state.routines
            .filter { it.isEnabled }
            .joinToString("\n") { r ->
                "- ${r.name}: ${r.trigger.type.name} (${r.scheduleStartTime} - ${r.scheduleEndTime}, ${r.trigger.dayFilter.name}, ${r.trigger.location.name})"
            }
            .ifEmpty { "None" }

        val historyText = state.recentConversation
            .takeLast(6)
            .joinToString("\n") { (isUser, msg) ->
                val role = if (isUser) "User" else "Assistant"
                "$role: $msg"
            }

        return buildString {
            appendLine("Preferred response language: ${if (isArabic) "Arabic" else "English"}")
            appendLine("Existing enabled routines (for context only):")
            appendLine(existingSummary)
            if (historyText.isNotBlank()) {
                appendLine("Recent conversation:")
                appendLine(historyText)
            }
            appendLine("Latest user message:")
            appendLine(prompt)
        }
    }

    companion object {
        const val PRIMARY_MODEL_NAME = "gemini-3.8-flash"
        const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"
        private const val MAX_HIGH_DEMAND_RETRIES = 2
        private const val HIGH_DEMAND_RETRY_DELAY_MS = 1_200L

        private const val SYSTEM_INSTRUCTION = """
You are the Smart Companion routine builder assistant.
You MUST ONLY return valid JSON matching the schema. Do NOT output markdown or prose outside JSON.
Your ONLY purpose is to convert user requests in English or Arabic into a routine creation proposal.
You MUST NEVER execute or agree to any other action on the phone (do not change settings directly, send messages, check weather, or run timers). If the user asks for anything other than creating a routine, set status="needs_clarification" and set followUpQuestion to one short polite sentence explaining that you can only help create routines and asking what routine they want.
Rules:
1. If any required field is missing or ambiguous (for example missing time range, days, place, or desired sound/focus mode), set status="needs_clarification" and ask ONE short follow-up question in the user's language instead of guessing.
2. When a routine is clear, set status="ok" and fill name, trigger, conditions, action, and exceptions.
3. For trigger.type="time_range", provide startTime and endTime in 24-hour HH:mm format (e.g. "20:00", "22:00"). If the user says "8 to 10" in a study/evening context without am/pm, you may ask whether they mean morning or evening if unclear, or use clear context cues.
4. For trigger.type="place_enter" or "place_exit", pick the closest place from HOME, UNIVERSITY, WORKPLACE, LIBRARY; if not one of those or unclear, ask in followUpQuestion.
5. action.mode MUST be one of: "sound", "vibrate", "silent", "focus".
6. Always include "EMERGENCY_CONTACTS" in exceptions. Emergency contacts are never blocked.
7. Write routine name and followUpQuestion in the same language as the user's message (Arabic or English).
"""
    }
}

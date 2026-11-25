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

class GeminiAssistantEngine(
    private val context: Context? = null,
    private val fallbackEngine: CompanionAssistantEngine = LocalDemoAssistantEngine(),
    private val connectivityChecker: (() -> Boolean)? = null,
    private val modelInvoker: (suspend (String) -> String?)? = null,
    private val modelByNameInvoker: (suspend (modelName: String, prompt: String) -> String?)? = null,
    private val retryDelayMs: Long = RETRY_DELAY_MS,
    private val attemptTimeoutMs: Long = ATTEMPT_TIMEOUT_MS
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
        val failures = mutableListOf<AttemptFailure>()

        // 1. Primary model (12s timeout)
        val primaryOutcome = executeTimedAttempt(
            attemptLabel = "Attempt 1 (primary)",
            modelName = PRIMARY_MODEL_NAME,
            fullPrompt = fullPrompt,
            failures = failures
        )
        var rawJson = primaryOutcome.json
        var shouldTryFallback = primaryOutcome.shouldTryFallback

        // 2. If primary failed with 503 / high demand / timeout -> wait 1s, retry primary once (12s timeout)
        if (rawJson == null && primaryOutcome.shouldRetryPrimary) {
            if (retryDelayMs > 0L) delay(retryDelayMs)
            val retryOutcome = executeTimedAttempt(
                attemptLabel = "Attempt 2 (primary retry)",
                modelName = PRIMARY_MODEL_NAME,
                fullPrompt = fullPrompt,
                failures = failures
            )
            rawJson = retryOutcome.json
            shouldTryFallback = retryOutcome.shouldTryFallback
        }

        // 3. If still no response -> try fallback model once (12s timeout)
        if (rawJson == null && shouldTryFallback) {
            val fallbackOutcome = executeTimedAttempt(
                attemptLabel = "Attempt ${failures.size + 1} (fallback)",
                modelName = FALLBACK_MODEL_NAME,
                fullPrompt = fullPrompt,
                failures = failures
            )
            rawJson = fallbackOutcome.json
        }

        if (rawJson != null) {
            return when (val validated = RoutineJsonValidator.parseAndValidate(
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
        }

        // 4. Local assistant as last fallback
        val debugToken = readPersistedAppCheckDebugToken()
        val attemptLog = failures.joinToString("\n") { f ->
            "${f.attemptLabel} [${f.modelName}] failed in ${f.elapsedMs} ms: ${f.errorSummary}"
        }
        val debugDetails = buildString {
            append(attemptLog)
            if (debugToken != null) {
                if (isNotEmpty()) append("\n")
                append("App Check debug token: $debugToken")
            }
        }.ifBlank { null }

        Log.e(TAG, "All Gemini attempts failed; falling back to local assistant.\n$debugDetails")
        return runOfflineFallback(
            prompt = prompt,
            state = state,
            isArabic = isArabic,
            isOffline = false,
            debugError = debugDetails
        )
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

    private data class AttemptFailure(
        val attemptLabel: String,
        val modelName: String,
        val elapsedMs: Long,
        val errorSummary: String
    )

    private data class AttemptOutcome(
        val json: String?,
        val shouldRetryPrimary: Boolean = false,
        val shouldTryFallback: Boolean = false
    )

    private suspend fun executeTimedAttempt(
        attemptLabel: String,
        modelName: String,
        fullPrompt: String,
        failures: MutableList<AttemptFailure>
    ): AttemptOutcome {
        val startNanos = System.nanoTime()
        return try {
            val responseText = withTimeout(attemptTimeoutMs) {
                invokeSingleModel(modelName, fullPrompt)
            }.orEmpty()
            AttemptOutcome(json = responseText)
        } catch (e: TimeoutCancellationException) {
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L
            val summary = formatErrorDetails(e)
            Log.w(TAG, "$attemptLabel [$modelName] timed out after ${elapsedMs}ms: $summary", e)
            failures += AttemptFailure(attemptLabel, modelName, elapsedMs, summary)
            AttemptOutcome(json = null, shouldRetryPrimary = true, shouldTryFallback = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L
            val summary = formatErrorDetails(e)
            Log.w(TAG, "$attemptLabel [$modelName] failed after ${elapsedMs}ms: $summary", e)
            failures += AttemptFailure(attemptLabel, modelName, elapsedMs, summary)
            val transient = isTransientHighDemandError(e)
            val authOrAppCheckBlocked = isAppCheckOrAuthError(e)
            AttemptOutcome(
                json = null,
                shouldRetryPrimary = transient,
                shouldTryFallback = !authOrAppCheckBlocked
            )
        }
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

    private fun isAppCheckOrAuthError(throwable: Throwable): Boolean {
        val details = generateSequence(throwable) { it.cause }
            .take(5)
            .joinToString(" | ") { "${it.javaClass.name}: ${it.message.orEmpty()}" }
            .lowercase()
        return details.contains("403") ||
            details.contains("401") ||
            details.contains("app check") ||
            details.contains("appcheck") ||
            details.contains("permission_denied")
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
        private const val ATTEMPT_TIMEOUT_MS = 12_000L
        private const val RETRY_DELAY_MS = 1_000L

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

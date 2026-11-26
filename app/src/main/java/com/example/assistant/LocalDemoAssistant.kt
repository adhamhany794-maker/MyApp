package com.example.assistant

import com.example.models.ActionType
import com.example.models.ActivityEvent
import com.example.models.AppLanguage
import com.example.models.ConditionType
import com.example.models.DayFilter
import com.example.models.ExceptionType
import com.example.models.FocusSessionRecord
import com.example.models.NotificationStat
import com.example.models.PriorityContact
import com.example.models.RoutineProposal
import com.example.models.RuleAction
import com.example.models.RuleCondition
import com.example.models.RuleException
import com.example.models.RuleTrigger
import com.example.models.SimulatedActivity
import com.example.models.SimulatedLocation
import com.example.models.SmartProfile
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TriggerType
import com.example.utils.TimeUtils
import java.util.UUID

data class AssistantStateSnapshot(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val activeProfile: SmartProfile,
    val soundMode: SoundMode,
    val isAutomationPaused: Boolean,
    val routines: List<SmartRoutine>,
    val contacts: List<PriorityContact>,
    val notificationStats: List<NotificationStat>,
    val focusSessions: List<FocusSessionRecord>,
    val recentEvents: List<ActivityEvent>,
    val recentConversation: List<Pair<Boolean, String>> = emptyList(),
    val pendingRoutineDraft: SmartRoutine? = null,
    val clarificationTurnCount: Int = 0
)

data class AssistantReply(
    val text: String,
    val proposal: RoutineProposal? = null,
    val focusMinutesToStart: Int? = null,
    val noticeText: String? = null,
    val debugErrorLog: String? = null
)

/**
 * Abstraction designed so a future version can plug in a real Gemini API backend
 * without modifying the Assistant UI or ViewModel contract.
 */
interface CompanionAssistantEngine {
    suspend fun processPrompt(prompt: String, state: AssistantStateSnapshot): AssistantReply
}

class LocalDemoAssistantEngine : CompanionAssistantEngine {

    override suspend fun processPrompt(
        prompt: String,
        state: AssistantStateSnapshot
    ): AssistantReply {
        val lower = prompt.lowercase().trim()
        val isArabic = state.language == AppLanguage.ARABIC || containsArabic(prompt)

        // 1. Focus session intent (EN & AR)
        val isFocusIntent = (lower.contains("focus") || lower.contains("تركيز")) &&
            (lower.contains("start") || lower.contains("begin") || lower.contains("session") ||
                lower.contains("ابدأ") || lower.contains("جلسة"))
        if (isFocusIntent) {
            val mins = extractNumber(lower) ?: 45
            val text = if (isArabic) {
                "جاري بدء جلسة تركيز محاكاة لمدة $mins دقيقة باستخدام وضع الدراسة حتى تبقى الإشعارات المزعجة مكتومة مع حماية جهات الأولوية."
            } else {
                "Starting a $mins-minute simulated focus session using your Study Profile so distractions stay muted while priority exceptions remain protected."
            }
            return AssistantReply(text = text, focusMinutesToStart = mins)
        }

        // 2. Most distracting apps question (EN & AR)
        val isDistractingAppsIntent = lower.contains("distracting") ||
            lower.contains("most notifications") ||
            lower.contains("تطبيقات") ||
            lower.contains("إزعاج") ||
            (lower.contains("app") && lower.contains("most"))
        if (isDistractingAppsIntent) {
            val topApps = state.notificationStats
                .groupBy { it.appName }
                .mapValues { (_, list) -> list.maxOf { it.totalCount } to list.maxOf { it.blockedCount } }
                .entries
                .sortedByDescending { it.value.first }
                .take(3)
            val lines = topApps.joinToString("\n") { (app, counts) ->
                if (isArabic) {
                    "• $app: ${counts.first} تنبيه محاكى (تم حجب ${counts.second} أثناء الأوضاع الهادئة)"
                } else {
                    "• $app: ${counts.first} simulated alerts (${counts.second} blocked during quiet profiles)"
                }
            }
            val text = if (isArabic) {
                "استناداً إلى بيانات العرض الحالية، أكثر التطبيقات مقاطعة لديك:\n\n$lines\n\nنصيحة: تحدث معظم التنبيهات الاجتماعية بين 7:00 م و 9:00 م."
            } else {
                "Based on your current demonstration analytics, your highest-interruption apps are:\n\n$lines\n\nTip: Most social spikes occur between 7:00 PM and 9:00 PM."
            }
            return AssistantReply(text = text)
        }

        // 3. Why did my phone switch to silent / active profile question (EN & AR)
        val isWhySilentIntent = (lower.contains("why") || lower.contains("لماذا")) &&
            (lower.contains("silent") || lower.contains("mode") || lower.contains("profile") ||
                lower.contains("switch") || lower.contains("صامت") || lower.contains("الوضع"))
        if (isWhySilentIntent) {
            val latestSoundOrProfile = state.recentEvents.firstOrNull {
                it.title.contains("Silent", ignoreCase = true) ||
                    it.title.contains("Profile", ignoreCase = true) ||
                    it.title.contains("Routine", ignoreCase = true)
            }
            val text = if (isArabic) {
                val reason = if (latestSoundOrProfile != null) {
                    "آخر حدث مسجل كان \"${latestSoundOrProfile.title}\" (${latestSoundOrProfile.triggerSource}): ${latestSoundOrProfile.explanation}"
                } else {
                    "الوضع النشط حالياً هو ${state.activeProfile.name} مع صوت محاكى: ${state.soundMode.arabicName}."
                }
                "الحالة المحاكاة الحالية: ${state.activeProfile.name} (${state.soundMode.arabicName}).\n\n$reason"
            } else {
                val reason = if (latestSoundOrProfile != null) {
                    "Your latest recorded event was \"${latestSoundOrProfile.title}\" (${latestSoundOrProfile.triggerSource}): ${latestSoundOrProfile.explanation}"
                } else {
                    "Your active profile is ${state.activeProfile.name} with simulated sound mode set to ${state.soundMode.displayName}."
                }
                "Current simulated state: ${state.activeProfile.name} (${state.soundMode.displayName}).\n\n$reason"
            }
            return AssistantReply(text = text)
        }

        // 4. Daily activity overview or focus/routine stats Q&A (EN & AR)
        val isDailyActivityQuery = lower.contains("daily activity") ||
            lower.contains("my activity") ||
            lower.contains("today's overview") ||
            lower.contains("نشاطي") ||
            lower.contains("ملخص اليوم") ||
            (lower.contains("show") && lower.contains("activity"))
        if (isDailyActivityQuery) {
            val totalMins = state.focusSessions.sumOf { it.completedMinutes }
            val executedCount = state.routines.sumOf { it.executionCount }
            val recentLines = state.recentEvents.take(3).joinToString("\n") { "• ${it.title} — ${it.result}" }
            val text = if (isArabic) {
                "ملخص نشاطك اليومي المحاكى:\n" +
                    "• الوضع النشط: ${state.activeProfile.name} (${state.soundMode.arabicName})\n" +
                    "• وقت التركيز المسجل: ${TimeUtils.formatDurationMinutes(totalMins, true)}\n" +
                    "• مرات تنفيذ الروتين: $executedCount\n\n" +
                    "أحدث الأحداث:\n$recentLines"
            } else {
                "Here is your simulated daily overview:\n" +
                    "• Active Profile: ${state.activeProfile.name} (${state.soundMode.displayName})\n" +
                    "• Recorded Focus Time: ${TimeUtils.formatDurationMinutes(totalMins)}\n" +
                    "• Routine Executions: $executedCount\n\n" +
                    "Latest activity:\n$recentLines"
            }
            return AssistantReply(text = text)
        }

        val isStatsOrRoutinesQuery = (lower.contains("how") && lower.contains("focus")) ||
            (lower.contains("routine") && (lower.contains("active") || lower.contains("list") || lower.contains("which"))) ||
            (lower.contains("روتين") && (lower.contains("نشط") || lower.contains("ما هي")))
        if (isStatsOrRoutinesQuery) {
            val totalMins = state.focusSessions.sumOf { it.completedMinutes }
            val enabledNames = state.routines.filter { it.isEnabled }.joinToString(", ") { it.name }
            val text = if (isArabic) {
                "لديك حالياً ${state.routines.count { it.isEnabled }} روتينات مفعّلة ($enabledNames).\n\nإجمالي وقت التركيز المسجل: ${TimeUtils.formatDurationMinutes(totalMins, true)} عبر ${state.focusSessions.size} جلسات."
            } else {
                "You currently have ${state.routines.count { it.isEnabled }} active routines ($enabledNames).\n\nTotal recorded focus time: ${TimeUtils.formatDurationMinutes(totalMins)} across ${state.focusSessions.size} sessions. Automation is ${if (state.isAutomationPaused) "Paused" else "Enabled"}."
            }
            return AssistantReply(text = text)
        }

        // 5. Out-of-scope external requests (EN & AR)
        if (lower.contains("weather") || lower.contains("stock") || lower.contains("الطقس") ||
            (lower.contains("email") && lower.contains("send")) || lower.contains("install")
        ) {
            val text = if (isArabic) {
                "أنا مساعد عرض محلي بدون إنترنت مخصص لأوضاع وروتينات المرافق الذكي فقط. الاستعلامات الخارجية أو تعديل النظام الفعلي خارج نطاق هذا العرض."
            } else {
                "I am a local offline demo assistant focused on Smart Companion profiles, routines, focus sessions, and interruption analytics. External web queries and real OS controls are outside this demo's offline scope."
            }
            return AssistantReply(text = text)
        }

        // 6. Natural-language Routine Creation Proposals & Multi-turn Draft Merging (EN & AR)
        val hasDraft = state.pendingRoutineDraft != null
        if (isRoutineCreationIntent(lower) || hasDraft) {
            val detailedRange = RoutineTimeParser.parseDetailedTimeRange(lower)
            val extractedPoints = RoutineTimeParser.extractTimePoints(lower)
            val isSleep = lower.contains("sleep") || lower.contains("sleeping") || lower.contains("bed") || lower.contains("نوم") || lower.contains("ليل") || lower.contains("بليل")
            val isStudy = lower.contains("study") || lower.contains("studying") || lower.contains("دراسة")
            val isUniversity = lower.contains("university") || lower.contains("campus") || lower.contains("جامعة")
            val isWork = lower.contains("work") || lower.contains("office") || lower.contains("عمل")
            val hasStartAndEnd = detailedRange != null || extractedPoints.size >= 2
            val hasPresetDefaults = isSleep || isStudy || isUniversity || isWork || hasStartAndEnd

            if (extractedPoints.isEmpty() && !hasPresetDefaults && state.pendingRoutineDraft == null && !hasStartAndEnd) {
                val text = if (isArabic) {
                    "فهمت طلبك لإنشاء الروتين. ما هي أوقات البداية والنهاية (مثلاً من 8 إلى 11)؟"
                } else {
                    "I understand you want to create a routine. What are the start and end times (for example 8 to 11)?"
                }
                val placeholderRoutine = SmartRoutine(
                    id = "routine_draft_${UUID.randomUUID().toString().take(6)}",
                    name = if (isArabic) "روتين مخصص" else "Custom Routine",
                    description = prompt,
                    isEnabled = true,
                    trigger = RuleTrigger(type = TriggerType.SPECIFIC_TIME, hour = 20, minute = 0, dayFilter = DayFilter.EVERY_DAY, summary = ""),
                    conditions = emptyList(),
                    actions = listOf(RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SILENT.name, "Silent")),
                    exceptions = emptyList(),
                    priority = 5,
                    scheduleStartTime = "",
                    scheduleEndTime = "",
                    targetProfileId = "profile_study",
                    lastExecutionResult = "",
                    lastExecutedAtMillis = null,
                    executionCount = 0,
                    skippedCount = 0
                )
                return AssistantReply(text = text, proposal = null)
            }

            val proposal = buildOrMergeRoutineProposal(prompt, lower, isArabic, state.pendingRoutineDraft)
            val isComplete = proposal.routine.scheduleStartTime.isNotBlank() && proposal.routine.scheduleEndTime.isNotBlank() &&
                (lower.contains("am") || lower.contains("pm") || lower.contains("مساء") || lower.contains("صباح") || lower.contains("م") || lower.contains("ص") || existingDraftHasPeriod(state.pendingRoutineDraft, lower) || hasPresetDefaults)

            val text = if (isArabic) {
                if (isComplete || state.clarificationTurnCount >= 1) {
                    "يمكنني إنشاء روتين بالإعدادات التالية:\n\n" +
                        "• الاسم: ${proposal.routine.name}\n" +
                        "• المُشغّل: ${proposal.triggerSummary}\n" +
                        "• وقت الانتهاء: ${proposal.endTimeLabel}\n" +
                        "• الإجراء: ${proposal.actionSummary}\n" +
                        "• الاستثناء: ${proposal.exceptionSummary}\n\n" +
                        "هل تريد مراجعة هذا الروتين أو تأكيد إنشائه؟"
                } else {
                    "هل تعني من ${proposal.routine.scheduleStartTime} إلى ${proposal.routine.scheduleEndTime} صباحاً أم مساءً؟"
                }
            } else {
                if (isComplete || state.clarificationTurnCount >= 1) {
                    "I can create a routine with the following configuration:\n\n" +
                        "• Name: ${proposal.routine.name}\n" +
                        "• Trigger: ${proposal.triggerSummary}\n" +
                        "• End time: ${proposal.endTimeLabel}\n" +
                        "• Action: ${proposal.actionSummary}\n" +
                        "• Exception: ${proposal.exceptionSummary}\n\n" +
                        "Would you like to review or confirm creating this routine?"
                } else {
                    "Do you mean ${proposal.routine.scheduleStartTime} to ${proposal.routine.scheduleEndTime} AM or PM?"
                }
            }
            return AssistantReply(
                text = text,
                proposal = if (isComplete || state.clarificationTurnCount >= 1) proposal else null
            )
        }

        val fallback = if (isArabic) {
            "يمكنني مساعدتك في إنشاء روتين ذكي (مثلاً: \"اكتم الإشعارات يومياً من 8 مساءً إلى 10 مساءً مع السماح بجهات الطوارئ\")، أو بدء جلسة تركيز، أو تفسير حالة هاتفك المحاكاة."
        } else {
            "I can help you create smart routines (for example: \"Create a study routine\"), start focus sessions, or explain why your simulated phone status changed. Try one of the suggested commands below!"
        }
        return AssistantReply(text = fallback)
    }

    private fun containsArabic(text: String): Boolean {
        return text.any { ch -> ch in '\u0600'..'\u06FF' }
    }

    private fun isRoutineCreationIntent(lower: String): Boolean {
        if (RoutineTimeParser.parseTimeRange(lower) != null) return true
        val keywords = listOf(
            "silent", "mute", "quiet", "routine", "study", "studying",
            "sleep", "sleeping", "university", "work", "allow", "family",
            "emergency", "stop", "interruptions", "pm", "am", "schedule",
            "organize", "create", "to", "من", "إلى", "الى",
            "صامت", "اكتم", "كتم", "هدوء", "روتين", "دراسة", "الدراسة",
            "نوم", "النوم", "نومي", "تنظيم", "جدول", "جامعة", "الجامعة", "عمل",
            "عائلة", "عائلتي", "طوارئ", "اسمح", "سماح", "مساء", "صباح", "إشعارات"
        )
        return keywords.any { lower.contains(it) } ||
            lower.contains("create a routine") ||
            lower.contains("keep my phone") ||
            lower.contains("make my phone") ||
            lower.contains("اجعل هاتفي") ||
            lower.contains("أنشئ روتين")
    }

    private fun extractTimeHours(text: String): List<Int> = RoutineTimeParser.extractTimeHours(text)

    private fun parseTimeRange(text: String, isArabic: Boolean): Pair<Int, Int>? = RoutineTimeParser.parseTimeRange(text)

    private fun buildRoutineProposal(
        original: String,
        lower: String,
        isArabic: Boolean
    ): RoutineProposal {
        val isStudy = lower.contains("study") || lower.contains("studying") || lower.contains("library") ||
            lower.contains("دراسة") || lower.contains("مكتبة")
        val isUniversity = lower.contains("university") || lower.contains("campus") || lower.contains("class") ||
            lower.contains("جامعة")
        val isSleep = lower.contains("sleep") || lower.contains("sleeping") || lower.contains("11 pm") ||
            lower.contains("bed") || lower.contains("نوم") || lower.contains("ليل") || lower.contains("بليل")
        val isWork = lower.contains("work") || lower.contains("office") || lower.contains("عمل")

        val detailedRange = RoutineTimeParser.parseDetailedTimeRange(lower)
        val extractedPoints = RoutineTimeParser.extractTimePoints(lower)

        val startHour: Int
        val startMinute: Int
        val endHour: Int
        val endMinute: Int

        if (detailedRange != null) {
            startHour = detailedRange.startHour
            startMinute = detailedRange.startMinute
            endHour = detailedRange.endHour
            endMinute = detailedRange.endMinute
        } else if (extractedPoints.isNotEmpty()) {
            startHour = extractedPoints.first().hour24
            startMinute = extractedPoints.first().minute
            if (extractedPoints.size >= 2) {
                endHour = extractedPoints[1].hour24
                endMinute = extractedPoints[1].minute
            } else {
                endHour = if (isSleep) 7 else ((startHour + 2) % 24)
                endMinute = startMinute
            }
        } else {
            startHour = when {
                isSleep -> 23
                isStudy -> 18
                isUniversity -> 9
                isWork -> 9
                else -> 20
            }
            startMinute = 0
            endHour = when {
                isSleep -> 7
                else -> (startHour + 2) % 24
            }
            endMinute = 0
        }

        val dayFilter = when {
            lower.contains("every day") || lower.contains("daily") || lower.contains("يومياً") || lower.contains("كل يوم") ->
                DayFilter.EVERY_DAY
            lower.contains("weekend") || lower.contains("عطلة") -> DayFilter.WEEKENDS
            lower.contains("weekday") || isUniversity || isWork -> DayFilter.WEEKDAYS
            else -> DayFilter.EVERY_DAY
        }

        val soundMode = RoutineTimeParser.parseExplicitSoundMode(lower) ?: SoundMode.SILENT

        val profileId = when {
            isSleep -> "profile_sleep"
            isStudy || isUniversity -> "profile_study"
            isWork -> "profile_work"
            else -> "profile_study"
        }

        val profileLabel = if (isArabic) {
            when (profileId) {
                "profile_sleep" -> "وضع النوم"
                "profile_work" -> "وضع العمل"
                else -> "وضع الدراسة"
            }
        } else {
            when (profileId) {
                "profile_sleep" -> "Sleep Profile"
                "profile_work" -> "Work Profile"
                else -> "Study Profile"
            }
        }

        val routineName = if (isArabic) {
            when {
                isUniversity -> "أيام الجامعة المركزة"
                isSleep -> "حماية الهدوء الليلي"
                isStudy -> "هدوء وقت الدراسة"
                isWork -> "تركيز ساعات العمل"
                else -> "ساعات الهدوء"
            }
        } else {
            when {
                isUniversity -> "University Focus Days"
                isSleep -> "Night Quiet Protection"
                isStudy -> "Focused Study Silence"
                isWork -> "Workday Deep Focus"
                else -> "Quiet Hours"
            }
        }

        val startLabel = TimeUtils.formatTime12h(startHour, startMinute, isArabic)
        val endLabel = TimeUtils.formatTime12h(endHour, endMinute, isArabic)

        val trigger = when {
            isUniversity -> RuleTrigger(
                type = TriggerType.LOCATION_ENTERED,
                hour = startHour,
                minute = startMinute,
                dayFilter = dayFilter,
                location = SimulatedLocation.UNIVERSITY,
                summary = if (isArabic) "الموقع المحاكى: الجامعة" else "Simulated location: University (${dayFilter.label})"
            )
            lower.contains("while i'm studying") || lower.contains("while studying") || lower.contains("أثناء الدراسة") -> RuleTrigger(
                type = TriggerType.ACTIVITY_STARTED,
                hour = startHour,
                minute = startMinute,
                dayFilter = dayFilter,
                activity = SimulatedActivity.STUDYING,
                summary = if (isArabic) "النشاط المحاكى: الدراسة" else "Simulated activity: Studying"
            )
            else -> RuleTrigger(
                type = TriggerType.SPECIFIC_TIME,
                hour = startHour,
                minute = startMinute,
                dayFilter = dayFilter,
                summary = if (isArabic) {
                    "يومياً من $startLabel إلى $endLabel"
                } else {
                    "Daily from $startLabel to $endLabel"
                }
            )
        }

        val triggerSummary = trigger.summary
        val endTimeLabel = endLabel
        val soundLabel = if (isArabic) soundMode.arabicName else soundMode.displayName
        val actionSummary = if (isArabic) {
            when (soundMode) {
                SoundMode.SILENT -> "تفعيل الوضع الصامت"
                SoundMode.VIBRATE -> "تفعيل وضع الاهتزاز"
                SoundMode.SOUND -> "تفعيل وضع الصوت العادي"
            }
        } else {
            when (soundMode) {
                SoundMode.SILENT -> "Set phone to Silent"
                SoundMode.VIBRATE -> "Set phone to Vibrate"
                SoundMode.SOUND -> "Set phone to Sound"
            }
        }
        val exceptionSummary = if (isArabic) {
            "جهات الطوارئ مستثناة دائماً ولا يمكن حجبها"
        } else {
            "Emergency contacts always allowed"
        }

        val exceptions = listOf(RuleException(ExceptionType.EMERGENCY_CONTACTS, "Allow Emergency contacts"))

        val routine = SmartRoutine(
            id = "routine_ai_${UUID.randomUUID().toString().take(6)}",
            name = routineName,
            description = if (isArabic) "تم إنشاؤه من الطلب: \"$original\"" else "Created from prompt: \"$original\"",
            isEnabled = true,
            trigger = trigger,
            conditions = listOf(RuleCondition(ConditionType.AUTOMATION_ENABLED, "Enabled")),
            actions = listOf(
                RuleAction(ActionType.ACTIVATE_PROFILE, profileId, "Activate $profileLabel"),
                RuleAction(ActionType.SET_SOUND_MODE, soundMode.name, "Set simulated $soundLabel Mode")
            ),
            exceptions = exceptions,
            priority = 8,
            scheduleStartTime = startLabel,
            scheduleEndTime = endLabel,
            targetProfileId = profileId,
            lastExecutionResult = if (isArabic) "تم الإنشاء عبر المساعد — جاهز للاختبار" else "Created via Assistant — ready to test",
            lastExecutedAtMillis = null,
            executionCount = 0,
            skippedCount = 0
        )

        return RoutineProposal(
            routine = routine,
            triggerSummary = triggerSummary,
            endTimeLabel = endTimeLabel,
            actionSummary = actionSummary,
            exceptionSummary = exceptionSummary
        )
    }

    private fun buildOrMergeRoutineProposal(
        original: String,
        lower: String,
        isArabic: Boolean,
        existingDraft: SmartRoutine?
    ): RoutineProposal {
        val baseProposal = buildRoutineProposal(original, lower, isArabic)
        if (existingDraft == null) return baseProposal

        val explicitMode = RoutineTimeParser.parseExplicitSoundMode(lower)
        val draftSoundAction = existingDraft.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }
        val draftSoundMode = draftSoundAction?.let { action ->
            runCatching { SoundMode.valueOf(action.targetValue) }.getOrNull()
        } ?: SoundMode.SILENT

        // 1. Explicit sound mode in user message overrides draft mode
        val effectiveSoundMode = explicitMode ?: draftSoundMode

        val detailedRange = RoutineTimeParser.parseDetailedTimeRange(lower)
        val extractedPoints = RoutineTimeParser.extractTimePoints(lower)
        val isEvening = lower.contains("مساء") || lower.contains("pm") || lower.contains("evening") || lower.contains("بليل") || lower.contains("ليل") || lower.trim() == "م"
        val isMorning = lower.contains("صباح") || lower.contains("صبح") || lower.contains("am") || lower.contains("morning") || lower.trim() == "ص"

        val existingDraftPoints = RoutineTimeParser.extractTimePoints(existingDraft.scheduleStartTime + " " + existingDraft.scheduleEndTime)
        var startHour = existingDraftPoints.firstOrNull()?.hour24 ?: existingDraft.trigger.hour
        var startMinute = existingDraftPoints.firstOrNull()?.minute ?: existingDraft.trigger.minute
        var endHour = if (existingDraftPoints.size >= 2) existingDraftPoints[1].hour24 else ((startHour + 3) % 24)
        var endMinute = if (existingDraftPoints.size >= 2) existingDraftPoints[1].minute else startMinute

        if (detailedRange != null) {
            // 2. Explicit start and end times in user message completely override draft times
            startHour = detailedRange.startHour
            startMinute = detailedRange.startMinute
            endHour = detailedRange.endHour
            endMinute = detailedRange.endMinute
        } else if (extractedPoints.isNotEmpty()) {
            startHour = extractedPoints.first().hour24
            startMinute = extractedPoints.first().minute
            if (extractedPoints.size >= 2) {
                endHour = extractedPoints[1].hour24
                endMinute = extractedPoints[1].minute
            } else {
                endHour = (startHour + 3) % 24
                endMinute = startMinute
            }
        } else {
            if (isEvening && !isMorning) {
                if (startHour < 12) startHour += 12
                if (endHour < 12) endHour += 12
            } else if (isMorning && !isEvening) {
                if (startHour == 12) startHour = 0 else if (startHour > 12) startHour -= 12
                if (endHour == 12) endHour = 0 else if (endHour > 12) endHour -= 12
            }
        }

        val startLabel = TimeUtils.formatTime12h(startHour, startMinute, isArabic)
        val endLabel = TimeUtils.formatTime12h(endHour, endMinute, isArabic)

        val triggerSummary = if (isArabic) {
            "يومياً من $startLabel إلى $endLabel"
        } else {
            "Daily from $startLabel to $endLabel"
        }

        val soundLabel = if (isArabic) effectiveSoundMode.arabicName else effectiveSoundMode.displayName
        val actionSummary = if (isArabic) {
            when (effectiveSoundMode) {
                SoundMode.SILENT -> "تفعيل الوضع الصامت"
                SoundMode.VIBRATE -> "تفعيل وضع الاهتزاز"
                SoundMode.SOUND -> "تفعيل وضع الصوت العادي"
            }
        } else {
            when (effectiveSoundMode) {
                SoundMode.SILENT -> "Set phone to Silent"
                SoundMode.VIBRATE -> "Set phone to Vibrate"
                SoundMode.SOUND -> "Set phone to Sound"
            }
        }

        val profileId = if (detailedRange != null || explicitMode != null) {
            baseProposal.routine.targetProfileId ?: "profile_study"
        } else {
            existingDraft.targetProfileId ?: "profile_study"
        }

        val routineName = if (detailedRange != null) {
            baseProposal.routine.name
        } else {
            existingDraft.name
        }

        val updatedTrigger = baseProposal.routine.trigger.copy(
            hour = startHour,
            minute = startMinute,
            summary = triggerSummary
        )

        val updatedRoutine = baseProposal.routine.copy(
            name = routineName,
            trigger = updatedTrigger,
            actions = listOf(
                RuleAction(ActionType.ACTIVATE_PROFILE, profileId, "Activate profile"),
                RuleAction(ActionType.SET_SOUND_MODE, effectiveSoundMode.name, "Set simulated $soundLabel Mode")
            ),
            scheduleStartTime = startLabel,
            scheduleEndTime = endLabel,
            description = if (isArabic) "تم التحديث بناءً على إجابتك: \"$original\"" else "Updated from your response: \"$original\""
        )

        return baseProposal.copy(
            routine = updatedRoutine,
            triggerSummary = triggerSummary,
            endTimeLabel = endLabel,
            actionSummary = actionSummary
        )
    }

    private fun extractNumber(text: String): Int? {
        val match = Regex("(\\d+)\\s*(min|minute|minutes|m|د|دقيقة)?").find(text)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(5, 180)
    }

    private fun existingDraftHasPeriod(draft: SmartRoutine?, lower: String): Boolean {
        if (draft == null) return false
        return draft.scheduleStartTime.contains("AM", ignoreCase = true) ||
            draft.scheduleStartTime.contains("PM", ignoreCase = true) ||
            draft.scheduleStartTime.contains("صباح") ||
            draft.scheduleStartTime.contains("صبح") ||
            draft.scheduleStartTime.contains("ص") ||
            draft.scheduleStartTime.contains("مساء") ||
            draft.scheduleStartTime.contains("بليل") ||
            draft.scheduleStartTime.contains("ليل") ||
            draft.scheduleStartTime.contains("م") ||
            lower.contains("مساء") || lower.contains("صباح") || lower.contains("am") || lower.contains("pm") ||
            lower.contains("بليل") || lower.contains("ليل") || lower.contains("صبح") || lower.contains("الصبح")
    }
}

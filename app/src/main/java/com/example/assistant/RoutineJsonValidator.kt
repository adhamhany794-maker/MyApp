package com.example.assistant

import com.example.models.ActionType
import com.example.models.ConditionType
import com.example.models.DayFilter
import com.example.models.ExceptionType
import com.example.models.RoutineProposal
import com.example.models.RuleAction
import com.example.models.RuleCondition
import com.example.models.RuleException
import com.example.models.RuleTrigger
import com.example.models.SimulatedLocation
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TriggerType
import com.example.simulation.AutomationSimulationEngine
import com.example.utils.TimeUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface RoutineJsonValidationResult {
    data class Valid(
        val proposal: RoutineProposal,
        val summaryMessage: String
    ) : RoutineJsonValidationResult

    data class NeedsClarification(
        val question: String
    ) : RoutineJsonValidationResult
}

object RoutineConflictChecker {

    private val simEngine = AutomationSimulationEngine()

    fun findConflictWarning(
        proposed: SmartRoutine,
        existingRoutines: List<SmartRoutine>,
        isArabic: Boolean
    ): String? {
        val enabled = existingRoutines.filter { it.isEnabled && it.id != proposed.id }
        val conflicts = enabled.filter { existing -> routinesConflict(proposed, existing) }
        if (conflicts.isEmpty()) return null

        val first = conflicts.first()
        val names = conflicts.joinToString(", ") { "\"${it.name}\"" }
        return if (isArabic) {
            if (conflicts.size == 1) {
                "تنبيه تعارض: يتداخل هذا الروتين مع \"${first.name}\" (${first.scheduleStartTime} – ${first.scheduleEndTime})."
            } else {
                "تنبيه تعارض: يتداخل هذا الروتين مع $names."
            }
        } else {
            if (conflicts.size == 1) {
                "Conflict warning: This overlaps with \"${first.name}\" (${first.scheduleStartTime} – ${first.scheduleEndTime})."
            } else {
                "Conflict warning: This overlaps with $names."
            }
        }
    }

    private fun routinesConflict(a: SmartRoutine, b: SmartRoutine): Boolean {
        val aPlace = a.trigger.type == TriggerType.LOCATION_ENTERED || a.trigger.type == TriggerType.LOCATION_EXITED
        val bPlace = b.trigger.type == TriggerType.LOCATION_ENTERED || b.trigger.type == TriggerType.LOCATION_EXITED

        if (aPlace && bPlace) {
            return a.trigger.type == b.trigger.type &&
                a.trigger.location == b.trigger.location &&
                actionsDiffer(a, b)
        }

        val aTimed = a.trigger.type in listOf(
            TriggerType.SPECIFIC_TIME,
            TriggerType.DATE_AND_TIME,
            TriggerType.SPECIFIC_DAYS
        )
        val bTimed = b.trigger.type in listOf(
            TriggerType.SPECIFIC_TIME,
            TriggerType.DATE_AND_TIME,
            TriggerType.SPECIFIC_DAYS
        )
        if (!aTimed || !bTimed) return false

        val daysOverlap = simEngine.dayMatches(a.trigger.dayFilter, b.trigger.dayFilter) ||
            simEngine.dayMatches(b.trigger.dayFilter, a.trigger.dayFilter)
        if (!daysOverlap) return false

        if (a.trigger.type == TriggerType.SPECIFIC_DAYS || b.trigger.type == TriggerType.SPECIFIC_DAYS) {
            return true
        }

        val aRange = parseMinuteRange(a.scheduleStartTime, a.scheduleEndTime, a.trigger.hour, a.trigger.minute)
        val bRange = parseMinuteRange(b.scheduleStartTime, b.scheduleEndTime, b.trigger.hour, b.trigger.minute)
        return rangesOverlap(aRange.first, aRange.second, bRange.first, bRange.second)
    }

    private fun actionsDiffer(a: SmartRoutine, b: SmartRoutine): Boolean {
        val aSound = a.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }?.targetValue
        val bSound = b.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }?.targetValue
        val aProfile = a.targetProfileId
        val bProfile = b.targetProfileId
        return (aSound != null && bSound != null && aSound != bSound) ||
            (aProfile != null && bProfile != null && aProfile != bProfile)
    }

    private fun parseMinuteRange(
        startLabel: String,
        endLabel: String,
        fallbackStartHour: Int,
        fallbackStartMin: Int
    ): Pair<Int, Int> {
        val start = parseClockMinutes(startLabel) ?: (fallbackStartHour * 60 + fallbackStartMin)
        val end = parseClockMinutes(endLabel) ?: ((start + 120) % 1440)
        return start to end
    }

    internal fun parseClockMinutes(label: String): Int? {
        val trimmed = label.trim()
        val m24 = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(trimmed)
        if (m24 != null) {
            val h = m24.groupValues[1].toIntOrNull() ?: return null
            val min = m24.groupValues[2].toIntOrNull() ?: return null
            if (h in 0..23 && min in 0..59) return h * 60 + min
        }
        val m12 = Regex("^(\\d{1,2}):(\\d{2})\\s*(AM|PM|ص|م)$", RegexOption.IGNORE_CASE).matchEntire(trimmed)
        if (m12 != null) {
            val rawH = m12.groupValues[1].toIntOrNull() ?: return null
            val min = m12.groupValues[2].toIntOrNull() ?: return null
            val period = m12.groupValues[3].uppercase()
            if (rawH !in 1..12 || min !in 0..59) return null
            val h24 = when {
                (period == "PM" || period == "م") && rawH < 12 -> rawH + 12
                (period == "AM" || period == "ص") && rawH == 12 -> 0
                else -> rawH
            }
            return h24 * 60 + min
        }
        return null
    }

    private fun rangesOverlap(startA: Int, endA: Int, startB: Int, endB: Int): Boolean {
        val segsA = splitOvernight(startA, endA)
        val segsB = splitOvernight(startB, endB)
        return segsA.any { (sa, ea) ->
            segsB.any { (sb, eb) -> sa < eb && sb < ea }
        }
    }

    private fun splitOvernight(start: Int, end: Int): List<Pair<Int, Int>> {
        return when {
            start == end -> listOf(start to (start + 60).coerceAtMost(1440))
            end > start -> listOf(start to end)
            else -> listOf(start to 1440, 0 to end)
        }
    }
}

object RoutineJsonValidator {

    fun parseAndValidate(
        rawJson: String,
        originalPrompt: String,
        existingRoutines: List<SmartRoutine>,
        isArabic: Boolean
    ): RoutineJsonValidationResult {
        if (rawJson.isBlank()) {
            return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "ما الروتين الذي تريد إنشاءه؟ حدد الوقت أو المكان وماذا يفعل الهاتف."
                else "What routine would you like to create? Tell me when it runs and what your phone should do."
            )
        }

        val json = runCatching { JSONObject(rawJson) }.getOrNull()
            ?: return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "لم أتمكن من فهم تفاصيل الروتين بدقة. متى تريد تشغيله وما الإجراء المطلوب؟"
                else "Could you clarify when this routine should run and whether your phone should switch to Silent, Vibrate, Sound, or Focus?"
            )

        val status = json.optString("status", "ok").trim().lowercase()
        if (status == "needs_clarification") {
            val question = json.optString("followUpQuestion", "").trim().ifEmpty {
                if (isArabic) "هل يمكنك توضيح الوقت أو المكان والإجراء المطلوب لهذا الروتين؟"
                else "Could you clarify the trigger time or place and what action you want?"
            }
            return RoutineJsonValidationResult.NeedsClarification(question)
        }
        if (status != "ok") {
            return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "هل يمكنك توضيح تفاصيل الروتين المطلوب في جملة واحدة؟"
                else "Could you clarify the details of the routine you want to create?"
            )
        }

        // 1. Validate name
        val name = if (json.isNull("name")) "" else json.optString("name", "").trim()
        if (name.isEmpty()) {
            return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "ما الاسم الذي تريد إطلاقه على هذا الروتين؟"
                else "What name would you like to give this routine?"
            )
        }

        // 2. Validate trigger
        val triggerObj = json.optJSONObject("trigger")
            ?: return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "متى يجب أن يعمل هذا الروتين — في وقت محدد، أم في أيام معينة، أم عند دخول أو مغادرة مكان؟"
                else "When should this routine run — at a specific time, on certain days, or when entering or leaving a place?"
            )

        val triggerTypeRaw = if (triggerObj.isNull("type")) "" else triggerObj.optString("type", "").trim().lowercase()
        if (triggerTypeRaw !in setOf("time_range", "days", "place_enter", "place_exit")) {
            return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "متى يجب أن يبدأ هذا الروتين (وقت محدد، أيام الأسبوع، أم دخول/مغادرة مكان)؟"
                else "When should this routine start — a time range, specific days, or entering or leaving a place?"
            )
        }

        val parsedDays = parseDaysField(triggerObj)
            ?: return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "في أي أيام من الأسبوع تريد تشغيل هذا الروتين؟"
                else "Which days of the week should this routine apply to?"
            )

        val dayFilter = collapseDayFilters(parsedDays)

        var startHour = 20
        var startMinute = 0
        var endHour = 22
        var endMinute = 0
        var place = SimulatedLocation.LIBRARY

        when (triggerTypeRaw) {
            "time_range" -> {
                val startStr = if (triggerObj.isNull("startTime")) "" else triggerObj.optString("startTime", "").trim()
                val endStr = if (triggerObj.isNull("endTime")) "" else triggerObj.optString("endTime", "").trim()
                val startParsed = parseTime24(startStr)
                val endParsed = parseTime24(endStr)
                if (startParsed == null || endParsed == null || startParsed == endParsed) {
                    return RoutineJsonValidationResult.NeedsClarification(
                        if (isArabic) "ما وقت البداية والنهاية المحددان لهذا الروتين؟"
                        else "What start and end times should this routine use?"
                    )
                }
                startHour = startParsed.first
                startMinute = startParsed.second
                endHour = endParsed.first
                endMinute = endParsed.second
            }
            "days" -> {
                if (parsedDays.isEmpty()) {
                    return RoutineJsonValidationResult.NeedsClarification(
                        if (isArabic) "في أي أيام تريد تشغيل هذا الروتين؟"
                        else "Which days should trigger this routine?"
                    )
                }
                val startStr = if (triggerObj.isNull("startTime")) "" else triggerObj.optString("startTime", "").trim()
                val endStr = if (triggerObj.isNull("endTime")) "" else triggerObj.optString("endTime", "").trim()
                if (startStr.isNotEmpty()) {
                    val startParsed = parseTime24(startStr)
                        ?: return RoutineJsonValidationResult.NeedsClarification(
                            if (isArabic) "ما الوقت المحدد لهذا الروتين؟"
                            else "What time should this routine start?"
                        )
                    startHour = startParsed.first
                    startMinute = startParsed.second
                }
                if (endStr.isNotEmpty()) {
                    val endParsed = parseTime24(endStr)
                        ?: return RoutineJsonValidationResult.NeedsClarification(
                            if (isArabic) "ما وقت الانتهاء المحدد لهذا الروتين؟"
                            else "What time should this routine end?"
                        )
                    endHour = endParsed.first
                    endMinute = endParsed.second
                }
            }
            "place_enter", "place_exit" -> {
                val placeStr = if (triggerObj.isNull("place")) "" else triggerObj.optString("place", "").trim()
                place = parsePlace(placeStr)
                    ?: return RoutineJsonValidationResult.NeedsClarification(
                        if (isArabic) "ما المكان المطلوب (المنزل، الجامعة، العمل، أم المكتبة)؟"
                        else "Which place should trigger this routine (Home, University, Workplace, or Library)?"
                    )
            }
        }

        // 3. Validate conditions
        val conditions = parseConditionsField(json)
            ?: return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "هل هناك شرط إضافي غير صالح؟ يرجى توضيح شروط الروتين."
                else "Could you clarify the condition for when this routine should apply?"
            )

        // 4. Validate action (only sound / vibrate / silent / focus — no other phone actions allowed)
        val actionObj = json.optJSONObject("action")
            ?: return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "ماذا تريد أن يفعل الهاتف: صامت، اهتزاز، صوت عادي، أم وضع التركيز؟"
                else "What should your phone do when triggered — Silent, Vibrate, Sound, or Focus?"
            )

        val actionMode = if (actionObj.isNull("mode")) "" else actionObj.optString("mode", "").trim().lowercase()
        if (actionMode !in setOf("sound", "vibrate", "silent", "focus")) {
            return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "هل تريد وضع الصوت على صامت، أم اهتزاز، أم صوت عادي، أم وضع التركيز؟"
                else "Should this routine set your phone to Silent, Vibrate, Sound, or start Focus mode?"
            )
        }

        val focusMinutes: Int? = if (actionMode == "focus") {
            if (!actionObj.has("focusMinutes") || actionObj.isNull("focusMinutes")) {
                45
            } else {
                val mins = actionObj.optInt("focusMinutes", -1)
                if (mins !in 5..240) {
                    return RoutineJsonValidationResult.NeedsClarification(
                        if (isArabic) "كم دقيقة يجب أن تستمر جلسة التركيز (من 5 إلى 240 دقيقة)؟"
                        else "How many minutes should the focus session last (between 5 and 240)?"
                    )
                }
                mins
            }
        } else {
            null
        }

        // 5. Validate exceptions & ALWAYS include EMERGENCY_CONTACTS
        val extraExceptions = parseExceptionsField(json)
            ?: return RoutineJsonValidationResult.NeedsClarification(
                if (isArabic) "من هم الأشخاص المستثنون من هذا الروتين؟ تذكّر أن جهات الطوارئ مستثناة دائماً."
                else "Could you clarify which exceptions you want? Note that emergency contacts are always allowed."
            )

        val exceptions = buildList {
            add(RuleException(ExceptionType.EMERGENCY_CONTACTS, "Emergency contacts always allowed"))
            extraExceptions
                .filter { it != ExceptionType.EMERGENCY_CONTACTS }
                .distinct()
                .forEach { add(RuleException(it, it.displayName)) }
        }

        val soundMode = when (actionMode) {
            "sound" -> SoundMode.SOUND
            "vibrate" -> SoundMode.VIBRATE
            else -> SoundMode.SILENT
        }
        val targetProfileId = when {
            actionMode == "focus" -> "profile_study"
            place == SimulatedLocation.WORKPLACE -> "profile_work"
            actionMode == "silent" && (startHour >= 22 || startHour < 6) -> "profile_sleep"
            actionMode == "silent" -> "profile_study"
            actionMode == "vibrate" -> "profile_work"
            else -> "profile_personal"
        }

        val triggerType = when (triggerTypeRaw) {
            "time_range" -> TriggerType.SPECIFIC_TIME
            "days" -> TriggerType.SPECIFIC_DAYS
            "place_enter" -> TriggerType.LOCATION_ENTERED
            else -> TriggerType.LOCATION_EXITED
        }

        val startLabel = TimeUtils.formatTime12h(startHour, startMinute, isArabic)
        val endLabel = TimeUtils.formatTime12h(endHour, endMinute, isArabic)

        val triggerSummary = when (triggerType) {
            TriggerType.SPECIFIC_TIME -> {
                val dayText = localizedDayFilter(dayFilter, isArabic)
                if (isArabic) "$dayText • $startLabel" else "$dayText at $startLabel"
            }
            TriggerType.SPECIFIC_DAYS -> localizedDayFilter(dayFilter, isArabic)
            TriggerType.LOCATION_ENTERED -> if (isArabic) {
                "دخول المكان: ${localizedPlace(place, true)}"
            } else {
                "Enter ${localizedPlace(place, false)}"
            }
            TriggerType.LOCATION_EXITED -> if (isArabic) {
                "مغادرة المكان: ${localizedPlace(place, true)}"
            } else {
                "Exit ${localizedPlace(place, false)}"
            }
            else -> name
        }

        val trigger = RuleTrigger(
            type = triggerType,
            hour = startHour,
            minute = startMinute,
            dayFilter = dayFilter,
            location = place,
            summary = triggerSummary
        )

        val actions = buildList {
            add(RuleAction(ActionType.ACTIVATE_PROFILE, targetProfileId, "Activate profile"))
            add(RuleAction(ActionType.SET_SOUND_MODE, soundMode.name, "Set ${soundMode.displayName} mode"))
            if (actionMode == "focus" && focusMinutes != null) {
                add(RuleAction(ActionType.START_FOCUS_SESSION, focusMinutes.toString(), "Focus (${focusMinutes}m)"))
            }
        }

        val actionSummary = when (actionMode) {
            "focus" -> if (isArabic) {
                "بدء التركيز (${focusMinutes} د) مع كتم التنبيهات"
            } else {
                "Start Focus (${focusMinutes}m) & Silent mode"
            }
            "vibrate" -> if (isArabic) "تفعيل وضع الاهتزاز" else "Set phone to Vibrate"
            "sound" -> if (isArabic) "تفعيل الصوت العادي" else "Set phone to Sound"
            else -> if (isArabic) "تفعيل الوضع الصامت" else "Set phone to Silent"
        }

        val hasPriority = exceptions.any { it.type == ExceptionType.PRIORITY_CONTACTS }
        val exceptionSummary = if (isArabic) {
            if (hasPriority) "جهات الطوارئ تصلك دائماً + الأولوية العالية"
            else "جهات الطوارئ تصلك دائماً"
        } else {
            if (hasPriority) "Emergency contacts always allowed + Priority contacts"
            else "Emergency contacts always allowed"
        }

        val routine = SmartRoutine(
            id = "routine_ai_${UUID.randomUUID().toString().take(6)}",
            name = name,
            description = if (isArabic) "تم إعداده من طلبك: \"$originalPrompt\"" else "Prepared from your request: \"$originalPrompt\"",
            isEnabled = true,
            trigger = trigger,
            conditions = conditions.ifEmpty {
                listOf(RuleCondition(ConditionType.AUTOMATION_ENABLED, "Active"))
            },
            actions = actions,
            exceptions = exceptions,
            priority = 7,
            scheduleStartTime = startLabel,
            scheduleEndTime = endLabel,
            targetProfileId = targetProfileId,
            lastExecutionResult = if (isArabic) "بانتظار موافقتك" else "Awaiting your approval",
            lastExecutedAtMillis = null,
            executionCount = 0,
            skippedCount = 0
        )

        val conflictWarning = RoutineConflictChecker.findConflictWarning(
            proposed = routine,
            existingRoutines = existingRoutines,
            isArabic = isArabic
        )

        val summaryMessage = buildString {
            if (isArabic) {
                append("جهّزت لك هذا الروتين للمراجعة:\n\n")
                append("• الاسم: $name\n")
                append("• متى: $triggerSummary")
                if (triggerType == TriggerType.SPECIFIC_TIME) append(" – $endLabel")
                append("\n• الإجراء: $actionSummary\n")
                append("• الاستثناءات: $exceptionSummary")
                if (conflictWarning != null) {
                    append("\n\n$conflictWarning")
                }
            } else {
                append("I prepared this routine for your review:\n\n")
                append("• Name: $name\n")
                append("• When: $triggerSummary")
                if (triggerType == TriggerType.SPECIFIC_TIME) append(" – $endLabel")
                append("\n• Action: $actionSummary\n")
                append("• Exceptions: $exceptionSummary")
                if (conflictWarning != null) {
                    append("\n\n$conflictWarning")
                }
            }
        }

        val proposal = RoutineProposal(
            routine = routine,
            triggerSummary = triggerSummary,
            endTimeLabel = endLabel,
            actionSummary = actionSummary,
            exceptionSummary = exceptionSummary,
            conflictWarning = conflictWarning
        )
        return RoutineJsonValidationResult.Valid(proposal, summaryMessage)
    }

    internal fun parseTime24(value: String): Pair<Int, Int>? {
        val m = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(value.trim()) ?: return null
        val h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || min !in 0..59) return null
        return h to min
    }

    private fun parseDaysField(triggerObj: JSONObject): List<DayFilter>? {
        if (!triggerObj.has("days") || triggerObj.isNull("days")) return emptyList()
        val arr = triggerObj.optJSONArray("days") ?: return null
        val result = mutableListOf<DayFilter>()
        for (i in 0 until arr.length()) {
            val raw = arr.optString(i, "").trim().uppercase()
            val mapped = when (raw) {
                "EVERY_DAY", "EVERYDAY", "DAILY" -> DayFilter.EVERY_DAY
                "WEEKDAYS", "WEEKDAY" -> DayFilter.WEEKDAYS
                "WEEKENDS", "WEEKEND" -> DayFilter.WEEKENDS
                "MONDAY", "MON" -> DayFilter.MONDAY
                "TUESDAY", "TUE" -> DayFilter.TUESDAY
                "WEDNESDAY", "WED" -> DayFilter.WEDNESDAY
                "THURSDAY", "THU" -> DayFilter.THURSDAY
                "FRIDAY", "FRI" -> DayFilter.FRIDAY
                "SATURDAY", "SAT" -> DayFilter.SATURDAY
                "SUNDAY", "SUN" -> DayFilter.SUNDAY
                else -> return null
            }
            result.add(mapped)
        }
        return result
    }

    private fun collapseDayFilters(days: List<DayFilter>): DayFilter {
        if (days.isEmpty()) return DayFilter.EVERY_DAY
        if (DayFilter.EVERY_DAY in days || days.size >= 7) return DayFilter.EVERY_DAY
        if (days.size == 1) return days.first()
        val weekdays = setOf(
            DayFilter.MONDAY, DayFilter.TUESDAY, DayFilter.WEDNESDAY, DayFilter.THURSDAY, DayFilter.FRIDAY
        )
        val weekends = setOf(DayFilter.SATURDAY, DayFilter.SUNDAY)
        return when {
            days.all { it in weekdays } || DayFilter.WEEKDAYS in days -> DayFilter.WEEKDAYS
            days.all { it in weekends } || DayFilter.WEEKENDS in days -> DayFilter.WEEKENDS
            else -> DayFilter.EVERY_DAY
        }
    }

    private fun parsePlace(raw: String): SimulatedLocation? {
        return when (raw.trim().uppercase().replace(" ", "_")) {
            "HOME" -> SimulatedLocation.HOME
            "UNIVERSITY", "CAMPUS" -> SimulatedLocation.UNIVERSITY
            "LIBRARY" -> SimulatedLocation.LIBRARY
            "WORKPLACE", "WORK_OFFICE", "OFFICE", "WORK" -> SimulatedLocation.WORKPLACE
            "CUSTOM" -> SimulatedLocation.CUSTOM
            else -> null
        }
    }

    private fun parseConditionsField(json: JSONObject): List<RuleCondition>? {
        if (!json.has("conditions") || json.isNull("conditions")) return emptyList()
        val arr = json.optJSONArray("conditions") ?: return null
        val out = mutableListOf<RuleCondition>()
        for (i in 0 until arr.length()) {
            val raw = arr.optString(i, "").trim().uppercase()
            val mapped = runCatching { ConditionType.valueOf(raw) }.getOrNull() ?: return null
            out.add(RuleCondition(mapped, raw.lowercase().replace('_', ' ')))
        }
        return out
    }

    private fun parseExceptionsField(json: JSONObject): List<ExceptionType>? {
        if (!json.has("exceptions") || json.isNull("exceptions")) return emptyList()
        val arr: JSONArray = json.optJSONArray("exceptions") ?: return null
        val out = mutableListOf<ExceptionType>()
        for (i in 0 until arr.length()) {
            val raw = arr.optString(i, "").trim().uppercase()
            val mapped = when (raw) {
                "EMERGENCY_CONTACTS", "EMERGENCY", "FAMILY" -> ExceptionType.EMERGENCY_CONTACTS
                "PRIORITY_CONTACTS", "HIGH_PRIORITY", "REPEAT_CALLERS" -> ExceptionType.PRIORITY_CONTACTS
                "SPECIFIC_DAYS" -> ExceptionType.SPECIFIC_DAYS
                "TIME_RANGE" -> ExceptionType.TIME_RANGE
                "MANUALLY_DISABLED" -> ExceptionType.MANUALLY_DISABLED
                else -> return null
            }
            out.add(mapped)
        }
        return out
    }

    private fun localizedDayFilter(dayFilter: DayFilter, isArabic: Boolean): String {
        if (!isArabic) return dayFilter.label
        return when (dayFilter) {
            DayFilter.EVERY_DAY -> "كل يوم"
            DayFilter.WEEKDAYS -> "أيام الأسبوع"
            DayFilter.WEEKENDS -> "عطلة نهاية الأسبوع"
            DayFilter.MONDAY -> "الإثنين"
            DayFilter.TUESDAY -> "الثلاثاء"
            DayFilter.WEDNESDAY -> "الأربعاء"
            DayFilter.THURSDAY -> "الخميس"
            DayFilter.FRIDAY -> "الجمعة"
            DayFilter.SATURDAY -> "السبت"
            DayFilter.SUNDAY -> "الأحد"
        }
    }

    private fun localizedPlace(place: SimulatedLocation, isArabic: Boolean): String {
        if (!isArabic) return place.displayName
        return when (place) {
            SimulatedLocation.HOME -> "المنزل"
            SimulatedLocation.UNIVERSITY -> "الجامعة"
            SimulatedLocation.WORKPLACE -> "العمل"
            SimulatedLocation.LIBRARY -> "المكتبة"
            SimulatedLocation.CUSTOM -> "مكان مخصص"
        }
    }
}

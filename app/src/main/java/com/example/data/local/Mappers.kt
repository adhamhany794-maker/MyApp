package com.example.data.local

import com.example.models.ActionType
import com.example.models.ActivityEvent
import com.example.models.ConditionType
import com.example.models.ContactPriority
import com.example.models.DayFilter
import com.example.models.EventCategory
import com.example.models.ExceptionType
import com.example.models.FocusSessionRecord
import com.example.models.InterruptionType
import com.example.models.NotificationCategory
import com.example.models.NotificationStat
import com.example.models.PriorityContact
import com.example.models.ProfileIconKey
import com.example.models.RepeatCallerRule
import com.example.models.RoutineLogEntry
import com.example.models.RoutineLogEventType
import com.example.models.RuleAction
import com.example.models.RuleCondition
import com.example.models.RuleException
import com.example.models.RuleTrigger
import com.example.models.SimulatedActivity
import com.example.models.SimulatedLocation
import com.example.models.SmartProfile
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TimePeriodFilter
import com.example.models.TimeWindow
import com.example.models.TriggerType
import org.json.JSONArray
import org.json.JSONObject

object EntityMappers {

    fun ProfileEntity.toDomain(): SmartProfile {
        val categories = parseStringList(allowedCategoriesJson)
            .mapNotNull { runCatching { NotificationCategory.valueOf(it) }.getOrNull() }
        return SmartProfile(
            id = id,
            name = name,
            description = description,
            iconKey = runCatching { ProfileIconKey.valueOf(iconKey) }.getOrDefault(ProfileIconKey.CUSTOM),
            accentColorHex = accentColorHex,
            soundMode = runCatching { SoundMode.valueOf(soundMode) }.getOrDefault(SoundMode.SOUND),
            allowedCategories = categories,
            priorityContactBehavior = priorityContactBehavior,
            allowHighPriorityContacts = allowHighPriorityContacts,
            defaultFocusMinutes = defaultFocusMinutes,
            isDefault = isDefault,
            isProtected = isProtected
        )
    }

    fun SmartProfile.toEntity(): ProfileEntity {
        val catArray = JSONArray().apply {
            allowedCategories.forEach { put(it.name) }
        }
        return ProfileEntity(
            id = id,
            name = name,
            description = description,
            iconKey = iconKey.name,
            accentColorHex = accentColorHex,
            soundMode = soundMode.name,
            allowedCategoriesJson = catArray.toString(),
            priorityContactBehavior = priorityContactBehavior,
            allowHighPriorityContacts = allowHighPriorityContacts,
            defaultFocusMinutes = defaultFocusMinutes,
            isDefault = isDefault,
            isProtected = isProtected
        )
    }

    fun RoutineEntity.toDomain(): SmartRoutine {
        val extras = parseTriggerExtras(triggerJson)
        return SmartRoutine(
            id = id,
            name = name,
            description = description,
            isEnabled = isEnabled,
            trigger = parseTrigger(triggerJson),
            conditions = parseConditions(conditionsJson),
            actions = parseActions(actionsJson),
            exceptions = parseExceptions(exceptionsJson),
            priority = priority,
            scheduleStartTime = scheduleStartTime,
            scheduleEndTime = scheduleEndTime,
            targetProfileId = targetProfileId,
            lastExecutionResult = lastExecutionResult,
            lastExecutedAtMillis = lastExecutedAtMillis,
            executionCount = executionCount,
            skippedCount = skippedCount,
            enableDoNotDisturb = extras.enableDoNotDisturb,
            isOneTime = extras.isOneTime,
            skipDates = extras.skipDates,
            extraTimeWindows = extras.extraTimeWindows,
            customDays = extras.customDays
        )
    }

    fun SmartRoutine.toEntity(): RoutineEntity {
        val trigObj = JSONObject().apply {
            put("type", trigger.type.name)
            put("hour", trigger.hour)
            put("minute", trigger.minute)
            put("dayFilter", trigger.dayFilter.name)
            put("dateLabel", trigger.dateLabel)
            put("location", trigger.location.name)
            put("activity", trigger.activity.name)
            put("summary", trigger.summary)
            put("enableDoNotDisturb", enableDoNotDisturb)
            put("isOneTime", isOneTime)
            put("skipDates", JSONArray().apply { skipDates.forEach { put(it) } })
            put("extraTimeWindows", JSONArray().apply {
                extraTimeWindows.forEach { w ->
                    put(JSONObject().apply {
                        put("startHour", w.startHour)
                        put("startMinute", w.startMinute)
                        put("endHour", w.endHour)
                        put("endMinute", w.endMinute)
                    })
                }
            })
            put("customDays", JSONArray().apply { customDays.forEach { put(it.name) } })
        }
        val condArray = JSONArray().apply {
            conditions.forEach { c ->
                put(JSONObject().apply {
                    put("type", c.type.name)
                    put("value", c.value)
                })
            }
        }
        val actArray = JSONArray().apply {
            actions.forEach { a ->
                put(JSONObject().apply {
                    put("type", a.type.name)
                    put("targetValue", a.targetValue)
                    put("label", a.label)
                })
            }
        }
        val excArray = JSONArray().apply {
            exceptions.forEach { e ->
                put(JSONObject().apply {
                    put("type", e.type.name)
                    put("value", e.value)
                })
            }
        }
        return RoutineEntity(
            id = id,
            name = name,
            description = description,
            isEnabled = isEnabled,
            triggerJson = trigObj.toString(),
            conditionsJson = condArray.toString(),
            actionsJson = actArray.toString(),
            exceptionsJson = excArray.toString(),
            priority = priority,
            scheduleStartTime = scheduleStartTime,
            scheduleEndTime = scheduleEndTime,
            targetProfileId = targetProfileId,
            lastExecutionResult = lastExecutionResult,
            lastExecutedAtMillis = lastExecutedAtMillis,
            executionCount = executionCount,
            skippedCount = skippedCount
        )
    }

    fun ContactEntity.toDomain(): PriorityContact {
        val interruptions = parseStringList(allowedInterruptionsJson)
            .mapNotNull { runCatching { InterruptionType.valueOf(it) }.getOrNull() }
        return PriorityContact(
            id = id,
            name = name,
            initials = initials,
            relationship = relationship,
            priorityLevel = runCatching { ContactPriority.valueOf(priorityLevel) }
                .getOrDefault(ContactPriority.EMERGENCY),
            allowedInterruptions = interruptions,
            isEnabled = isEnabled,
            allowRepeatCallers = allowRepeatCallers,
            phoneNumber = phoneNumber,
            systemContactId = systemContactId
        )
    }

    fun PriorityContact.toEntity(): ContactEntity {
        val arr = JSONArray().apply {
            allowedInterruptions.forEach { put(it.name) }
        }
        return ContactEntity(
            id = id,
            name = name,
            initials = initials,
            relationship = relationship,
            priorityLevel = priorityLevel.name,
            allowedInterruptionsJson = arr.toString(),
            isEnabled = isEnabled,
            allowRepeatCallers = allowRepeatCallers,
            phoneNumber = phoneNumber,
            systemContactId = systemContactId
        )
    }

    fun ActivityEventEntity.toDomain(): ActivityEvent {
        return ActivityEvent(
            id = id,
            title = title,
            timestampMillis = timestampMillis,
            category = runCatching { EventCategory.valueOf(category) }.getOrDefault(EventCategory.SYSTEM),
            triggerSource = triggerSource,
            result = result,
            explanation = explanation,
            relatedRoutineName = relatedRoutineName,
            relatedProfileName = relatedProfileName
        )
    }

    fun ActivityEvent.toEntity(): ActivityEventEntity {
        return ActivityEventEntity(
            id = id,
            title = title,
            timestampMillis = timestampMillis,
            category = category.name,
            triggerSource = triggerSource,
            result = result,
            explanation = explanation,
            relatedRoutineName = relatedRoutineName,
            relatedProfileName = relatedProfileName
        )
    }

    fun FocusSessionEntity.toDomain(): FocusSessionRecord {
        return FocusSessionRecord(
            id = id,
            profileId = profileId,
            profileName = profileName,
            plannedMinutes = plannedMinutes,
            completedMinutes = completedMinutes,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
            simulatedInterruptionsBlocked = simulatedInterruptionsBlocked,
            status = status
        )
    }

    fun FocusSessionRecord.toEntity(): FocusSessionEntity {
        return FocusSessionEntity(
            id = id,
            profileId = profileId,
            profileName = profileName,
            plannedMinutes = plannedMinutes,
            completedMinutes = completedMinutes,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
            simulatedInterruptionsBlocked = simulatedInterruptionsBlocked,
            status = status
        )
    }

    fun NotificationStatEntity.toDomain(): NotificationStat {
        return NotificationStat(
            id = id,
            appName = appName,
            category = runCatching { NotificationCategory.valueOf(category) }
                .getOrDefault(NotificationCategory.OTHER),
            totalCount = totalCount,
            importantCount = importantCount,
            blockedCount = blockedCount,
            peakHourRange = peakHourRange,
            timePeriod = runCatching { TimePeriodFilter.valueOf(timePeriod) }
                .getOrDefault(TimePeriodFilter.TODAY)
        )
    }

    fun NotificationStat.toEntity(): NotificationStatEntity {
        return NotificationStatEntity(
            id = id,
            appName = appName,
            category = category.name,
            totalCount = totalCount,
            importantCount = importantCount,
            blockedCount = blockedCount,
            peakHourRange = peakHourRange,
            timePeriod = timePeriod.name
        )
    }

    fun RoutineLogEntity.toDomain(): RoutineLogEntry {
        return RoutineLogEntry(
            id = id,
            routineId = routineId,
            routineName = routineName,
            eventType = runCatching { RoutineLogEventType.valueOf(eventType) }
                .getOrDefault(RoutineLogEventType.STARTED),
            timestampMillis = timestampMillis,
            ringerMode = ringerMode?.let { runCatching { SoundMode.valueOf(it) }.getOrNull() },
            dndEnabled = dndEnabled,
            interruptionFilter = interruptionFilter,
            note = note
        )
    }

    fun RoutineLogEntry.toEntity(): RoutineLogEntity {
        return RoutineLogEntity(
            id = id,
            routineId = routineId,
            routineName = routineName,
            eventType = eventType.name,
            timestampMillis = timestampMillis,
            ringerMode = ringerMode?.name,
            dndEnabled = dndEnabled,
            interruptionFilter = interruptionFilter,
            note = note
        )
    }

    fun RepeatCallerEntity.toDomain(): RepeatCallerRule {
        return RepeatCallerRule(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            requiredCalls = requiredCalls.coerceIn(2, 20),
            windowMinutes = windowMinutes.coerceIn(1, 240),
            isEnabled = isEnabled,
            systemContactId = systemContactId
        )
    }

    fun RepeatCallerRule.toEntity(): RepeatCallerEntity {
        return RepeatCallerEntity(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            requiredCalls = requiredCalls.coerceIn(2, 20),
            windowMinutes = windowMinutes.coerceIn(1, 240),
            isEnabled = isEnabled,
            systemContactId = systemContactId
        )
    }

    private fun parseStringList(json: String): List<String> {
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    private fun parseTrigger(json: String): RuleTrigger {
        return runCatching {
            val obj = JSONObject(json)
            RuleTrigger(
                type = enumValueOf(obj.optString("type", TriggerType.SPECIFIC_TIME.name)),
                hour = obj.optInt("hour", 18),
                minute = obj.optInt("minute", 0),
                dayFilter = runCatching {
                    DayFilter.valueOf(obj.optString("dayFilter", DayFilter.WEEKDAYS.name))
                }.getOrDefault(DayFilter.WEEKDAYS),
                dateLabel = obj.optString("dateLabel", ""),
                location = runCatching {
                    SimulatedLocation.valueOf(obj.optString("location", SimulatedLocation.HOME.name))
                }.getOrDefault(SimulatedLocation.HOME),
                activity = runCatching {
                    SimulatedActivity.valueOf(obj.optString("activity", SimulatedActivity.STUDYING.name))
                }.getOrDefault(SimulatedActivity.STUDYING),
                summary = obj.optString("summary", "")
            )
        }.getOrDefault(RuleTrigger(type = TriggerType.MANUAL, summary = "Manual activation"))
    }

    private fun parseConditions(json: String): List<RuleCondition> {
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                RuleCondition(
                    type = enumValueOf(obj.getString("type")),
                    value = obj.getString("value")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun parseActions(json: String): List<RuleAction> {
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                RuleAction(
                    type = enumValueOf(obj.getString("type")),
                    targetValue = obj.getString("targetValue"),
                    label = obj.getString("label")
                )
            }
        }.getOrDefault(emptyList())
    }

    private data class TriggerExtras(
        val enableDoNotDisturb: Boolean = false,
        val isOneTime: Boolean = false,
        val skipDates: List<String> = emptyList(),
        val extraTimeWindows: List<TimeWindow> = emptyList(),
        val customDays: Set<DayFilter> = emptySet()
    )

    private fun parseTriggerExtras(json: String): TriggerExtras {
        return runCatching {
            val obj = JSONObject(json)
            val skipArr = obj.optJSONArray("skipDates")
            val skipDates = if (skipArr != null) {
                List(skipArr.length()) { idx -> skipArr.optString(idx, "") }.filter { it.isNotBlank() }
            } else {
                emptyList()
            }
            val winArr = obj.optJSONArray("extraTimeWindows")
            val windows = if (winArr != null) {
                List(winArr.length()) { idx ->
                    val w = winArr.getJSONObject(idx)
                    TimeWindow(
                        startHour = w.optInt("startHour", 18),
                        startMinute = w.optInt("startMinute", 0),
                        endHour = w.optInt("endHour", 19),
                        endMinute = w.optInt("endMinute", 0)
                    )
                }
            } else {
                emptyList()
            }
            val daysArr = obj.optJSONArray("customDays")
            val days = if (daysArr != null) {
                List(daysArr.length()) { idx ->
                    runCatching { DayFilter.valueOf(daysArr.getString(idx)) }.getOrNull()
                }.filterNotNull().toSet()
            } else {
                emptySet()
            }
            TriggerExtras(
                enableDoNotDisturb = obj.optBoolean("enableDoNotDisturb", false),
                isOneTime = obj.optBoolean("isOneTime", false),
                skipDates = skipDates,
                extraTimeWindows = windows,
                customDays = days
            )
        }.getOrDefault(TriggerExtras())
    }

    private fun parseExceptions(json: String): List<RuleException> {
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                RuleException(
                    type = enumValueOf(obj.getString("type")),
                    value = obj.getString("value")
                )
            }
        }.getOrDefault(emptyList())
    }
}

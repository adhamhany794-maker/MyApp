package com.example.data

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
import com.example.models.TriggerType

object DemoSeedData {

    fun defaultProfiles(): List<SmartProfile> = listOf(
        SmartProfile(
            id = "profile_study",
            name = "Study",
            description = "Deep academic focus with social alerts silenced and emergency exceptions intact.",
            iconKey = ProfileIconKey.STUDY,
            accentColorHex = 0xFF0284C7,
            soundMode = SoundMode.SILENT,
            allowedCategories = listOf(NotificationCategory.EDUCATION, NotificationCategory.SYSTEM),
            priorityContactBehavior = "Emergency contacts allowed; anyone else is normal",
            allowHighPriorityContacts = false,
            defaultFocusMinutes = 45,
            isDefault = true,
            isProtected = true
        ),
        SmartProfile(
            id = "profile_work",
            name = "Work",
            description = "Balanced productivity keeping work channels reachable while muting social noise.",
            iconKey = ProfileIconKey.WORK,
            accentColorHex = 0xFF0F294A,
            soundMode = SoundMode.VIBRATE,
            allowedCategories = listOf(NotificationCategory.WORK, NotificationCategory.COMMUNICATION),
            priorityContactBehavior = "Emergency contacts allowed; repeat callers configurable",
            allowHighPriorityContacts = false,
            defaultFocusMinutes = 60,
            isDefault = true,
            isProtected = true
        ),
        SmartProfile(
            id = "profile_sleep",
            name = "Sleep",
            description = "Overnight protection blocking all non-urgent alerts while keeping emergency callers reachable.",
            iconKey = ProfileIconKey.SLEEP,
            accentColorHex = 0xFF4F46E5,
            soundMode = SoundMode.SILENT,
            allowedCategories = emptyList(),
            priorityContactBehavior = "Only Emergency contacts & repeat callers break through",
            allowHighPriorityContacts = false,
            defaultFocusMinutes = 480,
            isDefault = true,
            isProtected = true
        ),
        SmartProfile(
            id = "profile_personal",
            name = "Personal",
            description = "Relaxed everyday profile with normal simulated sound and priority communication.",
            iconKey = ProfileIconKey.PERSONAL,
            accentColorHex = 0xFF0D7C7B,
            soundMode = SoundMode.SOUND,
            allowedCategories = listOf(
                NotificationCategory.COMMUNICATION,
                NotificationCategory.EDUCATION,
                NotificationCategory.WORK,
                NotificationCategory.ENTERTAINMENT
            ),
            priorityContactBehavior = "All contacts permitted normally",
            allowHighPriorityContacts = false,
            defaultFocusMinutes = 25,
            isDefault = true,
            isProtected = true
        ),
        SmartProfile(
            id = "profile_custom",
            name = "Custom Focus",
            description = "Flexible template profile tailored for library research or commuting.",
            iconKey = ProfileIconKey.CUSTOM,
            accentColorHex = 0xFFD97706,
            soundMode = SoundMode.VIBRATE,
            allowedCategories = listOf(NotificationCategory.EDUCATION, NotificationCategory.COMMUNICATION),
            priorityContactBehavior = "Emergency contacts allowed",
            allowHighPriorityContacts = false,
            defaultFocusMinutes = 30,
            isDefault = true,
            isProtected = false
        )
    )

    fun defaultRoutines(): List<SmartRoutine> {
        val now = System.currentTimeMillis()
        return listOf(
            SmartRoutine(
                id = "routine_study_time",
                name = "Study Time",
                description = "Switches phone to Study Profile, silences interruptions, and starts focus protection on weekday evenings.",
                isEnabled = true,
                trigger = RuleTrigger(
                    type = TriggerType.SPECIFIC_TIME,
                    hour = 18,
                    minute = 0,
                    dayFilter = DayFilter.WEEKDAYS,
                    summary = "Weekdays at 6:00 PM"
                ),
                conditions = listOf(
                    RuleCondition(ConditionType.DAY_OF_WEEK, "Weekdays"),
                    RuleCondition(ConditionType.AUTOMATION_ENABLED, "Enabled")
                ),
                actions = listOf(
                    RuleAction(ActionType.ACTIVATE_PROFILE, "profile_study", "Activate Study Profile"),
                    RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SILENT.name, "Set simulated sound mode to Silent"),
                    RuleAction(ActionType.START_FOCUS_SESSION, "45", "Start 45m focus session")
                ),
                exceptions = listOf(
                    RuleException(ExceptionType.PRIORITY_CONTACTS, "Allow Emergency & High Priority callers")
                ),
                priority = 9,
                scheduleStartTime = "6:00 PM",
                scheduleEndTime = "8:30 PM",
                targetProfileId = "profile_study",
                lastExecutionResult = "Executed smoothly — Study Profile & 45m Focus activated",
                lastExecutedAtMillis = now - 3_600_000L * 18,
                executionCount = 12,
                skippedCount = 1
            ),
            SmartRoutine(
                id = "routine_sleep_schedule",
                name = "Sleep Schedule",
                description = "Protects overnight rest every day at 11:00 PM while keeping emergency family callers reachable.",
                isEnabled = true,
                trigger = RuleTrigger(
                    type = TriggerType.SPECIFIC_TIME,
                    hour = 23,
                    minute = 0,
                    dayFilter = DayFilter.EVERY_DAY,
                    summary = "Every day at 11:00 PM"
                ),
                conditions = listOf(
                    RuleCondition(ConditionType.AUTOMATION_ENABLED, "Enabled")
                ),
                actions = listOf(
                    RuleAction(ActionType.ACTIVATE_PROFILE, "profile_sleep", "Activate Sleep Profile"),
                    RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SILENT.name, "Set simulated sound mode to Silent"),
                    RuleAction(ActionType.SHOW_NOTIFICATION, "Sleep protection enabled", "Show sleep protection notice")
                ),
                exceptions = listOf(
                    RuleException(ExceptionType.EMERGENCY_CONTACTS, "Allow Emergency contacts")
                ),
                priority = 10,
                scheduleStartTime = "11:00 PM",
                scheduleEndTime = "7:00 AM",
                targetProfileId = "profile_sleep",
                lastExecutionResult = "Executed — Sleep Profile active, 4 overnight alerts filtered",
                lastExecutedAtMillis = now - 3_600_000L * 12,
                executionCount = 24,
                skippedCount = 0
            ),
            SmartRoutine(
                id = "routine_work_hours",
                name = "Work Hours",
                description = "Adapts phone to Work Profile at 9:00 AM on weekdays and reduces non-work interruptions.",
                isEnabled = true,
                trigger = RuleTrigger(
                    type = TriggerType.SPECIFIC_TIME,
                    hour = 9,
                    minute = 0,
                    dayFilter = DayFilter.WEEKDAYS,
                    summary = "Weekdays at 9:00 AM"
                ),
                conditions = listOf(
                    RuleCondition(ConditionType.DAY_OF_WEEK, "Weekdays")
                ),
                actions = listOf(
                    RuleAction(ActionType.ACTIVATE_PROFILE, "profile_work", "Activate Work Profile"),
                    RuleAction(ActionType.SET_SOUND_MODE, SoundMode.VIBRATE.name, "Reduce interruptions (Vibrate)")
                ),
                exceptions = listOf(
                    RuleException(ExceptionType.PRIORITY_CONTACTS, "Allow Work & High Priority contacts")
                ),
                priority = 7,
                scheduleStartTime = "9:00 AM",
                scheduleEndTime = "5:00 PM",
                targetProfileId = "profile_work",
                lastExecutionResult = "Executed at 9:00 AM — Work Profile enabled",
                lastExecutedAtMillis = now - 3_600_000L * 4,
                executionCount = 19,
                skippedCount = 2
            ),
            SmartRoutine(
                id = "routine_personal_time",
                name = "Personal Time",
                description = "Restores Personal Profile and normal sound mode when the workday ends at 5:00 PM.",
                isEnabled = true,
                trigger = RuleTrigger(
                    type = TriggerType.SPECIFIC_TIME,
                    hour = 17,
                    minute = 0,
                    dayFilter = DayFilter.WEEKDAYS,
                    summary = "Weekdays at 5:00 PM"
                ),
                conditions = listOf(
                    RuleCondition(ConditionType.DAY_OF_WEEK, "Weekdays")
                ),
                actions = listOf(
                    RuleAction(ActionType.ACTIVATE_PROFILE, "profile_personal", "Activate Personal Profile"),
                    RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SOUND.name, "Restore normal simulated sound mode")
                ),
                exceptions = emptyList(),
                priority = 6,
                scheduleStartTime = "5:00 PM",
                scheduleEndTime = "6:00 PM",
                targetProfileId = "profile_personal",
                lastExecutionResult = "Pending next 5:00 PM trigger",
                lastExecutedAtMillis = now - 3_600_000L * 20,
                executionCount = 15,
                skippedCount = 0
            ),
            SmartRoutine(
                id = "routine_evening_unwind",
                name = "Evening Unwind (Conflict Demo)",
                description = "Overlaps at 6:00 PM on weekdays to demonstrate priority & conflict resolution against Study Time.",
                isEnabled = true,
                trigger = RuleTrigger(
                    type = TriggerType.SPECIFIC_TIME,
                    hour = 18,
                    minute = 0,
                    dayFilter = DayFilter.WEEKDAYS,
                    summary = "Weekdays at 6:00 PM"
                ),
                conditions = listOf(
                    RuleCondition(ConditionType.DAY_OF_WEEK, "Weekdays")
                ),
                actions = listOf(
                    RuleAction(ActionType.ACTIVATE_PROFILE, "profile_personal", "Activate Personal Profile"),
                    RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SOUND.name, "Restore Sound mode")
                ),
                exceptions = emptyList(),
                priority = 4,
                scheduleStartTime = "6:00 PM",
                scheduleEndTime = "7:30 PM",
                targetProfileId = "profile_personal",
                lastExecutionResult = "Skipped due to conflict with higher-priority Study Time",
                lastExecutedAtMillis = now - 3_600_000L * 18,
                executionCount = 3,
                skippedCount = 5
            )
        )
    }

    fun defaultContacts(): List<PriorityContact> = listOf(
        PriorityContact(
            id = "contact_mom",
            name = "Mom (Nadia)",
            initials = "MN",
            relationship = "Family",
            priorityLevel = ContactPriority.EMERGENCY,
            allowedInterruptions = listOf(InterruptionType.CALL, InterruptionType.MESSAGE),
            isEnabled = true
        ),
        PriorityContact(
            id = "contact_kareem",
            name = "Dr. Kareem Mansour",
            initials = "KM",
            relationship = "Medical / Advisor",
            priorityLevel = ContactPriority.EMERGENCY,
            allowedInterruptions = listOf(InterruptionType.CALL),
            isEnabled = true
        )
    )

    fun defaultNotificationStats(): List<NotificationStat> = listOf(
        // TODAY
        NotificationStat("ns_t_1", "Campus LMS", NotificationCategory.EDUCATION, 14, 11, 2, "2:00 PM – 4:00 PM", TimePeriodFilter.TODAY),
        NotificationStat("ns_t_2", "Socialgram", NotificationCategory.SOCIAL_MEDIA, 26, 2, 22, "7:00 PM – 9:00 PM", TimePeriodFilter.TODAY),
        NotificationStat("ns_t_3", "TeamChat", NotificationCategory.WORK, 19, 12, 5, "10:00 AM – 12:00 PM", TimePeriodFilter.TODAY),
        NotificationStat("ns_t_4", "Messages", NotificationCategory.COMMUNICATION, 16, 10, 4, "6:00 PM – 8:00 PM", TimePeriodFilter.TODAY),
        NotificationStat("ns_t_5", "StreamBox", NotificationCategory.ENTERTAINMENT, 11, 0, 11, "8:00 PM – 10:00 PM", TimePeriodFilter.TODAY),
        NotificationStat("ns_t_6", "Device Health", NotificationCategory.SYSTEM, 5, 3, 1, "9:00 AM – 10:00 AM", TimePeriodFilter.TODAY),
        NotificationStat("ns_t_7", "QuickNotes", NotificationCategory.OTHER, 4, 2, 1, "1:00 PM – 2:00 PM", TimePeriodFilter.TODAY),

        // LAST 7 DAYS
        NotificationStat("ns_7_1", "Socialgram", NotificationCategory.SOCIAL_MEDIA, 174, 11, 148, "7:00 PM – 9:00 PM", TimePeriodFilter.LAST_7_DAYS),
        NotificationStat("ns_7_2", "TeamChat", NotificationCategory.WORK, 132, 86, 30, "9:00 AM – 12:00 PM", TimePeriodFilter.LAST_7_DAYS),
        NotificationStat("ns_7_3", "Messages", NotificationCategory.COMMUNICATION, 108, 69, 25, "6:00 PM – 9:00 PM", TimePeriodFilter.LAST_7_DAYS),
        NotificationStat("ns_7_4", "Campus LMS", NotificationCategory.EDUCATION, 84, 67, 9, "1:00 PM – 5:00 PM", TimePeriodFilter.LAST_7_DAYS),
        NotificationStat("ns_7_5", "StreamBox", NotificationCategory.ENTERTAINMENT, 76, 4, 68, "8:00 PM – 11:00 PM", TimePeriodFilter.LAST_7_DAYS),
        NotificationStat("ns_7_6", "Device Health", NotificationCategory.SYSTEM, 28, 18, 4, "8:00 AM – 10:00 AM", TimePeriodFilter.LAST_7_DAYS),
        NotificationStat("ns_7_7", "QuickNotes", NotificationCategory.OTHER, 22, 9, 8, "3:00 PM – 5:00 PM", TimePeriodFilter.LAST_7_DAYS),

        // LAST 30 DAYS
        NotificationStat("ns_30_1", "Socialgram", NotificationCategory.SOCIAL_MEDIA, 710, 42, 595, "7:00 PM – 9:00 PM", TimePeriodFilter.LAST_30_DAYS),
        NotificationStat("ns_30_2", "TeamChat", NotificationCategory.WORK, 528, 355, 118, "9:00 AM – 12:00 PM", TimePeriodFilter.LAST_30_DAYS),
        NotificationStat("ns_30_3", "Messages", NotificationCategory.COMMUNICATION, 442, 290, 96, "6:00 PM – 9:00 PM", TimePeriodFilter.LAST_30_DAYS),
        NotificationStat("ns_30_4", "Campus LMS", NotificationCategory.EDUCATION, 340, 275, 34, "1:00 PM – 5:00 PM", TimePeriodFilter.LAST_30_DAYS),
        NotificationStat("ns_30_5", "StreamBox", NotificationCategory.ENTERTAINMENT, 295, 16, 260, "8:00 PM – 11:00 PM", TimePeriodFilter.LAST_30_DAYS),
        NotificationStat("ns_30_6", "Device Health", NotificationCategory.SYSTEM, 112, 74, 18, "8:00 AM – 10:00 AM", TimePeriodFilter.LAST_30_DAYS),
        NotificationStat("ns_30_7", "QuickNotes", NotificationCategory.OTHER, 88, 35, 30, "3:00 PM – 5:00 PM", TimePeriodFilter.LAST_30_DAYS)
    )

    fun defaultFocusSessions(): List<FocusSessionRecord> {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val day = 86_400_000L
        return listOf(
            FocusSessionRecord("fs_1", "profile_study", "Study", 45, 45, now - 3 * hour, now - 2 * hour, 6, "Completed"),
            FocusSessionRecord("fs_2", "profile_work", "Work", 60, 60, now - 7 * hour, now - 6 * hour, 9, "Completed"),
            FocusSessionRecord("fs_3", "profile_study", "Study", 45, 45, now - day - 2 * hour, now - day - hour, 5, "Completed"),
            FocusSessionRecord("fs_4", "profile_work", "Work", 25, 25, now - 2 * day, now - 2 * day + 25 * 60_000L, 3, "Completed"),
            FocusSessionRecord("fs_5", "profile_study", "Study", 60, 50, now - 3 * day, now - 3 * day + 50 * 60_000L, 7, "Ended Early")
        )
    }

    fun defaultActivityEvents(): List<ActivityEvent> {
        val now = System.currentTimeMillis()
        val min = 60_000L
        val hour = 3_600_000L
        return listOf(
            ActivityEvent(
                id = "ev_1",
                title = "Work Hours Routine Executed",
                timestampMillis = now - 25 * min,
                category = EventCategory.ROUTINE,
                triggerSource = "Schedule Trigger (9:00 AM)",
                result = "Work Profile activated (Simulated)",
                explanation = "Weekday morning schedule matched Work Hours with priority 7.",
                relatedRoutineName = "Work Hours",
                relatedProfileName = "Work"
            ),
            ActivityEvent(
                id = "ev_2",
                title = "Focus Session Completed (45m)",
                timestampMillis = now - 2 * hour,
                category = EventCategory.FOCUS,
                triggerSource = "Focus Timer",
                result = "6 simulated interruptions blocked",
                explanation = "Study profile protected 45 minutes of uninterrupted coursework.",
                relatedProfileName = "Study"
            ),
            ActivityEvent(
                id = "ev_3",
                title = "Emergency Contact Exception Applied",
                timestampMillis = now - 3 * hour,
                category = EventCategory.CONTACT_EXCEPTION,
                triggerSource = "Simulated Incoming Call",
                result = "Allowed call from Mom (Nadia)",
                explanation = "Study Profile blocked normal callers, but Emergency priority bypassed Silent mode.",
                relatedProfileName = "Study"
            ),
            ActivityEvent(
                id = "ev_4",
                title = "Social Interruption Blocked",
                timestampMillis = now - 3 * hour - 15 * min,
                category = EventCategory.SOUND_MODE,
                triggerSource = "Simulated Social Notification",
                result = "Muted alert from Socialgram",
                explanation = "Social Media category is filtered under active Study Profile.",
                relatedProfileName = "Study"
            ),
            ActivityEvent(
                id = "ev_5",
                title = "Rule Conflict Resolved",
                timestampMillis = now - 18 * hour,
                category = EventCategory.CONFLICT,
                triggerSource = "Simulation Engine (6:00 PM)",
                result = "Study Time chosen over Evening Unwind",
                explanation = "Both routines triggered at 6:00 PM; Study Time priority (9) outranked Evening Unwind (4).",
                relatedRoutineName = "Study Time",
                relatedProfileName = "Study"
            ),
            ActivityEvent(
                id = "ev_6",
                title = "Simulated Sound Mode Changed to Silent",
                timestampMillis = now - 18 * hour - 1 * min,
                category = EventCategory.SOUND_MODE,
                triggerSource = "Study Time Routine",
                result = "Simulated Sound Mode -> Silent",
                explanation = "Applied as part of Study Time automated action sequence.",
                relatedRoutineName = "Study Time",
                relatedProfileName = "Study"
            ),
            ActivityEvent(
                id = "ev_7",
                title = "Sleep Schedule Activated",
                timestampMillis = now - 12 * hour,
                category = EventCategory.ROUTINE,
                triggerSource = "Schedule Trigger (11:00 PM)",
                result = "Sleep Profile enabled",
                explanation = "Every-day bedtime schedule enabled Sleep protection with Emergency exceptions.",
                relatedRoutineName = "Sleep Schedule",
                relatedProfileName = "Sleep"
            ),
            ActivityEvent(
                id = "ev_8",
                title = "Context Changed to University Library",
                timestampMillis = now - 22 * hour,
                category = EventCategory.CONTEXT,
                triggerSource = "Context Simulator",
                result = "Location=Library, Activity=Studying",
                explanation = "Simulated location change updated context awareness state.",
                relatedProfileName = "Study"
            ),
            ActivityEvent(
                id = "ev_9",
                title = "Personal Profile Activated",
                timestampMillis = now - 24 * hour,
                category = EventCategory.PROFILE,
                triggerSource = "Personal Time Routine",
                result = "Simulated Sound Mode -> Sound",
                explanation = "Weekday 5:00 PM transition restored Personal profile.",
                relatedRoutineName = "Personal Time",
                relatedProfileName = "Personal"
            ),
            ActivityEvent(
                id = "ev_10",
                title = "Automation Resumed by User",
                timestampMillis = now - 28 * hour,
                category = EventCategory.AUTOMATION,
                triggerSource = "Safety Center",
                result = "All enabled routines active",
                explanation = "Automation resumed after manual override check.",
                relatedProfileName = "Personal"
            ),
            ActivityEvent(
                id = "ev_11",
                title = "Routine Bypass Tested",
                timestampMillis = now - 30 * hour,
                category = EventCategory.CONTACT_EXCEPTION,
                triggerSource = "Simulated Incoming Call",
                result = "Allowed call from Dr. Kareem Mansour",
                explanation = "Emergency contacts always reach you regardless of active routine.",
                relatedProfileName = "Work"
            ),
            ActivityEvent(
                id = "ev_12",
                title = "Demo Environment Initialized",
                timestampMillis = now - 36 * hour,
                category = EventCategory.SYSTEM,
                triggerSource = "Smart Companion Demo",
                result = "5 profiles, 5 routines, 2 emergency contacts seeded",
                explanation = "Local synthetic dataset loaded for offline demonstration.",
                relatedProfileName = "Personal"
            )
        )
    }
}

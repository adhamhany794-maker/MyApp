package com.example.utils

import com.example.models.ActionType
import com.example.models.AppLanguage
import com.example.models.ConditionType
import com.example.models.ConflictResolutionPolicy
import com.example.models.ContactPriority
import com.example.models.DayFilter
import com.example.models.EventCategory
import com.example.models.ExceptionType
import com.example.models.InterruptionType
import com.example.models.NotificationCategory
import com.example.models.SimulatedActivity
import com.example.models.SimulatedLocation
import com.example.models.SmartProfile
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.ThemeMode
import com.example.models.TimePeriodFilter
import com.example.models.TriggerType

abstract class AppStrings {
    abstract val isArabic: Boolean
    abstract val appName: String
    abstract val tagline: String
    abstract val demoBadge: String
    abstract val simulatedChip: String
    abstract val appEventChip: String

    // Navigation & Titles
    abstract val navHome: String
    abstract val navAssistant: String
    abstract val navRoutines: String
    abstract val navNewRoutine: String
    abstract val navContacts: String
    abstract val navInsights: String
    abstract val navSettings: String
    abstract val screenProfiles: String
    abstract val screenFocus: String
    abstract val screenContext: String
    abstract val screenNotifications: String
    abstract val screenHistory: String
    abstract val screenSafety: String
    abstract val screenSettings: String
    abstract val screenPlayground: String
    abstract val screenRuleBuilder: String
    abstract val screenSimulation: String
    abstract val screenActivity: String
    abstract val screenAdvanced: String
    abstract val screenMore: String
    abstract val openMenuDesc: String
    abstract val backDesc: String

    // Feature Help Dialog Labels
    abstract val featureHelpContentDesc: String
    abstract val helpWhatIsIt: String
    abstract val helpProblemSolved: String
    abstract val helpHowToUse: String
    abstract val helpWhatHappens: String
    abstract val gotItBtn: String

    // More Hub Categories & Descriptions
    abstract val moreSubtitle: String
    abstract val catFocus: String
    abstract val catActivity: String
    abstract val catIntelligence: String
    abstract val catProfiles: String
    abstract val catProtection: String
    abstract val catPreferences: String
    abstract val descFocusSessions: String
    abstract val descActivityHistory: String
    abstract val descNotifications: String
    abstract val descContext: String
    abstract val descSimulation: String
    abstract val descPlayground: String
    abstract val descProfiles: String
    abstract val descSafety: String
    abstract val descSettings: String

    // Common Buttons & Dialogs
    abstract val skip: String
    abstract val next: String
    abstract val back: String
    abstract val enterDemoMode: String
    abstract val confirm: String
    abstract val approveBtn: String
    abstract val rejectBtn: String
    abstract val cancel: String
    abstract val edit: String
    abstract val delete: String
    abstract val save: String
    abstract val close: String
    abstract val restore: String
    abstract val duplicate: String
    abstract val activate: String
    abstract val activeLabel: String
    abstract val simulate: String
    abstract val testAction: String
    abstract val runNow: String
    abstract val viewDetails: String
    abstract val viewAllLabel: String
    abstract val moreOptionsDesc: String
    abstract val moreModules: String
    abstract val showAllModules: String
    abstract val hideModules: String

    // Simplified Home Product Idea & Cards
    abstract val homeHeadline: String
    abstract val homeSubtitle: String
    abstract val homeCardAssistantTitle: String
    abstract val homeCardAssistantSub: String
    abstract val homeCardRoutinesTitle: String
    abstract val homeCardContactsTitle: String
    abstract val homeCardContactsSubSuffix: String
    abstract val manageProfilesBtn: String
    abstract val activitySubtitle: String
    abstract val advancedSubtitle: String
    abstract val settingsSubtitle: String
    abstract val settingsMoreTitle: String
    abstract val descActivityMerged: String
    abstract val descAdvanced: String
    abstract val descEmergencyContacts: String

    // Dashboard
    abstract val activeProfile: String
    abstract val switchProfilePrompt: String
    abstract val simulatedPhoneStatus: String
    abstract val simulatedPhoneHint: String
    abstract val simulatedNotRealOs: String
    abstract val automationStatus: String
    abstract val automationActive: String
    abstract val automationPaused: String
    abstract val pauseAutomation: String
    abstract val resumeAutomation: String
    abstract val quickActions: String
    abstract val todaysSchedule: String
    abstract val todaysOverviewTitle: String
    abstract val executedRoutinesLabel: String
    abstract val importantActivityLabel: String
    abstract val focusSummary: String
    abstract val recentActivity: String
    abstract val allRoutines: String
    abstract val openTimer: String
    abstract val fullHistory: String
    abstract val demoPlaygroundAction: String
    abstract val startFocus25m: String
    abstract val pauseAllRules: String
    abstract val resumeAllRules: String
    abstract val studyQuickTitle: String
    abstract val studyQuickSub: String
    abstract val workQuickTitle: String
    abstract val workQuickSub: String
    abstract val sleepQuickTitle: String
    abstract val sleepQuickSub: String
    abstract val totalFocusLabel: String
    abstract val sessionsLabel: String
    abstract val filteredLabel: String
    abstract val dailyFocusGoal: String
    abstract val noRoutinesScheduled: String
    abstract val scheduledLabel: String
    abstract val disabledLabel: String
    abstract val noRecentActivity: String
    abstract val activeFocusBannerPaused: String
    abstract val activeFocusBannerRunning: String
    abstract val distractionsBlockedSuffix: String

    // Routines Redesign & Guided Builder
    abstract val routinesPageExplain: String
    abstract val addRoutineBtn: String
    abstract val searchRoutinesHint: String
    abstract val filterAllRoutines: String
    abstract val filterActiveRoutines: String
    abstract val filterPausedRoutines: String
    abstract val stepWhenTitle: String
    abstract val stepConditionsTitle: String
    abstract val stepActionsTitle: String
    abstract val stepExceptionsTitle: String
    abstract val stepReviewTitle: String
    abstract val builderRowWhenTitle: String
    abstract val builderRowOnlyIfTitle: String
    abstract val builderRowDoThisTitle: String
    abstract val builderRowExceptTitle: String
    abstract val builderDoneBtn: String
    abstract val priorityHighLabel: String
    abstract val priorityMediumLabel: String
    abstract val priorityLowLabel: String
    abstract val advancedRuleOptions: String
    abstract val alwaysRunNote: String

    // Contacts Redesign
    abstract val contactsPriorityExplain: String
    abstract val emergencyTierTitle: String
    abstract val highPriorityTierTitle: String
    abstract val normalTierTitle: String
    abstract val toggleInterruptionTester: String

    // Insights Redesign
    abstract val focusChartExplain: String
    abstract val interruptionChartExplain: String
    abstract val profileChartExplain: String

    // Onboarding
    abstract val onboardingPages: List<Triple<String, String, String>>

    // Profiles
    abstract val profilesSubtitle: String
    abstract val defaultBadge: String
    abstract val newProfileTitle: String
    abstract val editProfileTitle: String
    abstract val profileNameLabel: String
    abstract val profileDescLabel: String
    abstract val defaultFocusMinsLabel: String
    abstract val allowEmergencyAlwaysNote: String
    abstract val allowHighPriorityContacts: String
    abstract val allowedCategoriesLabel: String
    abstract val profileNameError: String
    abstract val deleteProfileTitle: String
    abstract val deleteProfileMsg: String
    abstract val restoreProfilesTitle: String
    abstract val restoreProfilesMsg: String
    abstract val restoreDefaultsBtn: String

    // Routines & Rule Builder
    abstract val routinesSubtitlePaused: String
    abstract val routinesEnabledSuffix: String
    abstract val noRoutinesTitle: String
    abstract val noRoutinesMsg: String
    abstract val lastResultLabel: String
    abstract val deleteRoutineTitle: String
    abstract val deleteRoutineMsg: String
    abstract val executeNow: String
    abstract val ruleBuilderSubtitle: String
    abstract val ruleNameLabel: String
    abstract val ruleNameHint: String
    abstract val ruleDescLabel: String
    abstract val ruleDescHint: String
    abstract val priorityLevelPrefix: String
    abstract val whenHeader: String
    abstract val whenHint: String
    abstract val conditionsHeader: String
    abstract val conditionsHint: String
    abstract val actionsHeader: String
    abstract val actionsHint: String
    abstract val exceptionsHeader: String
    abstract val exceptionsHint: String
    abstract val optionalFiltersShow: String
    abstract val optionalFiltersHide: String
    abstract val targetProfileLabel: String
    abstract val focusDurationLabel: String
    abstract val ruleSummaryTitle: String
    abstract val testRuleBtn: String
    abstract val saveRuleBtn: String
    abstract val errRuleNameRequired: String
    abstract val errRuleActionRequired: String

    // Simulation Engine
    abstract val simSubtitle: String
    abstract val simClockTitle: String
    abstract val simEvaluateClockBtn: String
    abstract val simLocActTitle: String
    abstract val simEnterLocBtn: String
    abstract val simExitLocBtn: String
    abstract val simStartActBtn: String
    abstract val simInterruptionTitle: String
    abstract val simTraceTitle: String
    abstract val simTraceEmpty: String
    abstract val simTraceTrigger: String
    abstract val simTraceMatched: String
    abstract val simTraceConditions: String
    abstract val simTraceExceptions: String
    abstract val simTraceActions: String
    abstract val simTraceFinal: String

    // Contacts
    abstract val contactsSubtitle: String
    abstract val interruptionTesterTitle: String
    abstract val repeatCallerLabel: String
    abstract val testCallBtn: String
    abstract val testMsgBtn: String
    abstract val allowedUpper: String
    abstract val blockedUpper: String
    abstract val addContactTitle: String
    abstract val editContactTitle: String
    abstract val contactNameLabel: String
    abstract val contactRelLabel: String
    abstract val contactPriorityLabel: String
    abstract val contactTypesLabel: String
    abstract val allowRepeatCallers: String
    abstract val contactNameError: String
    abstract val removeContactTitle: String
    abstract val removeContactMsg: String
    abstract val removeBtn: String

    // Notification Intelligence
    abstract val notifSubtitle: String
    abstract val simulatedAlertsLabel: String
    abstract val importantLabel: String
    abstract val nonImportantLabel: String
    abstract val notifByAppTitle: String
    abstract val notifByCategoryTitle: String
    abstract val notifSuggestionsTitle: String

    // Context Awareness
    abstract val contextSubtitle: String
    abstract val currentContextTitle: String
    abstract val timeContextTitle: String
    abstract val simLocationTitle: String
    abstract val simActivityTitle: String
    abstract val matchingRoutinesTitle: String
    abstract val noMatchingContextRoutines: String
    abstract val testContextBtn: String

    // Assistant
    abstract val assistantHeaderTitle: String
    abstract val assistantHeaderSub: String
    abstract val assistantEmptyGuide: String
    abstract val assistantInputHint: String
    abstract val assistantThinking: String
    abstract val assistantSuggestedPrompts: List<String>

    // Focus Sessions
    abstract val focusSubtitle: String
    abstract val focusPausedBadge: String
    abstract val focusActiveBadge: String
    abstract val remainingLabel: String
    abstract val elapsedLabel: String
    abstract val configureFocusTitle: String
    abstract val durationLabel: String
    abstract val customLabel: String
    abstract val customMinutesLabel: String
    abstract val focusProfileLabel: String
    abstract val autoActivateProfile: String
    abstract val startFocusBtn: String
    abstract val playBtn: String
    abstract val pauseBtn: String
    abstract val resumeBtn: String
    abstract val resetBtn: String
    abstract val endBtn: String
    abstract val simBlockedDistractionBtn: String
    abstract val sessionSummaryTitle: String
    abstract val sessionHistoryTitle: String

    // Activity History
    abstract val historySubtitle: String
    abstract val historyLegend: String
    abstract val searchHistoryHint: String
    abstract val allLabel: String
    abstract val noHistoryTitle: String
    abstract val noHistoryMsg: String
    abstract val clearHistoryTitle: String
    abstract val clearHistoryMsg: String
    abstract val clearHistoryBtn: String

    // Insights
    abstract val insightsSubtitle: String
    abstract val focusAnalyticsTitle: String
    abstract val dailyAvgLabel: String
    abstract val focusTrendTitle: String
    abstract val interruptionAnalyticsTitle: String
    abstract val exploreAppsBtn: String
    abstract val simTotalLabel: String
    abstract val blockedLabel: String
    abstract val allowedLabel: String
    abstract val automationAnalyticsTitle: String
    abstract val executionsLabel: String
    abstract val skippedLabel: String
    abstract val conflictsLabel: String
    abstract val frequentProfilesLabel: String
    abstract val recommendationsTitle: String

    // Safety Center
    abstract val safetySubtitle: String
    abstract val globalPauseTitle: String
    abstract val globalPauseOnSub: String
    abstract val globalPauseOffSub: String
    abstract val manualOverrideTitle: String
    abstract val currentSimStatePrefix: String
    abstract val restorePrevStatePrefix: String
    abstract val conflictWarningsPrefix: String
    abstract val noConflictsMsg: String
    abstract val testResolveConflictBtn: String
    abstract val emergencyTestTitle: String
    abstract val emergencyTestSub: String
    abstract val simulateEmergencyCallPrefix: String
    abstract val recoveryTitle: String
    abstract val recoverySub: String
    abstract val resetDemoRoutinesBtn: String
    abstract val resetEntireAppBtn: String

    // Settings Grouped Sections & 1-Sentence Plain Explanations
    abstract val settingsGeneralTitle: String
    abstract val settingsGeneralExplain: String
    abstract val settingsSafetyTitle: String
    abstract val settingsSafetyExplain: String
    abstract val settingsAutomationTitle: String
    abstract val settingsAutomationExplain: String
    abstract val settingsAdvancedTitle: String
    abstract val settingsAdvancedExplain: String
    abstract val appearanceTitle: String
    abstract val appearanceExplain: String
    abstract val languageTitle: String
    abstract val languageSub: String
    abstract val replayOnboardingExplain: String
    abstract val manualOverrideExplain: String
    abstract val conflictCheckExplain: String
    abstract val emergencyTestExplain: String
    abstract val automationPrefsTitle: String
    abstract val pauseAllAutomationLabel: String
    abstract val confirmImportantLabel: String
    abstract val confirmImportantExplain: String
    abstract val defaultProfileLabel: String
    abstract val defaultProfileExplain: String
    abstract val conflictPolicyLabel: String
    abstract val conflictPolicyExplain: String
    abstract val privacyTitle: String
    abstract val privacyExplain: String
    abstract val privacyBody: String
    abstract val demoMgmtTitle: String
    abstract val demoMgmtExplain: String
    abstract val resetDemoDataBtn: String
    abstract val viewOnboardingBtn: String
    abstract val aboutTitle: String
    abstract val aboutExplain: String
    abstract val aboutVersionLine: String

    // Conflict Dialog
    abstract val conflictDialogTitle: String
    abstract val conflictDefaultExplain: String
    abstract val keepCurrentStateBtn: String

    // Playground
    abstract val playgroundSubtitle: String

    fun priorityTierLabel(priority: Int): String = when {
        priority >= 8 -> priorityHighLabel
        priority >= 5 -> priorityMediumLabel
        else -> priorityLowLabel
    }

    fun conflictPromptSentence(names: List<String>): String {
        return when {
            names.size >= 2 && isArabic -> "يتعارض ${names[0]} مع ${names[1]}. أيهما يجب أن يربح؟"
            names.size >= 2 -> "${names[0]} and ${names[1]} overlap. Which one should win?"
            names.size == 1 && isArabic -> "هل تريد تشغيل ${names[0]} الآن؟"
            names.size == 1 -> "Run ${names[0]} now?"
            else -> conflictDefaultExplain
        }
    }

    fun soundModeLabel(mode: SoundMode): String = if (isArabic) {
        when (mode) {
            SoundMode.SOUND -> "صوت"
            SoundMode.VIBRATE -> "اهتزاز"
            SoundMode.SILENT -> "صامت"
        }
    } else mode.displayName

    fun themeModeLabel(mode: ThemeMode): String = if (isArabic) {
        when (mode) {
            ThemeMode.SYSTEM -> "تلقائي حسب النظام"
            ThemeMode.LIGHT -> "الوضع الفاتح"
            ThemeMode.DARK -> "الوضع الداكن"
        }
    } else mode.label

    fun categoryLabel(cat: NotificationCategory): String = if (isArabic) {
        when (cat) {
            NotificationCategory.COMMUNICATION -> "التواصل"
            NotificationCategory.SOCIAL_MEDIA -> "التواصل الاجتماعي"
            NotificationCategory.WORK -> "العمل"
            NotificationCategory.EDUCATION -> "التعليم"
            NotificationCategory.ENTERTAINMENT -> "الترفيه"
            NotificationCategory.SYSTEM -> "النظام"
            NotificationCategory.OTHER -> "أخرى"
        }
    } else cat.displayName

    fun contactPriorityLabel(p: ContactPriority): String = if (isArabic) {
        when (p) {
            ContactPriority.EMERGENCY -> "طوارئ"
            ContactPriority.HIGH_PRIORITY -> "أولوية عالية"
            ContactPriority.NORMAL -> "عادي"
        }
    } else p.displayName

    fun interruptionTypeLabel(t: InterruptionType): String = if (isArabic) {
        when (t) {
            InterruptionType.CALL -> "المكالمات"
            InterruptionType.MESSAGE -> "الرسائل"
            InterruptionType.REPEAT_CALLER -> "المتصل المتكرر"
        }
    } else t.displayName

    fun triggerTypeLabel(t: TriggerType): String = if (isArabic) {
        when (t) {
            TriggerType.SPECIFIC_TIME -> "وقت محدد"
            TriggerType.SPECIFIC_DAYS -> "أيام محددة"
            TriggerType.DATE_AND_TIME -> "تاريخ ووقت"
            TriggerType.MANUAL -> "تشغيل يدوي"
            TriggerType.FOCUS_STARTED -> "بدء جلسة تركيز"
            TriggerType.FOCUS_ENDED -> "انتهاء جلسة تركيز"
            TriggerType.LOCATION_ENTERED -> "دخول موقع محاكى"
            TriggerType.LOCATION_EXITED -> "مغادرة موقع محاكى"
            TriggerType.ACTIVITY_STARTED -> "بدء نشاط محاكى"
            TriggerType.ACTIVITY_ENDED -> "انتهاء نشاط محاكى"
        }
    } else t.displayName

    fun conditionTypeLabel(c: ConditionType): String = if (isArabic) {
        when (c) {
            ConditionType.DAY_OF_WEEK -> "يوم الأسبوع"
            ConditionType.TIME_RANGE -> "نطاق الوقت"
            ConditionType.ACTIVE_PROFILE -> "الوضع النشط"
            ConditionType.AUTOMATION_ENABLED -> "حالة الأتمتة"
            ConditionType.SIMULATED_LOCATION -> "الموقع المحاكى"
            ConditionType.SIMULATED_ACTIVITY -> "النشاط المحاكى"
            ConditionType.FOCUS_SESSION_ACTIVE -> "حالة جلسة التركيز"
        }
    } else c.displayName

    fun actionTypeLabel(a: ActionType): String = if (isArabic) {
        when (a) {
            ActionType.ACTIVATE_PROFILE -> "تفعيل وضع ذكي"
            ActionType.SET_SOUND_MODE -> "ضبط وضع الصوت المحاكى"
            ActionType.START_FOCUS_SESSION -> "بدء جلسة تركيز"
            ActionType.END_FOCUS_SESSION -> "إنهاء جلسة تركيز"
            ActionType.PAUSE_AUTOMATION -> "إيقاف الأتمتة مؤقتاً"
            ActionType.RESUME_AUTOMATION -> "استئناف الأتمتة"
            ActionType.SHOW_NOTIFICATION -> "عرض إشعار داخلي"
            ActionType.RECORD_ACTIVITY -> "تسجيل حدث نشاط"
        }
    } else a.displayName

    fun exceptionTypeLabel(e: ExceptionType): String = if (isArabic) {
        when (e) {
            ExceptionType.PRIORITY_CONTACTS -> "جهات الاتصال ذات الأولوية"
            ExceptionType.EMERGENCY_CONTACTS -> "جهات اتصال الطوارئ"
            ExceptionType.SPECIFIC_DAYS -> "أيام مستثناة"
            ExceptionType.TIME_RANGE -> "نطاق زمني مستثنى"
            ExceptionType.MANUALLY_DISABLED -> "إيقاف يدوي للقاعدة"
        }
    } else e.displayName

    fun locationLabel(l: SimulatedLocation): String = if (isArabic) {
        when (l) {
            SimulatedLocation.HOME -> "المنزل"
            SimulatedLocation.UNIVERSITY -> "الجامعة"
            SimulatedLocation.WORKPLACE -> "مكان العمل"
            SimulatedLocation.LIBRARY -> "المكتبة"
            SimulatedLocation.CUSTOM -> "موقع مخصص"
        }
    } else l.displayName

    fun activityLabel(a: SimulatedActivity): String = if (isArabic) {
        when (a) {
            SimulatedActivity.STUDYING -> "الدراسة"
            SimulatedActivity.WORKING -> "العمل"
            SimulatedActivity.SLEEPING -> "النوم"
            SimulatedActivity.DRIVING -> "القيادة"
            SimulatedActivity.PERSONAL_TIME -> "وقت شخصي"
            SimulatedActivity.CUSTOM -> "نشاط مخصص"
        }
    } else a.displayName

    fun conflictPolicyLabel(p: ConflictResolutionPolicy): String = if (isArabic) {
        when (p) {
            ConflictResolutionPolicy.ASK_USER -> "اسألني للتأكيد"
            ConflictResolutionPolicy.HIGHEST_PRIORITY -> "اختر الأعلى أولوية تلقائياً"
            ConflictResolutionPolicy.KEEP_CURRENT -> "حافظ على الحالة الحالية"
        }
    } else p.displayName

    fun eventCategoryLabel(c: EventCategory): String = if (isArabic) {
        when (c) {
            EventCategory.PROFILE -> "الأوضاع"
            EventCategory.ROUTINE -> "الروتين"
            EventCategory.AUTOMATION -> "الأتمتة"
            EventCategory.SOUND_MODE -> "وضع الصوت"
            EventCategory.FOCUS -> "التركيز"
            EventCategory.CONTACT_EXCEPTION -> "استثناء اتصال"
            EventCategory.CONTEXT -> "السياق"
            EventCategory.CONFLICT -> "تعارض"
            EventCategory.ASSISTANT -> "المساعد"
            EventCategory.SYSTEM -> "النظام"
        }
    } else c.displayName

    fun dayFilterLabel(d: DayFilter, short: Boolean = false): String = if (isArabic) {
        when (d) {
            DayFilter.EVERY_DAY -> "يومياً"
            DayFilter.WEEKDAYS -> "أيام الأسبوع"
            DayFilter.WEEKENDS -> "عطلة نهاية الأسبوع"
            DayFilter.MONDAY -> if (short) "إثنين" else "الإثنين"
            DayFilter.TUESDAY -> if (short) "ثلاثاء" else "الثلاثاء"
            DayFilter.WEDNESDAY -> if (short) "أربعاء" else "الأربعاء"
            DayFilter.THURSDAY -> if (short) "خميس" else "الخميس"
            DayFilter.FRIDAY -> if (short) "جمعة" else "الجمعة"
            DayFilter.SATURDAY -> if (short) "سبت" else "السبت"
            DayFilter.SUNDAY -> if (short) "أحد" else "الأحد"
        }
    } else {
        if (short) d.shortLabel else d.label
    }

    fun timePeriodLabel(t: TimePeriodFilter): String = if (isArabic) {
        when (t) {
            TimePeriodFilter.TODAY -> "اليوم"
            TimePeriodFilter.LAST_7_DAYS -> "آخر 7 أيام"
            TimePeriodFilter.LAST_30_DAYS -> "آخر 30 يوماً"
        }
    } else t.label

    fun localizedProfileName(profile: SmartProfile): String {
        if (!isArabic) return profile.name
        return when (profile.id) {
            "profile_study" -> "الدراسة"
            "profile_work" -> "العمل"
            "profile_sleep" -> "النوم"
            "profile_personal" -> "شخصي"
            "profile_custom" -> "تركيز مخصص"
            else -> profile.name
        }
    }

    fun localizedProfileDescription(profile: SmartProfile): String {
        if (!isArabic) return profile.description
        return when (profile.id) {
            "profile_study" -> "تركيز دراسي عميق مع كتم تنبيهات التواصل الاجتماعي والسماح بمكالمات الطوارئ."
            "profile_work" -> "إنتاجية متوازنة تبقي قنوات العمل متاحة وتقلل ضوضاء التطبيقات الترفيهية."
            "profile_sleep" -> "حماية ليلية تحجب التنبيهات غير العاجلة مع إبقاء مكالمات الطوارئ مسموحة."
            "profile_personal" -> "وضع يومي مريح بصوت طبيعي محاكى وتواصل متاح."
            "profile_custom" -> "قالب مرن قابل للتخصيص للمكتبة أو التنقل اليومي."
            else -> profile.description
        }
    }

    fun localizedRoutineName(routine: SmartRoutine): String {
        if (!isArabic) return routine.name
        return when (routine.id) {
            "routine_study_time" -> "وقت الدراسة"
            "routine_sleep_schedule" -> "جدول النوم"
            "routine_work_hours" -> "ساعات العمل"
            "routine_personal_time" -> "الوقت الشخصي"
            "routine_evening_unwind" -> "الاسترخاء المسائي (تجربة تعارض)"
            else -> routine.name
        }
    }

    fun localizedRoutineDescription(routine: SmartRoutine): String {
        if (!isArabic) return routine.description
        return when (routine.id) {
            "routine_study_time" -> "يفعّل وضع الدراسة، يكتم التنبيهات المحاكاة، ويبدأ جلسة تركيز في أمسيات أيام العمل."
            "routine_sleep_schedule" -> "يحمي راحتك الليلية يومياً الساعة 11:00 م مع إبقاء مكالمات الطوارئ متاحة."
            "routine_work_hours" -> "يهيئ الهاتف لوضع العمل الساعة 9:00 ص في أيام الأسبوع ويقلل المقاطعات."
            "routine_personal_time" -> "يعيد الوضع الشخصي وصوت الهاتف المحاكى الطبيعي عند انتهاء العمل الساعة 5:00 م."
            "routine_evening_unwind" -> "يتقاطع الساعة 6:00 م لتوضيح كيفية اكتشاف التعارض وحله حسب الأولوية."
            else -> routine.description
        }
    }
}

fun stringsFor(language: AppLanguage): AppStrings {
    return if (language == AppLanguage.ARABIC) ArabicAppStrings else EnglishAppStrings
}

private object EnglishAppStrings : AppStrings() {
    override val isArabic = false
    override val appName = "Smart Companion"
    override val tagline = "Stay focused without missing what matters."
    override val demoBadge = "Demo / simulation"
    override val simulatedChip = "Demo / simulation"
    override val appEventChip = "App update"
    override val navHome = "Home"
    override val navAssistant = "Assistant"
    override val navRoutines = "Routines"
    override val navNewRoutine = "New routine"
    override val navContacts = "Emergency contacts"
    override val navInsights = "Insights"
    override val navSettings = "Settings"
    override val screenProfiles = "Smart Profiles"
    override val screenFocus = "Focus Sessions"
    override val screenContext = "Context Awareness"
    override val screenNotifications = "Notifications"
    override val screenHistory = "History"
    override val screenSafety = "Safety"
    override val screenSettings = "Settings"
    override val screenPlayground = "Demo Playground"
    override val screenRuleBuilder = "Create Routine"
    override val screenSimulation = "Simulation Engine"
    override val screenActivity = "Activity"
    override val screenAdvanced = "Advanced"
    override val screenMore = "More"
    override val openMenuDesc = "Open More menu"
    override val backDesc = "Go back"
    override val featureHelpContentDesc = "About this feature"
    override val helpWhatIsIt = "What is this?"
    override val helpProblemSolved = "What problem does it solve?"
    override val helpHowToUse = "How do I use it?"
    override val helpWhatHappens = "What happens when activated?"
    override val gotItBtn = "Got it"
    override val moreSubtitle = "Organized secondary tools for focus, diagnostics, profiles, and preferences."
    override val catFocus = "Focus"
    override val catActivity = "Activity"
    override val catIntelligence = "Intelligence"
    override val catProfiles = "Profiles"
    override val catProtection = "Protection"
    override val catPreferences = "Preferences"
    override val descFocusSessions = "Timed focus blocks synced with your active profile."
    override val descActivityHistory = "Full log of simulated actions and app state updates."
    override val descNotifications = "Breakdown of simulated app noise and peak hours."
    override val descContext = "Test simulated time, location, and activity signals."
    override val descSimulation = "Inspect rule matching, traces, and conflict resolution."
    override val descPlayground = "One-tap end-to-end interactive demo walkthroughs."
    override val descProfiles = "Customize Study, Work, Sleep, and Personal modes."
    override val descSafety = "Global automation pause, state rollback, and resets."
    override val descSettings = "Theme, English/Arabic RTL language, and demo controls."
    override val skip = "Skip"
    override val next = "Next"
    override val back = "Back"
    override val enterDemoMode = "Enter Demo Mode"
    override val confirm = "Confirm"
    override val approveBtn = "Approve"
    override val rejectBtn = "Reject"
    override val cancel = "Cancel"
    override val edit = "Edit"
    override val delete = "Delete"
    override val save = "Save"
    override val close = "Close"
    override val restore = "Restore"
    override val duplicate = "Duplicate"
    override val activate = "Activate"
    override val activeLabel = "Active"
    override val simulate = "Simulate"
    override val testAction = "Test"
    override val runNow = "Run Now"
    override val viewDetails = "View Details"
    override val viewAllLabel = "View all"
    override val moreOptionsDesc = "More options"
    override val moreModules = "Explore Modules"
    override val showAllModules = "Show all tools"
    override val hideModules = "Hide tools"
    override val homeHeadline = "Your phone adapts to your day."
    override val homeSubtitle = "Set a rule once, like 'silent while I study 8 to 10', and Smart Companion handles calls and notifications for you. Emergency contacts always get through."
    override val homeCardAssistantTitle = "Tell the assistant what you want"
    override val homeCardAssistantSub = "Describe a routine in plain language and review it before saving."
    override val homeCardRoutinesTitle = "My routines"
    override val homeCardContactsTitle = "Emergency contacts"
    override val homeCardContactsSubSuffix = "protected — emergency contacts always get through"
    override val manageProfilesBtn = "Manage profiles"
    override val activitySubtitle = "Insights, event timeline, and notification analytics in one place."
    override val advancedSubtitle = "Simulated context signals, interactive demo scenarios, and rule engine."
    override val settingsSubtitle = "Customize appearance, emergency safety, routines behavior, and advanced tools."
    override val settingsMoreTitle = "More"
    override val descActivityMerged = "See focus stats, past activity, and notification trends."
    override val descAdvanced = "Test simulated context, demo scenarios, and the rule engine."
    override val descEmergencyContacts = "Choose callers who always get through, even when your phone is silent."
    override val activeProfile = "Current Profile"
    override val switchProfilePrompt = "Switch profile:"
    override val simulatedPhoneStatus = "Phone Status"
    override val simulatedPhoneHint = "Demo state only — does not change real device audio."
    override val simulatedNotRealOs = "SIMULATED"
    override val automationStatus = "Automation Status"
    override val automationActive = "Automation Enabled"
    override val automationPaused = "Automation Paused"
    override val pauseAutomation = "Pause"
    override val resumeAutomation = "Resume"
    override val quickActions = "Quick Actions"
    override val todaysSchedule = "Today's Overview"
    override val todaysOverviewTitle = "Today's Overview"
    override val executedRoutinesLabel = "Executed"
    override val importantActivityLabel = "Priority"
    override val focusSummary = "Focus Summary"
    override val recentActivity = "Recent Activity"
    override val allRoutines = "All Routines"
    override val openTimer = "Open Timer"
    override val fullHistory = "View all"
    override val demoPlaygroundAction = "Demo Scenarios"
    override val startFocus25m = "Start Focus (25m)"
    override val pauseAllRules = "Pause All Rules"
    override val resumeAllRules = "Resume Rules"
    override val studyQuickTitle = "Study Mode"
    override val studyQuickSub = "Silent + Focus"
    override val workQuickTitle = "Work Mode"
    override val workQuickSub = "Vibrate + Priority"
    override val sleepQuickTitle = "Sleep Mode"
    override val sleepQuickSub = "Emergency Only"
    override val totalFocusLabel = "Focus Time"
    override val sessionsLabel = "Sessions"
    override val filteredLabel = "Filtered"
    override val dailyFocusGoal = "Daily Focus Goal"
    override val noRoutinesScheduled = "No routines scheduled yet."
    override val scheduledLabel = "Scheduled"
    override val disabledLabel = "Paused"
    override val noRecentActivity = "No simulated activity recorded yet."
    override val activeFocusBannerPaused = "Focus Paused"
    override val activeFocusBannerRunning = "Focus Active"
    override val distractionsBlockedSuffix = "simulated distractions blocked"
    override val routinesPageExplain = "Routines switch your simulated profile and sound automatically when triggers match."
    override val addRoutineBtn = "Add Routine"
    override val searchRoutinesHint = "Search routines..."
    override val filterAllRoutines = "All"
    override val filterActiveRoutines = "Active"
    override val filterPausedRoutines = "Paused"
    override val stepWhenTitle = "When should it happen?"
    override val stepConditionsTitle = "Only if these apply"
    override val stepActionsTitle = "Do this automatically"
    override val stepExceptionsTitle = "Except for these"
    override val stepReviewTitle = "Name & save routine"
    override val builderRowWhenTitle = "When"
    override val builderRowOnlyIfTitle = "Only if"
    override val builderRowDoThisTitle = "Do this"
    override val builderRowExceptTitle = "Except"
    override val builderDoneBtn = "Done"
    override val priorityHighLabel = "High"
    override val priorityMediumLabel = "Medium"
    override val priorityLowLabel = "Low"
    override val advancedRuleOptions = "Importance"
    override val alwaysRunNote = "Optional — leave unchecked to run whenever the trigger happens."
    override val contactsPriorityExplain = "Emergency contacts always reach you even in Silent mode. High Priority callers depend on your active profile, and Normal contacts follow your current sound mode."
    override val emergencyTierTitle = "Emergency"
    override val highPriorityTierTitle = "High Priority"
    override val normalTierTitle = "Normal"
    override val toggleInterruptionTester = "Test how priority contacts behave"
    override val focusChartExplain = "Minutes completed in your most recent recorded focus sessions."
    override val interruptionChartExplain = "7-day simulated alerts grouped by category and peak hours."
    override val profileChartExplain = "Combined profile usage from routine runs, focus blocks, and active selection."
    override val onboardingPages = listOf(
        Triple("Automatic Routines", "Set a rule once by time, day of week, or entering/leaving a place — your phone adapts automatically.", "Smart profiles & focus tools built in."),
        Triple("Emergency Contacts Always Get Through", "Chosen emergency callers are never affected by any routine, even in Silent mode.", "Your safety net stays active 24/7."),
        Triple("AI Assistant", "Type what you want in plain language and get a routine card to Approve, Edit, or Reject.", "Nothing activates without your approval.")
    )
    override val profilesSubtitle = "Each profile controls simulated sound mode, allowed alert categories, and priority exceptions."
    override val defaultBadge = "DEFAULT"
    override val newProfileTitle = "New Smart Profile"
    override val editProfileTitle = "Edit Profile"
    override val profileNameLabel = "Profile Name *"
    override val profileDescLabel = "What does this profile do?"
    override val defaultFocusMinsLabel = "Default Focus Minutes"
    override val allowEmergencyAlwaysNote = "Emergency callers and messages always get through in every profile."
    override val allowHighPriorityContacts = "Allow High-Priority Contacts"
    override val allowedCategoriesLabel = "Allowed Notification Categories"
    override val profileNameError = "Please enter a profile name."
    override val deleteProfileTitle = "Delete Custom Profile?"
    override val deleteProfileMsg = "Remove this custom profile? Existing routines will remain editable."
    override val restoreProfilesTitle = "Restore Default Profiles?"
    override val restoreProfilesMsg = "Resets Study, Work, Sleep, Personal, and Custom Focus to their original demo setup."
    override val restoreDefaultsBtn = "Restore Defaults"
    override val routinesSubtitlePaused = "Global Automation Pause is active — manual runs still work."
    override val routinesEnabledSuffix = "routines enabled"
    override val noRoutinesTitle = "No routines yet"
    override val noRoutinesMsg = "Tap + to build a rule with WHEN, CONDITIONS, ACTIONS, and EXCEPTIONS."
    override val lastResultLabel = "Last result"
    override val deleteRoutineTitle = "Delete Routine?"
    override val deleteRoutineMsg = "Are you sure you want to delete this routine? This cannot be undone."
    override val executeNow = "Execute Now"
    override val ruleBuilderSubtitle = "WHEN → CONDITIONS → ACTIONS → EXCEPTIONS"
    override val ruleNameLabel = "Routine Name *"
    override val ruleNameHint = "e.g. Library Study Focus"
    override val ruleDescLabel = "Description"
    override val ruleDescHint = "Explain what this routine protects"
    override val priorityLevelPrefix = "Priority Level"
    override val whenHeader = "1. WHEN (Trigger)"
    override val whenHint = "Choose what starts this routine."
    override val conditionsHeader = "2. CONDITIONS (Optional Filters)"
    override val conditionsHint = "Extra checks that must be true before running."
    override val actionsHeader = "3. ACTIONS *"
    override val actionsHint = "What Smart Companion does when triggered."
    override val exceptionsHeader = "4. EXCEPTIONS"
    override val exceptionsHint = "Who or when can bypass this routine."
    override val optionalFiltersShow = "Show optional conditions & exceptions"
    override val optionalFiltersHide = "Hide optional conditions & exceptions"
    override val targetProfileLabel = "Target Profile"
    override val focusDurationLabel = "Focus Duration"
    override val ruleSummaryTitle = "Rule Summary"
    override val testRuleBtn = "Test Rule"
    override val saveRuleBtn = "Save Routine"
    override val errRuleNameRequired = "Please enter a routine name."
    override val errRuleActionRequired = "Select at least one action before continuing."
    override val simSubtitle = "Test triggers, conditions, priority conflicts, and interruptions against live app state."
    override val simClockTitle = "1. Simulate Clock & Day Trigger"
    override val simEvaluateClockBtn = "Evaluate Time Trigger"
    override val simLocActTitle = "2. Simulate Location & Activity Triggers"
    override val simEnterLocBtn = "Enter Location"
    override val simExitLocBtn = "Exit Location"
    override val simStartActBtn = "Simulate Starting Activity"
    override val simInterruptionTitle = "3. Simulate Incoming Call or Message"
    override val simTraceTitle = "Latest Engine Execution Trace"
    override val simTraceEmpty = "Run any trigger above to inspect step-by-step evaluation."
    override val simTraceTrigger = "Trigger Received"
    override val simTraceMatched = "Matching Routines"
    override val simTraceConditions = "Conditions Evaluated"
    override val simTraceExceptions = "Exceptions Checked"
    override val simTraceActions = "Actions Selected"
    override val simTraceFinal = "Final Simulated Result"
    override val contactsSubtitle = "Emergency callers are never silenced by any routine (synthetic demo)."
    override val interruptionTesterTitle = "Incoming Interruption Tester"
    override val repeatCallerLabel = "Simulate repeat caller (2nd call in 3m)"
    override val testCallBtn = "Test Call"
    override val testMsgBtn = "Test Message"
    override val allowedUpper = "ALLOWED"
    override val blockedUpper = "BLOCKED"
    override val addContactTitle = "Add Synthetic Contact"
    override val editContactTitle = "Edit Contact"
    override val contactNameLabel = "Name *"
    override val contactRelLabel = "Relationship Label"
    override val contactPriorityLabel = "Priority Level"
    override val contactTypesLabel = "Allowed Interruption Types"
    override val allowRepeatCallers = "Allow repeat callers"
    override val contactNameError = "Please enter a contact name."
    override val removeContactTitle = "Remove Priority Contact?"
    override val removeContactMsg = "Remove this contact from your priority list?"
    override val removeBtn = "Remove"
    override val notifSubtitle = "Synthetic demonstration data — does not read real OS notifications."
    override val simulatedAlertsLabel = "Simulated Alerts"
    override val importantLabel = "Important"
    override val nonImportantLabel = "Non-Important"
    override val notifByAppTitle = "Notifications by Application (Demo)"
    override val notifByCategoryTitle = "Category Distribution (Demo)"
    override val notifSuggestionsTitle = "Suggested Focus Improvements"
    override val contextSubtitle = "Simulated environment signals — no GPS or location permission used."
    override val currentContextTitle = "Current Simulated Context"
    override val timeContextTitle = "Time Context"
    override val simLocationTitle = "Simulated Location"
    override val simActivityTitle = "Simulated Activity"
    override val matchingRoutinesTitle = "Routines Matching Current Context"
    override val noMatchingContextRoutines = "No routines match this exact simulated context snapshot."
    override val testContextBtn = "Test Context Automation Decision"
    override val assistantHeaderTitle = "Smart Assistant"
    override val assistantHeaderSub = "Type what you want — Approve, Edit, or Reject a routine."
    override val assistantEmptyGuide = "I can build routines from plain language, start a focus session, or summarize your daily simulated activity. Every generated rule asks for your confirmation before saving."
    override val assistantInputHint = "Ask or describe a routine..."
    override val assistantThinking = "Thinking..."
    override val assistantSuggestedPrompts = listOf(
        "Silence my phone while I study 8 to 10 but let my family call me",
        "Vibrate during work hours weekdays 9 AM to 5 PM",
        "Mute notifications when I enter the Library",
        "Start a 45m focus routine every evening at 7 PM"
    )
    override val focusSubtitle = "Track both elapsed and remaining focus time synced with your Smart Profiles."
    override val focusPausedBadge = "SESSION PAUSED"
    override val focusActiveBadge = "FOCUS ACTIVE"
    override val remainingLabel = "Remaining"
    override val elapsedLabel = "Elapsed"
    override val configureFocusTitle = "Configure New Focus Session"
    override val durationLabel = "Duration"
    override val customLabel = "Custom"
    override val customMinutesLabel = "Custom Minutes (5–240)"
    override val focusProfileLabel = "Focus Profile"
    override val autoActivateProfile = "Activate selected profile automatically"
    override val startFocusBtn = "Start Focus Session"
    override val playBtn = "Play"
    override val pauseBtn = "Pause"
    override val resumeBtn = "Resume"
    override val resetBtn = "Reset"
    override val endBtn = "End"
    override val simBlockedDistractionBtn = "Simulate Blocked Distraction"
    override val sessionSummaryTitle = "Session Summary"
    override val sessionHistoryTitle = "Session History"
    override val historySubtitle = "Timeline of simulated actions and app state updates."
    override val historyLegend = "SIMULATED = Demo phone/trigger simulation • APP STATE = In-app change"
    override val searchHistoryHint = "Search events, routines, or profiles..."
    override val allLabel = "All"
    override val noHistoryTitle = "No matching events"
    override val noHistoryMsg = "Switch a profile, run a routine, or start a focus session to record events."
    override val clearHistoryTitle = "Clear Activity History?"
    override val clearHistoryMsg = "This will erase all recorded demo events from the timeline."
    override val clearHistoryBtn = "Clear History"
    override val insightsSubtitle = "Metrics derived from your live focus sessions, routines, and demo telemetry."
    override val focusAnalyticsTitle = "Focus Analytics"
    override val dailyAvgLabel = "Daily Avg"
    override val focusTrendTitle = "Recent Focus Sessions"
    override val interruptionAnalyticsTitle = "Interruption Analytics (7d Demo)"
    override val exploreAppsBtn = "Explore Apps"
    override val simTotalLabel = "Simulated Total"
    override val blockedLabel = "Blocked"
    override val allowedLabel = "Allowed"
    override val automationAnalyticsTitle = "Automation Analytics"
    override val executionsLabel = "Executions"
    override val skippedLabel = "Skipped"
    override val conflictsLabel = "Conflicts"
    override val frequentProfilesLabel = "Most Used Profiles"
    override val recommendationsTitle = "Smart Recommendations"
    override val safetySubtitle = "Fail-safe controls, manual overrides, conflict checks, and demo recovery."
    override val globalPauseTitle = "Global Automation Pause"
    override val globalPauseOnSub = "All automatic triggers are paused safely."
    override val globalPauseOffSub = "Pause every automated routine instantly without deleting rules."
    override val manualOverrideTitle = "Manual Override & Rollback"
    override val currentSimStatePrefix = "Current Simulated State"
    override val restorePrevStatePrefix = "Restore Previous State"
    override val conflictWarningsPrefix = "Conflict Warnings"
    override val noConflictsMsg = "No conflicting active routine schedules detected."
    override val testResolveConflictBtn = "Test & Resolve Conflict"
    override val emergencyTestTitle = "Emergency Access Simulation"
    override val emergencyTestSub = "Verify that Emergency callers bypass Study or Sleep Silent Mode."
    override val simulateEmergencyCallPrefix = "Simulate Emergency Call"
    override val recoveryTitle = "Recovery & Demo Reset"
    override val recoverySub = "All recovery actions ask for confirmation first."
    override val resetDemoRoutinesBtn = "Reset All Demo Routines"
    override val resetEntireAppBtn = "Reset Entire Demo Application"
    override val settingsGeneralTitle = "General"
    override val settingsGeneralExplain = "Manage how the app looks, which language it uses, and your activity logs."
    override val settingsSafetyTitle = "Emergency and safety"
    override val settingsSafetyExplain = "Keep emergency callers reachable and pause or undo routine changes anytime."
    override val settingsAutomationTitle = "Automation"
    override val settingsAutomationExplain = "Choose how routines behave by default and what happens when two overlap."
    override val settingsAdvancedTitle = "Advanced"
    override val settingsAdvancedExplain = "Developer simulation tools, demo data resets, and privacy boundaries."
    override val appearanceTitle = "Appearance"
    override val appearanceExplain = "Switch between light mode, dark mode, or follow your device setting."
    override val languageTitle = "Language & Layout Direction"
    override val languageSub = "Change app text between English and Arabic with automatic right-to-left layout."
    override val replayOnboardingExplain = "Revisit the 3-step introduction to see how routines and safety work."
    override val manualOverrideExplain = "Temporarily switch simulated sound mode or undo back to your previous state."
    override val conflictCheckExplain = "Check if any active routines run at the same time and choose a winner."
    override val emergencyTestExplain = "Simulate an incoming call to confirm emergency contacts always ring through."
    override val automationPrefsTitle = "Automation Preferences"
    override val pauseAllAutomationLabel = "Pause All Automation"
    override val confirmImportantLabel = "Confirm Important Actions"
    override val confirmImportantExplain = "Ask before saving or applying major routine changes."
    override val defaultProfileLabel = "Default Profile"
    override val defaultProfileExplain = "Pick the mode your phone returns to when no routine is active."
    override val conflictPolicyLabel = "When routines overlap"
    override val conflictPolicyExplain = "Choose whether the app asks you, picks the higher importance routine, or waits."
    override val privacyTitle = "Privacy & Demo Boundaries"
    override val privacyExplain = "All data in this app is local demo data and never reads your real phone."
    override val privacyBody = "• Uses synthetic demonstration data only.\n• Does not read real notifications.\n• Does not access real contacts.\n• Does not track actual GPS location.\n• Does not change real Android sound settings."
    override val demoMgmtTitle = "Demo Management"
    override val demoMgmtExplain = "Reset demo routines, profiles, or activity back to the starting examples."
    override val resetDemoDataBtn = "Reset Demo Data"
    override val viewOnboardingBtn = "Replay App Guide (Onboarding)"
    override val aboutTitle = "About"
    override val aboutExplain = "App version and current interactive demo edition details."
    override val aboutVersionLine = "Version 1.0.0 • Interactive Offline Demo Edition"
    override val conflictDialogTitle = "Two routines overlap"
    override val conflictDefaultExplain = "Two active routines overlap right now. Which one should win?"
    override val keepCurrentStateBtn = "Keep current state"
    override val playgroundSubtitle = "One-tap interactive walkthroughs using live app state."
}

private object ArabicAppStrings : AppStrings() {
    override val isArabic = true
    override val appName = "المرافق الذكي"
    override val tagline = "ابقَ مركزاً دون أن تفوتك الأمور المهمة."
    override val demoBadge = "عرض / محاكاة"
    override val simulatedChip = "عرض / محاكاة"
    override val appEventChip = "تحديث التطبيق"
    override val navHome = "الرئيسية"
    override val navAssistant = "المساعد"
    override val navRoutines = "الروتين"
    override val navNewRoutine = "روتين جديد"
    override val navContacts = "جهات الطوارئ"
    override val navInsights = "التحليلات"
    override val navSettings = "الإعدادات"
    override val screenProfiles = "الأوضاع الذكية"
    override val screenFocus = "جلسات التركيز"
    override val screenContext = "الوعي بالسياق"
    override val screenNotifications = "الإشعارات"
    override val screenHistory = "السجل"
    override val screenSafety = "الأمان"
    override val screenSettings = "الإعدادات"
    override val screenPlayground = "ساحة التجارب"
    override val screenRuleBuilder = "إنشاء روتين"
    override val screenSimulation = "محرك المحاكاة"
    override val screenActivity = "النشاط"
    override val screenAdvanced = "إعدادات متقدمة"
    override val screenMore = "المزيد"
    override val openMenuDesc = "فتح قائمة المزيد"
    override val backDesc = "رجوع"
    override val featureHelpContentDesc = "شرح هذه الميزة"
    override val helpWhatIsIt = "ما هذه الميزة؟"
    override val helpProblemSolved = "ما المشكلة التي تحلها؟"
    override val helpHowToUse = "كيف تستخدمها؟"
    override val helpWhatHappens = "ماذا يحدث عند تفعيلها؟"
    override val gotItBtn = "فهمت"
    override val moreSubtitle = "أدوات منظمة للتركيز، السجل، الوعي بالسياق، الأوضاع، الأمان، والإعدادات."
    override val catFocus = "التركيز"
    override val catActivity = "النشاط"
    override val catIntelligence = "الذكاء"
    override val catProfiles = "الأوضاع"
    override val catProtection = "الحماية"
    override val catPreferences = "التفضيلات"
    override val descFocusSessions = "جلسات تركيز مؤقتة مرتبطة بوضعك الذكي."
    override val descActivityHistory = "سجل كامل للإجراءات المحاكاة وتحديثات التطبيق."
    override val descNotifications = "تحليل ضوضاء التنبيهات المحاكاة وأوقات الذروة."
    override val descContext = "اختبار الوقت والموقع والنشاط المحاكى بأمان."
    override val descSimulation = "فحص مطابقة القواعد والتتبع وحل التعارضات."
    override val descPlayground = "تجارب تفاعلية جاهزة بضغطة واحدة."
    override val descProfiles = "تخصيص أوضاع الدراسة والعمل والنوم والشخصي."
    override val descSafety = "إيقاف شامل للأتمتة واستعادة الحالة السابقة."
    override val descSettings = "المظهر واللغة العربية/الإنجليزية وإدارة العرض."
    override val skip = "تخطي"
    override val next = "التالي"
    override val back = "السابق"
    override val enterDemoMode = "بدء الوضع التجريبي"
    override val confirm = "تأكيد"
    override val approveBtn = "موافقة"
    override val rejectBtn = "رفض"
    override val cancel = "إلغاء"
    override val edit = "تعديل"
    override val delete = "حذف"
    override val save = "حفظ"
    override val close = "إغلاق"
    override val restore = "استعادة"
    override val duplicate = "تكرار"
    override val activate = "تفعيل"
    override val activeLabel = "نشط"
    override val simulate = "محاكاة"
    override val testAction = "اختبار"
    override val runNow = "تشغيل الآن"
    override val viewDetails = "التفاصيل"
    override val viewAllLabel = "عرض الكل"
    override val moreOptionsDesc = "خيارات إضافية"
    override val moreModules = "استكشاف الأدوات"
    override val showAllModules = "عرض جميع الأدوات"
    override val hideModules = "إخفاء الأدوات"
    override val homeHeadline = "هاتفك يتكيّف مع يومك."
    override val homeSubtitle = "اضبط القاعدة مرة واحدة، مثل 'صامت أثناء الدراسة من 8 إلى 10'، وسيتولى المرافق الذكي المكالمات والإشعارات نيابةً عنك. جهات الطوارئ تصلك دائماً."
    override val homeCardAssistantTitle = "أخبر المساعد بما تريد"
    override val homeCardAssistantSub = "اكتب ما تريده بلغة بسيطة وراجع المقترح قبل الحفظ."
    override val homeCardRoutinesTitle = "روتيناتي"
    override val homeCardContactsTitle = "جهات الطوارئ"
    override val homeCardContactsSubSuffix = "جهات محمية — تصلك دائماً حتى في الصامت"
    override val manageProfilesBtn = "إدارة الأوضاع"
    override val activitySubtitle = "التحليلات وسجل النشاط وذكاء الإشعارات في شاشة واحدة."
    override val advancedSubtitle = "الوعي بالسياق وساحة التجارب ومحرك المحاكاة."
    override val settingsSubtitle = "تخصيص المظهر، أمان الطوارئ، تفضيلات الروتين، والأدوات المتقدمة."
    override val settingsMoreTitle = "المزيد"
    override val descActivityMerged = "اعرض إحصائيات التركيز وسجل النشاط واتجاهات الإشعارات."
    override val descAdvanced = "اختبر السياق المحاكى وسيناريوهات التجربة ومحرك القواعد."
    override val descEmergencyContacts = "اختر الأشخاص الذين يصلونك دائماً حتى لو كان الهاتف صامتاً."
    override val activeProfile = "الوضع الحالي"
    override val switchProfilePrompt = "تبديل الوضع:"
    override val simulatedPhoneStatus = "حالة الهاتف"
    override val simulatedPhoneHint = "حالة عرض تجريبية — لا تعدل صوت هاتفك الحقيقي."
    override val simulatedNotRealOs = "محاكاة"
    override val automationStatus = "حالة الأتمتة"
    override val automationActive = "الأتمتة مفعّلة"
    override val automationPaused = "الأتمتة متوقفة مؤقتاً"
    override val pauseAutomation = "إيقاف مؤقت"
    override val resumeAutomation = "استئناف"
    override val quickActions = "إجراءات سريعة"
    override val todaysSchedule = "ملخص اليوم"
    override val todaysOverviewTitle = "ملخص اليوم"
    override val executedRoutinesLabel = "تم تنفيذها"
    override val importantActivityLabel = "نشاط مهم"
    override val focusSummary = "ملخص التركيز"
    override val recentActivity = "أحدث الأنشطة"
    override val allRoutines = "كل الروتينات"
    override val openTimer = "فتح المؤقت"
    override val fullHistory = "عرض الكل"
    override val demoPlaygroundAction = "سيناريوهات جاهزة"
    override val startFocus25m = "بدء التركيز (25 د)"
    override val pauseAllRules = "إيقاف القواعد مؤقتاً"
    override val resumeAllRules = "استئناف القواعد"
    override val studyQuickTitle = "وضع الدراسة"
    override val studyQuickSub = "صامت + تركيز"
    override val workQuickTitle = "وضع العمل"
    override val workQuickSub = "اهتزاز + أولوية"
    override val sleepQuickTitle = "وضع النوم"
    override val sleepQuickSub = "الطوارئ فقط"
    override val totalFocusLabel = "وقت التركيز"
    override val sessionsLabel = "الجلسات"
    override val filteredLabel = "تم حجبها"
    override val dailyFocusGoal = "هدف التركيز اليومي"
    override val noRoutinesScheduled = "لا توجد روتينات مجدولة حالياً."
    override val scheduledLabel = "مجدول"
    override val disabledLabel = "متوقف"
    override val noRecentActivity = "لم يتم تسجيل أي نشاط محاكى بعد."
    override val activeFocusBannerPaused = "التركيز متوقف مؤقتاً"
    override val activeFocusBannerRunning = "جلسة تركيز نشطة"
    override val distractionsBlockedSuffix = "مقاطعة محاكاة تم حجبها"
    override val routinesPageExplain = "تقوم الروتينات بتبديل الوضع والصوت المحاكى تلقائياً عند تحقق شروطها."
    override val addRoutineBtn = "إضافة روتين"
    override val searchRoutinesHint = "ابحث في الروتينات..."
    override val filterAllRoutines = "الكل"
    override val filterActiveRoutines = "المفعّلة"
    override val filterPausedRoutines = "المتوقفة"
    override val stepWhenTitle = "متى يجب أن يعمل؟"
    override val stepConditionsTitle = "فقط إذا توفرت هذه الشروط"
    override val stepActionsTitle = "افعل هذا تلقائياً"
    override val stepExceptionsTitle = "ما عدا هذه الحالات"
    override val stepReviewTitle = "اسم الروتين وحفظه"
    override val builderRowWhenTitle = "متى"
    override val builderRowOnlyIfTitle = "فقط إذا"
    override val builderRowDoThisTitle = "افعل هذا"
    override val builderRowExceptTitle = "ما عدا"
    override val builderDoneBtn = "تم"
    override val priorityHighLabel = "عالية"
    override val priorityMediumLabel = "متوسطة"
    override val priorityLowLabel = "منخفضة"
    override val advancedRuleOptions = "الأهمية"
    override val alwaysRunNote = "اختياري — اتركه فارغاً للتشغيل بمجرد حدوث المُشغّل."
    override val contactsPriorityExplain = "جهات الطوارئ تصلك دائماً حتى في الوضع الصامت. جهات الأولوية العالية تعتمد على الوضع النشط، والجهات العادية تتبع نمط الصوت الحالي."
    override val emergencyTierTitle = "الطوارئ"
    override val highPriorityTierTitle = "أولوية عالية"
    override val normalTierTitle = "عادي"
    override val toggleInterruptionTester = "اختبر كيف تستجيب جهات الأولوية"
    override val focusChartExplain = "الدقائق المكتملة في أحدث جلسات التركيز المسجلة."
    override val interruptionChartExplain = "تنبيهات محاكاة خلال 7 أيام مصنفة حسب الفئة ووقت الذروة."
    override val profileChartExplain = "الاستخدام الإجمالي للأوضاع من الروتينات وجلسات التركيز والتفعيل الحالي."
    override val onboardingPages = listOf(
        Triple("روتينات تلقائية", "اضبط القاعدة مرة واحدة حسب الوقت أو اليوم أو دخول/مغادرة مكان، وسيتكيّف هاتفك تلقائياً.", "تشمل الأوضاع الذكية وجلسات التركيز."),
        Triple("جهات الطوارئ تصلك دائماً", "الأشخاص الذين تختارهم للطوارئ لا يتأثرون بأي روتين أو وضع صامت إطلاقاً.", "أمانك متاح في كل الأوقات."),
        Triple("المساعد الذكي", "اكتب ما تريده بلغة بسيطة، فيحوّله المساعد إلى روتين مع خيارات موافقة أو تعديل أو رفض.", "لا يتم حفظ أي قاعدة إلا بموافقتك.")
    )
    override val profilesSubtitle = "يحدد كل وضع نمط الصوت المحاكى، فئات الإشعارات المسموحة، واستثناءات جهات الاتصال."
    override val defaultBadge = "افتراضي"
    override val newProfileTitle = "وضع ذكي جديد"
    override val editProfileTitle = "تعديل الوضع"
    override val profileNameLabel = "اسم الوضع *"
    override val profileDescLabel = "ماذا يغيّر هذا الوضع؟"
    override val defaultFocusMinsLabel = "دقائق التركيز الافتراضية"
    override val allowEmergencyAlwaysNote = "مكالمات ورسائل جهات الطوارئ تصلك دائماً في جميع الأوضاع دون استثناء."
    override val allowHighPriorityContacts = "السماح لجهات الأولوية العالية"
    override val allowedCategoriesLabel = "فئات الإشعارات المسموحة"
    override val profileNameError = "يرجى إدخال اسم الوضع."
    override val deleteProfileTitle = "حذف الوضع المخصص؟"
    override val deleteProfileMsg = "هل تريد حذف هذا الوضع المخصص؟ ستبقى الروتينات قابلة للتعديل."
    override val restoreProfilesTitle = "استعادة الأوضاع الافتراضية؟"
    override val restoreProfilesMsg = "يعيد أوضاع الدراسة والعمل والنوم والشخصي والمخصص إلى إعداداتها الأصلية."
    override val restoreDefaultsBtn = "استعادة الافتراضي"
    override val routinesSubtitlePaused = "الإيقاف العام للأتمتة مفعّل — لا يزال التشغيل اليدوي متاحاً."
    override val routinesEnabledSuffix = "روتينات مفعّلة"
    override val noRoutinesTitle = "لا توجد روتينات بعد"
    override val noRoutinesMsg = "اضغط + لإنشاء قاعدة ذكية باستخدام متى والشروط والإجراءات والاستثناءات."
    override val lastResultLabel = "آخر نتيجة"
    override val deleteRoutineTitle = "حذف الروتين؟"
    override val deleteRoutineMsg = "هل أنت متأكد من حذف هذا الروتين؟ لا يمكن التراجع عن هذا الإجراء."
    override val executeNow = "تشغيل الآن"
    override val ruleBuilderSubtitle = "متى ← الشروط ← الإجراءات ← الاستثناءات"
    override val ruleNameLabel = "اسم الروتين *"
    override val ruleNameHint = "مثال: تركيز المكتبة"
    override val ruleDescLabel = "الوصف"
    override val ruleDescHint = "اشرح الهدف من هذا الروتين"
    override val priorityLevelPrefix = "مستوى الأولوية"
    override val whenHeader = "1. متى (المُشغّل)"
    override val whenHint = "اختر الحدث الذي يبدأ هذا الروتين."
    override val conditionsHeader = "2. الشروط (فلاتر اختيارية)"
    override val conditionsHint = "شروط إضافية يجب توفرها قبل التنفيذ."
    override val actionsHeader = "3. الإجراءات *"
    override val actionsHint = "ما ينفذه المرافق الذكي عند تحقق القاعدة."
    override val exceptionsHeader = "4. الاستثناءات"
    override val exceptionsHint = "من أو متى يُسمح بتجاوز هذا الروتين."
    override val optionalFiltersShow = "إظهار الشروط والاستثناءات الاختيارية"
    override val optionalFiltersHide = "إخفاء الشروط والاستثناءات الاختيارية"
    override val targetProfileLabel = "الوضع المستهدف"
    override val focusDurationLabel = "مدة التركيز"
    override val ruleSummaryTitle = "ملخص القاعدة"
    override val testRuleBtn = "اختبار القاعدة"
    override val saveRuleBtn = "حفظ القاعدة"
    override val errRuleNameRequired = "يرجى إدخال اسم الروتين."
    override val errRuleActionRequired = "اختر إجراءً واحداً على الأقل قبل المتابعة."
    override val simSubtitle = "اختبر المشغلات والشروط وتعارض الأولويات والمقاطعات على حالة التطبيق الحالية."
    override val simClockTitle = "1. محاكاة الوقت واليوم"
    override val simEvaluateClockBtn = "تقييم مشغّل الوقت"
    override val simLocActTitle = "2. محاكاة الموقع والنشاط"
    override val simEnterLocBtn = "دخول الموقع"
    override val simExitLocBtn = "مغادرة الموقع"
    override val simStartActBtn = "محاكاة بدء النشاط"
    override val simInterruptionTitle = "3. محاكاة مكالمة أو رسالة واردة"
    override val simTraceTitle = "سجل تنفيذ محرك المحاكاة الأخير"
    override val simTraceEmpty = "شغّل أي محاكاة أعلاه لعرض خطوات التقييم التفصيلية."
    override val simTraceTrigger = "المُشغّل المستلم"
    override val simTraceMatched = "الروتينات المطابقة"
    override val simTraceConditions = "الشروط التي تم فحصها"
    override val simTraceExceptions = "الاستثناءات المطبّقة"
    override val simTraceActions = "الإجراءات المختارة"
    override val simTraceFinal = "النتيجة النهائية المحاكاة"
    override val contactsSubtitle = "جهات الطوارئ لا تتأثر بأي روتين أو وضع صامت (جهات تجريبية)."
    override val interruptionTesterTitle = "اختبار المقاطعات الواردة"
    override val repeatCallerLabel = "محاكاة متصل متكرر (مكالمة ثانية خلال 3 دقائق)"
    override val testCallBtn = "اختبار مكالمة"
    override val testMsgBtn = "اختبار رسالة"
    override val allowedUpper = "مسموح"
    override val blockedUpper = "محجوب"
    override val addContactTitle = "إضافة جهة اتصال تجريبية"
    override val editContactTitle = "تعديل جهة الاتصال"
    override val contactNameLabel = "الاسم *"
    override val contactRelLabel = "صفة العلاقة"
    override val contactPriorityLabel = "مستوى الأولوية"
    override val contactTypesLabel = "أنواع المقاطعة المسموحة"
    override val allowRepeatCallers = "السماح للمتصل المتكرر"
    override val contactNameError = "يرجى إدخال اسم جهة الاتصال."
    override val removeContactTitle = "إزالة جهة الاتصال؟"
    override val removeContactMsg = "هل تريد إزالة جهة الاتصال من قائمة الأولوية؟"
    override val removeBtn = "إزالة"
    override val notifSubtitle = "بيانات تجريبية محاكاة — لا يقرأ التطبيق إشعارات النظام الحقيقية."
    override val simulatedAlertsLabel = "تنبيهات محاكاة"
    override val importantLabel = "مهمة"
    override val nonImportantLabel = "غير مهمة"
    override val notifByAppTitle = "الإشعارات حسب التطبيق (تجريبي)"
    override val notifByCategoryTitle = "توزيع الفئات (تجريبي)"
    override val notifSuggestionsTitle = "اقتراحات لتحسين التركيز"
    override val contextSubtitle = "إشارات بيئية محاكاة — لا يستخدم التطبيق أذونات الموقع الحقيقي."
    override val currentContextTitle = "السياق المحاكى الحالي"
    override val timeContextTitle = "سياق الوقت"
    override val simLocationTitle = "الموقع المحاكى"
    override val simActivityTitle = "النشاط المحاكى"
    override val matchingRoutinesTitle = "الروتينات المطابقة للسياق الحالي"
    override val noMatchingContextRoutines = "لا توجد روتينات مطابقة تماماً لهذه اللقطة الزمنية والسياقية."
    override val testContextBtn = "اختبار قرار الأتمتة للسياق"
    override val assistantHeaderTitle = "المساعد الذكي"
    override val assistantHeaderSub = "اكتب ما تريده — وافق على الروتين أو عدّله أو ارفضه."
    override val assistantEmptyGuide = "يمكنني إنشاء روتينات ذكية من أوامرك، بدء جلسة تركيز، أو عرض نشاطك اليومي المحاكى. سأطلب تأكيدك قبل حفظ أي روتين."
    override val assistantInputHint = "اكتب سؤالك أو صِف الروتين المطلوب..."
    override val assistantThinking = "جاري التفكير..."
    override val assistantSuggestedPrompts = listOf(
        "اجعل هاتفي صامتاً أثناء الدراسة 8 إلى 10 مساءً واسمح لعائلتي بالاتصال",
        "اهتزاز أثناء ساعات العمل أيام الأسبوع 9 ص إلى 5 م",
        "كتم الإشعارات عند دخولي المكتبة",
        "روتين تركيز 45 دقيقة كل مساء الساعة 7"
    )
    override val focusSubtitle = "تابع الوقت المنقضي والمتبقي لتركيزك مع ربط تلقائي بالأوضاع الذكية."
    override val focusPausedBadge = "الجلسة متوقفة مؤقتاً"
    override val focusActiveBadge = "جلسة التركيز نشطة"
    override val remainingLabel = "المتبقي"
    override val elapsedLabel = "المنقضي"
    override val configureFocusTitle = "إعداد جلسة تركيز جديدة"
    override val durationLabel = "المدة"
    override val customLabel = "مخصص"
    override val customMinutesLabel = "دقائق مخصصة (5–240)"
    override val focusProfileLabel = "وضع التركيز"
    override val autoActivateProfile = "تفعيل الوضع المختار تلقائياً عند البدء"
    override val startFocusBtn = "بدء جلسة التركيز"
    override val playBtn = "تشغيل"
    override val pauseBtn = "إيقاف مؤقت"
    override val resumeBtn = "استئناف"
    override val resetBtn = "إعادة ضبط"
    override val endBtn = "إنهاء"
    override val simBlockedDistractionBtn = "محاكاة حجب إشعار مزعج"
    override val sessionSummaryTitle = "ملخص الجلسة"
    override val sessionHistoryTitle = "سجل الجلسات"
    override val historySubtitle = "تسلسل زمني للإجراءات المحاكاة وتحديثات حالة التطبيق."
    override val historyLegend = "محاكاة = حدث هاتف/مشغل محاكى • حالة التطبيق = تعديل داخلي"
    override val searchHistoryHint = "ابحث في الأحداث أو الروتينات أو الأوضاع..."
    override val allLabel = "الكل"
    override val noHistoryTitle = "لا توجد أحداث مطابقة"
    override val noHistoryMsg = "بدّل وضعاً أو شغّل روتيناً أو ابدأ جلسة تركيز لتظهر الأحداث هنا."
    override val clearHistoryTitle = "مسح سجل النشاط؟"
    override val clearHistoryMsg = "سيتم حذف جميع الأحداث المسجلة في هذا السجل."
    override val clearHistoryBtn = "مسح السجل"
    override val insightsSubtitle = "مؤشرات محسوبة من جلسات التركيز الفعلية والروتينات والبيانات التجريبية."
    override val focusAnalyticsTitle = "تحليلات التركيز"
    override val dailyAvgLabel = "المتوسط اليومي"
    override val focusTrendTitle = "أحدث جلسات التركيز"
    override val interruptionAnalyticsTitle = "تحليلات المقاطعات (7 أيام تجريبية)"
    override val exploreAppsBtn = "التطبيقات"
    override val simTotalLabel = "الإجمالي المحاكى"
    override val blockedLabel = "تم حجبه"
    override val allowedLabel = "مسموح"
    override val automationAnalyticsTitle = "تحليلات الأتمتة"
    override val executionsLabel = "مرات التنفيذ"
    override val skippedLabel = "تم تخطيها"
    override val conflictsLabel = "تعارضات"
    override val frequentProfilesLabel = "الأوضاع الأكثر استخداماً"
    override val recommendationsTitle = "توصيات ذكية"
    override val safetySubtitle = "ضوابط أمان فورية، تحكم يدوي، فحص التعارضات، واسترداد الحالة."
    override val globalPauseTitle = "الإيقاف الشامل للأتمتة"
    override val globalPauseOnSub = "جميع المشغلات التلقائية متوقفة بأمان حالياً."
    override val globalPauseOffSub = "أوقف جميع الروتينات التلقائية فوراً دون حذف القواعد."
    override val manualOverrideTitle = "التحكم اليدوي واستعادة الحالة"
    override val currentSimStatePrefix = "الحالة المحاكاة الحالية"
    override val restorePrevStatePrefix = "استعادة الحالة السابقة"
    override val conflictWarningsPrefix = "تنبيهات التعارض"
    override val noConflictsMsg = "لا توجد تعارضات حالية بين الجداول المفعّلة."
    override val testResolveConflictBtn = "اختبار وحل التعارض"
    override val emergencyTestTitle = "محاكاة الوصول الطارئ"
    override val emergencyTestSub = "تأكد من وصول مكالمات الطوارئ حتى أثناء صامت الدراسة أو النوم."
    override val simulateEmergencyCallPrefix = "محاكاة مكالمة طوارئ"
    override val recoveryTitle = "الاسترداد وإعادة ضبط العرض"
    override val recoverySub = "تطلب جميع عمليات الاسترداد تأكيداً صريحاً قبل التنفيذ."
    override val resetDemoRoutinesBtn = "إعادة ضبط روتينات العرض"
    override val resetEntireAppBtn = "إعادة ضبط التطبيق بالكامل"
    override val settingsGeneralTitle = "عام"
    override val settingsGeneralExplain = "التحكم في المظهر واللغة وسجل النشاط اليومي."
    override val settingsSafetyTitle = "الطوارئ والأمان"
    override val settingsSafetyExplain = "إبقاء جهات الطوارئ متاحة وإيقاف الأتمتة أو استعادة الحالة في أي وقت."
    override val settingsAutomationTitle = "الأتمتة"
    override val settingsAutomationExplain = "اختر كيف تتصرف الروتينات افتراضياً وماذا يحدث عند تعارضها."
    override val settingsAdvancedTitle = "متقدم"
    override val settingsAdvancedExplain = "أدوات المحاكاة وإعادة ضبط البيانات التجريبية وحدود الخصوصية."
    override val appearanceTitle = "المظهر"
    override val appearanceExplain = "التبديل بين الوضع الفاتح والداكن أو اتباع إعدادات النظام."
    override val languageTitle = "اللغة واتجاه الواجهة"
    override val languageSub = "تغيير لغة التطبيق بين الإنجليزية والعربية مع تفعيل اتجاه RTL تلقائياً."
    override val replayOnboardingExplain = "إعادة عرض الدليل التعريفي المكون من 3 خطوات لمعرفة طريقة عمل التطبيق."
    override val manualOverrideExplain = "تبديل وضع الصوت المحاكى مؤقتاً أو التراجع إلى الحالة السابقة."
    override val conflictCheckExplain = "تحقق مما إذا كانت هناك روتينات متداخلة واختر الأفضل."
    override val emergencyTestExplain = "جرّب مكالمة واردة للتأكد من وصول جهات الطوارئ دائماً."
    override val automationPrefsTitle = "تفضيلات الأتمتة"
    override val pauseAllAutomationLabel = "إيقاف الأتمتة مؤقتاً"
    override val confirmImportantLabel = "تأكيد الإجراءات المهمة"
    override val confirmImportantExplain = "اطلب التأكيد قبل حفظ أو تطبيق التغييرات الرئيسية."
    override val defaultProfileLabel = "الوضع الافتراضي"
    override val defaultProfileExplain = "اختر الوضع الذي يعود إليه الهاتف عند عدم تشغيل أي روتين."
    override val conflictPolicyLabel = "عند تعارض الروتينات"
    override val conflictPolicyExplain = "حدد ما إذا كان التطبيق يسألك أو يختار الأعلى أهمية أو ينتظر."
    override val privacyTitle = "الخصوصية ونطاق المحاكاة"
    override val privacyExplain = "جميع البيانات هنا محاكاة محلية ولا تصل أبداً إلى بيانات هاتفك الحقيقية."
    override val privacyBody = "• يستخدم بيانات تجريبية افتراضية فقط.\n• لا يقرأ الإشعارات الحقيقية.\n• لا يصل إلى جهات الاتصال الحقيقية.\n• لا يتتبع الموقع الجغرافي الفعلي.\n• لا يغيّر إعدادات الصوت الحقيقية في أندرويد."
    override val demoMgmtTitle = "إدارة العرض التجريبي"
    override val demoMgmtExplain = "استعد الروتينات أو الأوضاع أو السجل إلى أمثلة البداية."
    override val resetDemoDataBtn = "إعادة ضبط البيانات"
    override val viewOnboardingBtn = "عرض دليل التطبيق التعريفي"
    override val aboutTitle = "حول التطبيق"
    override val aboutExplain = "معلومات الإصدار الحالي ونسخة العرض التفاعلية."
    override val aboutVersionLine = "الإصدار 1.0.0 • نسخة عرض تفاعلية بدون إنترنت"
    override val conflictDialogTitle = "تعارض بين روتينين"
    override val conflictDefaultExplain = "يتعارض روتينان نشطان الآن. أيهما يجب أن يربح؟"
    override val keepCurrentStateBtn = "الإبقاء على الحالة الحالية"
    override val playgroundSubtitle = "تجارب تفاعلية بضغطة واحدة متصلة بحالة التطبيق المباشرة."
}

package com.example.ui.navigation

object NavRoutes {
    const val ONBOARDING = "onboarding"

    // 4 Primary Bottom Bar Tabs
    const val HOME = "home"
    const val ROUTINES = "routines"
    const val ASSISTANT = "assistant"
    const val SETTINGS = "settings"

    // Secondary Screens (all reachable within <= 2 taps)
    const val CONTACTS = HOME
    const val ACTIVITY = "activity"
    const val ADVANCED = "advanced"
    const val PROFILES = "profiles"
    const val FOCUS = "focus"
    const val SIMULATION = "simulation"
    const val RULE_BUILDER = "rule_builder"
    const val ROUTINE_LOG = "routine_log"
    const val NOTIFICATION_ACCESS = "notification_access"

    fun ruleBuilderRoute(routineId: String? = null): String {
        return if (routineId.isNullOrBlank()) RULE_BUILDER else "$RULE_BUILDER?routineId=$routineId"
    }
}

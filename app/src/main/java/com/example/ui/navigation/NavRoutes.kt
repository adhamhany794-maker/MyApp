package com.example.ui.navigation

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ASSISTANT = "assistant"
    const val ROUTINES = "routines"
    const val CONTACTS = "contacts"
    const val INSIGHTS = "insights"

    // Secondary More Hub & Modules
    const val MORE = "more"
    const val PROFILES = "profiles"
    const val FOCUS = "focus"
    const val CONTEXT = "context"
    const val NOTIFICATIONS = "notifications"
    const val HISTORY = "history"
    const val SAFETY = "safety"
    const val SETTINGS = "settings"
    const val PLAYGROUND = "playground"
    const val SIMULATION = "simulation"
    const val RULE_BUILDER = "rule_builder"

    fun ruleBuilderRoute(routineId: String? = null): String {
        return if (routineId.isNullOrBlank()) RULE_BUILDER else "$RULE_BUILDER?routineId=$routineId"
    }
}

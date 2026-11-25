package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.models.AppLanguage
import com.example.models.ConflictResolutionPolicy
import com.example.models.DayFilter
import com.example.models.PhoneSnapshot
import com.example.models.SimulatedActivity
import com.example.models.SimulatedLocation
import com.example.models.SimulationContextState
import com.example.models.SoundMode
import com.example.models.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_companion_prefs")

data class AppPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val activeProfileId: String = "profile_personal",
    val simulatedSoundMode: SoundMode = SoundMode.SOUND,
    val isAutomationPaused: Boolean = false,
    val requireConfirmationForImportantActions: Boolean = true,
    val defaultProfileId: String = "profile_personal",
    val conflictPolicy: ConflictResolutionPolicy = ConflictResolutionPolicy.ASK_USER,
    val dailyFocusGoalMinutes: Int = 120,
    val previousSnapshot: PhoneSnapshot = PhoneSnapshot(
        activeProfileId = "profile_personal",
        soundMode = SoundMode.SOUND,
        isAutomationPaused = false
    ),
    val simulatedContext: SimulationContextState = SimulationContextState()
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val SIMULATED_SOUND_MODE = stringPreferencesKey("simulated_sound_mode")
        val IS_AUTOMATION_PAUSED = booleanPreferencesKey("is_automation_paused")
        val REQUIRE_CONFIRMATION = booleanPreferencesKey("require_confirmation")
        val DEFAULT_PROFILE_ID = stringPreferencesKey("default_profile_id")
        val CONFLICT_POLICY = stringPreferencesKey("conflict_policy")
        val DAILY_FOCUS_GOAL = intPreferencesKey("daily_focus_goal_minutes")

        val PREV_PROFILE_ID = stringPreferencesKey("prev_profile_id")
        val PREV_SOUND_MODE = stringPreferencesKey("prev_sound_mode")
        val PREV_AUTOMATION_PAUSED = booleanPreferencesKey("prev_automation_paused")

        val SIM_HOUR = intPreferencesKey("sim_hour")
        val SIM_MINUTE = intPreferencesKey("sim_minute")
        val SIM_DAY = stringPreferencesKey("sim_day")
        val SIM_LOCATION = stringPreferencesKey("sim_location")
        val SIM_CUSTOM_LOCATION = stringPreferencesKey("sim_custom_location")
        val SIM_ACTIVITY = stringPreferencesKey("sim_activity")
        val SIM_CUSTOM_ACTIVITY = stringPreferencesKey("sim_custom_activity")
        val SIM_REPEAT_CALLER = booleanPreferencesKey("sim_repeat_caller")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            themeMode = enumOrDefault(prefs[Keys.THEME_MODE], ThemeMode.DARK),
            language = enumOrDefault(prefs[Keys.LANGUAGE], AppLanguage.ENGLISH),
            activeProfileId = prefs[Keys.ACTIVE_PROFILE_ID] ?: "profile_personal",
            simulatedSoundMode = enumOrDefault(prefs[Keys.SIMULATED_SOUND_MODE], SoundMode.SOUND),
            isAutomationPaused = prefs[Keys.IS_AUTOMATION_PAUSED] ?: false,
            requireConfirmationForImportantActions = prefs[Keys.REQUIRE_CONFIRMATION] ?: true,
            defaultProfileId = prefs[Keys.DEFAULT_PROFILE_ID] ?: "profile_personal",
            conflictPolicy = enumOrDefault(prefs[Keys.CONFLICT_POLICY], ConflictResolutionPolicy.ASK_USER),
            dailyFocusGoalMinutes = prefs[Keys.DAILY_FOCUS_GOAL] ?: 120,
            previousSnapshot = PhoneSnapshot(
                activeProfileId = prefs[Keys.PREV_PROFILE_ID] ?: "profile_personal",
                soundMode = enumOrDefault(prefs[Keys.PREV_SOUND_MODE], SoundMode.SOUND),
                isAutomationPaused = prefs[Keys.PREV_AUTOMATION_PAUSED] ?: false
            ),
            simulatedContext = SimulationContextState(
                simulatedHour = prefs[Keys.SIM_HOUR] ?: 18,
                simulatedMinute = prefs[Keys.SIM_MINUTE] ?: 0,
                simulatedDay = enumOrDefault(prefs[Keys.SIM_DAY], DayFilter.MONDAY),
                location = enumOrDefault(prefs[Keys.SIM_LOCATION], SimulatedLocation.HOME),
                customLocationName = prefs[Keys.SIM_CUSTOM_LOCATION] ?: "Downtown Study Cafe",
                activity = enumOrDefault(prefs[Keys.SIM_ACTIVITY], SimulatedActivity.PERSONAL_TIME),
                customActivityName = prefs[Keys.SIM_CUSTOM_ACTIVITY] ?: "Reading",
                repeatCallerDetected = prefs[Keys.SIM_REPEAT_CALLER] ?: false
            )
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = completed }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun savePhoneState(
        activeProfileId: String,
        soundMode: SoundMode,
        isAutomationPaused: Boolean,
        recordPreviousSnapshot: Boolean = true
    ) {
        context.dataStore.edit { prefs ->
            if (recordPreviousSnapshot) {
                prefs[Keys.PREV_PROFILE_ID] = prefs[Keys.ACTIVE_PROFILE_ID] ?: "profile_personal"
                prefs[Keys.PREV_SOUND_MODE] = prefs[Keys.SIMULATED_SOUND_MODE] ?: SoundMode.SOUND.name
                prefs[Keys.PREV_AUTOMATION_PAUSED] = prefs[Keys.IS_AUTOMATION_PAUSED] ?: false
            }
            prefs[Keys.ACTIVE_PROFILE_ID] = activeProfileId
            prefs[Keys.SIMULATED_SOUND_MODE] = soundMode.name
            prefs[Keys.IS_AUTOMATION_PAUSED] = isAutomationPaused
        }
    }

    suspend fun setRequireConfirmation(require: Boolean) {
        context.dataStore.edit { it[Keys.REQUIRE_CONFIRMATION] = require }
    }

    suspend fun setDefaultProfileId(profileId: String) {
        context.dataStore.edit { it[Keys.DEFAULT_PROFILE_ID] = profileId }
    }

    suspend fun setConflictPolicy(policy: ConflictResolutionPolicy) {
        context.dataStore.edit { it[Keys.CONFLICT_POLICY] = policy.name }
    }

    suspend fun setSimulatedContext(ctx: SimulationContextState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SIM_HOUR] = ctx.simulatedHour
            prefs[Keys.SIM_MINUTE] = ctx.simulatedMinute
            prefs[Keys.SIM_DAY] = ctx.simulatedDay.name
            prefs[Keys.SIM_LOCATION] = ctx.location.name
            prefs[Keys.SIM_CUSTOM_LOCATION] = ctx.customLocationName
            prefs[Keys.SIM_ACTIVITY] = ctx.activity.name
            prefs[Keys.SIM_CUSTOM_ACTIVITY] = ctx.customActivityName
            prefs[Keys.SIM_REPEAT_CALLER] = ctx.repeatCallerDetected
        }
    }

    suspend fun resetDefaults(preserveOnboarding: Boolean = true) {
        context.dataStore.edit { prefs ->
            val onboarded = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: true
            prefs.clear()
            if (preserveOnboarding) {
                prefs[Keys.HAS_COMPLETED_ONBOARDING] = onboarded
            }
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}

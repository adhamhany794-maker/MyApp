package com.example.repositories

import com.example.data.AppPreferences
import com.example.data.DemoSeedData
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.data.local.EntityMappers.toDomain
import com.example.data.local.EntityMappers.toEntity
import com.example.models.ActivityEvent
import com.example.models.AppLanguage
import com.example.models.ConflictResolutionPolicy
import com.example.models.EventCategory
import com.example.models.FocusSessionRecord
import com.example.models.NotificationStat
import com.example.models.PriorityContact
import com.example.models.RoutineLogEntry
import com.example.models.SimulationContextState
import com.example.models.SmartProfile
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SmartCompanionRepository(
    private val db: AppDatabase,
    private val prefs: PreferencesManager
) {
    val preferences: Flow<AppPreferences> = prefs.preferencesFlow

    val profiles: Flow<List<SmartProfile>> =
        db.profileDao().observeAll().map { list -> list.map { it.toDomain() } }

    val routines: Flow<List<SmartRoutine>> =
        db.routineDao().observeAll().map { list -> list.map { it.toDomain() } }

    val contacts: Flow<List<PriorityContact>> =
        db.contactDao().observeAll().map { list -> list.map { it.toDomain() } }

    val activityEvents: Flow<List<ActivityEvent>> =
        db.activityEventDao().observeAll().map { list -> list.map { it.toDomain() } }

    val focusSessions: Flow<List<FocusSessionRecord>> =
        db.focusSessionDao().observeAll().map { list -> list.map { it.toDomain() } }

    val notificationStats: Flow<List<NotificationStat>> =
        db.notificationStatDao().observeAll().map { list -> list.map { it.toDomain() } }

    val routineLogs: Flow<List<RoutineLogEntry>> =
        db.routineLogDao().observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAllRoutinesOnce(): List<SmartRoutine> =
        db.routineDao().getAllOnce().map { it.toDomain() }

    suspend fun getRoutineById(id: String): SmartRoutine? =
        db.routineDao().getById(id)?.toDomain()

    suspend fun getAllProfilesOnce(): List<SmartProfile> =
        db.profileDao().getAllOnce().map { it.toDomain() }

    suspend fun ensureSeeded() {
        if (db.profileDao().count() == 0) {
            db.profileDao().upsertAll(DemoSeedData.defaultProfiles().map { it.toEntity() })
        }
        if (db.routineDao().count() == 0) {
            db.routineDao().upsertAll(DemoSeedData.defaultRoutines().map { it.toEntity() })
        }
        if (db.contactDao().count() == 0) {
            db.contactDao().upsertAll(DemoSeedData.defaultContacts().map { it.toEntity() })
        }
        if (db.notificationStatDao().count() == 0) {
            db.notificationStatDao().upsertAll(DemoSeedData.defaultNotificationStats().map { it.toEntity() })
        }
        if (db.focusSessionDao().count() == 0) {
            db.focusSessionDao().upsertAll(DemoSeedData.defaultFocusSessions().map { it.toEntity() })
        }
        if (db.activityEventDao().count() == 0) {
            db.activityEventDao().upsertAll(DemoSeedData.defaultActivityEvents().map { it.toEntity() })
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = prefs.setOnboardingCompleted(completed)
    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)
    suspend fun setLanguage(language: AppLanguage) = prefs.setLanguage(language)
    suspend fun setRequireConfirmation(require: Boolean) = prefs.setRequireConfirmation(require)
    suspend fun setDefaultProfileId(profileId: String) = prefs.setDefaultProfileId(profileId)
    suspend fun setConflictPolicy(policy: ConflictResolutionPolicy) = prefs.setConflictPolicy(policy)
    suspend fun setSimulatedContext(ctx: SimulationContextState) = prefs.setSimulatedContext(ctx)

    suspend fun savePhoneState(
        activeProfileId: String,
        soundMode: SoundMode,
        isAutomationPaused: Boolean,
        recordPreviousSnapshot: Boolean = true
    ) {
        prefs.savePhoneState(activeProfileId, soundMode, isAutomationPaused, recordPreviousSnapshot)
    }

    suspend fun upsertProfile(profile: SmartProfile) {
        db.profileDao().upsert(profile.toEntity())
    }

    suspend fun deleteProfile(profileId: String) {
        db.profileDao().deleteById(profileId)
    }

    suspend fun restoreDefaultProfiles() {
        db.profileDao().upsertAll(DemoSeedData.defaultProfiles().map { it.toEntity() })
    }

    suspend fun upsertRoutine(routine: SmartRoutine) {
        db.routineDao().upsert(routine.toEntity())
    }

    suspend fun deleteRoutine(routineId: String) {
        db.routineDao().deleteById(routineId)
    }

    suspend fun resetDemoRoutines() {
        db.routineDao().clearAll()
        db.routineDao().upsertAll(DemoSeedData.defaultRoutines().map { it.toEntity() })
    }

    suspend fun upsertContact(contact: PriorityContact) {
        db.contactDao().upsert(contact.toEntity())
    }

    suspend fun deleteContact(contactId: String) {
        db.contactDao().deleteById(contactId)
    }

    suspend fun addFocusSession(record: FocusSessionRecord) {
        db.focusSessionDao().insert(record.toEntity())
    }

    suspend fun logEvent(
        title: String,
        category: EventCategory,
        triggerSource: String,
        result: String,
        explanation: String,
        relatedRoutineName: String? = null,
        relatedProfileName: String? = null
    ) {
        val event = ActivityEvent(
            id = "ev_${UUID.randomUUID().toString().take(8)}",
            title = title,
            timestampMillis = System.currentTimeMillis(),
            category = category,
            triggerSource = triggerSource,
            result = result,
            explanation = explanation,
            relatedRoutineName = relatedRoutineName,
            relatedProfileName = relatedProfileName
        )
        db.activityEventDao().insert(event.toEntity())
    }

    suspend fun clearActivityHistory() {
        db.activityEventDao().clearAll()
    }

    suspend fun addRoutineLog(entry: RoutineLogEntry) {
        db.routineLogDao().insert(entry.toEntity())
    }

    suspend fun clearRoutineLogs() {
        db.routineLogDao().clearAll()
    }

    suspend fun resetAllDemoData(resetOnboarding: Boolean = false) {
        db.profileDao().clearAll()
        db.routineDao().clearAll()
        db.contactDao().clearAll()
        db.activityEventDao().clearAll()
        db.focusSessionDao().clearAll()
        db.notificationStatDao().clearAll()
        db.routineLogDao().clearAll()

        db.profileDao().upsertAll(DemoSeedData.defaultProfiles().map { it.toEntity() })
        db.routineDao().upsertAll(DemoSeedData.defaultRoutines().map { it.toEntity() })
        db.contactDao().upsertAll(DemoSeedData.defaultContacts().map { it.toEntity() })
        db.notificationStatDao().upsertAll(DemoSeedData.defaultNotificationStats().map { it.toEntity() })
        db.focusSessionDao().upsertAll(DemoSeedData.defaultFocusSessions().map { it.toEntity() })
        db.activityEventDao().upsertAll(DemoSeedData.defaultActivityEvents().map { it.toEntity() })

        prefs.resetDefaults(preserveOnboarding = !resetOnboarding)
    }
}

package com.example.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.models.AppLanguage
import com.example.models.ContactPriority
import com.example.models.EventCategory
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.AndroidPhoneControlGateway
import com.example.simulation.PhoneControlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RoutineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getStringExtra(AndroidRoutineAlarmScheduler.EXTRA_ROUTINE_ID) ?: return
        val eventType = intent.getStringExtra(AndroidRoutineAlarmScheduler.EXTRA_EVENT_TYPE) ?: return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleAlarmEvent(appContext, routineId, eventType)
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal suspend fun handleAlarmEvent(
        context: Context,
        routineId: String,
        eventType: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val db = AppDatabase.getInstance(context)
        val prefs = PreferencesManager(context)
        val repository = SmartCompanionRepository(db, prefs)

        val appPrefs = repository.preferences.first()
        val isArabic = appPrefs.language == AppLanguage.ARABIC
        val routine = repository.getRoutineById(routineId)
        val profiles = repository.getAllProfilesOnce()
        val contacts = repository.getAllContactsOnce()
        val hasEmergencyContacts = contacts.any { it.priorityLevel == ContactPriority.EMERGENCY }

        val phoneControlManager = PhoneControlManager(AndroidPhoneControlGateway(context))
        val backupStore = SharedPreferencesRoutineStateBackupStore(context)
        val controller = RoutinePhoneController(phoneControlManager, backupStore)
        val scheduler = AndroidRoutineAlarmScheduler(context)

        when (eventType) {
            AndroidRoutineAlarmScheduler.EVENT_START -> {
                if (routine != null && routine.isEnabled && !appPrefs.isAutomationPaused) {
                    val outcome = controller.applyRoutineStart(
                        routine = routine,
                        profiles = profiles,
                        isArabic = isArabic,
                        timestampMillis = nowMillis,
                        hasEmergencyContacts = hasEmergencyContacts
                    )
                    repository.addRoutineLog(outcome.logEntry)
                    repository.upsertRoutine(
                        routine.copy(
                            executionCount = routine.executionCount + 1,
                            lastExecutionResult = outcome.logEntry.note,
                            lastExecutedAtMillis = nowMillis
                        )
                    )
                    repository.logEvent(
                        title = if (isArabic) "بدء روتين: ${routine.name}" else "Routine Started: ${routine.name}",
                        category = EventCategory.ROUTINE,
                        triggerSource = "Exact Alarm",
                        result = outcome.logEntry.note,
                        explanation = outcome.logEntry.note,
                        relatedRoutineName = routine.name
                    )
                    if (outcome.permissionRequired) {
                        RoutineNotificationHelper.showDndPermissionRequiredNotification(
                            context = context,
                            routineName = routine.name,
                            isArabic = isArabic
                        )
                    }
                }
            }

            AndroidRoutineAlarmScheduler.EVENT_END -> {
                val routineName = routine?.name ?: routineId
                val outcome = controller.applyRoutineEnd(
                    routineId = routineId,
                    routineName = routineName,
                    isArabic = isArabic,
                    timestampMillis = nowMillis
                )
                repository.addRoutineLog(outcome.logEntry)
                if (routine != null) {
                    val updated = if (routine.isOneTime) {
                        routine.copy(
                            isEnabled = false,
                            lastExecutionResult = outcome.logEntry.note,
                            lastExecutedAtMillis = nowMillis
                        )
                    } else {
                        routine.copy(
                            lastExecutionResult = outcome.logEntry.note,
                            lastExecutedAtMillis = nowMillis
                        )
                    }
                    repository.upsertRoutine(updated)
                }
                repository.logEvent(
                    title = if (isArabic) "انتهاء روتين: $routineName" else "Routine Ended: $routineName",
                    category = EventCategory.ROUTINE,
                    triggerSource = "Exact Alarm",
                    result = outcome.logEntry.note,
                    explanation = outcome.logEntry.note,
                    relatedRoutineName = routineName
                )
                if (outcome.permissionRequired) {
                    RoutineNotificationHelper.showDndPermissionRequiredNotification(
                        context = context,
                        routineName = routineName,
                        isArabic = isArabic
                    )
                }
            }
        }

        // Schedule next upcoming start/end events for all routines
        val latestRoutines = repository.getAllRoutinesOnce()
        scheduler.rescheduleAll(latestRoutines, nowMillis = nowMillis + 1_000L)
    }
}

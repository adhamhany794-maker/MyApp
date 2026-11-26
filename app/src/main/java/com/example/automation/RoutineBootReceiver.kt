package com.example.automation

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.repositories.SmartCompanionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RoutineBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val supported = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        if (action !in supported) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                val prefs = PreferencesManager(appContext)
                val repository = SmartCompanionRepository(db, prefs)
                val routines = repository.getAllRoutinesOnce()
                AndroidRoutineAlarmScheduler(appContext).rescheduleAll(routines)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

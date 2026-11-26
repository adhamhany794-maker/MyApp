package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.assistant.GeminiAssistantEngine
import com.example.automation.AndroidRoutineAlarmScheduler
import com.example.automation.RoutineNotificationHelper
import com.example.automation.SharedPreferencesRoutineStateBackupStore
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.AndroidPhoneControlGateway
import com.example.simulation.PhoneControlManager
import com.example.ui.SmartCompanionApp
import com.example.viewmodels.SmartCompanionViewModel

class MainActivity : ComponentActivity() {
    private var appViewModel: SmartCompanionViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check before any UI, ViewModel, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val prefs = PreferencesManager(applicationContext)
        val repository = SmartCompanionRepository(db, prefs)
        val assistantEngine = GeminiAssistantEngine(applicationContext)
        val phoneControlManager = PhoneControlManager(AndroidPhoneControlGateway(applicationContext))
        val backupStore = SharedPreferencesRoutineStateBackupStore(applicationContext)
        val alarmScheduler = AndroidRoutineAlarmScheduler(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            SmartCompanionViewModel.Factory(
                repository = repository,
                assistantEngine = assistantEngine,
                phoneControlManager = phoneControlManager,
                routineBackupStore = backupStore,
                alarmScheduler = alarmScheduler,
                onDndPermissionMissingNotification = { routineName, isArabic ->
                    RoutineNotificationHelper.showDndPermissionRequiredNotification(
                        context = applicationContext,
                        routineName = routineName,
                        isArabic = isArabic
                    )
                }
            )
        )[SmartCompanionViewModel::class.java]
        appViewModel = viewModel

        setContent {
            SmartCompanionApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        appViewModel?.refreshPhoneControlState()
    }
}

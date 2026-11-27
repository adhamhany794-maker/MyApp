package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.automation.EmergencyBypassController
import com.example.automation.EmergencyPhoneNumberUtils
import com.example.automation.InMemoryRoutineStateBackupStore
import com.example.automation.NoOpRoutineAlarmScheduler
import com.example.automation.PhoneContactPickerResolver
import com.example.automation.RoutinePhoneController
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.models.ActionType
import com.example.models.AppLanguage
import com.example.models.ContactPriority
import com.example.models.DayFilter
import com.example.models.InterruptionType
import com.example.models.RuleAction
import com.example.models.RuleTrigger
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TriggerType
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.PhoneControlGateway
import com.example.simulation.PhoneControlManager
import com.example.viewmodels.SaveContactResult
import com.example.viewmodels.SmartCompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmergencyContactsBehaviorTest {

    private class FakePhoneGateway(
        var granted: Boolean = true,
        var ringerMode: SoundMode = SoundMode.SOUND,
        var interruptionFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL,
        var starredAllowed: Boolean = false
    ) : PhoneControlGateway {
        override fun isNotificationPolicyAccessGranted(): Boolean = granted
        override fun getCurrentRingerMode(): SoundMode = ringerMode
        override fun setRingerMode(mode: SoundMode): Boolean {
            if (!granted) return false
            ringerMode = mode
            return true
        }

        override fun isDndEnabled(): Boolean =
            interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                interruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN

        override fun isStarredContactsAllowedInDnd(): Boolean = starredAllowed

        override fun setDoNotDisturb(enabled: Boolean, allowStarredContacts: Boolean): Boolean {
            if (!granted) return false
            interruptionFilter = if (enabled) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            starredAllowed = enabled && allowStarredContacts
            return true
        }

        override fun getCurrentInterruptionFilter(): Int = interruptionFilter

        override fun setInterruptionFilter(filter: Int): Boolean {
            if (!granted) return false
            interruptionFilter = filter
            if (filter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                starredAllowed = false
            }
            return true
        }

        override fun createDndAccessSettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }

    private suspend fun awaitMainLooper(maxIterations: Int = 100, condition: suspend () -> Boolean) {
        var wait = 0
        while (!condition() && wait < maxIterations) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            delay(25)
            wait++
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `saving a picked contact resolves name and number and persists as EMERGENCY`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Register fake system contact provider returning a picked phone contact URI
        val pickedUri = Uri.parse("content://com.android.contacts/contacts/42")
        val provider = object : android.content.ContentProvider() {
            override fun onCreate(): Boolean = true
            override fun query(
                uri: Uri,
                projection: Array<out String>?,
                selection: String?,
                selectionArgs: Array<out String>?,
                sortOrder: String?
            ): android.database.Cursor {
                return if (uri == pickedUri) {
                    MatrixCursor(
                        arrayOf(
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.STARRED
                        )
                    ).apply {
                        addRow(arrayOf<Any>("42", "أمي (نادية)", 1))
                    }
                } else {
                    MatrixCursor(
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
                        )
                    ).apply {
                        addRow(arrayOf<Any>("+20 100 123 4567", 1))
                    }
                }
            }

            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: android.content.ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
            override fun update(uri: Uri, values: android.content.ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
        }
        Shadows.shadowOf(context.contentResolver)
            .let { org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("com.android.contacts", provider) }

        val resolved = PhoneContactPickerResolver.resolveFromUri(context.contentResolver, pickedUri)
        assertNotNull(resolved)
        assertEquals("أمي (نادية)", resolved!!.name)
        assertEquals("+20 100 123 4567", resolved.phoneNumber)
        assertTrue(resolved.isStarredInSystem)

        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = PreferencesManager(context)
        try {
            prefs.setLanguage(AppLanguage.ARABIC)
            val repository = SmartCompanionRepository(db, prefs)
            val viewModel = SmartCompanionViewModel(
                repository = repository,
                phoneControlManager = PhoneControlManager(FakePhoneGateway()),
                routineBackupStore = InMemoryRoutineStateBackupStore(),
                alarmScheduler = NoOpRoutineAlarmScheduler()
            )
            val job = launch(Dispatchers.Unconfined) { viewModel.uiState.collect {} }
            try {
                awaitMainLooper { viewModel.uiState.value.isReady && viewModel.uiState.value.preferences.language == AppLanguage.ARABIC }

                val result = viewModel.savePickedEmergencyContact(
                    name = resolved.name,
                    phoneNumber = resolved.phoneNumber
                )
                assertTrue(result is SaveContactResult.Saved)
                val saved = (result as SaveContactResult.Saved).contact
                assertEquals("أمي (نادية)", saved.name)
                assertEquals("+20 100 123 4567", saved.phoneNumber)
                assertEquals(ContactPriority.EMERGENCY, saved.priorityLevel)
                assertTrue(saved.allowedInterruptions.contains(InterruptionType.CALL))
                assertTrue(saved.allowedInterruptions.contains(InterruptionType.MESSAGE))

                val dbContacts = repository.getAllContactsOnce()
                assertTrue(dbContacts.any { it.name == "أمي (نادية)" && it.phoneNumber == "+20 100 123 4567" && it.priorityLevel == ContactPriority.EMERGENCY })
                val banner = viewModel.uiState.value.feedbackBanner ?: ""
                assertTrue("Expected Arabic confirmation with هيوصلك دايما, got: $banner", banner.contains("هيوصلك دايما"))
            } finally {
                job.cancel()
                prefs.resetDefaults(preserveOnboarding = false)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `duplicate phone number prevents creating a second contact even with different formatting`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = PreferencesManager(context)
        try {
            prefs.setLanguage(AppLanguage.ARABIC)
            val repository = SmartCompanionRepository(db, prefs)
            val viewModel = SmartCompanionViewModel(
                repository = repository,
                phoneControlManager = PhoneControlManager(FakePhoneGateway()),
                routineBackupStore = InMemoryRoutineStateBackupStore(),
                alarmScheduler = NoOpRoutineAlarmScheduler()
            )
            val job = launch(Dispatchers.Unconfined) { viewModel.uiState.collect {} }
            try {
                awaitMainLooper { viewModel.uiState.value.isReady && viewModel.uiState.value.preferences.language == AppLanguage.ARABIC }

                val first = viewModel.savePickedEmergencyContact("أمي", "+20 100 123 4567")
                assertTrue(first is SaveContactResult.Saved)
                val countAfterFirst = repository.getAllContactsOnce().size

                // Same number formatted locally with spaces/dashes or Arabic digits
                val duplicateLocal = viewModel.savePickedEmergencyContact("ماما", "0100-123-4567")
                assertTrue("Expected Duplicate result for local equivalent", duplicateLocal is SaveContactResult.Duplicate)
                assertEquals("أمي", (duplicateLocal as SaveContactResult.Duplicate).existingContact.name)

                val duplicateArabicDigits = viewModel.savePickedEmergencyContact("أمى ٢", "٠١٠٠١٢٣٤٥٦٧")
                assertTrue("Expected Duplicate result for Arabic digits", duplicateArabicDigits is SaveContactResult.Duplicate)

                // Different number succeeds
                val distinct = viewModel.savePickedEmergencyContact("أخي", "01009998887")
                assertTrue(distinct is SaveContactResult.Saved)
                assertEquals(countAfterFirst + 1, repository.getAllContactsOnce().size)

                // Editing the same existing contact ID with its own number succeeds
                val existing = (first as SaveContactResult.Saved).contact
                val edited = viewModel.saveContactChecked(existing.copy(relationship = "الوالدة"))
                assertTrue(edited is SaveContactResult.Saved)
                assertEquals(countAfterFirst + 1, repository.getAllContactsOnce().size)

                assertTrue(EmergencyPhoneNumberUtils.numbersMatch("+20 100 123 4567", "0100-123-4567"))
                assertFalse(EmergencyPhoneNumberUtils.numbersMatch("01001234567", "01009998887"))
            } finally {
                job.cancel()
                prefs.resetDefaults(preserveOnboarding = false)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `emergency contact bypasses active Silent and DND routine via OS priority mechanism and app fallback`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = PreferencesManager(context)
        try {
            prefs.setLanguage(AppLanguage.ARABIC)
            val repository = SmartCompanionRepository(db, prefs)
            val gateway = FakePhoneGateway(
                granted = true,
                ringerMode = SoundMode.SOUND,
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL
            )
            val phoneControlManager = PhoneControlManager(gateway)
            val viewModel = SmartCompanionViewModel(
                repository = repository,
                phoneControlManager = phoneControlManager,
                routineBackupStore = InMemoryRoutineStateBackupStore(),
                alarmScheduler = NoOpRoutineAlarmScheduler()
            )
            val job = launch(Dispatchers.Unconfined) { viewModel.uiState.collect {} }
            try {
                awaitMainLooper { viewModel.uiState.value.isReady && viewModel.uiState.value.preferences.language == AppLanguage.ARABIC }

                // Save an emergency contact with a phone number
                val saveRes = viewModel.savePickedEmergencyContact("د. كريم", "01112223334")
                assertTrue(saveRes is SaveContactResult.Saved)
                awaitMainLooper { viewModel.uiState.value.contacts.any { it.phoneNumber == "01112223334" } }

                // Activate a strict Sleep / Silent + DND routine with NO explicit exception list
                val sleepRoutine = SmartRoutine(
                    id = "r_sleep_strict",
                    name = "نوم عميق",
                    description = "صامت وعدم إزعاج",
                    isEnabled = true,
                    trigger = RuleTrigger(type = TriggerType.SPECIFIC_TIME, hour = 23, minute = 0, dayFilter = DayFilter.EVERY_DAY),
                    conditions = emptyList(),
                    actions = listOf(RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SILENT.name, "صامت")),
                    exceptions = emptyList(),
                    priority = 10,
                    scheduleStartTime = "23:00",
                    scheduleEndTime = "06:00",
                    targetProfileId = "profile_sleep",
                    lastExecutionResult = "",
                    lastExecutedAtMillis = null,
                    executionCount = 0,
                    skippedCount = 0,
                    enableDoNotDisturb = true
                )
                viewModel.saveRoutine(sleepRoutine, startImmediately = true)
                awaitMainLooper {
                    gateway.ringerMode == SoundMode.SILENT &&
                        gateway.interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
                }

                // OS DND mechanism MUST have enabled starred/priority contacts even without routine exceptions
                assertTrue("Expected OS DND to allow priority/starred contacts", gateway.starredAllowed)

                // Case 1: OS mechanism handles it when ringer is SOUND, starred in OS, and DND priority is allowed
                gateway.ringerMode = SoundMode.SOUND
                val osBypass = viewModel.evaluateIncomingEmergencyContact(
                    senderName = null,
                    senderPhoneNumber = "+20 111 222 3334",
                    interruptionType = InterruptionType.CALL,
                    isStarredInOs = true
                )
                assertTrue(osBypass.isEmergency)
                assertTrue(osBypass.allowed)
                assertTrue(osBypass.usedOsDndMechanism)
                assertFalse(osBypass.usedAppFallback)

                // Case 2: Active Silent + DND routine where OS starred mechanism does NOT cover (Silent ringer / unstarred contact)
                // -> App's own fallback check detects emergency contact and bypasses Silent + DND
                gateway.ringerMode = SoundMode.SILENT
                gateway.interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY
                val appFallbackCall = viewModel.evaluateIncomingEmergencyContact(
                    senderName = null,
                    senderPhoneNumber = "0111-222-3334",
                    interruptionType = InterruptionType.CALL,
                    isStarredInOs = false
                )
                assertTrue(appFallbackCall.isEmergency)
                assertTrue(appFallbackCall.allowed)
                assertFalse(appFallbackCall.usedOsDndMechanism)
                assertTrue(appFallbackCall.usedAppFallback)
                assertTrue(appFallbackCall.bypassAppliedOnDevice)
                assertEquals(SoundMode.SOUND, gateway.ringerMode)
                assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, gateway.interruptionFilter)
                assertTrue(appFallbackCall.explanation.contains("هيوصلك دايما"))

                // Notification from emergency contact during DND/Sleep also bypasses via app fallback
                gateway.ringerMode = SoundMode.SILENT
                gateway.interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY
                val appFallbackNotif = viewModel.evaluateIncomingEmergencyContact(
                    senderName = "د. كريم",
                    senderPhoneNumber = null,
                    interruptionType = InterruptionType.MESSAGE,
                    isStarredInOs = false
                )
                assertTrue(appFallbackNotif.isEmergency)
                assertTrue(appFallbackNotif.allowed)
                assertTrue(appFallbackNotif.usedAppFallback)
                assertEquals(SoundMode.SOUND, gateway.ringerMode)

                // Non-emergency caller does NOT bypass
                gateway.ringerMode = SoundMode.SILENT
                gateway.interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY
                val nonEmergency = viewModel.evaluateIncomingEmergencyContact(
                    senderName = "Unknown Caller",
                    senderPhoneNumber = "01555555555",
                    interruptionType = InterruptionType.CALL,
                    isStarredInOs = false
                )
                assertFalse(nonEmergency.isEmergency)
                assertFalse(nonEmergency.allowed)
                assertEquals(SoundMode.SILENT, gateway.ringerMode)

                // Also verify Controller keeps DND starred contacts enabled directly when hasEmergencyContacts = true
                val controller = RoutinePhoneController(phoneControlManager, InMemoryRoutineStateBackupStore())
                val startOutcome = controller.applyRoutineStart(sleepRoutine, emptyList(), isArabic = true, hasEmergencyContacts = true)
                assertTrue(startOutcome.success)
                assertTrue(gateway.starredAllowed)

                // And EmergencyBypassController triggers callback alert when bypassing
                var alertTriggered = false
                val bypassController = EmergencyBypassController(phoneControlManager) { _, _, _ ->
                    alertTriggered = true
                }
                val directBypass = bypassController.handleIncomingCommunication(
                    senderName = null,
                    senderPhoneNumber = "01112223334",
                    interruptionType = InterruptionType.MESSAGE,
                    contacts = repository.getAllContactsOnce(),
                    isStarredInOs = false,
                    isArabic = true
                )
                assertTrue(directBypass.allowed)
                assertTrue(alertTriggered)
            } finally {
                job.cancel()
                prefs.resetDefaults(preserveOnboarding = false)
            }
        } finally {
            db.close()
        }
    }
}

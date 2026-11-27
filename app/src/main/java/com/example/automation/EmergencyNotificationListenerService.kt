package com.example.automation

import android.app.Notification
import android.app.Person
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.models.AppLanguage
import com.example.models.EventCategory
import com.example.models.InterruptionType
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.AndroidPhoneControlGateway
import com.example.simulation.PhoneControlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EmergencyNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        if (posted.packageName == packageName) return

        val notification = posted.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val senderName = title ?: conversationTitle

        var extractedPhone: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val people = extras.getParcelableArrayList<Person>(Notification.EXTRA_PEOPLE_LIST)
            val personUri = people?.firstOrNull()?.uri
            if (personUri != null && personUri.startsWith("tel:", ignoreCase = true)) {
                extractedPhone = personUri.substringAfter("tel:", "").trim()
            }
        }
        val interruptionType = if (notification.category == Notification.CATEGORY_CALL) {
            InterruptionType.CALL
        } else {
            InterruptionType.MESSAGE
        }

        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val prefs = PreferencesManager(applicationContext)
            val repository = SmartCompanionRepository(db, prefs)
            val contacts = repository.getAllContactsOnce()
            val appPrefs = repository.preferences.first()
            val isArabic = appPrefs.language == AppLanguage.ARABIC

            val phoneControlManager = PhoneControlManager(AndroidPhoneControlGateway(applicationContext))
            val controller = EmergencyBypassController(
                phoneControlManager = phoneControlManager,
                onBypassAlert = { matched, type, ar ->
                    RoutineNotificationHelper.showEmergencyBypassAlert(
                        context = applicationContext,
                        contactName = matched.name,
                        phoneNumber = matched.phoneNumber,
                        isCall = type == InterruptionType.CALL,
                        isArabic = ar
                    )
                }
            )

            val matched = EmergencyPhoneNumberUtils.findMatchingEmergencyContact(
                senderName = senderName ?: text,
                senderPhoneNumber = extractedPhone ?: title,
                contacts = contacts
            ) ?: return@launch

            val isStarredInOs = PhoneContactPickerResolver.isNumberStarredInSystemContacts(
                context = applicationContext,
                phoneNumber = matched.phoneNumber
            )

            val outcome = controller.handleIncomingCommunication(
                senderName = senderName ?: text,
                senderPhoneNumber = extractedPhone ?: matched.phoneNumber,
                interruptionType = interruptionType,
                contacts = contacts,
                isStarredInOs = isStarredInOs,
                isArabic = isArabic
            )

            if (outcome.isEmergency && outcome.allowed) {
                repository.logEvent(
                    title = if (isArabic) "تجاوز طوارئ: ${matched.name}" else "Emergency Bypass: ${matched.name}",
                    category = EventCategory.CONTACT_EXCEPTION,
                    triggerSource = if (outcome.usedOsDndMechanism) "OS DND Priority" else "App Emergency Fallback",
                    result = if (isArabic) "هيوصلك دايما" else "Always gets through",
                    explanation = outcome.explanation
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

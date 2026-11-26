package com.example.simulation

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.example.models.SoundMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhoneControlState(
    val hasDndAccess: Boolean = false,
    val currentRingerMode: SoundMode = SoundMode.SOUND,
    val isDndEnabled: Boolean = false,
    val starredContactsAllowedInDnd: Boolean = false,
    val lastError: String? = null
)

data class PhoneControlActionResult(
    val success: Boolean,
    val permissionRequired: Boolean,
    val state: PhoneControlState
)

interface PhoneControlGateway {
    fun isNotificationPolicyAccessGranted(): Boolean
    fun getCurrentRingerMode(): SoundMode
    fun setRingerMode(mode: SoundMode): Boolean
    fun isDndEnabled(): Boolean
    fun isStarredContactsAllowedInDnd(): Boolean
    fun setDoNotDisturb(enabled: Boolean, allowStarredContacts: Boolean): Boolean
    fun createDndAccessSettingsIntent(context: Context): Intent
}

class NoOpPhoneControlGateway : PhoneControlGateway {
    override fun isNotificationPolicyAccessGranted(): Boolean = false
    override fun getCurrentRingerMode(): SoundMode = SoundMode.SOUND
    override fun setRingerMode(mode: SoundMode): Boolean = false
    override fun isDndEnabled(): Boolean = false
    override fun isStarredContactsAllowedInDnd(): Boolean = false
    override fun setDoNotDisturb(enabled: Boolean, allowStarredContacts: Boolean): Boolean = false
    override fun createDndAccessSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
}

class AndroidPhoneControlGateway(
    private val context: Context
) : PhoneControlGateway {

    private val notificationManager: NotificationManager?
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private val audioManager: AudioManager?
        get() = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    override fun isNotificationPolicyAccessGranted(): Boolean {
        return notificationManager?.isNotificationPolicyAccessGranted == true
    }

    override fun getCurrentRingerMode(): SoundMode {
        return when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> SoundMode.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> SoundMode.VIBRATE
            else -> SoundMode.SOUND
        }
    }

    override fun setRingerMode(mode: SoundMode): Boolean {
        val nm = notificationManager ?: return false
        val am = audioManager ?: return false
        if (!nm.isNotificationPolicyAccessGranted) return false
        val targetMode = when (mode) {
            SoundMode.SILENT -> AudioManager.RINGER_MODE_SILENT
            SoundMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            SoundMode.SOUND -> AudioManager.RINGER_MODE_NORMAL
        }
        return try {
            am.ringerMode = targetMode
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun isDndEnabled(): Boolean {
        val filter = notificationManager?.currentInterruptionFilter
            ?: NotificationManager.INTERRUPTION_FILTER_ALL
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    override fun isStarredContactsAllowedInDnd(): Boolean {
        val nm = notificationManager ?: return false
        if (!nm.isNotificationPolicyAccessGranted) return false
        val policy = runCatching { nm.notificationPolicy }.getOrNull() ?: return false
        val callsEnabled = (policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) != 0
        return callsEnabled &&
            policy.priorityCallSenders == NotificationManager.Policy.PRIORITY_SENDERS_STARRED
    }

    override fun setDoNotDisturb(enabled: Boolean, allowStarredContacts: Boolean): Boolean {
        val nm = notificationManager ?: return false
        if (!nm.isNotificationPolicyAccessGranted) return false
        return try {
            if (enabled) {
                if (allowStarredContacts) {
                    val existing = runCatching { nm.notificationPolicy }.getOrNull()
                    val categories = (existing?.priorityCategories ?: 0) or
                        NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                        NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES or
                        NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS or
                        NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
                    val policy = NotificationManager.Policy(
                        categories,
                        NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                        NotificationManager.Policy.PRIORITY_SENDERS_STARRED
                    )
                    nm.setNotificationPolicy(policy)
                }
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun createDndAccessSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

class PhoneControlManager(
    private val gateway: PhoneControlGateway = NoOpPhoneControlGateway()
) {
    private val _state = MutableStateFlow(readSnapshot(lastError = null))
    val state: StateFlow<PhoneControlState> = _state.asStateFlow()

    fun refreshPermissionAndDeviceState(): PhoneControlState {
        val refreshed = readSnapshot(lastError = null)
        _state.value = refreshed
        return refreshed
    }

    fun applyRingerMode(mode: SoundMode): PhoneControlActionResult {
        if (!gateway.isNotificationPolicyAccessGranted()) {
            val refreshed = readSnapshot(lastError = "DND_ACCESS_REQUIRED")
            _state.value = refreshed
            return PhoneControlActionResult(
                success = false,
                permissionRequired = true,
                state = refreshed
            )
        }
        val applied = gateway.setRingerMode(mode)
        val refreshed = readSnapshot(
            lastError = if (applied) null else "RINGER_MODE_FAILED"
        )
        _state.value = refreshed
        return PhoneControlActionResult(
            success = applied,
            permissionRequired = !gateway.isNotificationPolicyAccessGranted(),
            state = refreshed
        )
    }

    fun toggleDoNotDisturb(allowStarredContacts: Boolean = true): PhoneControlActionResult {
        if (!gateway.isNotificationPolicyAccessGranted()) {
            val refreshed = readSnapshot(lastError = "DND_ACCESS_REQUIRED")
            _state.value = refreshed
            return PhoneControlActionResult(
                success = false,
                permissionRequired = true,
                state = refreshed
            )
        }
        val targetEnabled = !gateway.isDndEnabled()
        val applied = gateway.setDoNotDisturb(
            enabled = targetEnabled,
            allowStarredContacts = allowStarredContacts
        )
        val refreshed = readSnapshot(
            lastError = if (applied) null else "DND_TOGGLE_FAILED"
        )
        _state.value = refreshed
        return PhoneControlActionResult(
            success = applied,
            permissionRequired = !gateway.isNotificationPolicyAccessGranted(),
            state = refreshed
        )
    }

    fun createDndAccessSettingsIntent(context: Context): Intent {
        return gateway.createDndAccessSettingsIntent(context)
    }

    private fun readSnapshot(lastError: String?): PhoneControlState {
        val granted = gateway.isNotificationPolicyAccessGranted()
        return PhoneControlState(
            hasDndAccess = granted,
            currentRingerMode = gateway.getCurrentRingerMode(),
            isDndEnabled = if (granted) gateway.isDndEnabled() else false,
            starredContactsAllowedInDnd = if (granted) gateway.isStarredContactsAllowedInDnd() else false,
            lastError = lastError
        )
    }
}

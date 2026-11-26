package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.models.SoundMode
import com.example.simulation.AndroidPhoneControlGateway
import com.example.simulation.PhoneControlGateway
import com.example.simulation.PhoneControlManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhoneControlManagerTest {

    private class FakePhoneControlGateway(
        var granted: Boolean = false,
        var ringerMode: SoundMode = SoundMode.SOUND,
        var dndEnabled: Boolean = false,
        var starredAllowed: Boolean = false
    ) : PhoneControlGateway {
        var setRingerCalls = 0
        var setDndCalls = 0

        override fun isNotificationPolicyAccessGranted(): Boolean = granted
        override fun getCurrentRingerMode(): SoundMode = ringerMode
        override fun setRingerMode(mode: SoundMode): Boolean {
            setRingerCalls++
            if (!granted) return false
            ringerMode = mode
            return true
        }

        override fun isDndEnabled(): Boolean = dndEnabled
        override fun isStarredContactsAllowedInDnd(): Boolean = starredAllowed
        override fun setDoNotDisturb(enabled: Boolean, allowStarredContacts: Boolean): Boolean {
            setDndCalls++
            if (!granted) return false
            dndEnabled = enabled
            if (enabled && allowStarredContacts) {
                starredAllowed = true
            }
            return true
        }

        override fun createDndAccessSettingsIntent(context: Context): Intent {
            return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }
    }

    @Test
    fun `when DND access is not granted, ringer and DND changes are blocked and state reflects denied`() {
        val gateway = FakePhoneControlGateway(granted = false, ringerMode = SoundMode.SOUND, dndEnabled = true)
        val manager = PhoneControlManager(gateway)

        assertFalse(manager.state.value.hasDndAccess)
        assertFalse(manager.state.value.isDndEnabled)

        val ringerResult = manager.applyRingerMode(SoundMode.SILENT)
        assertFalse(ringerResult.success)
        assertTrue(ringerResult.permissionRequired)
        assertEquals(0, gateway.setRingerCalls)
        assertEquals("DND_ACCESS_REQUIRED", manager.state.value.lastError)

        val dndResult = manager.toggleDoNotDisturb()
        assertFalse(dndResult.success)
        assertTrue(dndResult.permissionRequired)
        assertEquals(0, gateway.setDndCalls)
    }

    @Test
    fun `refreshPermissionAndDeviceState re-checks permission and updates state when returning to app`() {
        val gateway = FakePhoneControlGateway(granted = false, ringerMode = SoundMode.VIBRATE)
        val manager = PhoneControlManager(gateway)

        assertFalse(manager.state.value.hasDndAccess)

        // Simulate user granting permission in Android Settings and returning to the app
        gateway.granted = true
        val refreshed = manager.refreshPermissionAndDeviceState()

        assertTrue(refreshed.hasDndAccess)
        assertTrue(manager.state.value.hasDndAccess)
        assertEquals(SoundMode.VIBRATE, refreshed.currentRingerMode)
        assertNull(refreshed.lastError)
    }

    @Test
    fun `when DND access is granted, silent vibrate sound and DND toggle update state`() {
        val gateway = FakePhoneControlGateway(granted = true, ringerMode = SoundMode.SOUND, dndEnabled = false)
        val manager = PhoneControlManager(gateway)

        assertTrue(manager.applyRingerMode(SoundMode.SILENT).success)
        assertEquals(SoundMode.SILENT, manager.state.value.currentRingerMode)

        assertTrue(manager.applyRingerMode(SoundMode.VIBRATE).success)
        assertEquals(SoundMode.VIBRATE, manager.state.value.currentRingerMode)

        assertTrue(manager.applyRingerMode(SoundMode.SOUND).success)
        assertEquals(SoundMode.SOUND, manager.state.value.currentRingerMode)

        val dndOn = manager.toggleDoNotDisturb(allowStarredContacts = true)
        assertTrue(dndOn.success)
        assertTrue(dndOn.state.isDndEnabled)
        assertTrue(dndOn.state.starredContactsAllowedInDnd)

        val dndOff = manager.toggleDoNotDisturb(allowStarredContacts = true)
        assertTrue(dndOff.success)
        assertFalse(dndOff.state.isDndEnabled)
    }

    @Test
    fun `when DND access is revoked externally, refresh updates state back to denied`() {
        val gateway = FakePhoneControlGateway(granted = true)
        val manager = PhoneControlManager(gateway)
        assertTrue(manager.state.value.hasDndAccess)

        gateway.granted = false
        val refreshed = manager.refreshPermissionAndDeviceState()
        assertFalse(refreshed.hasDndAccess)

        val result = manager.applyRingerMode(SoundMode.SILENT)
        assertFalse(result.success)
        assertTrue(result.permissionRequired)
    }

    @Test
    fun `AndroidPhoneControlGateway integrates with NotificationManager and AudioManager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val shadowNm = Shadows.shadowOf(nm)

        val gateway = AndroidPhoneControlGateway(context)
        val manager = PhoneControlManager(gateway)

        val settingsIntent = manager.createDndAccessSettingsIntent(context)
        assertEquals(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, settingsIntent.action)

        shadowNm.setNotificationPolicyAccessGranted(false)
        assertFalse(manager.refreshPermissionAndDeviceState().hasDndAccess)

        shadowNm.setNotificationPolicyAccessGranted(true)
        assertTrue(manager.refreshPermissionAndDeviceState().hasDndAccess)

        assertTrue(manager.applyRingerMode(SoundMode.VIBRATE).success)
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, am.ringerMode)
        assertEquals(SoundMode.VIBRATE, manager.state.value.currentRingerMode)

        assertTrue(manager.applyRingerMode(SoundMode.SILENT).success)
        assertEquals(AudioManager.RINGER_MODE_SILENT, am.ringerMode)
        assertEquals(SoundMode.SILENT, manager.state.value.currentRingerMode)

        assertTrue(manager.applyRingerMode(SoundMode.SOUND).success)
        assertEquals(AudioManager.RINGER_MODE_NORMAL, am.ringerMode)
        assertEquals(SoundMode.SOUND, manager.state.value.currentRingerMode)

        assertTrue(manager.toggleDoNotDisturb(allowStarredContacts = true).success)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, nm.currentInterruptionFilter)
        assertEquals(NotificationManager.Policy.PRIORITY_SENDERS_STARRED, nm.notificationPolicy.priorityCallSenders)
        assertTrue(manager.state.value.isDndEnabled)

        assertTrue(manager.toggleDoNotDisturb(allowStarredContacts = true).success)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, nm.currentInterruptionFilter)
        assertFalse(manager.state.value.isDndEnabled)
    }
}

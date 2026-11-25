package com.example.simulation

import com.example.models.SoundMode

/**
 * Abstraction layer decoupling Smart Companion's automation decisions from
 * how actions are applied to a device.
 *
 * The current demo binds [SimulatedDeviceActionExecutor]. A future production
 * release can bind [AndroidDeviceActionExecutor] after explicit user permissions
 * (e.g., Do Not Disturb Policy Access).
 */
sealed interface DeviceActionCommand {
    data class ApplySoundMode(
        val soundMode: SoundMode,
        val reason: String
    ) : DeviceActionCommand

    data class ActivateProfileState(
        val profileId: String,
        val profileName: String,
        val soundMode: SoundMode
    ) : DeviceActionCommand

    data class DeliverInAppNotice(
        val title: String,
        val message: String
    ) : DeviceActionCommand
}

data class DeviceActionResult(
    val executed: Boolean,
    val isSimulated: Boolean,
    val summary: String,
    val appliedSoundMode: SoundMode? = null
)

interface DeviceActionExecutor {
    val isSimulatedMode: Boolean
    suspend fun execute(command: DeviceActionCommand): DeviceActionResult
}

/**
 * Active executor for the Smart Companion demonstration build.
 * Updates only in-app simulated state and never touches real Android AudioManager
 * or NotificationManager policies.
 */
class SimulatedDeviceActionExecutor : DeviceActionExecutor {
    override val isSimulatedMode: Boolean = true

    override suspend fun execute(command: DeviceActionCommand): DeviceActionResult {
        return when (command) {
            is DeviceActionCommand.ApplySoundMode -> DeviceActionResult(
                executed = true,
                isSimulated = true,
                summary = "Simulated sound mode set to ${command.soundMode.displayName} (${command.reason})",
                appliedSoundMode = command.soundMode
            )
            is DeviceActionCommand.ActivateProfileState -> DeviceActionResult(
                executed = true,
                isSimulated = true,
                summary = "Simulated profile '${command.profileName}' active (${command.soundMode.displayName})",
                appliedSoundMode = command.soundMode
            )
            is DeviceActionCommand.DeliverInAppNotice -> DeviceActionResult(
                executed = true,
                isSimulated = true,
                summary = "Simulated in-app notice: ${command.title} — ${command.message}"
            )
        }
    }
}

/**
 * Conceptual placeholder showing how future Android OS integration
 * (AudioManager / NotificationManager.setInterruptionFilter) plugs into the
 * same contract without rewriting business logic or UI screens.
 *
 * Intentionally inert in this demo build.
 */
@Suppress("unused")
class AndroidDeviceActionExecutor : DeviceActionExecutor {
    override val isSimulatedMode: Boolean = false

    override suspend fun execute(command: DeviceActionCommand): DeviceActionResult {
        return DeviceActionResult(
            executed = false,
            isSimulated = false,
            summary = "Real Android OS control is disabled in this demo version. Use SimulatedDeviceActionExecutor."
        )
    }
}

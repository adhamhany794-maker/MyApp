package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.ContactPriority
import com.example.models.InterruptionType
import com.example.models.SoundMode
import com.example.simulation.AutomationSimulationEngine
import com.example.simulation.SimulationEvent
import com.example.ui.components.CompanionCard
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SafetyCenterScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel
) {
    val conflicts = remember(state.routines) {
        AutomationSimulationEngine().findPotentialConflicts(state.routines)
    }

    var confirmAction by remember { mutableStateOf<SafetyConfirmAction?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenIntroHeader(
                title = strings.screenSafety,
                subtitle = strings.safetySubtitle,
                strings = strings,
                onInfoClick = { showHelp = true }
            )
        }

        // Global Pause
        item {
            CompanionCard(
                containerColor = if (state.preferences.isAutomationPaused) StatusAmber.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.globalPauseTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (state.preferences.isAutomationPaused) strings.globalPauseOnSub
                            else strings.globalPauseOffSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = state.preferences.isAutomationPaused,
                        onCheckedChange = { viewModel.toggleGlobalAutomationPause() },
                        modifier = Modifier.testTag("safety_global_pause_switch")
                    )
                }
            }
        }

        // Manual Override & Restore Previous State
        item {
            val activeName = state.activeProfile?.let { strings.localizedProfileName(it) } ?: "Personal"
            val soundName = strings.soundModeLabel(state.preferences.simulatedSoundMode)

            CompanionCard {
                Text(strings.manualOverrideTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${strings.currentSimStatePrefix}: $activeName • $soundName",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SoundMode.entries.forEach { mode ->
                        OutlinedButton(
                            onClick = { viewModel.setSimulatedSoundMode(mode, "Safety Manual Override") }
                        ) {
                            Text(strings.soundModeLabel(mode))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                val prev = state.preferences.previousSnapshot
                val prevProfile = state.profiles.firstOrNull { it.id == prev.activeProfileId }
                val prevName = prevProfile?.let { strings.localizedProfileName(it) } ?: "Personal"
                Button(
                    onClick = { viewModel.restorePreviousPhoneState() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_previous_state_button")
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("${strings.restorePrevStatePrefix} ($prevName • ${strings.soundModeLabel(prev.soundMode)})")
                }
            }
        }

        // Conflict Warnings
        item {
            CompanionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = if (conflicts.isNotEmpty()) StatusAmber else StatusEmerald
                    )
                    Text("${strings.conflictWarningsPrefix} (${conflicts.size})", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                if (conflicts.isEmpty()) {
                    Text(
                        strings.noConflictsMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusEmerald
                    )
                } else {
                    conflicts.forEach { (a, b) ->
                        val aName = strings.localizedRoutineName(a)
                        val bName = strings.localizedRoutineName(b)
                        Text(
                            text = if (strings.isArabic) {
                                "• يتقاطع '$aName' (P${a.priority}) مع '$bName' (P${b.priority})."
                            } else {
                                "• '$aName' (P${a.priority}) overlaps with '$bName' (P${b.priority})."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusAmber
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.runSimulationEvent(
                                    SimulationEvent.TimeAndDay(a.trigger.hour, a.trigger.minute, a.trigger.dayFilter)
                                )
                            },
                            modifier = Modifier.testTag("safety_test_conflict_button")
                        ) {
                            Text(strings.testResolveConflictBtn)
                        }
                    }
                }
            }
        }

        // Emergency Access Simulation
        item {
            CompanionCard {
                Text(strings.emergencyTestTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    strings.emergencyTestSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                val emergency = state.contacts.firstOrNull { it.priorityLevel == ContactPriority.EMERGENCY }
                Button(
                    onClick = {
                        emergency?.let {
                            viewModel.simulateIncomingInterruption(it, InterruptionType.CALL, isRepeatCaller = true)
                        }
                    },
                    enabled = emergency != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("safety_emergency_test_button")
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("${strings.simulateEmergencyCallPrefix} (${emergency?.name ?: "-"})")
                }
                state.lastInterruptionDecision?.let { d ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${if (d.isAllowed) strings.allowedUpper else strings.blockedUpper}: ${d.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (d.isAllowed) StatusEmerald else StatusCrimson,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Recovery Operations (All require confirmation!)
        item {
            CompanionCard {
                Text(strings.recoveryTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    strings.recoverySub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { confirmAction = SafetyConfirmAction.RESTORE_PROFILES },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.restoreProfilesTitle.removeSuffix("?").removeSuffix("؟"))
                    }
                    OutlinedButton(
                        onClick = { confirmAction = SafetyConfirmAction.RESET_ROUTINES },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.resetDemoRoutinesBtn)
                    }
                    Button(
                        onClick = { confirmAction = SafetyConfirmAction.RESET_ALL_DEMO },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("safety_reset_all_button")
                    ) {
                        Text(strings.resetEntireAppBtn)
                    }
                }
            }
        }
    }

    confirmAction?.let { action ->
        val (title, msg, label) = when (action) {
            SafetyConfirmAction.RESTORE_PROFILES -> Triple(
                strings.restoreProfilesTitle,
                strings.restoreProfilesMsg,
                strings.restoreDefaultsBtn
            )
            SafetyConfirmAction.RESET_ROUTINES -> Triple(
                strings.resetDemoRoutinesBtn,
                if (strings.isArabic) "سيتم استبدال الروتينات المخصصة بالروتينات التجريبية الأصلية."
                else "Custom routines will be replaced by the original demonstration routines.",
                strings.confirm
            )
            SafetyConfirmAction.RESET_ALL_DEMO -> Triple(
                strings.resetEntireAppBtn,
                if (strings.isArabic) "ستعود جميع الأوضاع والروتينات وجهات الاتصال والجلسات والسجل إلى حالة البداية."
                else "All profiles, routines, contacts, focus sessions, and activity logs will return to the initial demo state.",
                strings.confirm
            )
        }
        ConfirmActionDialog(
            title = title,
            message = msg,
            confirmText = label,
            cancelText = strings.cancel,
            isDestructive = action != SafetyConfirmAction.RESTORE_PROFILES,
            onConfirm = {
                when (action) {
                    SafetyConfirmAction.RESTORE_PROFILES -> viewModel.restoreDefaultProfiles()
                    SafetyConfirmAction.RESET_ROUTINES -> viewModel.resetDemoRoutines()
                    SafetyConfirmAction.RESET_ALL_DEMO -> viewModel.resetAllDemoData(resetOnboarding = false)
                }
                confirmAction = null
            },
            onDismiss = { confirmAction = null }
        )
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.SAFETY_CENTER, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

private enum class SafetyConfirmAction {
    RESTORE_PROFILES,
    RESET_ROUTINES,
    RESET_ALL_DEMO
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.models.AppLanguage
import com.example.models.ConflictResolutionPolicy
import com.example.models.ContactPriority
import com.example.models.InterruptionType
import com.example.models.SoundMode
import com.example.models.ThemeMode
import com.example.simulation.AutomationSimulationEngine
import com.example.simulation.SimulationEvent
import com.example.ui.components.CompanionCard
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.SimulatedDemoBadge
import com.example.ui.navigation.NavRoutes
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel,
    onNavigate: (String) -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showAdvancedSection by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPhoneControlState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val conflicts = remember(state.routines) {
        AutomationSimulationEngine().findPotentialConflicts(state.routines)
    }
    val emergencyCount = state.contacts.count {
        it.isEnabled && it.priorityLevel == ContactPriority.EMERGENCY
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenIntroHeader(
                title = strings.screenSettings,
                subtitle = strings.settingsSubtitle,
                strings = strings,
                onInfoClick = { showHelp = true }
            )
        }

        // SECTION 1: GENERAL
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = strings.settingsGeneralTitle,
                    subtitle = strings.settingsGeneralExplain
                )

                CompanionCard {
                    // Appearance
                    Text(strings.appearanceTitle, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        strings.appearanceExplain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.preferences.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(strings.themeModeLabel(mode)) },
                                modifier = Modifier.testTag("theme_${mode.name}")
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Language & RTL
                    Text(strings.languageTitle, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        strings.languageSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = state.preferences.language == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                label = { Text(lang.label) },
                                modifier = Modifier.testTag("lang_${lang.name}")
                            )
                        }
                    }
                }
            }
        }

        item {
            CompanionCard {
                // Activity (Insights + History + Notifications)
                SettingsNavRow(
                    title = strings.screenActivity,
                    subtitle = strings.descActivityMerged,
                    icon = Icons.Rounded.BarChart,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(NavRoutes.ACTIVITY) },
                    modifier = Modifier.testTag("settings_activity_row")
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Routine Log (Real phone control history)
                SettingsNavRow(
                    title = strings.screenRoutineLog,
                    subtitle = strings.routineLogSubtitle,
                    icon = Icons.Rounded.History,
                    accentColor = StatusIndigo,
                    onClick = { onNavigate(NavRoutes.ROUTINE_LOG) },
                    modifier = Modifier.testTag("settings_routine_log_row")
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Replay App Guide
                SettingsNavRow(
                    title = strings.viewOnboardingBtn,
                    subtitle = strings.replayOnboardingExplain,
                    icon = Icons.Rounded.PlayCircleOutline,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { viewModel.reopenOnboarding() },
                    modifier = Modifier.testTag("settings_replay_onboarding_btn")
                )
            }
        }

        // PHONE CONTROL (Real Android DND & AudioManager Step 1)
        item {
            val phoneControl = state.phoneControl
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("phone_control_section")
            ) {
                SectionHeader(
                    title = strings.phoneControlTitle,
                    subtitle = strings.phoneControlSubtitle
                )

                CompanionCard(
                    containerColor = if (phoneControl.hasDndAccess) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        StatusAmber.copy(alpha = 0.08f)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (phoneControl.hasDndAccess) Icons.Rounded.DoNotDisturbOn else Icons.Rounded.NotificationsOff,
                                contentDescription = strings.phoneControlAccessStatusLabel,
                                tint = if (phoneControl.hasDndAccess) StatusEmerald else StatusAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = strings.phoneControlAccessStatusLabel,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Surface(
                            color = (if (phoneControl.hasDndAccess) StatusEmerald else StatusAmber).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("phone_control_dnd_status")
                        ) {
                            Text(
                                text = if (phoneControl.hasDndAccess) strings.phoneControlAccessGranted else strings.phoneControlAccessDenied,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (phoneControl.hasDndAccess) StatusEmerald else StatusAmber,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (!phoneControl.hasDndAccess) {
                        Text(
                            text = strings.phoneControlAccessExplain,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("phone_control_access_explain")
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = viewModel.createDndAccessSettingsIntent(context)
                                runCatching { context.startActivity(intent) }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_control_grant_dnd_button")
                        ) {
                            Text(strings.phoneControlOpenSettingsBtn)
                        }
                    } else {
                        Text(
                            text = "${strings.phoneControlCurrentRingerPrefix}: ${strings.soundModeLabel(phoneControl.currentRingerMode)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("phone_control_current_ringer")
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = strings.phoneControlRingerTestHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(SoundMode.SILENT, Icons.Rounded.VolumeOff, "phone_control_ringer_silent_button"),
                                Triple(SoundMode.VIBRATE, Icons.Rounded.Vibration, "phone_control_ringer_vibrate_button"),
                                Triple(SoundMode.SOUND, Icons.Rounded.VolumeUp, "phone_control_ringer_sound_button")
                            ).forEach { (mode, icon, tag) ->
                                val isActive = phoneControl.currentRingerMode == mode
                                if (isActive) {
                                    Button(
                                        onClick = { viewModel.setRealRingerMode(mode) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag(tag)
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.soundModeLabel(mode))
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.setRealRingerMode(mode) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag(tag)
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.soundModeLabel(mode))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${strings.phoneControlDndStatusPrefix}: ${if (phoneControl.isDndEnabled) strings.phoneControlDndOnLabel else strings.phoneControlDndOffLabel}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (phoneControl.isDndEnabled) StatusIndigo else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("phone_control_dnd_state_text")
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = strings.phoneControlStarredNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.toggleRealDoNotDisturb() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("phone_control_dnd_toggle_button")
                            ) {
                                Text(
                                    if (phoneControl.isDndEnabled) strings.phoneControlDndToggleOffBtn
                                    else strings.phoneControlDndToggleOnBtn
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.exactAlarmStatusLabel,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = (if (phoneControl.canScheduleExactAlarms) StatusEmerald else StatusAmber).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("phone_control_exact_alarm_status")
                        ) {
                            Text(
                                text = if (phoneControl.canScheduleExactAlarms) strings.phoneControlAccessGranted else strings.phoneControlAccessDenied,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (phoneControl.canScheduleExactAlarms) StatusEmerald else StatusAmber,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!phoneControl.canScheduleExactAlarms) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.exactAlarmDeniedBanner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = viewModel.createExactAlarmSettingsIntent(context)
                                runCatching { context.startActivity(intent) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_control_grant_exact_alarm_button")
                        ) {
                            Text(strings.exactAlarmGrantBtn)
                        }
                    }
                }
            }
        }

        // SECTION 2: EMERGENCY AND SAFETY
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = strings.settingsSafetyTitle,
                    subtitle = strings.settingsSafetyExplain
                )

                CompanionCard(
                    containerColor = if (state.preferences.isAutomationPaused) StatusAmber.copy(alpha = 0.10f)
                    else MaterialTheme.colorScheme.surface
                ) {
                    // Emergency Contacts
                    SettingsNavRow(
                        title = strings.navContacts,
                        subtitle = "$emergencyCount ${strings.homeCardContactsSubSuffix}",
                        icon = Icons.Rounded.Shield,
                        accentColor = StatusEmerald,
                        onClick = { onNavigate(NavRoutes.CONTACTS) },
                        modifier = Modifier.testTag("settings_contacts_row")
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Global Automation Pause
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.globalPauseTitle, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(2.dp))
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

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Manual Control & Rollback (Real Device)
                    val currentRinger = state.phoneControl.currentRingerMode
                    val activeBackup = state.activePhoneBackup
                    val prevRinger = activeBackup?.previousRingerMode ?: currentRinger

                    Text(strings.manualOverrideTitle, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        strings.manualOverrideExplain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${strings.phoneControlCurrentRingerPrefix}: ${strings.soundModeLabel(currentRinger)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SoundMode.entries.forEach { mode ->
                            val isActive = currentRinger == mode
                            if (isActive) {
                                Button(
                                    onClick = { viewModel.setRealManualRingerMode(mode) }
                                ) {
                                    Text(strings.soundModeLabel(mode))
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.setRealManualRingerMode(mode) }
                                ) {
                                    Text(strings.soundModeLabel(mode))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.restoreRealPreviousState() },
                        enabled = activeBackup != null,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_previous_state_button")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (activeBackup != null) "${strings.restorePrevStatePrefix} (${strings.soundModeLabel(prevRinger)})" else strings.restorePrevStatePrefix)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Conflict Check
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (conflicts.isNotEmpty()) StatusAmber else StatusEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "${strings.conflictWarningsPrefix} (${conflicts.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        strings.conflictCheckExplain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
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
                                text = strings.conflictPromptSentence(listOf(aName, bName)),
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

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Emergency Access Simulation (Clearly labeled as a simulation)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.emergencyTestTitle, style = MaterialTheme.typography.titleSmall)
                        Surface(
                            color = StatusIndigo.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (strings.isArabic) "محاكاة" else "Simulation",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusIndigo,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        strings.emergencyTestExplain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    val emergency = state.contacts.firstOrNull { it.priorityLevel == ContactPriority.EMERGENCY }
                    OutlinedButton(
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
                        Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${strings.simulateEmergencyCallPrefix} (${emergency?.name ?: "-"})")
                    }
                    state.lastInterruptionDecision?.let { d ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${if (d.isAllowed) strings.allowedUpper else strings.blockedUpper}: ${d.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (d.isAllowed) StatusEmerald else StatusCrimson,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // SECTION 3: AUTOMATION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = strings.settingsAutomationTitle,
                    subtitle = strings.settingsAutomationExplain
                )

                CompanionCard {
                    // Confirm Important Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.confirmImportantLabel, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                strings.confirmImportantExplain,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = state.preferences.requireConfirmationForImportantActions,
                            onCheckedChange = { viewModel.setRequireConfirmation(it) }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Default Profile
                    Text(strings.defaultProfileLabel, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        strings.defaultProfileExplain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.profiles.forEach { p ->
                            FilterChip(
                                selected = state.preferences.defaultProfileId == p.id,
                                onClick = { viewModel.setDefaultProfileId(p.id) },
                                label = { Text(strings.localizedProfileName(p)) }
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // When routines overlap
                    Text(strings.conflictPolicyLabel, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        strings.conflictPolicyExplain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ConflictResolutionPolicy.entries.forEach { policy ->
                            FilterChip(
                                selected = state.preferences.conflictPolicy == policy,
                                onClick = { viewModel.setConflictPolicy(policy) },
                                label = { Text(strings.conflictPolicyLabel(policy)) }
                            )
                        }
                    }
                }
            }
        }

        // SECTION 4: ADVANCED (Collapsed Expandable Section)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = strings.settingsAdvancedTitle,
                    subtitle = strings.settingsAdvancedExplain
                )

                CompanionCard {
                    Surface(
                        onClick = { showAdvancedSection = !showAdvancedSection },
                        color = Color.Transparent,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_advanced_toggle")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                color = StatusIndigo.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Science,
                                        contentDescription = null,
                                        tint = StatusIndigo,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.settingsAdvancedTitle,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = strings.descAdvanced,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (showAdvancedSection) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = showAdvancedSection) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 10.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            // Context Awareness, Demo Playground, Simulation Engine
                            SettingsNavRow(
                                title = strings.screenAdvanced,
                                subtitle = strings.descAdvanced,
                                icon = Icons.Rounded.Science,
                                accentColor = StatusIndigo,
                                onClick = { onNavigate(NavRoutes.ADVANCED) },
                                modifier = Modifier.testTag("settings_advanced_row")
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Privacy & Demo Boundaries
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    strings.privacyTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                SimulatedDemoBadge(strings.demoBadge)
                            }
                            Text(
                                text = strings.privacyExplain,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = strings.privacyBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Demo Data Resets
                            Text(strings.demoMgmtTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = strings.demoMgmtExplain,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (strings.isArabic) {
                                    "${state.profiles.size} أوضاع • ${state.routines.size} روتينات • ${state.contacts.size} جهات • ${state.activityEvents.size} أحداث"
                                } else {
                                    "${state.profiles.size} profiles • ${state.routines.size} routines • ${state.contacts.size} contacts • ${state.activityEvents.size} events"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(onClick = { viewModel.restoreDefaultProfiles() }) {
                                    Text(strings.restoreDefaultsBtn)
                                }
                                OutlinedButton(onClick = { viewModel.resetDemoRoutines() }) {
                                    Text(strings.resetDemoRoutinesBtn)
                                }
                                Button(
                                    onClick = { showResetConfirm = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("settings_reset_demo_btn")
                                ) {
                                    Text(strings.resetDemoDataBtn)
                                }
                            }
                        }
                    }
                }
            }
        }

        // About Card
        item {
            CompanionCard {
                Text(strings.aboutTitle, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    strings.aboutExplain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(strings.appName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "\"${strings.tagline}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.aboutVersionLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showResetConfirm) {
        ConfirmActionDialog(
            title = strings.resetDemoDataBtn,
            message = if (strings.isArabic) {
                "يعيد جميع الأوضاع والروتينات وجهات الاتصال والتحليلات وسجل النشاط إلى بيانات البداية التجريبية."
            } else {
                "Restores all profiles, routines, contacts, analytics, and activity history to the initial demonstration seed."
            },
            confirmText = strings.confirm,
            cancelText = strings.cancel,
            onConfirm = {
                viewModel.resetAllDemoData(resetOnboarding = false)
                showResetConfirm = false
            },
            onDismiss = { showResetConfirm = false }
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

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

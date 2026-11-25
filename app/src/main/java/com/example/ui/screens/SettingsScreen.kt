package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenIntroHeader(
                title = strings.screenSettings,
                subtitle = strings.settingsSubtitle,
                strings = strings,
                onInfoClick = { showHelp = true }
            )
        }

        // 1. Emergency Contacts & More > Activity & Advanced Links
        item {
            CompanionCard {
                SettingsNavRow(
                    title = strings.navContacts,
                    subtitle = "$emergencyCount ${strings.homeCardContactsSubSuffix}",
                    icon = Icons.Rounded.Shield,
                    accentColor = StatusEmerald,
                    onClick = { onNavigate(NavRoutes.CONTACTS) },
                    modifier = Modifier.testTag("settings_contacts_row")
                )
            }
        }

        // More Section -> Activity (Insights + History + Notifications) & Advanced (Context + Demo Playground)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.settingsMoreTitle.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                CompanionCard {
                    SettingsNavRow(
                        title = strings.screenActivity,
                        subtitle = strings.descActivityMerged,
                        icon = Icons.Rounded.BarChart,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { onNavigate(NavRoutes.ACTIVITY) },
                        modifier = Modifier.testTag("settings_activity_row")
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingsNavRow(
                        title = strings.screenAdvanced,
                        subtitle = strings.descAdvanced,
                        icon = Icons.Rounded.Science,
                        accentColor = StatusIndigo,
                        onClick = { onNavigate(NavRoutes.ADVANCED) },
                        modifier = Modifier.testTag("settings_advanced_row")
                    )
                }
            }
        }

        // 2. Safety Section (Moved from Safety Center into Settings)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.screenSafety.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                CompanionCard(
                    containerColor = if (state.preferences.isAutomationPaused) StatusAmber.copy(alpha = 0.10f)
                    else MaterialTheme.colorScheme.surface
                ) {
                    // Global Automation Pause
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

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Manual Override & Restore Previous State
                    val activeName = state.activeProfile?.let { strings.localizedProfileName(it) } ?: "Personal"
                    val soundName = strings.soundModeLabel(state.preferences.simulatedSoundMode)
                    val prev = state.preferences.previousSnapshot
                    val prevProfile = state.profiles.firstOrNull { it.id == prev.activeProfileId }
                    val prevName = prevProfile?.let { strings.localizedProfileName(it) } ?: "Personal"

                    Text(strings.manualOverrideTitle, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${strings.currentSimStatePrefix}: $activeName • $soundName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SoundMode.entries.forEach { mode ->
                            OutlinedButton(
                                onClick = { viewModel.setSimulatedSoundMode(mode, "Settings Override") }
                            ) {
                                Text(strings.soundModeLabel(mode))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.restorePreviousPhoneState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_previous_state_button")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${strings.restorePrevStatePrefix} ($prevName • ${strings.soundModeLabel(prev.soundMode)})")
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Conflict Check & Emergency Access Test
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
                    Spacer(Modifier.height(4.dp))
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

                    Spacer(Modifier.height(10.dp))
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

        // 3. Appearance
        item {
            CompanionCard {
                Text(strings.appearanceTitle, style = MaterialTheme.typography.titleMedium)
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
            }
        }

        // 4. Language (English / Arabic RTL)
        item {
            CompanionCard {
                Text(strings.languageTitle, style = MaterialTheme.typography.titleMedium)
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

        // 5. Automation Preferences
        item {
            CompanionCard {
                Text(strings.automationPrefsTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.confirmImportantLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.preferences.requireConfirmationForImportantActions,
                        onCheckedChange = { viewModel.setRequireConfirmation(it) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(strings.defaultProfileLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.profiles.forEach { p ->
                        FilterChip(
                            selected = state.preferences.defaultProfileId == p.id,
                            onClick = { viewModel.setDefaultProfileId(p.id) },
                            label = { Text(strings.localizedProfileName(p)) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(strings.conflictPolicyLabel, style = MaterialTheme.typography.labelLarge)
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

        // 6. Privacy Disclosure
        item {
            CompanionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.privacyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    SimulatedDemoBadge(strings.simulatedChip)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.privacyBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 7. Demo Management & Replay Onboarding
        item {
            CompanionCard {
                Text(strings.demoMgmtTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (strings.isArabic) {
                        "${state.profiles.size} أوضاع • ${state.routines.size} روتينات • ${state.contacts.size} جهات • ${state.activityEvents.size} أحداث"
                    } else {
                        "${state.profiles.size} profiles • ${state.routines.size} routines • ${state.contacts.size} contacts • ${state.activityEvents.size} events"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
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
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.reopenOnboarding() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_replay_onboarding_btn")
                ) {
                    Text(strings.viewOnboardingBtn)
                }
            }
        }

        // 8. About
        item {
            CompanionCard {
                Text(strings.aboutTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(strings.appName, style = MaterialTheme.typography.titleSmall)
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
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
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

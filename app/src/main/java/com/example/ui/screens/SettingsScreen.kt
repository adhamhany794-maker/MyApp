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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.example.models.AppLanguage
import com.example.models.ConflictResolutionPolicy
import com.example.models.ThemeMode
import com.example.ui.components.CompanionCard
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.SimulatedDemoBadge
import com.example.utils.AppStrings
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.screenSettings, style = MaterialTheme.typography.headlineSmall)
        }

        // Appearance
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

        // Language (English / Arabic RTL)
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

        // Automation Preferences
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
                        strings.pauseAllAutomationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.preferences.isAutomationPaused,
                        onCheckedChange = { viewModel.toggleGlobalAutomationPause() }
                    )
                }
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

        // Privacy Disclosure
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

        // Demo Management & Replay Onboarding
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

        // About
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
}

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusSessionsScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel
) {
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var customMinutesText by remember { mutableStateOf("30") }
    var useCustom by remember { mutableStateOf(false) }
    var selectedProfile by remember(state.profiles) {
        mutableStateOf(state.profiles.firstOrNull { it.id == "profile_study" } ?: state.profiles.firstOrNull())
    }
    var activateProfileOnStart by remember { mutableStateOf(true) }
    var showHelp by remember { mutableStateOf(false) }

    val focus = state.activeFocus

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenIntroHeader(
                title = strings.screenFocus,
                subtitle = strings.focusSubtitle,
                strings = strings,
                onInfoClick = { showHelp = true }
            )
        }

        item {
            CompanionCard(modifier = Modifier.testTag("focus_timer_card")) {
                if (focus.isActive) {
                    val progress = (focus.remainingSeconds.toFloat() / focus.totalSeconds.coerceAtLeast(1).toFloat())
                        .coerceIn(0f, 1f)
                    val elapsedSeconds = (focus.totalSeconds - focus.remainingSeconds).coerceAtLeast(0)
                    val matchedProfile = state.profiles.firstOrNull { it.id == focus.profileId }
                    val displayProfile = matchedProfile?.let { strings.localizedProfileName(it) } ?: focus.profileName

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (focus.isPaused) strings.focusPausedBadge else strings.focusActiveBadge,
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(14.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 10.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = TimeUtils.formatTimer(focus.remainingSeconds),
                                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    modifier = Modifier.testTag("focus_countdown_text")
                                )
                                Text(
                                    text = displayProfile,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        // Both Remaining & Elapsed time metrics side by side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = strings.remainingLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = TimeUtils.formatTimer(focus.remainingSeconds),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = strings.elapsedLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = TimeUtils.formatTimer(elapsedSeconds),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("focus_elapsed_text")
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${focus.simulatedInterruptionsBlocked} ${strings.distractionsBlockedSuffix}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusEmerald,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(14.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.pauseOrResumeFocusSession() },
                                modifier = Modifier.testTag("focus_pause_resume_btn")
                            ) {
                                Icon(
                                    imageVector = if (focus.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (focus.isPaused) strings.resumeBtn else strings.pauseBtn)
                            }
                            OutlinedButton(
                                onClick = { viewModel.endFocusSession(completedNaturally = false) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("focus_end_btn")
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.endBtn)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.simulateBlockedInterruptionDuringFocus() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.simBlockedDistractionBtn)
                        }
                    }
                } else {
                    Text(strings.configureFocusTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Text(strings.durationLabel, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(25, 45, 60).forEach { mins ->
                            FilterChip(
                                selected = !useCustom && selectedMinutes == mins,
                                onClick = { useCustom = false; selectedMinutes = mins },
                                label = { Text(TimeUtils.formatDurationMinutes(mins, strings.isArabic)) },
                                modifier = Modifier.testTag("focus_dur_$mins")
                            )
                        }
                        FilterChip(
                            selected = useCustom,
                            onClick = { useCustom = true },
                            label = { Text(strings.customLabel) }
                        )
                    }
                    if (useCustom) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = { customMinutesText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(strings.customMinutesLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(strings.focusProfileLabel, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.profiles.forEach { p ->
                            FilterChip(
                                selected = selectedProfile?.id == p.id,
                                onClick = { selectedProfile = p },
                                label = { Text(strings.localizedProfileName(p)) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.autoActivateProfile,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = activateProfileOnStart, onCheckedChange = { activateProfileOnStart = it })
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val mins = if (useCustom) {
                                (customMinutesText.toIntOrNull() ?: 30).coerceIn(5, 240)
                            } else {
                                selectedMinutes
                            }
                            selectedProfile?.let {
                                viewModel.startFocusSession(mins, it, activateProfileOnStart)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("focus_start_btn")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.startFocusBtn)
                    }
                }
            }
        }

        state.lastCompletedFocusSummary?.let { summary ->
            item {
                CompanionCard(
                    containerColor = StatusEmerald.copy(alpha = 0.12f)
                ) {
                    Text(strings.sessionSummaryTitle, style = MaterialTheme.typography.titleSmall, color = StatusEmerald)
                    Spacer(Modifier.height(4.dp))
                    Text(summary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Text(strings.sessionHistoryTitle, style = MaterialTheme.typography.titleMedium)
        }

        items(state.focusSessions, key = { it.id }) { session ->
            val sessionProfile = state.profiles.firstOrNull { it.id == session.profileId }
            val sessionProfileName = sessionProfile?.let { strings.localizedProfileName(it) } ?: session.profileName
            val statusLabel = if (strings.isArabic) {
                if (session.status.contains("Completed", ignoreCase = true)) "مكتملة" else "منتهية مبكراً"
            } else {
                session.status
            }

            CompanionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$sessionProfileName • ${TimeUtils.formatDurationMinutes(session.completedMinutes, strings.isArabic)}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "$statusLabel • ${session.simulatedInterruptionsBlocked} ${strings.distractionsBlockedSuffix}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = TimeUtils.formatTimestamp(session.endedAtMillis, strings.isArabic),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.FOCUS_SESSIONS, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

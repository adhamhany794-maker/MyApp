package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val plannedIdleMinutes = if (useCustom) {
        (customMinutesText.toIntOrNull() ?: 30).coerceIn(5, 240)
    } else {
        selectedMinutes
    }
    val ringProgress = if (focus.isActive) {
        (focus.remainingSeconds.toFloat() / focus.totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val centerTimeText = if (focus.isActive) {
        TimeUtils.formatTimer(focus.remainingSeconds)
    } else {
        TimeUtils.formatTimer(plannedIdleMinutes * 60)
    }
    val matchedProfile = if (focus.isActive) {
        state.profiles.firstOrNull { it.id == focus.profileId }
    } else {
        selectedProfile
    }
    val displayProfile = matchedProfile?.let { strings.localizedProfileName(it) } ?: focus.profileName

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenIntroHeader(
                title = strings.screenFocus,
                subtitle = strings.focusSubtitle,
                strings = strings,
                onInfoClick = { showHelp = true }
            )
        }

        // Big Circular Progress Ring + Time in Center + Simple Play, Pause, Reset Buttons
        item {
            CompanionCard(modifier = Modifier.testTag("focus_timer_card")) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (focus.isActive) {
                        Text(
                            text = if (focus.isPaused) strings.focusPausedBadge else strings.focusActiveBadge,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    val ringColor = MaterialTheme.colorScheme.primary
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(216.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * ringProgress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = centerTimeText,
                                style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("focus_countdown_text")
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = displayProfile,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Simple Play, Pause, Reset Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset Button
                        FilledTonalIconButton(
                            onClick = {
                                if (focus.isActive) {
                                    viewModel.resetFocusSession()
                                } else {
                                    useCustom = false
                                    selectedMinutes = 25
                                }
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("focus_reset_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = strings.resetBtn,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play / Start Primary Orange Circle Button
                        Button(
                            onClick = {
                                if (!focus.isActive) {
                                    selectedProfile?.let {
                                        viewModel.startFocusSession(plannedIdleMinutes, it, activateProfileOnStart)
                                    }
                                } else if (focus.isPaused) {
                                    viewModel.pauseOrResumeFocusSession()
                                } else {
                                    viewModel.pauseOrResumeFocusSession()
                                }
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(68.dp)
                                .testTag(if (!focus.isActive) "focus_start_btn" else "focus_pause_resume_btn")
                        ) {
                            Icon(
                                imageVector = if (!focus.isActive || focus.isPaused) {
                                    Icons.Rounded.PlayArrow
                                } else {
                                    Icons.Rounded.Pause
                                },
                                contentDescription = if (!focus.isActive) {
                                    strings.playBtn
                                } else if (focus.isPaused) {
                                    strings.resumeBtn
                                } else {
                                    strings.pauseBtn
                                },
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Pause (when idle, secondary Pause/Stop state or when active, Pause/End)
                        if (focus.isActive) {
                            FilledTonalIconButton(
                                onClick = { viewModel.endFocusSession(completedNaturally = false) },
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("focus_end_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = strings.endBtn,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            FilledTonalIconButton(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Pause,
                                    contentDescription = strings.pauseBtn,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.resetBtn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (!focus.isActive) strings.playBtn
                            else if (focus.isPaused) strings.resumeBtn
                            else strings.pauseBtn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (focus.isActive) strings.endBtn else strings.pauseBtn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (focus.isActive) {
                        val elapsedSeconds = (focus.totalSeconds - focus.remainingSeconds).coerceAtLeast(0)
                        Spacer(Modifier.height(18.dp))
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
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { viewModel.simulateBlockedInterruptionDuringFocus() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.simBlockedDistractionBtn)
                        }
                    }
                }
            }
        }

        // Session setup card when idle
        if (!focus.isActive) {
            item {
                CompanionCard {
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

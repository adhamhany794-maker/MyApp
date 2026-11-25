package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.ContactPriority
import com.example.models.SoundMode
import com.example.ui.components.ActiveStatusPill
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.FeatureInfoButton
import com.example.ui.components.ProfileIconBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.SimulatedDemoBadge
import com.example.ui.components.soundIconFor
import com.example.ui.navigation.NavRoutes
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

@Composable
fun HomeDashboardScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel,
    onNavigate: (String) -> Unit
) {
    var activeHelpTopic by remember { mutableStateOf<FeatureHelpTopic?>(null) }

    val activeProfile = state.activeProfile
    val enabledRoutines = state.routines.filter { it.isEnabled }
    val nextRoutine = enabledRoutines.firstOrNull()
    val totalFocusMinutes = state.focusSessions.sumOf { it.completedMinutes }
    val totalExecutions = state.routines.sumOf { it.executionCount }
    val importantActivityCount = state.contacts.count {
        it.isEnabled && it.priorityLevel != ContactPriority.NORMAL
    } + state.activityEvents.count {
        it.category == com.example.models.EventCategory.CONTACT_EXCEPTION
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Calm Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimeUtils.formattedCurrentDate(strings.isArabic),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    SimulatedDemoBadge(text = strings.demoBadge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = TimeUtils.currentGreeting(strings.isArabic),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureInfoButton(
                        contentDescription = strings.featureHelpContentDesc,
                        onClick = { activeHelpTopic = FeatureHelpTopic.HOME_DASHBOARD }
                    )
                }
                val profileName = activeProfile?.let { strings.localizedProfileName(it) } ?: ""
                Text(
                    text = if (strings.isArabic) {
                        if (state.preferences.isAutomationPaused) {
                            "الأتمتة متوقفة مؤقتاً — يمكنك التحكم يدوياً بأمان."
                        } else {
                            "وضع $profileName نشط الآن مع ${enabledRoutines.size} روتينات جاهزة."
                        }
                    } else {
                        if (state.preferences.isAutomationPaused) {
                            "Automation is paused — manual demo mode is active."
                        } else {
                            "$profileName mode is active with ${enabledRoutines.size} automated routines."
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Conditional Active Focus Banner (only visible when a focus session is live)
        if (state.activeFocus.isActive) {
            item {
                val activeFocusProfileName = state.profiles
                    .firstOrNull { it.id == state.activeFocus.profileId }
                    ?.let { strings.localizedProfileName(it) }
                    ?: state.activeFocus.profileName
                Surface(
                    onClick = { onNavigate(NavRoutes.FOCUS) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_focus_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = strings.screenFocus,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (state.activeFocus.isPaused) {
                                        "${strings.activeFocusBannerPaused} • $activeFocusProfileName"
                                    } else {
                                        "${strings.activeFocusBannerRunning} • $activeFocusProfileName"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${state.activeFocus.simulatedInterruptionsBlocked} ${strings.distractionsBlockedSuffix}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = TimeUtils.formatTimer(state.activeFocus.remainingSeconds),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Current Profile
        item {
            CompanionCard(modifier = Modifier.testTag("current_profile_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = strings.activeProfile,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FeatureInfoButton(
                            contentDescription = strings.featureHelpContentDesc,
                            onClick = { activeHelpTopic = FeatureHelpTopic.SMART_PROFILES }
                        )
                    }
                    ActiveStatusPill(text = strings.activeLabel.uppercase(), isActive = true)
                }

                if (activeProfile != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProfileIconBadge(
                            iconKey = activeProfile.iconKey,
                            accentHex = activeProfile.accentColorHex,
                            size = 48.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.localizedProfileName(activeProfile),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = strings.localizedProfileDescription(activeProfile),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.profiles.forEach { profile ->
                            val selected = profile.id == activeProfile.id
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.activateProfile(profile, "Dashboard Switcher") },
                                label = {
                                    Text(
                                        text = strings.localizedProfileName(profile),
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(profile.accentColorHex).copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = Color(profile.accentColorHex)
                                ),
                                modifier = Modifier.testTag("switch_profile_${profile.id}")
                            )
                        }
                    }
                }
            }
        }

        // 3. Compact Phone Status (Simulated)
        item {
            CompanionCard(modifier = Modifier.testTag("simulated_phone_status_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = strings.simulatedPhoneStatus,
                            style = MaterialTheme.typography.titleMedium
                        )
                        FeatureInfoButton(
                            contentDescription = strings.featureHelpContentDesc,
                            onClick = { activeHelpTopic = FeatureHelpTopic.PHONE_STATUS }
                        )
                    }
                    SimulatedDemoBadge(text = strings.simulatedNotRealOs)
                }
                Text(
                    text = strings.simulatedPhoneHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SoundMode.entries.forEachIndexed { index, mode ->
                        val selected = state.preferences.simulatedSoundMode == mode
                        SegmentedButton(
                            selected = selected,
                            onClick = { viewModel.setSimulatedSoundMode(mode, "Dashboard Toggle") },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = SoundMode.entries.size
                            ),
                            icon = {
                                Icon(
                                    imageVector = soundIconFor(mode),
                                    contentDescription = strings.soundModeLabel(mode),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("sound_mode_${mode.name}")
                        ) {
                            Text(
                                text = strings.soundModeLabel(mode),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Automation Status Card
        item {
            CompanionCard(modifier = Modifier.testTag("automation_status_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ActiveStatusPill(
                            text = if (state.preferences.isAutomationPaused) strings.automationPaused
                            else strings.automationActive,
                            isActive = !state.preferences.isAutomationPaused
                        )
                        Spacer(Modifier.height(8.dp))
                        val nextLabel = nextRoutine?.let {
                            "${strings.localizedRoutineName(it)} (${it.scheduleStartTime})"
                        } ?: "-"
                        Text(
                            text = if (strings.isArabic) {
                                "${enabledRoutines.size} روتينات نشطة • التالي: $nextLabel"
                            } else {
                                "${enabledRoutines.size} active routines • Next: $nextLabel"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.toggleGlobalAutomationPause() },
                        modifier = Modifier.testTag("dashboard_pause_automation_button")
                    ) {
                        Icon(
                            imageVector = if (state.preferences.isAutomationPaused) Icons.Rounded.PlayCircle else Icons.Rounded.PauseCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.preferences.isAutomationPaused) strings.resumeAutomation
                            else strings.pauseAutomation
                        )
                    }
                }
            }
        }

        // 5. Quick Actions (Strictly 3 Primary Modes: Study, Work, Sleep)
        item {
            SectionHeader(title = strings.quickActions)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickModeTile(
                    title = strings.studyQuickTitle,
                    subtitle = strings.studyQuickSub,
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    accent = Color(0xFF3B82F6),
                    isSelected = activeProfile?.id == "profile_study",
                    onClick = {
                        state.profiles.firstOrNull { it.id == "profile_study" }?.let {
                            viewModel.activateProfile(it, "Quick Action")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qa_study")
                )
                QuickModeTile(
                    title = strings.workQuickTitle,
                    subtitle = strings.workQuickSub,
                    icon = Icons.Rounded.Work,
                    accent = Color(0xFF06B6D4),
                    isSelected = activeProfile?.id == "profile_work",
                    onClick = {
                        state.profiles.firstOrNull { it.id == "profile_work" }?.let {
                            viewModel.activateProfile(it, "Quick Action")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qa_work")
                )
                QuickModeTile(
                    title = strings.sleepQuickTitle,
                    subtitle = strings.sleepQuickSub,
                    icon = Icons.Rounded.Bedtime,
                    accent = Color(0xFF818CF8),
                    isSelected = activeProfile?.id == "profile_sleep",
                    onClick = {
                        state.profiles.firstOrNull { it.id == "profile_sleep" }?.let {
                            viewModel.activateProfile(it, "Quick Action")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qa_sleep")
                )
            }
        }

        // 6. Today's Overview (3 concise metrics: Focus Time, Executed Routines, Important Activity)
        item {
            SectionHeader(
                title = strings.todaysOverviewTitle,
                actionLabel = strings.navInsights,
                onActionClick = { onNavigate(NavRoutes.INSIGHTS) }
            )
            Spacer(Modifier.height(8.dp))
            CompanionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OverviewMetricTile(
                        value = TimeUtils.formatDurationMinutes(totalFocusMinutes, strings.isArabic),
                        label = strings.totalFocusLabel,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = { onNavigate(NavRoutes.FOCUS) },
                        modifier = Modifier.weight(1f)
                    )
                    OverviewMetricTile(
                        value = "$totalExecutions",
                        label = strings.executedRoutinesLabel,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = { onNavigate(NavRoutes.ROUTINES) },
                        modifier = Modifier.weight(1f)
                    )
                    OverviewMetricTile(
                        value = "$importantActivityCount",
                        label = strings.importantActivityLabel,
                        accent = StatusEmerald,
                        onClick = { onNavigate(NavRoutes.CONTACTS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 7. Recent Activity Preview + View all
        item {
            SectionHeader(
                title = strings.recentActivity,
                actionLabel = strings.viewAllLabel,
                onActionClick = { onNavigate(NavRoutes.HISTORY) }
            )
            Spacer(Modifier.height(8.dp))
            CompanionCard {
                val recent = state.activityEvents.take(3)
                if (recent.isEmpty()) {
                    Text(
                        text = strings.noRecentActivity,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column {
                        recent.forEachIndexed { idx, ev ->
                            Surface(
                                onClick = { onNavigate(NavRoutes.HISTORY) },
                                color = Color.Transparent,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ev.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${ev.result} • ${TimeUtils.formatTimestamp(ev.timestampMillis, strings.isArabic)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
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
                            if (idx < recent.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    activeHelpTopic?.let { topic ->
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(topic, strings.isArabic),
            strings = strings,
            onDismiss = { activeHelpTopic = null }
        )
    }
}

@Composable
private fun QuickModeTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverviewMetricTile(
    value: String,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

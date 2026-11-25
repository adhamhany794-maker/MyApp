package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.ContactPriority
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.FeatureInfoButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SimulatedDemoBadge
import com.example.ui.navigation.NavRoutes
import com.example.ui.theme.StatusAmber
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

    val enabledRoutines = state.routines.filter { it.isEnabled }
    val nextRoutine = enabledRoutines.firstOrNull()
    val emergencyCount = state.contacts.count {
        it.isEnabled && it.priorityLevel == ContactPriority.EMERGENCY
    }

    val routinesSummary = if (strings.isArabic) {
        val nextText = nextRoutine?.let {
            "${strings.localizedRoutineName(it)} (${it.scheduleStartTime})"
        } ?: "لا يوجد روتين مجدول"
        "${enabledRoutines.size} من ${state.routines.size} مفعّل • التالي: $nextText"
    } else {
        val nextText = nextRoutine?.let {
            "${strings.localizedRoutineName(it)} (${it.scheduleStartTime})"
        } ?: "None scheduled"
        "${enabledRoutines.size} of ${state.routines.size} active • Next: $nextText"
    }

    val emergencySummary = "$emergencyCount ${strings.homeCardContactsSubSuffix}"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Calm Demo / simulation indicator + optional Pause notice ONLY if user paused automation
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimulatedDemoBadge(
                        text = strings.demoBadge,
                        modifier = Modifier.testTag("home_demo_badge")
                    )
                    FeatureInfoButton(
                        contentDescription = strings.featureHelpContentDesc,
                        onClick = { activeHelpTopic = FeatureHelpTopic.HOME_DASHBOARD }
                    )
                }

                if (state.preferences.isAutomationPaused) {
                    Surface(
                        color = StatusAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_paused_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PauseCircleOutline,
                                contentDescription = strings.automationPaused,
                                tint = StatusAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.automationPaused,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.toggleGlobalAutomationPause() }) {
                                Text(strings.resumeAutomation)
                            }
                        }
                    }
                }
            }
        }

        // 1. Large Bold Headline + Subtitle (Product Idea & Emergency Promise)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.homeHeadline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.homeSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Three Clean Action Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HomeFeatureCard(
                    title = strings.homeCardAssistantTitle,
                    subtitle = strings.homeCardAssistantSub,
                    icon = Icons.Rounded.AutoAwesome,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(NavRoutes.ASSISTANT) },
                    modifier = Modifier.testTag("home_card_assistant")
                )

                HomeFeatureCard(
                    title = strings.homeCardRoutinesTitle,
                    subtitle = routinesSummary,
                    icon = Icons.Rounded.Schedule,
                    accent = MaterialTheme.colorScheme.tertiary,
                    onClick = { onNavigate(NavRoutes.ROUTINES) },
                    modifier = Modifier.testTag("home_card_routines")
                )

                HomeFeatureCard(
                    title = strings.homeCardContactsTitle,
                    subtitle = emergencySummary,
                    icon = Icons.Rounded.Shield,
                    accent = StatusEmerald,
                    onClick = { onNavigate(NavRoutes.CONTACTS) },
                    modifier = Modifier.testTag("home_card_contacts")
                )
            }
        }

        // 3. Recent Activity (Last 3 Events)
        item {
            Spacer(Modifier.height(2.dp))
            SectionHeader(
                title = strings.recentActivity,
                actionLabel = strings.viewAllLabel,
                onActionClick = { onNavigate(NavRoutes.ACTIVITY) }
            )
            Spacer(Modifier.height(8.dp))
            CompanionCard(modifier = Modifier.testTag("home_recent_activity_card")) {
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
                                onClick = { onNavigate(NavRoutes.ACTIVITY) },
                                color = Color.Transparent,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
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
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompanionCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
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

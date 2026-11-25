package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.FeatureInfoButton
import com.example.ui.components.SectionHeader
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
    @Suppress("UNUSED_PARAMETER") viewModel: SmartCompanionViewModel,
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
        if (state.preferences.isAutomationPaused) {
            "الأتمتة متوقفة مؤقتاً • ${enabledRoutines.size} من ${state.routines.size} مفعّل"
        } else {
            "${enabledRoutines.size} من ${state.routines.size} مفعّل • التالي: $nextText"
        }
    } else {
        val nextText = nextRoutine?.let {
            "${strings.localizedRoutineName(it)} (${it.scheduleStartTime})"
        } ?: "None scheduled"
        if (state.preferences.isAutomationPaused) {
            "Automation paused • ${enabledRoutines.size} of ${state.routines.size} active"
        } else {
            "${enabledRoutines.size} of ${state.routines.size} active • Next: $nextText"
        }
    }

    val emergencySummary = if (strings.isArabic) {
        "$emergencyCount ${strings.homeCardContactsSubSuffix}"
    } else {
        "$emergencyCount ${strings.homeCardContactsSubSuffix}"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Short Headline + Subtitle (Product Idea)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.homeHeadline,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureInfoButton(
                        contentDescription = strings.featureHelpContentDesc,
                        onClick = { activeHelpTopic = FeatureHelpTopic.HOME_DASHBOARD }
                    )
                }
                Text(
                    text = strings.homeSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Three Primary Action Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    accent = if (state.preferences.isAutomationPaused) StatusAmber else MaterialTheme.colorScheme.secondary,
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

        // 3. Small Recent Activity (Last 3 Events)
        item {
            Spacer(Modifier.height(2.dp))
            SectionHeader(
                title = strings.recentActivity,
                actionLabel = strings.viewAllLabel,
                onActionClick = { onNavigate(NavRoutes.ACTIVITY) }
            )
            Spacer(Modifier.height(6.dp))
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
                    .clip(RoundedCornerShape(14.dp))
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

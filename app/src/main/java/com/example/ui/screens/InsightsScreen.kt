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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.models.ContactPriority
import com.example.models.EventCategory
import com.example.models.TimePeriodFilter
import com.example.simulation.AutomationSimulationEngine
import com.example.ui.components.BarChartEntry
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.HorizontalMetricBars
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.components.VerticalTrendChart
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    onOpenNotificationsModule: () -> Unit,
    showHeader: Boolean = true
) {
    var showHelp by remember { mutableStateOf(false) }

    val totalFocusMins = state.focusSessions.sumOf { it.completedMinutes }
    val goalMins = state.preferences.dailyFocusGoalMinutes.coerceAtLeast(1)
    val todayMins = state.focusSessions.take(2).sumOf { it.completedMinutes }
    val goalRatio = (todayMins.toFloat() / goalMins.toFloat()).coerceIn(0f, 1f)

    val weekStats = state.notificationStats.filter { it.timePeriod == TimePeriodFilter.LAST_7_DAYS }
    val totalInterruptions = weekStats.sumOf { it.totalCount }
    val blockedInterruptions = weekStats.sumOf { it.blockedCount }
    val allowedInterruptions = (totalInterruptions - blockedInterruptions).coerceAtLeast(0)

    val totalExecutions = state.routines.sumOf { it.executionCount }
    val totalSkipped = state.routines.sumOf { it.skippedCount }
    val detectedConflicts = remember(state.routines) {
        AutomationSimulationEngine().findPotentialConflicts(state.routines).size
    } + state.activityEvents.count { it.category == EventCategory.CONFLICT }

    val hasSleepRoutine = state.routines.any { it.name.contains("Sleep", ignoreCase = true) && it.isEnabled }
    val hasEmergencyContact = state.contacts.any { it.priorityLevel == ContactPriority.EMERGENCY && it.isEnabled }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            item {
                ScreenIntroHeader(
                    title = strings.navInsights,
                    subtitle = strings.insightsSubtitle,
                    strings = strings,
                    onInfoClick = { showHelp = true },
                    showSimulatedBadge = true
                )
            }
        }

        // 1. Two Summary Cards at the Top
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    value = TimeUtils.formatDurationMinutes(totalFocusMins, strings.isArabic),
                    label = strings.totalFocusLabel,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("insights_summary_focus")
                )
                KpiCard(
                    value = "$blockedInterruptions",
                    label = strings.blockedLabel,
                    accent = StatusEmerald,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("insights_summary_blocked")
                )
            }
        }

        // 2. Focus Analytics Bar Chart (Rounded Bars) + Goal
        item {
            CompanionCard(modifier = Modifier.testTag("insights_focus_card")) {
                Text(strings.focusTrendTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    strings.focusChartExplain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                val trendBars = state.focusSessions.take(5).reversed().mapIndexed { idx, s ->
                    BarChartEntry(
                        label = if (strings.isArabic) "ج${idx + 1}" else "S${idx + 1}",
                        value = s.completedMinutes.toFloat(),
                        valueLabel = TimeUtils.formatDurationMinutes(s.completedMinutes, strings.isArabic),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (trendBars.isNotEmpty()) {
                    VerticalTrendChart(entries = trendBars)
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.dailyFocusGoal, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${TimeUtils.formatDurationMinutes(todayMins, strings.isArabic)} / ${TimeUtils.formatDurationMinutes(goalMins, strings.isArabic)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { goalRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
            }
        }

        // 3. Interruption Breakdown (Rounded Horizontal Bars)
        item {
            CompanionCard {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.interruptionAnalyticsTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.interruptionChartExplain,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onOpenNotificationsModule) {
                        Text(strings.exploreAppsBtn)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InsightMetric(strings.simTotalLabel, "$totalInterruptions", Modifier.weight(1f))
                    InsightMetric(strings.blockedLabel, "$blockedInterruptions", Modifier.weight(1f), StatusEmerald)
                    InsightMetric(strings.allowedLabel, "$allowedInterruptions", Modifier.weight(1f), StatusAmber)
                }
                Spacer(Modifier.height(16.dp))
                val catBars = weekStats.sortedByDescending { it.totalCount }.take(4).map { stat ->
                    BarChartEntry(
                        label = "${strings.categoryLabel(stat.category)} (${stat.peakHourRange})",
                        value = stat.totalCount.toFloat(),
                        valueLabel = "${stat.totalCount}",
                        color = if (stat.blockedCount > stat.importantCount) MaterialTheme.colorScheme.primary else StatusEmerald
                    )
                }
                HorizontalMetricBars(entries = catBars)
            }
        }

        // 4. Profile Usage & Automation Reliability (Rounded Bars)
        item {
            CompanionCard {
                Text(strings.frequentProfilesLabel, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    strings.profileChartExplain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InsightMetric(strings.executionsLabel, "$totalExecutions", Modifier.weight(1f))
                    InsightMetric(strings.skippedLabel, "$totalSkipped", Modifier.weight(1f), StatusAmber)
                    InsightMetric(strings.conflictsLabel, "$detectedConflicts", Modifier.weight(1f), StatusCrimson)
                }
                Spacer(Modifier.height(16.dp))
                val profileCounts = state.profiles.map { profile ->
                    val score = state.routines.filter { it.targetProfileId == profile.id }.sumOf { it.executionCount } +
                        state.focusSessions.count { it.profileId == profile.id } * 3 +
                        if (state.preferences.activeProfileId == profile.id) 2 else 0
                    BarChartEntry(
                        label = strings.localizedProfileName(profile),
                        value = score.toFloat(),
                        valueLabel = "$score",
                        color = Color(profile.accentColorHex)
                    )
                }.sortedByDescending { it.value }
                HorizontalMetricBars(entries = profileCounts)
            }
        }

        // 5. Concise Recommendations
        item {
            CompanionCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            ) {
                Text(strings.recommendationsTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val recommendations = if (strings.isArabic) {
                    buildList {
                        add("• أعلى فترة تشتيت في البيانات التجريبية بين 7:00 م و 9:00 م.")
                        if (hasSleepRoutine) {
                            add("• روتين النوم مفعّل حالياً مع حماية جهات الطوارئ.")
                        } else {
                            add("• فعّل روتين النوم لكتم تنبيهات التواصل الاجتماعي ليلاً.")
                        }
                        if (!hasEmergencyContact) {
                            add("• أضف جهة طوارئ واحدة على الأقل لضمان وصول المكالمات العاجلة.")
                        } else {
                            add("• لديك ${state.contacts.count { it.isEnabled }} جهات أولوية محمية من الكتم.")
                        }
                    }
                } else {
                    buildList {
                        add("• Peak simulated alert window occurs between 7:00 PM and 9:00 PM.")
                        if (hasSleepRoutine) {
                            add("• Your Sleep routine is active with Emergency contact exceptions.")
                        } else {
                            add("• Enable a Sleep routine to silence overnight social notifications.")
                        }
                        if (!hasEmergencyContact) {
                            add("• Add an Emergency contact so urgent calls bypass Silent mode.")
                        } else {
                            add("• ${state.contacts.count { it.isEnabled }} priority contacts are protected during quiet modes.")
                        }
                    }
                }
                Text(
                    text = recommendations.joinToString("\n"),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.INSIGHTS, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

@Composable
private fun KpiCard(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    CompanionCard(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
            color = accent
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InsightMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

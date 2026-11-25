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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.models.NotificationCategory
import com.example.models.TimePeriodFilter
import com.example.ui.components.BarChartEntry
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.HorizontalMetricBars
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.components.SimulatedDemoBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.viewmodels.SmartCompanionUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationIntelligenceScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    showHeader: Boolean = true
) {
    var selectedPeriod by remember { mutableStateOf(TimePeriodFilter.TODAY) }
    var showHelp by remember { mutableStateOf(false) }
    val filtered = state.notificationStats.filter { it.timePeriod == selectedPeriod }

    val totalCount = filtered.sumOf { it.totalCount }
    val importantCount = filtered.sumOf { it.importantCount }
    val nonImportantCount = (totalCount - importantCount).coerceAtLeast(0)
    val blockedCount = filtered.sumOf { it.blockedCount }
    val topPeak = filtered.maxByOrNull { it.totalCount }?.peakHourRange ?: "7:00 PM – 9:00 PM"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showHeader) {
            item {
                ScreenIntroHeader(
                    title = strings.screenNotifications,
                    subtitle = strings.notifSubtitle,
                    strings = strings,
                    onInfoClick = { showHelp = true },
                    showSimulatedBadge = true
                )
            }
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimePeriodFilter.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(strings.timePeriodLabel(period)) }
                    )
                }
            }
        }

        // Top KPI Summary
        item {
            CompanionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$totalCount",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(strings.simulatedAlertsLabel, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$importantCount",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = StatusEmerald
                        )
                        Text(strings.importantLabel, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$nonImportantCount",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = StatusAmber
                        )
                        Text(strings.nonImportantLabel, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (strings.isArabic) {
                        "فترة الذروة: $topPeak • تم تصفية $blockedCount إشعاراً محاكى"
                    } else {
                        "Peak interruption window: $topPeak • $blockedCount simulated interruptions filtered"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Notifications by Application
        item {
            CompanionCard {
                Text(strings.notifByAppTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val appEntries = filtered.sortedByDescending { it.totalCount }.mapIndexed { idx, stat ->
                    val barColor = when (idx % 4) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.secondary
                        2 -> StatusIndigo
                        else -> StatusAmber
                    }
                    val mutedText = if (strings.isArabic) "${stat.blockedCount} مكتوم" else "${stat.blockedCount} muted"
                    BarChartEntry(
                        label = stat.appName,
                        value = stat.totalCount.toFloat(),
                        valueLabel = "${stat.totalCount} ($mutedText)",
                        color = barColor
                    )
                }
                HorizontalMetricBars(entries = appEntries)
            }
        }

        // Notifications by Category
        item {
            CompanionCard {
                Text(strings.notifByCategoryTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val catEntries = NotificationCategory.entries.map { cat ->
                    val sum = filtered.filter { it.category == cat }.sumOf { it.totalCount }
                    val color = when (cat) {
                        NotificationCategory.SOCIAL_MEDIA -> Color(0xFFDC2626)
                        NotificationCategory.WORK -> Color(0xFF0F294A)
                        NotificationCategory.EDUCATION -> Color(0xFF0284C7)
                        NotificationCategory.COMMUNICATION -> Color(0xFF0D7C7B)
                        NotificationCategory.ENTERTAINMENT -> Color(0xFFD97706)
                        NotificationCategory.SYSTEM -> Color(0xFF64748B)
                        NotificationCategory.OTHER -> Color(0xFF6366F1)
                    }
                    BarChartEntry(
                        label = strings.categoryLabel(cat),
                        value = sum.toFloat(),
                        valueLabel = "$sum",
                        color = color
                    )
                }.sortedByDescending { it.value }
                HorizontalMetricBars(entries = catEntries)
            }
        }

        // Suggested Focus Improvements
        item {
            CompanionCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ) {
                Text(strings.notifSuggestionsTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val topNoise = filtered.maxByOrNull { it.totalCount - it.importantCount }
                val noiseCount = ((topNoise?.totalCount ?: 0) - (topNoise?.importantCount ?: 0)).coerceAtLeast(0)
                Text(
                    text = if (strings.isArabic) {
                        "• ذروة الإزعاج كانت خلال $topPeak.\n" +
                            "• أصدر تطبيق ${topNoise?.appName ?: "التواصل الاجتماعي"} أكبر عدد من التنبيهات غير المهمة ($noiseCount).\n" +
                            "• تفعيل وضع الدراسة أو النوم مساءً يقلل المقاطعات المحاكاة بنسبة تصل إلى 82%."
                    } else {
                        "• High-noise window detected during $topPeak.\n" +
                            "• ${topNoise?.appName ?: "Social apps"} generated the most non-important alerts ($noiseCount).\n" +
                            "• Enabling Study or Sleep Profile during evening hours cuts simulated distractions by up to 82%."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.NOTIFICATIONS, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

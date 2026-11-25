package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.models.ActivityEvent
import com.example.models.EventCategory
import com.example.ui.components.CompanionCard
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityHistoryScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel,
    showHeader: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<EventCategory?>(null) }
    var selectedEvent by remember { mutableStateOf<ActivityEvent?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    val filtered = remember(state.activityEvents, searchQuery, selectedCategory) {
        state.activityEvents.filter { ev ->
            val matchesCategory = selectedCategory == null || ev.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                ev.title.lowercase().contains(q) ||
                ev.result.lowercase().contains(q) ||
                ev.explanation.lowercase().contains(q) ||
                (ev.relatedProfileName?.lowercase()?.contains(q) == true) ||
                (ev.relatedRoutineName?.lowercase()?.contains(q) == true)
            matchesCategory && matchesQuery
        }
    }

    val grouped = remember(filtered, strings.isArabic) {
        filtered.groupBy { TimeUtils.formatDateGroup(it.timestampMillis, strings.isArabic) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (showHeader) {
                    ScreenIntroHeader(
                        title = strings.screenHistory,
                        subtitle = strings.historySubtitle,
                        strings = strings,
                        onInfoClick = { showHelp = true },
                        trailingContent = {
                            IconButton(
                                onClick = { showClearConfirm = true },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = strings.clearHistoryBtn)
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.historyLegend,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (!showHeader) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = strings.clearHistoryBtn)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text(strings.searchHistoryHint) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input")
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(strings.allLabel) }
                )
                EventCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(strings.eventCategoryLabel(cat)) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                CompanionCard {
                    Text(strings.noHistoryTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.noHistoryMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            grouped.forEach { (dateLabel, events) ->
                item {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(events, key = { it.id }) { event ->
                    val isSimulated = event.category in setOf(
                        EventCategory.SOUND_MODE,
                        EventCategory.CONTACT_EXCEPTION,
                        EventCategory.CONTEXT,
                        EventCategory.AUTOMATION,
                        EventCategory.CONFLICT
                    ) || event.triggerSource.contains("Simulat", ignoreCase = true) ||
                        event.triggerSource.contains("Engine", ignoreCase = true)

                    CompanionCard(
                        onClick = { selectedEvent = event },
                        modifier = Modifier.testTag("history_event_${event.id}")
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = strings.eventCategoryLabel(event.category),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = if (isSimulated) StatusAmber.copy(alpha = 0.14f)
                                    else StatusEmerald.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isSimulated) strings.simulatedChip else strings.appEventChip,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = if (isSimulated) StatusAmber else StatusEmerald,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = TimeUtils.formatTimestamp(event.timestampMillis, strings.isArabic),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(event.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${event.triggerSource} → ${event.result}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    selectedEvent?.let { ev ->
        AlertDialog(
            onDismissRequest = { selectedEvent = null },
            modifier = Modifier.widthIn(max = 420.dp),
            title = { Text(ev.title) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${strings.eventCategoryLabel(ev.category)} • ${TimeUtils.formatTimestamp(ev.timestampMillis, strings.isArabic)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${if (strings.isArabic) "المصدر" else "Trigger Source"}: ${ev.triggerSource}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${if (strings.isArabic) "النتيجة" else "Result"}: ${ev.result}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${if (strings.isArabic) "التفسير" else "Explanation"}: ${ev.explanation}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    ev.relatedRoutineName?.let {
                        Text(
                            text = "${strings.navRoutines}: $it",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    ev.relatedProfileName?.let {
                        Text(
                            text = "${strings.screenProfiles}: $it",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEvent = null }) { Text(strings.close) }
            }
        )
    }

    if (showClearConfirm) {
        ConfirmActionDialog(
            title = strings.clearHistoryTitle,
            message = strings.clearHistoryMsg,
            confirmText = strings.clearHistoryBtn,
            cancelText = strings.cancel,
            onConfirm = {
                viewModel.clearActivityHistory()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.ACTIVITY_HISTORY, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.example.models.SimulatedActivity
import com.example.models.SimulatedLocation
import com.example.models.TriggerType
import com.example.simulation.AutomationSimulationEngine
import com.example.simulation.SimulationEvent
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.components.SimulatedDemoBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContextAwarenessScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel,
    showHeader: Boolean = true
) {
    val ctx = state.preferences.simulatedContext
    var customLoc by remember(ctx.customLocationName) { mutableStateOf(ctx.customLocationName) }
    var customAct by remember(ctx.customActivityName) { mutableStateOf(ctx.customActivityName) }
    var showHelp by remember { mutableStateOf(false) }
    val engine = remember { AutomationSimulationEngine() }

    val matchingRoutines = state.routines.filter { routine ->
        if (!routine.isEnabled) return@filter false
        when (routine.trigger.type) {
            TriggerType.SPECIFIC_TIME ->
                routine.trigger.hour == ctx.simulatedHour &&
                    engine.dayMatches(routine.trigger.dayFilter, ctx.simulatedDay)
            TriggerType.LOCATION_ENTERED -> routine.trigger.location == ctx.location
            TriggerType.ACTIVITY_STARTED -> routine.trigger.activity == ctx.activity
            else -> false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showHeader) {
            item {
                ScreenIntroHeader(
                    title = strings.screenContext,
                    subtitle = strings.contextSubtitle,
                    strings = strings,
                    onInfoClick = { showHelp = true },
                    showSimulatedBadge = true
                )
            }
        }

        // Current Context Summary
        item {
            CompanionCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ) {
                Text(strings.currentContextTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val locDisplay = if (ctx.location == SimulatedLocation.CUSTOM) customLoc else strings.locationLabel(ctx.location)
                val actDisplay = if (ctx.activity == SimulatedActivity.CUSTOM) customAct else strings.activityLabel(ctx.activity)
                val timeDisplay = "${TimeUtils.timeContextLabel(ctx.simulatedHour, strings.isArabic)} (${
                    TimeUtils.formatTime12h(ctx.simulatedHour, ctx.simulatedMinute, strings.isArabic)
                }, ${strings.dayFilterLabel(ctx.simulatedDay)})"
                Text(
                    text = if (strings.isArabic) {
                        "الوقت: $timeDisplay\nالموقع: $locDisplay\nالنشاط: $actDisplay"
                    } else {
                        "Time: $timeDisplay\nLocation: $locDisplay\nActivity: $actDisplay"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Time Context
        item {
            CompanionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(strings.timeContextTitle, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(9, 14, 18, 23).forEach { hr ->
                        val label = TimeUtils.timeContextLabel(hr, strings.isArabic)
                        FilterChip(
                            selected = TimeUtils.timeContextLabel(ctx.simulatedHour, strings.isArabic) == label,
                            onClick = { viewModel.updateSimulatedContext(hour = hr, minute = 0) },
                            label = { Text("$label (${TimeUtils.formatTime12h(hr, 0, strings.isArabic)})") }
                        )
                    }
                }
                Text(
                    text = TimeUtils.formatTime12h(ctx.simulatedHour, 0, strings.isArabic),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily)
                )
                Slider(
                    value = ctx.simulatedHour.toFloat(),
                    onValueChange = { viewModel.updateSimulatedContext(hour = it.roundToInt().coerceIn(0, 23), minute = 0) },
                    valueRange = 0f..23f
                )
            }
        }

        // Location Context
        item {
            CompanionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(strings.simLocationTitle, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SimulatedLocation.entries.forEach { loc ->
                        FilterChip(
                            selected = ctx.location == loc,
                            onClick = { viewModel.updateSimulatedContext(location = loc, evaluateEngine = true) },
                            label = { Text(strings.locationLabel(loc)) },
                            modifier = Modifier.testTag("ctx_loc_${loc.name}")
                        )
                    }
                }
                if (ctx.location == SimulatedLocation.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customLoc,
                        onValueChange = {
                            customLoc = it
                            viewModel.updateSimulatedContext(customLocationName = it)
                        },
                        label = { Text(strings.simLocationTitle) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Activity Context
        item {
            CompanionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AutoMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(strings.simActivityTitle, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SimulatedActivity.entries.forEach { act ->
                        FilterChip(
                            selected = ctx.activity == act,
                            onClick = { viewModel.updateSimulatedContext(activity = act, evaluateEngine = true) },
                            label = { Text(strings.activityLabel(act)) },
                            modifier = Modifier.testTag("ctx_act_${act.name}")
                        )
                    }
                }
                if (ctx.activity == SimulatedActivity.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customAct,
                        onValueChange = {
                            customAct = it
                            viewModel.updateSimulatedContext(customActivityName = it)
                        },
                        label = { Text(strings.simActivityTitle) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Matching Routines & Evaluate
        item {
            CompanionCard {
                Text(strings.matchingRoutinesTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (matchingRoutines.isEmpty()) {
                    Text(
                        strings.noMatchingContextRoutines,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    matchingRoutines.forEach { r ->
                        Text(
                            text = "• ${strings.localizedRoutineName(r)} (${strings.priorityTierLabel(r.priority)}) — ${strings.triggerTypeLabel(r.trigger.type)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.runSimulationEvent(
                            SimulationEvent.TimeAndDay(ctx.simulatedHour, ctx.simulatedMinute, ctx.simulatedDay)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("evaluate_context_button")
                ) {
                    Text(strings.testContextBtn)
                }
                state.latestSimulationTrace?.let { trace ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${strings.lastResultLabel}: ${trace.finalResult}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.CONTEXT_AWARENESS, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

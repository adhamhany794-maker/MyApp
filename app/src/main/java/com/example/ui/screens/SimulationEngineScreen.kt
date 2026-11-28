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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import android.text.format.DateFormat
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.DayFilter
import com.example.models.InterruptionType
import com.example.models.PriorityContact
import com.example.models.SimulatedActivity
import com.example.models.SimulatedLocation
import com.example.simulation.SimulationEvent
import com.example.ui.components.CompanionCard
import com.example.ui.components.ExactTimeEntrySection
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.components.SimulatedDemoBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.RoutineTimeInputValidator
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimulationEngineScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel,
    showHeader: Boolean = true
) {
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    var simHour by remember { mutableIntStateOf(state.preferences.simulatedContext.simulatedHour) }
    var simMinute by remember { mutableIntStateOf(state.preferences.simulatedContext.simulatedMinute) }
    var simTimeError by remember { mutableStateOf<String?>(null) }
    var simDay by remember { mutableStateOf(state.preferences.simulatedContext.simulatedDay) }
    var simLocation by remember { mutableStateOf(state.preferences.simulatedContext.location) }
    var simActivity by remember { mutableStateOf(state.preferences.simulatedContext.activity) }
    var selectedContact by remember(state.contacts) { mutableStateOf<PriorityContact?>(state.contacts.firstOrNull()) }
    var showHelp by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showHeader) {
            item {
                ScreenIntroHeader(
                    title = strings.screenSimulation,
                    subtitle = strings.simSubtitle,
                    strings = strings,
                    onInfoClick = { showHelp = true },
                    showSimulatedBadge = true
                )
            }
        }

        // Time & Day Trigger Simulator
        item {
            CompanionCard {
                Text(strings.simClockTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${strings.dayFilterLabel(simDay)} • ${
                        RoutineTimeInputValidator.formatTimeForSetting(simHour, simMinute, is24Hour, strings.isArabic)
                    }",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                ExactTimeEntrySection(
                    label = strings.simClockTitle,
                    hour24 = simHour,
                    minute = simMinute,
                    strings = strings,
                    testTagPrefix = "sim_time",
                    onTimeChanged = { h, m ->
                        simHour = h
                        simMinute = m
                    },
                    onValidationErrorChanged = { simTimeError = it }
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        DayFilter.MONDAY, DayFilter.WEDNESDAY, DayFilter.FRIDAY,
                        DayFilter.SATURDAY, DayFilter.SUNDAY
                    ).forEach { day ->
                        FilterChip(
                            selected = simDay == day,
                            onClick = { simDay = day },
                            label = { Text(strings.dayFilterLabel(day, short = true)) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = { simHour = 9; simMinute = 0; simDay = DayFilter.MONDAY; simTimeError = null }) {
                        Text(if (strings.isArabic) "9 ص عمل" else "9 AM Work")
                    }
                    OutlinedButton(onClick = { simHour = 18; simMinute = 0; simDay = DayFilter.WEDNESDAY; simTimeError = null }) {
                        Text(if (strings.isArabic) "6 م تعارض" else "6 PM Conflict")
                    }
                    OutlinedButton(onClick = { simHour = 23; simMinute = 0; simDay = DayFilter.FRIDAY; simTimeError = null }) {
                        Text(if (strings.isArabic) "11 م نوم" else "11 PM Sleep")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    enabled = simTimeError == null,
                    onClick = {
                        viewModel.updateSimulatedContext(hour = simHour, minute = simMinute, day = simDay)
                        viewModel.runSimulationEvent(SimulationEvent.TimeAndDay(simHour, simMinute, simDay))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_time_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.simEvaluateClockBtn)
                }
            }
        }

        // Location & Activity Simulator
        item {
            CompanionCard {
                Text(strings.simLocActTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(strings.simLocationTitle, style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SimulatedLocation.entries.forEach { loc ->
                        FilterChip(
                            selected = simLocation == loc,
                            onClick = { simLocation = loc },
                            label = { Text(strings.locationLabel(loc)) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.updateSimulatedContext(location = simLocation)
                            viewModel.runSimulationEvent(SimulationEvent.LocationEntered(simLocation.displayName))
                        }
                    ) {
                        Text(strings.simEnterLocBtn)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.runSimulationEvent(SimulationEvent.LocationExited(simLocation.displayName))
                        }
                    ) {
                        Text(strings.simExitLocBtn)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(strings.simActivityTitle, style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SimulatedActivity.entries.forEach { act ->
                        FilterChip(
                            selected = simActivity == act,
                            onClick = { simActivity = act },
                            label = { Text(strings.activityLabel(act)) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        viewModel.updateSimulatedContext(activity = simActivity)
                        viewModel.runSimulationEvent(SimulationEvent.ActivityStarted(simActivity.displayName))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.AutoMode, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("${strings.simStartActBtn}: ${strings.activityLabel(simActivity)}")
                }
            }
        }

        // Incoming Call / Message Simulator
        item {
            CompanionCard {
                Text(strings.simInterruptionTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedContact == null,
                        onClick = { selectedContact = null },
                        label = { Text(strings.normalCallerAutomaticLabel) },
                        modifier = Modifier.testTag("sim_normal_caller_chip")
                    )
                    state.contacts.forEach { contact ->
                        FilterChip(
                            selected = selectedContact?.id == contact.id,
                            onClick = { selectedContact = contact },
                            label = { Text("${contact.name} (${strings.emergencyTierTitle})") }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.simulateIncomingInterruption(selectedContact, InterruptionType.CALL, false)
                        },
                        modifier = Modifier.testTag("sim_incoming_call_btn")
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.testCallBtn)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.simulateIncomingInterruption(selectedContact, InterruptionType.MESSAGE, false)
                        },
                        modifier = Modifier.testTag("sim_incoming_msg_btn")
                    ) {
                        Icon(Icons.Rounded.Message, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.testMsgBtn)
                    }
                }
                state.lastInterruptionDecision?.let { d ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${if (d.isAllowed) strings.allowedUpper else strings.blockedUpper}: ${d.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (d.isAllowed) StatusEmerald else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Live Simulation Execution Trace
        item {
            val trace = state.latestSimulationTrace
            val noneLabel = if (strings.isArabic) "لا يوجد" else "None"
            CompanionCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
                modifier = Modifier.testTag("simulation_trace_card")
            ) {
                Text(strings.simTraceTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (trace == null) {
                    Text(
                        strings.simTraceEmpty,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    TraceRow(strings.simTraceTrigger, trace.triggerReceived)
                    TraceRow(
                        strings.simTraceMatched,
                        trace.matchingRoutineNames.joinToString(", ").ifEmpty { noneLabel }
                    )
                    TraceRow(
                        strings.simTraceConditions,
                        trace.conditionsEvaluated.joinToString("\n").ifEmpty { noneLabel }
                    )
                    TraceRow(
                        strings.simTraceExceptions,
                        trace.exceptionsChecked.joinToString("\n").ifEmpty { noneLabel }
                    )
                    TraceRow(
                        strings.simTraceActions,
                        trace.actionsSelected.joinToString("\n").ifEmpty { noneLabel }
                    )
                    if (trace.conflictDetected) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = StatusAmber)
                            Text(
                                text = trace.conflictExplanation ?: strings.conflictDefaultExplain,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    TraceRow(strings.simTraceFinal, trace.finalResult, highlight = true)
                }
            }
        }
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.SIMULATION_ENGINE, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

@Composable
private fun TraceRow(label: String, value: String, highlight: Boolean = false) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

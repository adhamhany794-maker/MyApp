package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.text.format.DateFormat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.utils.RoutineTimeInputValidator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.models.SmartRoutine
import com.example.ui.components.ActiveStatusPill
import com.example.ui.components.CompanionCard
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ProfileIconBadge
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusAmber
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

private enum class RoutineStatusFilter { ALL, ACTIVE, PAUSED }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinesScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (String) -> Unit,
    onOpenSimulationEngine: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenFocusSessions: () -> Unit,
    onOpenRoutineLog: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(RoutineStatusFilter.ALL) }
    var detailRoutine by remember { mutableStateOf<SmartRoutine?>(null) }
    var deletingRoutine by remember { mutableStateOf<SmartRoutine?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    val activeProfile = state.activeProfile
    val filteredRoutines = remember(state.routines, searchQuery, statusFilter, strings) {
        val q = searchQuery.trim().lowercase()
        state.routines.filter { routine ->
            val matchesFilter = when (statusFilter) {
                RoutineStatusFilter.ALL -> true
                RoutineStatusFilter.ACTIVE -> routine.isEnabled
                RoutineStatusFilter.PAUSED -> !routine.isEnabled
            }
            val matchesQuery = q.isEmpty() ||
                strings.localizedRoutineName(routine).lowercase().contains(q) ||
                strings.localizedRoutineDescription(routine).lowercase().contains(q)
            matchesFilter && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("routines_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenIntroHeader(
                title = strings.navRoutines,
                subtitle = strings.routinesPageExplain,
                strings = strings,
                onInfoClick = { showHelp = true }
            )
            if (state.preferences.isAutomationPaused) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.routinesSubtitlePaused,
                    style = MaterialTheme.typography.labelMedium,
                    color = StatusAmber
                )
            }
        }

        // Focus Sessions Quick Action (merged into Routines)
        item {
            if (state.activeFocus.isActive) {
                val activeFocusProfileName = state.profiles
                    .firstOrNull { it.id == state.activeFocus.profileId }
                    ?.let { strings.localizedProfileName(it) }
                    ?: state.activeFocus.profileName
                Surface(
                    onClick = onOpenFocusSessions,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routines_focus_quick_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                CompanionCard(modifier = Modifier.testTag("routines_focus_quick_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = strings.screenFocus,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(strings.screenFocus, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = strings.descFocusSessions,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val targetProfile = state.profiles.firstOrNull { it.id == "profile_study" }
                                    ?: state.activeProfile
                                    ?: state.profiles.firstOrNull()
                                targetProfile?.let {
                                    viewModel.startFocusSession(25, it, true)
                                }
                            },
                            modifier = Modifier.testTag("routines_quick_start_focus_btn")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.startFocus25m)
                        }
                        OutlinedButton(
                            onClick = onOpenFocusSessions,
                            modifier = Modifier.testTag("routines_open_focus_btn")
                        ) {
                            Text(strings.openTimer)
                        }
                    }
                }
            }
        }

        // Smart Profiles Section (merged into Routines)
        item {
            CompanionCard(modifier = Modifier.testTag("routines_smart_profiles_section")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = strings.screenProfiles,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(strings.screenProfiles, style = MaterialTheme.typography.titleSmall)
                    }
                    TextButton(
                        onClick = onOpenProfiles,
                        modifier = Modifier.testTag("routines_manage_profiles_btn")
                    ) {
                        Text(strings.manageProfilesBtn)
                    }
                }

                if (activeProfile != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileIconBadge(
                            iconKey = activeProfile.iconKey,
                            accentHex = activeProfile.accentColorHex,
                            size = 40.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${strings.localizedProfileName(activeProfile)} • ${strings.soundModeLabel(activeProfile.soundMode)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = strings.localizedProfileDescription(activeProfile),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
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
                                onClick = { viewModel.activateProfile(profile, "Routines Profile Switcher") },
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

        // Primary Add Routine CTA + Simulation & Routine Log Shortcuts
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCreateRoutine,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_routine_fab")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.addRoutineBtn)
                    }
                    OutlinedButton(
                        onClick = onOpenSimulationEngine,
                        modifier = Modifier.testTag("open_simulation_engine_button")
                    ) {
                        Icon(Icons.Rounded.AutoMode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.simulate)
                    }
                }
                OutlinedButton(
                    onClick = onOpenRoutineLog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_routine_log_button")
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.screenRoutineLog)
                }
            }
        }

        // Search & Simple Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchRoutinesHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = strings.close)
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routines_search_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = statusFilter == RoutineStatusFilter.ALL,
                        onClick = { statusFilter = RoutineStatusFilter.ALL },
                        label = { Text("${strings.filterAllRoutines} (${state.routines.size})") },
                        modifier = Modifier.testTag("filter_routines_all")
                    )
                    FilterChip(
                        selected = statusFilter == RoutineStatusFilter.ACTIVE,
                        onClick = { statusFilter = RoutineStatusFilter.ACTIVE },
                        label = { Text("${strings.filterActiveRoutines} (${state.routines.count { it.isEnabled }})") },
                        modifier = Modifier.testTag("filter_routines_active")
                    )
                    FilterChip(
                        selected = statusFilter == RoutineStatusFilter.PAUSED,
                        onClick = { statusFilter = RoutineStatusFilter.PAUSED },
                        label = { Text("${strings.filterPausedRoutines} (${state.routines.count { !it.isEnabled }})") },
                        modifier = Modifier.testTag("filter_routines_paused")
                    )
                }
            }
        }

        if (filteredRoutines.isEmpty()) {
            item {
                CompanionCard {
                    Text(strings.noRoutinesTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.noRoutinesMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(filteredRoutines, key = { it.id }) { routine ->
            val context = LocalContext.current
            val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
            val displayName = strings.localizedRoutineName(routine)
            val displayDesc = strings.localizedRoutineDescription(routine)
            val rangeLabel = RoutineTimeInputValidator.formatRoutineRange(
                routine = routine,
                isArabic = strings.isArabic,
                is24Hour = is24Hour
            )
            val triggerTimeDisplay = rangeLabel ?: RoutineTimeInputValidator.formatTimeForSetting(
                routine.trigger.hour,
                routine.trigger.minute,
                is24Hour,
                strings.isArabic
            )
            val daysDisplay = if (routine.customDays.isNotEmpty()) {
                routine.customDays.joinToString(", ") { strings.dayFilterLabel(it, short = true) }
            } else {
                strings.dayFilterLabel(routine.trigger.dayFilter, short = true)
            }
            val extraWindowsCount = routine.extraTimeWindows.size
            val windowsSuffix = if (extraWindowsCount > 0) " (+$extraWindowsCount)" else ""
            val triggerLabel = "${strings.triggerTypeLabel(routine.trigger.type)} • $triggerTimeDisplay$windowsSuffix ($daysDisplay)"
            val mainAction = routine.actions.firstOrNull()?.let { strings.actionTypeLabel(it.type) } ?: "-"
            var menuExpanded by remember { mutableStateOf(false) }

            CompanionCard(modifier = Modifier.testTag("routine_card_${routine.id}")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ActiveStatusPill(
                                text = if (routine.isEnabled) strings.activeLabel else strings.disabledLabel,
                                isActive = routine.isEnabled
                            )
                            if (routine.enableDoNotDisturb) {
                                ActiveStatusPill(
                                    text = strings.phoneControlDndStatusPrefix,
                                    isActive = true
                                )
                            }
                            if (routine.isOneTime) {
                                ActiveStatusPill(
                                    text = strings.oneTimeRoutineLabel,
                                    isActive = false
                                )
                            }
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = displayDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = routine.isEnabled,
                        onCheckedChange = { viewModel.toggleRoutineEnabled(routine) },
                        modifier = Modifier.testTag("routine_toggle_${routine.id}")
                    )
                }

                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_when_${routine.id}")
                ) {
                    Text(
                        text = if (strings.isArabic) "متى: $triggerLabel" else "WHEN: $triggerLabel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
                if (routine.skipDates.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${strings.skipDatesLabel}: ${routine.skipDates.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = (if (strings.isArabic) "الإجراء الرئيسي: " else "Action: ") + mainAction +
                        (if (routine.enableDoNotDisturb) " • ${strings.phoneControlDndStatusPrefix}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { viewModel.runRoutineManually(routine) },
                            modifier = Modifier.testTag("run_routine_${routine.id}")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.runNow)
                        }
                        OutlinedButton(
                            onClick = { onEditRoutine(routine.id) },
                            modifier = Modifier.testTag("edit_routine_${routine.id}")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.edit)
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("routine_more_${routine.id}")
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = strings.moreOptionsDesc)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.viewDetails) },
                                leadingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    detailRoutine = routine
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.duplicate) },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.duplicateRoutine(routine)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    deletingRoutine = routine
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    detailRoutine?.let { routine ->
        val displayName = strings.localizedRoutineName(routine)
        val displayDesc = strings.localizedRoutineDescription(routine)
        AlertDialog(
            onDismissRequest = { detailRoutine = null },
            modifier = Modifier.widthIn(max = 420.dp),
            title = { Text(displayName) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(displayDesc, style = MaterialTheme.typography.bodyMedium)
                    val detailContext = LocalContext.current
                    val detail24h = remember(detailContext) { DateFormat.is24HourFormat(detailContext) }
                    val detailTime = RoutineTimeInputValidator.formatRoutineRange(
                        routine = routine,
                        isArabic = strings.isArabic,
                        is24Hour = detail24h
                    ) ?: RoutineTimeInputValidator.formatTimeForSetting(
                        routine.trigger.hour,
                        routine.trigger.minute,
                        detail24h,
                        strings.isArabic
                    )
                    Text(
                        text = (if (strings.isArabic) "متى: " else "WHEN: ") +
                            "${strings.triggerTypeLabel(routine.trigger.type)} ($detailTime، ${strings.dayFilterLabel(routine.trigger.dayFilter)})",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = (if (strings.isArabic) "الشروط: " else "CONDITIONS: ") +
                            routine.conditions.joinToString { "${strings.conditionTypeLabel(it.type)}: ${it.value}" }
                                .ifEmpty { if (strings.isArabic) "لا يوجد" else "None" },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = (if (strings.isArabic) "الإجراءات:\n" else "ACTIONS:\n") +
                            routine.actions.joinToString("\n") { "• ${strings.actionTypeLabel(it.type)} (${it.targetValue})" },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = (if (strings.isArabic) "الاستثناءات: " else "EXCEPTIONS: ") +
                            routine.exceptions.joinToString { strings.exceptionTypeLabel(it.type) }
                                .ifEmpty { if (strings.isArabic) "لا يوجد" else "None" },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${strings.lastResultLabel}: ${routine.lastExecutionResult}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = if (strings.isArabic) {
                            "تم التنفيذ ${routine.executionCount} مرة • تم التخطي ${routine.skippedCount} مرة" +
                                (routine.lastExecutedAtMillis?.let { "\nآخر تشغيل: ${TimeUtils.formatTimestamp(it, true)}" } ?: "")
                        } else {
                            "Executed ${routine.executionCount} times • Skipped ${routine.skippedCount} times" +
                                (routine.lastExecutedAtMillis?.let { "\nLast run: ${TimeUtils.formatTimestamp(it, false)}" } ?: "")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.runRoutineManually(routine)
                    detailRoutine = null
                }) {
                    Text(strings.executeNow)
                }
            },
            dismissButton = {
                TextButton(onClick = { detailRoutine = null }) { Text(strings.close) }
            }
        )
    }

    deletingRoutine?.let { target ->
        ConfirmActionDialog(
            title = strings.deleteRoutineTitle,
            message = strings.deleteRoutineMsg,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onConfirm = {
                viewModel.deleteRoutine(target)
                deletingRoutine = null
            },
            onDismiss = { deletingRoutine = null }
        )
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.ROUTINES, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

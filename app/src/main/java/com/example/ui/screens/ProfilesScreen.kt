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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.NotificationCategory
import com.example.models.ProfileIconKey
import com.example.models.SmartProfile
import com.example.models.SoundMode
import com.example.ui.components.CompanionCard
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ProfileIconBadge
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.components.SimulatedDemoBadge
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfilesScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel
) {
    var editingProfile by remember { mutableStateOf<SmartProfile?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var deletingProfile by remember { mutableStateOf<SmartProfile?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isCreatingNew = true
                    editingProfile = SmartProfile(
                        id = "profile_custom_${UUID.randomUUID().toString().take(6)}",
                        name = "",
                        description = "",
                        iconKey = ProfileIconKey.CUSTOM,
                        accentColorHex = 0xFF0284C7,
                        soundMode = SoundMode.VIBRATE,
                        allowedCategories = listOf(NotificationCategory.COMMUNICATION),
                        priorityContactBehavior = "Emergency contacts allowed",
                        allowHighPriorityContacts = false,
                        defaultFocusMinutes = 30,
                        isDefault = false,
                        isProtected = false
                    )
                },
                modifier = Modifier.testTag("add_profile_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.newProfileTitle)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenIntroHeader(
                    title = strings.screenProfiles,
                    subtitle = strings.profilesSubtitle,
                    strings = strings,
                    onInfoClick = { showHelp = true },
                    trailingContent = {
                        TextButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.testTag("restore_profiles_button")
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.restore)
                        }
                    }
                )
            }

            items(state.profiles, key = { it.id }) { profile ->
                val isActive = state.preferences.activeProfileId == profile.id
                val associatedRoutines = state.routines.filter { it.targetProfileId == profile.id }

                CompanionCard(
                    containerColor = if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier.testTag("profile_card_${profile.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileIconBadge(
                            iconKey = profile.iconKey,
                            accentHex = profile.accentColorHex,
                            size = 48.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = strings.localizedProfileName(profile),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (profile.isProtected) {
                                    SimulatedDemoBadge(strings.defaultBadge)
                                }
                                Surface(
                                    color = StatusEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (strings.isArabic) "فعلي" else "Real",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusEmerald,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = strings.localizedProfileDescription(profile),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${strings.soundModeLabel(profile.soundMode)}") }
                        )
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    if (strings.isArabic) "تركيز: ${profile.defaultFocusMinutes} د"
                                    else "Focus: ${profile.defaultFocusMinutes}m"
                                )
                            }
                        )
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    if (strings.isArabic) "${associatedRoutines.size} روتينات"
                                    else "${associatedRoutines.size} routines"
                                )
                            }
                        )
                    }

                    if (profile.allowedCategories.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${strings.allowedCategoriesLabel}: ${profile.allowedCategories.joinToString { strings.categoryLabel(it) }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isActive) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        strings.activeLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.activateProfile(profile, "Smart Profiles") },
                                modifier = Modifier.testTag("activate_profile_${profile.id}")
                            ) {
                                Text(strings.activate)
                            }
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    isCreatingNew = false
                                    editingProfile = profile
                                }
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.edit)
                            }
                            IconButton(
                                onClick = { viewModel.duplicateProfile(profile) }
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = strings.duplicate)
                            }
                            if (!profile.isProtected) {
                                IconButton(
                                    onClick = { deletingProfile = profile }
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = strings.delete,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingProfile?.let { draft ->
        ProfileEditorDialog(
            initial = draft,
            isNew = isCreatingNew,
            strings = strings,
            onDismiss = { editingProfile = null },
            onSave = { updated ->
                viewModel.saveProfile(updated)
                editingProfile = null
            }
        )
    }

    deletingProfile?.let { target ->
        ConfirmActionDialog(
            title = strings.deleteProfileTitle,
            message = strings.deleteProfileMsg,
            confirmText = strings.delete,
            cancelText = strings.cancel,
            onConfirm = {
                viewModel.deleteProfile(target)
                deletingProfile = null
            },
            onDismiss = { deletingProfile = null }
        )
    }

    if (showRestoreDialog) {
        ConfirmActionDialog(
            title = strings.restoreProfilesTitle,
            message = strings.restoreProfilesMsg,
            confirmText = strings.restoreDefaultsBtn,
            cancelText = strings.cancel,
            isDestructive = false,
            onConfirm = {
                viewModel.restoreDefaultProfiles()
                showRestoreDialog = false
            },
            onDismiss = { showRestoreDialog = false }
        )
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.SMART_PROFILES, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditorDialog(
    initial: SmartProfile,
    isNew: Boolean,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (SmartProfile) -> Unit
) {
    var name by remember { mutableStateOf(if (isNew) initial.name else strings.localizedProfileName(initial)) }
    var description by remember {
        mutableStateOf(if (isNew) initial.description else strings.localizedProfileDescription(initial))
    }
    var soundMode by remember { mutableStateOf(initial.soundMode) }
    var allowHighPriority by remember { mutableStateOf(initial.allowHighPriorityContacts) }
    var selectedCategories by remember { mutableStateOf(initial.allowedCategories.toSet()) }
    var focusMinutes by remember { mutableStateOf(initial.defaultFocusMinutes.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 420.dp),
        title = {
            Text(
                if (isNew) strings.newProfileTitle
                else "${strings.editProfileTitle}: ${strings.localizedProfileName(initial)}"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text(strings.profileNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.profileDescLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = focusMinutes,
                    onValueChange = { focusMinutes = it.filter { c -> c.isDigit() } },
                    label = { Text(strings.defaultFocusMinsLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(if (strings.isArabic) "وضع صوت الهاتف" else "Phone Sound Mode", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoundMode.entries.forEach { mode ->
                        FilterChip(
                            selected = soundMode == mode,
                            onClick = { soundMode = mode },
                            label = { Text(strings.soundModeLabel(mode)) }
                        )
                    }
                }
                Surface(
                    color = StatusEmerald.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_emergency_always_note")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = StatusEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = strings.allowEmergencyAlwaysNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.allowHighPriorityContacts, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = allowHighPriority, onCheckedChange = { allowHighPriority = it })
                }
                Text(strings.allowedCategoriesLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NotificationCategory.entries.forEach { cat ->
                        val checked = cat in selectedCategories
                        FilterChip(
                            selected = checked,
                            onClick = {
                                selectedCategories = if (checked) selectedCategories - cat else selectedCategories + cat
                            },
                            label = { Text(strings.categoryLabel(cat)) }
                        )
                    }
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = strings.profileNameError
                        return@Button
                    }
                    val mins = focusMinutes.toIntOrNull()?.coerceIn(5, 480) ?: 30
                    val behavior = if (allowHighPriority) {
                        "Emergency contacts always allowed; High-Priority contacts allowed"
                    } else {
                        "Emergency contacts always allowed"
                    }
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            description = description.trim().ifEmpty { "Custom smart phone profile." },
                            soundMode = soundMode,
                            allowHighPriorityContacts = allowHighPriority,
                            priorityContactBehavior = behavior,
                            allowedCategories = selectedCategories.toList(),
                            defaultFocusMinutes = mins
                        )
                    )
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

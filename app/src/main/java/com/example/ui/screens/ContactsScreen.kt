package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.automation.PhoneContactPickerResolver
import com.example.models.ContactPriority
import com.example.models.InterruptionType
import com.example.models.PriorityContact
import com.example.ui.components.CompanionCard
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactsScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel
) {
    val context = LocalContext.current
    var editingContact by remember { mutableStateOf<PriorityContact?>(null) }
    var isNewContact by remember { mutableStateOf(false) }
    var deletingContact by remember { mutableStateOf<PriorityContact?>(null) }
    var testContact by remember(state.contacts) { mutableStateOf(state.contacts.firstOrNull()) }
    var repeatCaller by remember { mutableStateOf(false) }
    var showTester by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showContactsRationaleDialog by remember { mutableStateOf(false) }
    var showContactsDeniedDialog by remember { mutableStateOf(false) }
    var missingNumberNotice by remember { mutableStateOf<String?>(null) }

    fun openManualAddDialog(prefillName: String = "") {
        isNewContact = true
        editingContact = PriorityContact(
            id = "contact_${UUID.randomUUID().toString().take(6)}",
            name = prefillName,
            initials = "",
            relationship = "",
            priorityLevel = ContactPriority.EMERGENCY,
            allowedInterruptions = listOf(InterruptionType.CALL, InterruptionType.MESSAGE, InterruptionType.REPEAT_CALLER),
            isEnabled = true,
            allowRepeatCallers = true,
            phoneNumber = ""
        )
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            val picked = PhoneContactPickerResolver.resolveFromUri(context.contentResolver, uri)
            if (picked != null && picked.phoneNumber.isNotBlank()) {
                viewModel.savePickedPhoneContact(
                    name = picked.name,
                    phoneNumber = picked.phoneNumber
                )
            } else {
                missingNumberNotice = if (strings.isArabic) {
                    "جهة الاتصال المختارة لا تحتوي على رقم هاتف — يمكنك إدخال الرقم يدوياً."
                } else {
                    "Selected contact has no phone number — please enter it manually."
                }
                openManualAddDialog(prefillName = picked?.name.orEmpty())
            }
        }
    }

    val readContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pickContactLauncher.launch(null)
        } else {
            showContactsDeniedDialog = true
        }
    }

    val groupedTiers = remember(state.contacts) {
        listOf(
            ContactPriority.EMERGENCY to state.contacts.filter { it.priorityLevel == ContactPriority.EMERGENCY },
            ContactPriority.HIGH_PRIORITY to state.contacts.filter { it.priorityLevel == ContactPriority.HIGH_PRIORITY },
            ContactPriority.NORMAL to state.contacts.filter { it.priorityLevel == ContactPriority.NORMAL }
        ).filter { it.second.isNotEmpty() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openManualAddDialog() },
                modifier = Modifier.testTag("add_contact_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.addContactTitle)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("contacts_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenIntroHeader(
                    title = strings.navContacts,
                    subtitle = strings.contactsSubtitle,
                    strings = strings,
                    onInfoClick = { showHelp = true },
                    showSimulatedBadge = false
                )
            }

            // Pick from Phone Contacts + Manual Add actions
            item {
                CompanionCard {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    pickContactLauncher.launch(null)
                                } else {
                                    showContactsRationaleDialog = true
                                }
                            },
                            modifier = Modifier.testTag("choose_phone_contact_btn")
                        ) {
                            Icon(Icons.Rounded.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.chooseFromPhoneContactsBtn)
                        }
                        OutlinedButton(
                            onClick = { openManualAddDialog() },
                            modifier = Modifier.testTag("add_contact_manual_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.addManuallyBtn)
                        }
                    }
                }
            }

            // Plain-language explanation of priority tiers
            item {
                CompanionCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = strings.contactsPriorityExplain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Collapsible Interruption Tester (keeps Contacts clean by default)
            item {
                val activeProfileName = state.activeProfile?.let { strings.localizedProfileName(it) }
                    ?: strings.localizedProfileName("profile_personal", "Personal")
                val soundLabel = strings.soundModeLabel(state.preferences.simulatedSoundMode)

                CompanionCard(modifier = Modifier.testTag("interruption_simulator_card")) {
                    TextButton(
                        onClick = { showTester = !showTester },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_interruption_tester")
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.toggleInterruptionTester,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${strings.activeProfile}: $activeProfileName ($soundLabel)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (showTester) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = showTester) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Spacer(Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.contacts.forEach { c ->
                                    FilterChip(
                                        selected = testContact?.id == c.id,
                                        onClick = { testContact = c },
                                        label = { Text(c.name) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    strings.repeatCallerLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(checked = repeatCaller, onCheckedChange = { repeatCaller = it })
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        testContact?.let {
                                            viewModel.simulateIncomingInterruption(it, InterruptionType.CALL, repeatCaller)
                                        }
                                    },
                                    modifier = Modifier.testTag("test_call_button")
                                ) {
                                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.testCallBtn)
                                }
                                OutlinedButton(
                                    onClick = {
                                        testContact?.let {
                                            viewModel.simulateIncomingInterruption(it, InterruptionType.MESSAGE, repeatCaller)
                                        }
                                    },
                                    modifier = Modifier.testTag("test_message_button")
                                ) {
                                    Icon(Icons.Rounded.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.testMsgBtn)
                                }
                            }

                            state.lastInterruptionDecision?.let { decision ->
                                Surface(
                                    color = if (decision.isAllowed) StatusEmerald.copy(alpha = 0.14f)
                                    else StatusCrimson.copy(alpha = 0.12f),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (decision.isAllowed) {
                                                "${strings.allowedUpper} • ${decision.contactName} (${strings.interruptionTypeLabel(decision.interruptionType)})"
                                            } else {
                                                "${strings.blockedUpper} • ${decision.contactName} (${strings.interruptionTypeLabel(decision.interruptionType)})"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (decision.isAllowed) StatusEmerald else StatusCrimson,
                                            modifier = Modifier.testTag("interruption_decision_title")
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = decision.explanation,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Grouped Contact Lists by Priority Tier
            groupedTiers.forEach { (tier, tierContacts) ->
                val tierTitle = when (tier) {
                    ContactPriority.EMERGENCY -> strings.emergencyTierTitle
                    ContactPriority.HIGH_PRIORITY -> strings.highPriorityTierTitle
                    ContactPriority.NORMAL -> strings.normalTierTitle
                }

                item(key = "tier_${tier.name}") {
                    val tierColor = when (tier) {
                        ContactPriority.EMERGENCY -> StatusCrimson
                        ContactPriority.HIGH_PRIORITY -> StatusAmber
                        ContactPriority.NORMAL -> MaterialTheme.colorScheme.secondary
                    }
                    Text(
                        text = "$tierTitle (${tierContacts.size})".uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = tierColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                items(tierContacts, key = { it.id }) { contact ->
                    val tierColor = when (tier) {
                        ContactPriority.EMERGENCY -> StatusCrimson
                        ContactPriority.HIGH_PRIORITY -> StatusAmber
                        ContactPriority.NORMAL -> MaterialTheme.colorScheme.secondary
                    }
                    CompanionCard(modifier = Modifier.testTag("contact_card_${contact.id}")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(tierColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.initials,
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = tierColor
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, style = MaterialTheme.typography.titleMedium)
                                if (contact.phoneNumber.isNotBlank()) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = contact.phoneNumber,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.testTag("contact_phone_${contact.id}")
                                        )
                                    }
                                }
                                Text(
                                    text = contact.relationship,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = contact.isEnabled,
                                onCheckedChange = {
                                    viewModel.saveContact(contact.copy(isEnabled = !contact.isEnabled))
                                }
                            )
                        }
                        if (contact.priorityLevel == ContactPriority.EMERGENCY) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = StatusEmerald.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("emergency_always_badge_${contact.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = StatusEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = strings.emergencyAlwaysReachesStatus,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StatusEmerald
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = tierColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = contact.allowedInterruptions.joinToString(" • ") { strings.interruptionTypeLabel(it) }
                                        .ifEmpty { if (strings.isArabic) "بلا استثناءات" else "No exceptions" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tierColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            TextButton(
                                onClick = {
                                    isNewContact = false
                                    editingContact = contact
                                }
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.edit)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showContactsRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showContactsRationaleDialog = false },
            modifier = Modifier.testTag("read_contacts_rationale_dialog"),
            title = { Text(strings.contactsPermDialogTitle) },
            text = {
                Text(
                    text = strings.contactsPermDialogBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showContactsRationaleDialog = false
                        readContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    modifier = Modifier.testTag("confirm_read_contacts_permission_btn")
                ) {
                    Text(strings.contactsPermAllowBtn)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showContactsRationaleDialog = false
                        openManualAddDialog()
                    },
                    modifier = Modifier.testTag("fallback_manual_contact_btn")
                ) {
                    Text(strings.addManuallyBtn)
                }
            }
        )
    }

    if (showContactsDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showContactsDeniedDialog = false },
            title = { Text(strings.contactsPermDeniedTitle) },
            text = { Text(strings.contactsPermDeniedBody) },
            confirmButton = {
                Button(
                    onClick = {
                        showContactsDeniedDialog = false
                        openManualAddDialog()
                    }
                ) {
                    Text(strings.addManuallyBtn)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showContactsDeniedDialog = false
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}")
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        }
                    }
                ) {
                    Text(if (strings.isArabic) "الإعدادات" else "Settings")
                }
            }
        )
    }

    editingContact?.let { draft ->
        ContactEditorDialog(
            initial = draft,
            isNew = isNewContact,
            initialError = missingNumberNotice,
            strings = strings,
            onDismiss = {
                editingContact = null
                missingNumberNotice = null
            },
            onDelete = if (!isNewContact) {
                {
                    deletingContact = draft
                    editingContact = null
                    missingNumberNotice = null
                }
            } else null,
            onSave = { saved ->
                viewModel.saveContact(saved)
                editingContact = null
                missingNumberNotice = null
            }
        )
    }

    deletingContact?.let { target ->
        ConfirmActionDialog(
            title = strings.removeContactTitle,
            message = strings.removeContactMsg,
            confirmText = strings.removeBtn,
            cancelText = strings.cancel,
            onConfirm = {
                viewModel.deleteContact(target)
                deletingContact = null
            },
            onDismiss = { deletingContact = null }
        )
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.CONTACTS, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactEditorDialog(
    initial: PriorityContact,
    isNew: Boolean,
    initialError: String? = null,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (PriorityContact) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var phoneNumber by remember { mutableStateOf(initial.phoneNumber) }
    var relationship by remember { mutableStateOf(initial.relationship) }
    var priority by remember { mutableStateOf(initial.priorityLevel) }
    var interruptions by remember { mutableStateOf(initial.allowedInterruptions.toSet()) }
    var allowRepeat by remember { mutableStateOf(initial.allowRepeatCallers) }
    var errorMsg by remember { mutableStateOf<String?>(initialError) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 420.dp),
        title = { Text(if (isNew) strings.addContactTitle else strings.editContactTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text(strings.contactNameLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_name_input")
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it; errorMsg = null },
                    label = { Text(strings.contactPhoneLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_phone_input")
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text(strings.contactRelLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(strings.contactPriorityLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ContactPriority.entries.forEach { level ->
                        FilterChip(
                            selected = priority == level,
                            onClick = { priority = level },
                            label = { Text(strings.contactPriorityLabel(level)) }
                        )
                    }
                }
                Text(strings.contactTypesLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InterruptionType.entries.forEach { type ->
                        val active = type in interruptions
                        FilterChip(
                            selected = active,
                            onClick = {
                                interruptions = if (active) interruptions - type else interruptions + type
                            },
                            label = { Text(strings.interruptionTypeLabel(type)) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.allowRepeatCallers, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = allowRepeat, onCheckedChange = { allowRepeat = it })
                }
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.removeBtn)
                    }
                }
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) {
                    errorMsg = strings.contactNameError
                    return@Button
                }
                val initials = name.trim().split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifEmpty { "SC" }
                onSave(
                    initial.copy(
                        name = name.trim(),
                        initials = initials,
                        relationship = relationship.trim().ifEmpty { if (strings.isArabic) "جهة اتصال" else "Contact" },
                        priorityLevel = priority,
                        allowedInterruptions = interruptions.toList(),
                        allowRepeatCallers = allowRepeat,
                        phoneNumber = phoneNumber.trim()
                    )
                )
            }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

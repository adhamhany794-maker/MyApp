package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.automation.PhoneContactPickerResolver
import com.example.automation.PickedPhoneContact
import com.example.models.ContactPriority
import com.example.models.PriorityContact
import com.example.ui.components.CompanionCard
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.FeatureInfoButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SimulatedDemoBadge
import com.example.ui.navigation.NavRoutes
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
fun HomeDashboardScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPhoneControlState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var activeHelpTopic by remember { mutableStateOf<FeatureHelpTopic?>(null) }
    var showContactsRationaleDialog by remember { mutableStateOf(false) }
    var showContactsDeniedDialog by remember { mutableStateOf(false) }
    var showWriteContactsRationaleDialog by remember { mutableStateOf(false) }
    var pendingPickedContact by remember { mutableStateOf<PickedPhoneContact?>(null) }
    var deletingContact by remember { mutableStateOf<PriorityContact?>(null) }

    val writeContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingPickedContact?.let { picked ->
            viewModel.savePickedPhoneContact(
                name = picked.name,
                phoneNumber = picked.phoneNumber,
                systemContactId = picked.systemContactId
            )
            pendingPickedContact = null
        }
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            val picked = PhoneContactPickerResolver.resolveFromUri(context.contentResolver, uri)
            if (picked != null && picked.phoneNumber.isNotBlank()) {
                val writeGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
                if (writeGranted) {
                    viewModel.savePickedPhoneContact(
                        name = picked.name,
                        phoneNumber = picked.phoneNumber,
                        systemContactId = picked.systemContactId
                    )
                } else {
                    pendingPickedContact = picked
                    showWriteContactsRationaleDialog = true
                }
            } else if (picked != null) {
                viewModel.notifyPickedContactMissingPhoneNumber(picked.name)
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

    fun startPickPhoneContactFlow() {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (readGranted) {
            pickContactLauncher.launch(null)
        } else {
            showContactsRationaleDialog = true
        }
    }

    val enabledRoutines = state.routines.filter { it.isEnabled }
    val nextRoutine = enabledRoutines.firstOrNull()
    val emergencyContacts = state.contacts.filter { it.priorityLevel == ContactPriority.EMERGENCY }

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

        // 1. Large Bold Headline + Subtitle
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

        // 2. Assistant & Routines Cards
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
            }
        }

        // 3. Single Emergency Contacts Section (pick from phone contacts only via "+")
        item {
            CompanionCard(
                modifier = Modifier.testTag("home_contacts_section")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StatusEmerald.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = strings.emergencyTierTitle,
                                tint = StatusEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = strings.emergencyTierTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = StatusEmerald.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${emergencyContacts.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = StatusEmerald,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = strings.contactsPriorityExplain,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    FilledTonalIconButton(
                        onClick = { startPickPhoneContactFlow() },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = StatusEmerald.copy(alpha = 0.18f),
                            contentColor = StatusEmerald
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("choose_phone_contact_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = strings.chooseFromPhoneContactsBtn
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Backup Notification Access compact status
                val notifAccessOn = state.phoneControl.hasNotificationListenerAccess
                Surface(
                    color = if (notifAccessOn) {
                        StatusEmerald.copy(alpha = 0.09f)
                    } else {
                        StatusAmber.copy(alpha = 0.12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (notifAccessOn) StatusEmerald.copy(alpha = 0.30f) else StatusAmber.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contacts_notif_backup_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (notifAccessOn) Icons.Rounded.VerifiedUser else Icons.Rounded.WarningAmber,
                                contentDescription = strings.screenNotificationAccess,
                                tint = if (notifAccessOn) StatusEmerald else StatusAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (notifAccessOn) strings.notifAccessActiveTitle else strings.notifAccessWarningOffTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (notifAccessOn) StatusEmerald else StatusAmber,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedButton(
                            onClick = { onNavigate(NavRoutes.NOTIFICATION_ACCESS) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contacts_notification_access_btn")
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.notifAccessManageBtn)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (emergencyContacts.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_contacts_empty")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.emptyEmergencyContactsMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("home_contacts_list")
                    ) {
                        emergencyContacts.forEach { contact ->
                            HomeEmergencyContactRow(
                                contact = contact,
                                strings = strings,
                                onDelete = { deletingContact = contact }
                            )
                        }
                    }
                }
            }
        }

        // 4. Recent Activity (Last 3 Events)
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
                    onClick = { showContactsRationaleDialog = false },
                    modifier = Modifier.testTag("dismiss_read_contacts_rationale_btn")
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showContactsDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showContactsDeniedDialog = false },
            modifier = Modifier.testTag("read_contacts_denied_dialog"),
            title = { Text(strings.contactsPermDeniedTitle) },
            text = {
                Text(
                    text = strings.contactsPermDeniedBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showContactsDeniedDialog = false
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.testTag("open_app_settings_contacts_btn")
                ) {
                    Text(strings.openAppSettingsBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactsDeniedDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }

    if (showWriteContactsRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showWriteContactsRationaleDialog = false },
            modifier = Modifier.testTag("write_contacts_rationale_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = strings.writeContactsPermDialogTitle,
                    tint = StatusEmerald
                )
            },
            title = { Text(strings.writeContactsPermDialogTitle) },
            text = {
                Text(
                    text = strings.writeContactsPermDialogBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWriteContactsRationaleDialog = false
                        writeContactsPermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                    },
                    modifier = Modifier.testTag("confirm_write_contacts_permission_btn")
                ) {
                    Text(strings.writeContactsPermAllowBtn)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWriteContactsRationaleDialog = false
                        pendingPickedContact?.let { picked ->
                            viewModel.savePickedPhoneContact(
                                name = picked.name,
                                phoneNumber = picked.phoneNumber,
                                systemContactId = picked.systemContactId
                            )
                            pendingPickedContact = null
                        }
                    },
                    modifier = Modifier.testTag("skip_write_contacts_btn")
                ) {
                    Text(strings.saveWithoutStarBtn)
                }
            }
        )
    }

    deletingContact?.let { target ->
        ConfirmActionDialog(
            title = strings.removeContactTitle,
            message = "${target.name}: ${strings.removeContactMsg}",
            confirmText = strings.removeBtn,
            cancelText = strings.cancel,
            onConfirm = {
                viewModel.deleteContact(target)
                deletingContact = null
            },
            onDismiss = { deletingContact = null }
        )
    }

    activeHelpTopic?.let { topic ->
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(topic, strings.isArabic),
            strings = strings,
            onDismiss = { activeHelpTopic = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeEmergencyContactRow(
    contact: PriorityContact,
    strings: AppStrings,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_card_${contact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StatusEmerald.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.initials,
                    style = MaterialTheme.typography.titleSmall,
                    color = StatusEmerald,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = StatusEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("emergency_always_badge_${contact.id}")
                    ) {
                        Text(
                            text = strings.emergencyAlwaysReachesStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusEmerald,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_contact_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = strings.delete,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
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


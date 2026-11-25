package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.models.ProfileIconKey
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.EmeraldGlowMid
import com.example.ui.theme.EmeraldGlowTop
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanation
import com.example.viewmodels.PendingConflictPrompt

@Composable
fun SmartCompanionBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bgModifier = if (isDark) {
        modifier
            .fillMaxSize()
            .background(DarkBackground)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EmeraldGlowTop.copy(alpha = 0.85f),
                        EmeraldGlowMid.copy(alpha = 0.45f),
                        DarkBackground
                    )
                )
            )
    } else {
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    }

    Box(modifier = bgModifier, content = content)
}

@Composable
fun CompanionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val border = BorderStroke(1.dp, borderColor)
    val elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    val shape = RoundedCornerShape(24.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = elevation
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = elevation
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
}

@Composable
fun SimulatedDemoBadge(
    text: String = "Demo / simulation",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Science,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActiveStatusPill(
    text: String,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val accent = if (isActive) StatusEmerald else StatusAmber
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FeatureInfoButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ScreenIntroHeader(
    title: String,
    subtitle: String,
    strings: AppStrings,
    onInfoClick: (() -> Unit)? = null,
    showSimulatedBadge: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (onInfoClick != null) {
                    FeatureInfoButton(
                        contentDescription = strings.featureHelpContentDesc,
                        onClick = onInfoClick
                    )
                }
            }
            if (showSimulatedBadge) {
                Spacer(Modifier.width(8.dp))
                SimulatedDemoBadge(strings.demoBadge)
            } else if (trailingContent != null) {
                Spacer(Modifier.width(8.dp))
                trailingContent()
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeatureExplanationDialog(
    explanation: FeatureExplanation,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 420.dp),
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = explanation.title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExplanationBlock(strings.helpWhatIsIt, explanation.whatItIs)
                ExplanationBlock(strings.helpProblemSolved, explanation.problemSolved)
                ExplanationBlock(strings.helpHowToUse, explanation.howToUse)
                ExplanationBlock(strings.helpWhatHappens, explanation.whatHappens)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.gotItBtn)
            }
        }
    )
}

@Composable
private fun ExplanationBlock(label: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun profileIconFor(key: ProfileIconKey): ImageVector = when (key) {
    ProfileIconKey.STUDY -> Icons.AutoMirrored.Rounded.MenuBook
    ProfileIconKey.WORK -> Icons.Rounded.Work
    ProfileIconKey.SLEEP -> Icons.Rounded.Bedtime
    ProfileIconKey.PERSONAL -> Icons.Rounded.Person
    ProfileIconKey.CUSTOM -> Icons.Rounded.Tune
}

fun soundIconFor(mode: SoundMode): ImageVector = when (mode) {
    SoundMode.SOUND -> Icons.Rounded.VolumeUp
    SoundMode.VIBRATE -> Icons.Rounded.Vibration
    SoundMode.SILENT -> Icons.Rounded.NotificationsOff
}

@Composable
fun ProfileIconBadge(
    iconKey: ProfileIconKey,
    accentHex: Long,
    size: Dp = 44.dp
) {
    val accent = Color(accentHex)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = profileIconFor(iconKey),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String = "Cancel",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 400.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text(cancelText)
            }
        }
    )
}

@Composable
fun ConflictResolutionDialog(
    prompt: PendingConflictPrompt,
    strings: AppStrings,
    onSelectRoutine: (SmartRoutine) -> Unit,
    onKeepCurrent: () -> Unit
) {
    val names = prompt.candidateRoutines.map { strings.localizedRoutineName(it) }
    val plainQuestion = strings.conflictPromptSentence(names)

    AlertDialog(
        onDismissRequest = onKeepCurrent,
        modifier = Modifier.widthIn(max = 420.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = strings.conflictDialogTitle,
                tint = StatusAmber,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = strings.conflictDialogTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = plainQuestion,
                    style = MaterialTheme.typography.bodyLarge
                )
                prompt.candidateRoutines.forEachIndexed { index, routine ->
                    val routineName = strings.localizedRoutineName(routine)
                    val actionSummary = routine.actions.joinToString(" • ") {
                        strings.actionTypeLabel(it.type)
                    }
                    if (index == 0) {
                        Button(
                            onClick = { onSelectRoutine(routine) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conflict_pick_${routine.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = routineName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (actionSummary.isNotBlank()) {
                                    Text(
                                        text = actionSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { onSelectRoutine(routine) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conflict_pick_${routine.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = routineName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (actionSummary.isNotBlank()) {
                                    Text(
                                        text = actionSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onKeepCurrent,
                modifier = Modifier.testTag("conflict_keep_current")
            ) {
                Text(strings.keepCurrentStateBtn)
            }
        }
    )
}

data class BarChartEntry(
    val label: String,
    val value: Float,
    val valueLabel: String,
    val color: Color
)

@Composable
fun HorizontalMetricBars(
    entries: List<BarChartEntry>,
    modifier: Modifier = Modifier
) {
    val maxVal = (entries.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        entries.forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = entry.valueLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val fraction = (entry.value / maxVal).coerceIn(0.06f, 1f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                ) {
                    val radius = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = radius
                    )
                    drawRoundRect(
                        color = entry.color,
                        topLeft = Offset.Zero,
                        size = Size(size.width * fraction, size.height),
                        cornerRadius = radius
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalTrendChart(
    entries: List<BarChartEntry>,
    modifier: Modifier = Modifier
) {
    val maxVal = (entries.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEach { entry ->
            val ratio = (entry.value / maxVal).coerceIn(0.10f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = entry.valueLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.56f)
                        .height(98.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val radius = CornerRadius(size.width / 2f, size.width / 2f)
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset.Zero,
                            size = size,
                            cornerRadius = radius
                        )
                        val barHeight = (size.height * ratio).coerceAtLeast(size.width)
                        drawRoundRect(
                            color = entry.color,
                            topLeft = Offset(0f, size.height - barHeight),
                            size = Size(size.width, barHeight),
                            cornerRadius = radius
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

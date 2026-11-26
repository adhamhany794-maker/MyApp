package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.RoutineLogEventType
import com.example.ui.components.CompanionCard
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo
import com.example.utils.AppStrings
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

@Composable
fun RoutineLogScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("routine_log_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenIntroHeader(
                title = strings.screenRoutineLog,
                subtitle = strings.routineLogSubtitle,
                strings = strings
            )
        }

        if (state.routineLogs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearRoutineLogs() },
                        modifier = Modifier.testTag("routine_log_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(strings.routineLogClearBtn)
                    }
                }
            }
        }

        if (state.routineLogs.isEmpty()) {
            item {
                CompanionCard(modifier = Modifier.testTag("routine_log_empty")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(strings.routineLogEmptyTitle, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = strings.routineLogEmptyMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        items(state.routineLogs, key = { it.id }) { entry ->
            val (badgeLabel, badgeColor, badgeIcon) = when (entry.eventType) {
                RoutineLogEventType.STARTED -> Triple(
                    strings.routineLogStartedBadge,
                    StatusIndigo,
                    Icons.Rounded.PlayCircle
                )
                RoutineLogEventType.ENDED -> Triple(
                    strings.routineLogEndedBadge,
                    StatusEmerald,
                    Icons.Rounded.Restore
                )
                RoutineLogEventType.PERMISSION_DENIED -> Triple(
                    strings.routineLogPermissionDeniedBadge,
                    StatusCrimson,
                    Icons.Rounded.WarningAmber
                )
                RoutineLogEventType.NO_ACTION -> Triple(
                    strings.routineLogNoActionBadge,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.Rounded.WarningAmber
                )
            }

            CompanionCard(modifier = Modifier.testTag("routine_log_item_${entry.id}")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = badgeLabel,
                                tint = badgeColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = TimeUtils.formatTimestamp(entry.timestampMillis, strings.isArabic),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))
                val displayRoutineName = if (strings.isArabic) {
                    val translatedProfile = strings.localizedProfileName(entry.routineId, entry.routineName)
                    if (translatedProfile != entry.routineName) translatedProfile
                    else strings.localizedRoutineName(entry.routineId, entry.routineName)
                } else entry.routineName
                Text(
                    text = displayRoutineName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (entry.ringerMode != null || entry.dndEnabled == true) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        entry.ringerMode?.let { mode ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = strings.soundModeLabel(mode),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (entry.dndEnabled == true) {
                            Surface(
                                color = StatusIndigo.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DoNotDisturbOn,
                                        contentDescription = null,
                                        tint = StatusIndigo,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = strings.phoneControlDndStatusPrefix,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusIndigo
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

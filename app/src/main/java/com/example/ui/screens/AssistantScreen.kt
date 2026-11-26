package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.FeatureInfoButton
import com.example.ui.components.SimulatedDemoBadge
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusAmber
import com.example.utils.AppStrings
import com.example.utils.RoutineTimeInputValidator
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel,
    onEditProposalInBuilder: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.assistantMessages.size, state.isAssistantThinking) {
        if (state.isAssistantThinking) {
            listState.animateScrollToItem(state.assistantMessages.size)
        } else if (state.assistantMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.assistantMessages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Clean Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.assistantHeaderTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.assistantHeaderSub,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SimulatedDemoBadge(strings.demoBadge)
                    FeatureInfoButton(
                        contentDescription = strings.featureHelpContentDesc,
                        onClick = { showHelp = true }
                    )
                    IconButton(
                        onClick = { viewModel.clearAssistantConversation() },
                        modifier = Modifier.testTag("clear_assistant_chat")
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = strings.clearHistoryBtn)
                    }
                }
            }
        }

        // Chat Transcript
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.assistantMessages.size <= 1) {
                item {
                    CompanionCard(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = strings.assistantEmptyGuide,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(state.assistantMessages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (msg.isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (msg.isUser) 18.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 18.dp
                        ),
                        border = if (msg.isUser) null
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            val notice = msg.noticeText
                            if (!notice.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .testTag("assistant_offline_notice")
                                ) {
                                    Text(
                                        text = notice,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )

                            val proposal = msg.proposal
                            if (proposal != null) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = strings.ruleSummaryTitle,
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        val cardContext = LocalContext.current
                                        val is24h = remember(cardContext) { DateFormat.is24HourFormat(cardContext) }
                                        val formattedRange = RoutineTimeInputValidator.formatRoutineRange(
                                            routine = proposal.routine,
                                            isArabic = strings.isArabic,
                                            is24Hour = is24h
                                        )
                                        val whenText = formattedRange ?: proposal.triggerSummary
                                        Text(
                                            text = "${if (strings.isArabic) "متى" else "WHEN"}: $whenText",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${if (strings.isArabic) "الإجراء" else "ACTION"}: ${proposal.actionSummary}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${if (strings.isArabic) "استثناء" else "EXCEPTIONS"}: ${proposal.exceptionSummary}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val conflictWarning = proposal.conflictWarning
                                        if (!conflictWarning.isNullOrBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                color = StatusAmber.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("assistant_conflict_warning")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.WarningAmber,
                                                        contentDescription = null,
                                                        tint = StatusAmber,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = conflictWarning,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = StatusAmber
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                if (!msg.proposalResolved) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.confirmAssistantProposal(msg.id, proposal)
                                            },
                                            modifier = Modifier.testTag("assistant_confirm_btn")
                                        ) {
                                            Text(strings.approveBtn)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.editAssistantProposal(msg.id, proposal)
                                                onEditProposalInBuilder()
                                            },
                                            modifier = Modifier.testTag("assistant_edit_btn")
                                        ) {
                                            Text(strings.edit)
                                        }
                                        TextButton(
                                            onClick = { viewModel.cancelAssistantProposal(msg.id) },
                                            modifier = Modifier.testTag("assistant_cancel_btn")
                                        ) {
                                            Text(strings.rejectBtn)
                                        }
                                    }
                                } else if (msg.proposalStatusNote != null) {
                                    Text(
                                        text = msg.proposalStatusNote,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.isAssistantThinking) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 18.dp
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("assistant_thinking_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                strings.assistantThinking,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4 Clean Suggested Commands
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            strings.assistantSuggestedPrompts.forEach { prompt ->
                AssistChip(
                    onClick = { viewModel.sendAssistantPrompt(prompt) },
                    label = { Text(prompt, maxLines = 1) }
                )
            }
        }

        // Message Composer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(strings.assistantInputHint) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_input")
                )
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendAssistantPrompt(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !state.isAssistantThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("assistant_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = strings.assistantHeaderTitle)
                }
            }
        }
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.ASSISTANT, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

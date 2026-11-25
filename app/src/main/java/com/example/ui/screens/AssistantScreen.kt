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
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.utils.AppStrings
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
        if (state.assistantMessages.isNotEmpty()) {
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
                        tint = MaterialTheme.colorScheme.secondary
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
                    SimulatedDemoBadge(strings.simulatedChip)
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
                                        Text(
                                            text = "${if (strings.isArabic) "متى" else "WHEN"}: ${proposal.triggerSummary} (${proposal.endTimeLabel})",
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
                                            Text(strings.confirm)
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
                                            Text(strings.cancel)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            strings.assistantThinking,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

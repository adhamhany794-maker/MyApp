package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

enum class AdvancedSectionTab {
    CONTEXT,
    PLAYGROUND,
    SIMULATION
}

@Composable
fun AdvancedScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel,
    initialTab: AdvancedSectionTab = AdvancedSectionTab.CONTEXT
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var showHelp by remember { mutableStateOf(false) }

    val tabs = listOf(
        AdvancedSectionTab.CONTEXT to strings.screenContext,
        AdvancedSectionTab.PLAYGROUND to strings.screenPlayground,
        AdvancedSectionTab.SIMULATION to strings.screenSimulation
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("advanced_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ScreenIntroHeader(
                title = strings.screenAdvanced,
                subtitle = strings.advancedSubtitle,
                strings = strings,
                onInfoClick = { showHelp = true },
                showSimulatedBadge = true
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, (tab, label) ->
                    SegmentedButton(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = tabs.size
                        ),
                        modifier = Modifier.testTag("advanced_tab_${tab.name}")
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                AdvancedSectionTab.CONTEXT -> ContextAwarenessScreen(
                    state = state,
                    strings = strings,
                    viewModel = viewModel,
                    showHeader = false
                )
                AdvancedSectionTab.PLAYGROUND -> DemoPlaygroundScreen(
                    state = state,
                    strings = strings,
                    viewModel = viewModel,
                    showHeader = false
                )
                AdvancedSectionTab.SIMULATION -> SimulationEngineScreen(
                    state = state,
                    strings = strings,
                    viewModel = viewModel,
                    showHeader = false
                )
            }
        }
    }

    if (showHelp) {
        val topic = when (selectedTab) {
            AdvancedSectionTab.CONTEXT -> FeatureHelpTopic.CONTEXT_AWARENESS
            AdvancedSectionTab.PLAYGROUND -> FeatureHelpTopic.SIMULATION_ENGINE
            AdvancedSectionTab.SIMULATION -> FeatureHelpTopic.SIMULATION_ENGINE
        }
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(topic, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

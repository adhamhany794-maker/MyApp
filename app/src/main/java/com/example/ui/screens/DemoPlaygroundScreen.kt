package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.components.SimulatedDemoBadge
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel

private data class DemoScenarioMeta(
    val number: Int,
    val title: String,
    val summary: String,
    val buttonLabel: String
)

@Composable
fun DemoPlaygroundScreen(
    state: SmartCompanionUiState,
    strings: AppStrings,
    viewModel: SmartCompanionViewModel
) {
    var showHelp by remember { mutableStateOf(false) }
    val scenarios = if (strings.isArabic) {
        listOf(
            DemoScenarioMeta(
                1,
                "السيناريو 1: وقت الدراسة",
                "تفعيل وضع الدراسة، تشغيل روتين الدراسة، حجب متصل عادي، والسماح بمكالمة طوارئ مع تسجيل القرارين.",
                "تشغيل السيناريو 1"
            ),
            DemoScenarioMeta(
                2,
                "السيناريو 2: حماية النوم",
                "تفعيل وضع النوم، كتم رسالة عادية ليلاً، والسماح بمكالمة طوارئ.",
                "تشغيل السيناريو 2"
            ),
            DemoScenarioMeta(
                3,
                "السيناريو 3: الجدولة الذكية",
                "محاكاة الوصول إلى الإثنين 9:00 صباحاً، مطابقة ساعات العمل، وتحديث الرئيسية والسجل.",
                "تشغيل السيناريو 3"
            ),
            DemoScenarioMeta(
                4,
                "السيناريو 4: إنشاء روتين بالمساعد",
                "تحليل أمر طبيعي لإنشاء قاعدة كتم مسائية من 8 م إلى 10 م مع استثناء الطوارئ وحفظها.",
                "تشغيل السيناريو 4"
            ),
            DemoScenarioMeta(
                5,
                "السيناريو 5: حل تعارض القواعد",
                "محاكاة الأربعاء 6:00 مساءً عند تقاطع روتينين وفتح نافذة الاختيار الآمن.",
                "تشغيل السيناريو 5"
            ),
            DemoScenarioMeta(
                6,
                "السيناريو 6: الإيقاف الشامل والاستعادة",
                "تفعيل الإيقاف المؤقت العام، التأكد من توقف المشغلات، ثم استعادة الأتمتة بأمان.",
                "تشغيل السيناريو 6"
            )
        )
    } else {
        listOf(
            DemoScenarioMeta(
                1,
                "Scenario 1: Study Time",
                "Activate Study Profile, start Study Time routine, block a normal caller, allow an emergency caller, and log both decisions.",
                "Run Scenario 1"
            ),
            DemoScenarioMeta(
                2,
                "Scenario 2: Sleep Protection",
                "Activate Sleep Profile, block a normal message, and allow an emergency contact call.",
                "Run Scenario 2"
            ),
            DemoScenarioMeta(
                3,
                "Scenario 3: Smart Scheduling",
                "Simulate reaching Monday 9:00 AM, match Work Hours, execute it, update Dashboard & History.",
                "Run Scenario 3"
            ),
            DemoScenarioMeta(
                4,
                "Scenario 4: AI Routine Creation",
                "Parse 'Keep my phone silent every day from 8 PM to 10 PM, but allow emergency contacts', confirm & save to Smart Routines.",
                "Run Scenario 4"
            ),
            DemoScenarioMeta(
                5,
                "Scenario 5: Conflict Resolution",
                "Trigger Wednesday 6:00 PM where Study Time & Evening Unwind overlap, inspect conflict, and resolve safely.",
                "Run Scenario 5"
            ),
            DemoScenarioMeta(
                6,
                "Scenario 6: Global Pause & Recovery",
                "Activate Global Pause, verify scheduled triggers stop, then restore automation intact.",
                "Run Scenario 6"
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenIntroHeader(
                title = strings.screenPlayground,
                subtitle = strings.playgroundSubtitle,
                strings = strings,
                onInfoClick = { showHelp = true },
                showSimulatedBadge = true
            )
        }

        state.demoScenarioResult?.let { res ->
            item {
                CompanionCard(
                    containerColor = StatusEmerald.copy(alpha = 0.12f),
                    modifier = Modifier.testTag("demo_scenario_output_card")
                ) {
                    Text(res.scenarioTitle, style = MaterialTheme.typography.titleMedium, color = StatusEmerald)
                    Spacer(Modifier.height(6.dp))
                    res.steps.forEach { step ->
                        Text(step, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(3.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${if (strings.isArabic) "النتيجة" else "Result"}: ${res.finalOutcome}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        scenarios.forEach { sc ->
            item(key = sc.number) {
                CompanionCard {
                    Text(sc.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        sc.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.executeDemoScenario(sc.number) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_demo_scenario_${sc.number}")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(sc.buttonLabel)
                    }
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

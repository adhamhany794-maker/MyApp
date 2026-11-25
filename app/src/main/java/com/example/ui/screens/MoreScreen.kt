package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.navigation.NavRoutes
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic

private data class MoreMenuItem(
    val route: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

private data class MoreCategory(
    val title: String,
    val items: List<MoreMenuItem>
)

@Composable
fun MoreScreen(
    strings: AppStrings,
    onNavigate: (String) -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }

    val categories = listOf(
        MoreCategory(
            title = strings.catFocus,
            items = listOf(
                MoreMenuItem(
                    route = NavRoutes.FOCUS,
                    title = strings.screenFocus,
                    description = strings.descFocusSessions,
                    icon = Icons.Rounded.Timer,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            )
        ),
        MoreCategory(
            title = strings.catActivity,
            items = listOf(
                MoreMenuItem(
                    route = NavRoutes.HISTORY,
                    title = strings.screenHistory,
                    description = strings.descActivityHistory,
                    icon = Icons.Rounded.History,
                    accentColor = MaterialTheme.colorScheme.secondary
                ),
                MoreMenuItem(
                    route = NavRoutes.NOTIFICATIONS,
                    title = strings.screenNotifications,
                    description = strings.descNotifications,
                    icon = Icons.Rounded.NotificationsActive,
                    accentColor = StatusAmber
                )
            )
        ),
        MoreCategory(
            title = strings.catIntelligence,
            items = listOf(
                MoreMenuItem(
                    route = NavRoutes.CONTEXT,
                    title = strings.screenContext,
                    description = strings.descContext,
                    icon = Icons.Rounded.LocationOn,
                    accentColor = MaterialTheme.colorScheme.tertiary
                ),
                MoreMenuItem(
                    route = NavRoutes.SIMULATION,
                    title = strings.screenSimulation,
                    description = strings.descSimulation,
                    icon = Icons.Rounded.AutoMode,
                    accentColor = StatusIndigo
                ),
                MoreMenuItem(
                    route = NavRoutes.PLAYGROUND,
                    title = strings.screenPlayground,
                    description = strings.descPlayground,
                    icon = Icons.Rounded.Science,
                    accentColor = MaterialTheme.colorScheme.secondary
                )
            )
        ),
        MoreCategory(
            title = strings.catProfiles,
            items = listOf(
                MoreMenuItem(
                    route = NavRoutes.PROFILES,
                    title = strings.screenProfiles,
                    description = strings.descProfiles,
                    icon = Icons.Rounded.Tune,
                    accentColor = StatusEmerald
                )
            )
        ),
        MoreCategory(
            title = strings.catProtection,
            items = listOf(
                MoreMenuItem(
                    route = NavRoutes.SAFETY,
                    title = strings.screenSafety,
                    description = strings.descSafety,
                    icon = Icons.Rounded.Security,
                    accentColor = StatusCrimson
                )
            )
        ),
        MoreCategory(
            title = strings.catPreferences,
            items = listOf(
                MoreMenuItem(
                    route = NavRoutes.SETTINGS,
                    title = strings.screenSettings,
                    description = strings.descSettings,
                    icon = Icons.Rounded.Settings,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            )
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("more_hub_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenIntroHeader(
                title = strings.screenMore,
                subtitle = strings.moreSubtitle,
                strings = strings,
                onInfoClick = { showHelp = true }
            )
        }

        categories.forEach { category ->
            item(key = category.title) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = category.title.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    CompanionCard {
                        category.items.forEachIndexed { index, item ->
                            Surface(
                                onClick = { onNavigate(item.route) },
                                color = Color.Transparent,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("more_item_${item.route}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        color = item.accentColor.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = item.accentColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = item.description,
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
                            if (index < category.items.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.MORE_HUB, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

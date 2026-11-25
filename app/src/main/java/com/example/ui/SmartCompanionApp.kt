package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.ConflictResolutionDialog
import com.example.ui.navigation.NavRoutes
import com.example.ui.screens.ActivityHistoryScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.ContextAwarenessScreen
import com.example.ui.screens.DemoPlaygroundScreen
import com.example.ui.screens.FocusSessionsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.NotificationIntelligenceScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.RuleBuilderScreen
import com.example.ui.screens.SafetyCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulationEngineScreen
import com.example.ui.theme.SmartCompanionTheme
import com.example.utils.stringsFor
import com.example.viewmodels.SmartCompanionViewModel

private data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCompanionApp(
    viewModel: SmartCompanionViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = remember(state.preferences.language) {
        stringsFor(state.preferences.language)
    }

    SmartCompanionTheme(
        themeMode = state.preferences.themeMode,
        language = state.preferences.language
    ) {
        if (!state.isReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@SmartCompanionTheme
        }

        if (!state.preferences.hasCompletedOnboarding) {
            OnboardingScreen(
                strings = strings,
                onFinishOnboarding = { viewModel.completeOnboarding() }
            )
            return@SmartCompanionTheme
        }

        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: NavRoutes.HOME
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.feedbackBanner) {
            val msg = state.feedbackBanner
            if (msg != null) {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearFeedbackBanner()
            }
        }

        // Strict 5 primary bottom navigation items in exact order:
        // Home -> Routines -> Assistant -> Contacts -> Insights
        val mainTabs = listOf(
            BottomNavTab(NavRoutes.HOME, strings.navHome, Icons.Rounded.Home),
            BottomNavTab(NavRoutes.ROUTINES, strings.navRoutines, Icons.Rounded.Schedule),
            BottomNavTab(NavRoutes.ASSISTANT, strings.navAssistant, Icons.Rounded.AutoAwesome),
            BottomNavTab(NavRoutes.CONTACTS, strings.navContacts, Icons.Rounded.Contacts),
            BottomNavTab(NavRoutes.INSIGHTS, strings.navInsights, Icons.Rounded.BarChart)
        )
        val mainRouteSet = mainTabs.map { it.route }.toSet()
        val isMainTab = currentRoute in mainRouteSet

        fun navigateTo(route: String, popUpToRoot: Boolean = false) {
            navController.navigate(route) {
                if (popUpToRoot) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        val topBarTitle = when {
            currentRoute == NavRoutes.HOME -> strings.appName
            currentRoute == NavRoutes.ROUTINES -> strings.navRoutines
            currentRoute == NavRoutes.ASSISTANT -> strings.navAssistant
            currentRoute == NavRoutes.CONTACTS -> strings.navContacts
            currentRoute == NavRoutes.INSIGHTS -> strings.navInsights
            currentRoute == NavRoutes.MORE -> strings.screenMore
            currentRoute == NavRoutes.PROFILES -> strings.screenProfiles
            currentRoute == NavRoutes.FOCUS -> strings.screenFocus
            currentRoute == NavRoutes.CONTEXT -> strings.screenContext
            currentRoute == NavRoutes.NOTIFICATIONS -> strings.screenNotifications
            currentRoute == NavRoutes.HISTORY -> strings.screenHistory
            currentRoute == NavRoutes.SAFETY -> strings.screenSafety
            currentRoute == NavRoutes.SETTINGS -> strings.screenSettings
            currentRoute == NavRoutes.PLAYGROUND -> strings.screenPlayground
            currentRoute == NavRoutes.SIMULATION -> strings.screenSimulation
            currentRoute.startsWith(NavRoutes.RULE_BUILDER) -> strings.screenRuleBuilder
            else -> strings.appName
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = topBarTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (!isMainTab) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("top_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = strings.backDesc
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute != NavRoutes.SETTINGS) {
                            IconButton(
                                onClick = { navigateTo(NavRoutes.SETTINGS) },
                                modifier = Modifier.testTag("top_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = strings.screenSettings
                                )
                            }
                        }
                        if (currentRoute != NavRoutes.MORE) {
                            IconButton(
                                onClick = { navigateTo(NavRoutes.MORE) },
                                modifier = Modifier.testTag("open_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GridView,
                                    contentDescription = strings.openMenuDesc
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (isMainTab) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(tab.route, popUpToRoot = true) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                } else {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = NavRoutes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(NavRoutes.HOME) {
                    HomeDashboardScreen(
                        state = state,
                        strings = strings,
                        viewModel = viewModel,
                        onNavigate = { route -> navigateTo(route) }
                    )
                }
                composable(NavRoutes.ROUTINES) {
                    RoutinesScreen(
                        state = state,
                        strings = strings,
                        viewModel = viewModel,
                        onCreateRoutine = {
                            viewModel.clearAssistantDraft()
                            navigateTo(NavRoutes.ruleBuilderRoute())
                        },
                        onEditRoutine = { id -> navigateTo(NavRoutes.ruleBuilderRoute(id)) },
                        onOpenSimulationEngine = { navigateTo(NavRoutes.SIMULATION) }
                    )
                }
                composable(NavRoutes.ASSISTANT) {
                    AssistantScreen(
                        state = state,
                        strings = strings,
                        viewModel = viewModel,
                        onEditProposalInBuilder = { navigateTo(NavRoutes.ruleBuilderRoute()) }
                    )
                }
                composable(NavRoutes.CONTACTS) {
                    ContactsScreen(state = state, strings = strings, viewModel = viewModel)
                }
                composable(NavRoutes.INSIGHTS) {
                    InsightsScreen(
                        state = state,
                        strings = strings,
                        onOpenNotificationsModule = { navigateTo(NavRoutes.NOTIFICATIONS) }
                    )
                }
                composable(NavRoutes.MORE) {
                    MoreScreen(
                        strings = strings,
                        onNavigate = { route -> navigateTo(route) }
                    )
                }
                composable(NavRoutes.PROFILES) {
                    ProfilesScreen(state = state, strings = strings, viewModel = viewModel)
                }
                composable(NavRoutes.FOCUS) {
                    FocusSessionsScreen(state = state, strings = strings, viewModel = viewModel)
                }
                composable(NavRoutes.CONTEXT) {
                    ContextAwarenessScreen(state = state, strings = strings, viewModel = viewModel)
                }
                composable(NavRoutes.NOTIFICATIONS) {
                    NotificationIntelligenceScreen(state = state, strings = strings)
                }
                composable(NavRoutes.HISTORY) {
                    ActivityHistoryScreen(state = state, strings = strings, viewModel = viewModel)
                }
                composable(NavRoutes.SAFETY) {
                    SafetyCenterScreen(state = state, strings = strings, viewModel = viewModel)
                }
                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(state = state, strings = strings, viewModel = viewModel)
                }
                composable(NavRoutes.PLAYGROUND) {
                    DemoPlaygroundScreen(state = state, strings = strings, viewModel = viewModel)
                }
                composable(NavRoutes.SIMULATION) {
                    SimulationEngineScreen(state = state, strings = strings, viewModel = viewModel)
                }
                composable(
                    route = "${NavRoutes.RULE_BUILDER}?routineId={routineId}",
                    arguments = listOf(
                        navArgument("routineId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    val routineId = entry.arguments?.getString("routineId")
                    val existing = state.routines.firstOrNull { it.id == routineId }
                    RuleBuilderScreen(
                        existingRoutine = existing,
                        strings = strings,
                        state = state,
                        viewModel = viewModel,
                        onSaved = { navController.popBackStack() }
                    )
                }
            }
        }

        state.pendingConflict?.let { conflictPrompt ->
            ConflictResolutionDialog(
                prompt = conflictPrompt,
                strings = strings,
                onSelectRoutine = { chosen -> viewModel.resolvePendingConflict(chosen) },
                onKeepCurrent = { viewModel.resolvePendingConflict(null) }
            )
        }
    }
}

package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.ConflictResolutionDialog
import com.example.ui.components.SmartCompanionBackground
import com.example.ui.navigation.NavRoutes
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AdvancedScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.FocusSessionsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.RoutineLogScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.RuleBuilderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulationEngineScreen
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SmartCompanionTheme
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.utils.AppStrings
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
        SmartCompanionBackground {
            if (!state.isReady) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@SmartCompanionBackground
            }

            if (!state.preferences.hasCompletedOnboarding) {
                OnboardingScreen(
                    strings = strings,
                    onFinishOnboarding = { viewModel.completeOnboarding() }
                )
                return@SmartCompanionBackground
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

            // Strictly 4 primary bottom navigation tabs: Home, Routines, Assistant, Settings
            val mainTabs = listOf(
                BottomNavTab(NavRoutes.HOME, strings.navHome, Icons.Rounded.Home),
                BottomNavTab(NavRoutes.ROUTINES, strings.navRoutines, Icons.Rounded.Schedule),
                BottomNavTab(NavRoutes.ASSISTANT, strings.navAssistant, Icons.Rounded.AutoAwesome),
                BottomNavTab(NavRoutes.SETTINGS, strings.navSettings, Icons.Rounded.Settings)
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

            val context = LocalContext.current
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            fun requestNotificationPermissionIfNeeded() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val topBarTitle = when {
                currentRoute == NavRoutes.HOME -> strings.appName
                currentRoute == NavRoutes.ROUTINES -> strings.navRoutines
                currentRoute == NavRoutes.ASSISTANT -> strings.navAssistant
                currentRoute == NavRoutes.SETTINGS -> strings.navSettings
                currentRoute == NavRoutes.CONTACTS -> strings.navContacts
                currentRoute == NavRoutes.ACTIVITY -> strings.screenActivity
                currentRoute == NavRoutes.ADVANCED -> strings.screenAdvanced
                currentRoute == NavRoutes.PROFILES -> strings.screenProfiles
                currentRoute == NavRoutes.FOCUS -> strings.screenFocus
                currentRoute == NavRoutes.SIMULATION -> strings.screenSimulation
                currentRoute == NavRoutes.ROUTINE_LOG -> strings.screenRoutineLog
                currentRoute.startsWith(NavRoutes.RULE_BUILDER) -> strings.screenRuleBuilder
                else -> strings.appName
            }

            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (!isMainTab) {
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
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.testTag("top_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = strings.backDesc
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                },
                bottomBar = {
                    if (isMainTab) {
                        FloatingPillBottomBar(
                            leftTabs = mainTabs.subList(0, 2),
                            rightTabs = mainTabs.subList(2, 4),
                            currentRoute = currentRoute,
                            strings = strings,
                            onSelectTab = { route -> navigateTo(route, popUpToRoot = true) },
                            onNewRoutineClick = {
                                viewModel.clearAssistantDraft()
                                navigateTo(NavRoutes.ruleBuilderRoute())
                            }
                        )
                    } else {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val alertRoutine = state.dndPermissionAlertRoutine
                    if (alertRoutine != null && !state.phoneControl.hasDndAccess) {
                        Surface(
                            color = StatusCrimson.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("in_app_dnd_permission_banner")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsOff,
                                            contentDescription = strings.dndBannerTitle,
                                            tint = StatusCrimson,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (alertRoutine.isNotBlank()) "${strings.dndBannerTitle} ($alertRoutine)" else strings.dndBannerTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = StatusCrimson
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissDndPermissionAlert() },
                                        modifier = Modifier.testTag("banner_dismiss_dnd_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = strings.close,
                                            tint = StatusCrimson
                                        )
                                    }
                                }
                                Text(
                                    text = strings.dndBannerBody,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val intent = viewModel.createDndAccessSettingsIntent(context)
                                        runCatching { context.startActivity(intent) }
                                    },
                                    modifier = Modifier.testTag("banner_grant_dnd_btn")
                                ) {
                                    Text(strings.phoneControlOpenSettingsBtn)
                                }
                            }
                        }
                    }

                    if (!state.phoneControl.canScheduleExactAlarms || state.exactAlarmDeniedNotice) {
                        Surface(
                            color = StatusAmber.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("in_app_exact_alarm_banner")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AlarmOff,
                                        contentDescription = strings.exactAlarmStatusLabel,
                                        tint = StatusAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = strings.exactAlarmDeniedBanner,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val intent = viewModel.createExactAlarmSettingsIntent(context)
                                        runCatching { context.startActivity(intent) }
                                    },
                                    modifier = Modifier.testTag("banner_grant_exact_alarm_btn")
                                ) {
                                    Text(strings.exactAlarmGrantBtn)
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.HOME,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
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
                                onOpenSimulationEngine = { navigateTo(NavRoutes.SIMULATION) },
                                onOpenProfiles = { navigateTo(NavRoutes.PROFILES) },
                                onOpenFocusSessions = { navigateTo(NavRoutes.FOCUS) },
                                onOpenRoutineLog = { navigateTo(NavRoutes.ROUTINE_LOG) }
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
                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                state = state,
                                strings = strings,
                                viewModel = viewModel,
                                onNavigate = { route -> navigateTo(route) }
                            )
                        }
                        composable(NavRoutes.CONTACTS) {
                            ContactsScreen(state = state, strings = strings, viewModel = viewModel)
                        }
                        composable(NavRoutes.ACTIVITY) {
                            ActivityScreen(state = state, strings = strings, viewModel = viewModel)
                        }
                        composable(NavRoutes.ROUTINE_LOG) {
                            RoutineLogScreen(state = state, strings = strings, viewModel = viewModel)
                        }
                        composable(NavRoutes.ADVANCED) {
                            AdvancedScreen(state = state, strings = strings, viewModel = viewModel)
                        }
                        composable(NavRoutes.PROFILES) {
                            ProfilesScreen(state = state, strings = strings, viewModel = viewModel)
                        }
                        composable(NavRoutes.FOCUS) {
                            FocusSessionsScreen(state = state, strings = strings, viewModel = viewModel)
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
                                onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() },
                                onSaved = { navController.popBackStack() }
                            )
                        }
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
}

@Composable
private fun FloatingPillBottomBar(
    leftTabs: List<BottomNavTab>,
    rightTabs: List<BottomNavTab>,
    currentRoute: String,
    strings: AppStrings,
    onSelectTab: (String) -> Unit,
    onNewRoutineClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val pillColor = if (isDark) DarkSurfaceElevated else Color.White
    val pillBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(34.dp),
            color = pillColor,
            border = pillBorder,
            shadowElevation = if (isDark) 4.dp else 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                leftTabs.forEach { tab ->
                    PillNavItem(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        onClick = { onSelectTab(tab.route) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Center round orange "+" button for New routine
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = onNewRoutineClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("nav_new_routine")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = strings.navNewRoutine,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                rightTabs.forEach { tab ->
                    PillNavItem(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        onClick = { onSelectTab(tab.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PillNavItem(
    tab: BottomNavTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tint = if (selected) activeColor else inactiveColor

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("nav_${tab.route}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

package com.sentinel.android.core.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.sentinel.android.SentinelApplication
import com.sentinel.android.core.common.AppLanguage
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.common.UiState
import com.sentinel.android.core.designsystem.SentinelTheme
import com.sentinel.android.core.security.BiometricAuthHelper
import com.sentinel.android.data.local.SentinelPreferences
import com.sentinel.android.domain.model.EnrollmentStatus
import com.sentinel.android.feature.activity.ActivityLogsScreen
import com.sentinel.android.feature.auth.AuthScreen
import com.sentinel.android.feature.auth.BiometricLockScreen
import com.sentinel.android.feature.auth.SplashScreen
import com.sentinel.android.feature.auth.attemptAutoSignIn
import com.sentinel.android.feature.auth.authStateFlow
import com.sentinel.android.feature.auth.signOutSafely
import com.sentinel.android.feature.dashboard.DashboardScreen
import com.sentinel.android.feature.dashboard.SentinelOperationsViewModel
import com.sentinel.android.feature.devices.DeviceManagerScreen
import com.sentinel.android.feature.enrollment.DeviceEnrollmentScreen
import com.sentinel.android.feature.media.MediaStudioScreen
import com.sentinel.android.feature.media.MediaStudioViewModel
import com.sentinel.android.feature.security.SecurityCenterScreen
import com.sentinel.android.feature.sessions.CommandCenterScreen
import com.sentinel.android.feature.sessions.LiveSessionScreen
import com.sentinel.android.feature.settings.SettingsScreen
import com.sentinel.android.service.ScreenShareForegroundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun SentinelAppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as SentinelApplication
    val container = app.container
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val preferences by container.preferencesRepository.preferencesFlow
        .collectAsStateWithLifecycle(initialValue = SentinelPreferences())
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var splashDone by remember { mutableStateOf(false) }
    var splashStatus by remember { mutableStateOf("Initializing hardware keystore...") }
    var biometricUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(350L)
        splashStatus = "Restoring verified operator session..."
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { splashDone = true },
            onUnauthenticated = { splashDone = true },
            scope = scope
        )
        delay(950L)
        splashDone = true
    }

    SentinelTheme(
        themeMode = preferences.themeMode,
        language = preferences.language
    ) {
        val strings = LocalSentinelStrings.current
        if (!splashDone) {
            SplashScreen(statusLabel = splashStatus)
        } else {
            val user = currentUser
            if (user == null) {
                biometricUnlocked = false
                AuthScreen(
                    currentLanguage = preferences.language,
                    onToggleLanguage = {
                        val next = if (preferences.language == AppLanguage.ENGLISH) {
                            AppLanguage.ARABIC
                        } else {
                            AppLanguage.ENGLISH
                        }
                        scope.launch { container.preferencesRepository.setLanguage(next) }
                    },
                    onAuthSuccess = {
                        biometricUnlocked = true
                    }
                )
            } else {
                val requiresBiometric = preferences.biometricLockEnabled &&
                    !biometricUnlocked &&
                    BiometricAuthHelper.canAuthenticate(context)

                if (requiresBiometric) {
                    val activity = context as? FragmentActivity
                    LaunchedEffect(user.uid) {
                        if (activity != null) {
                            BiometricAuthHelper.authenticate(
                                activity = activity,
                                title = strings.biometricPromptTitle,
                                subtitle = strings.biometricPromptSubtitle,
                                onSuccess = { biometricUnlocked = true },
                                onError = {}
                            )
                        }
                    }
                    BiometricLockScreen(
                        onRequestBiometric = {
                            if (activity != null) {
                                BiometricAuthHelper.authenticate(
                                    activity = activity,
                                    title = strings.biometricPromptTitle,
                                    subtitle = strings.biometricPromptSubtitle,
                                    onSuccess = { biometricUnlocked = true },
                                    onError = {}
                                )
                            } else {
                                biometricUnlocked = true
                            }
                        },
                        onSignOut = {
                            ScreenShareForegroundService.stop(context)
                            signOutSafely(
                                credentialManager = credentialManager,
                                scope = scope,
                                beforeSignOut = { container.preferencesRepository.clearSessionVault() }
                            )
                        }
                    )
                } else {
                    // Key by authenticated UID so account switches tear down listeners cleanly
                    key(user.uid) {
                        AuthenticatedSentinelShell(
                            userId = user.uid,
                            operatorName = user.displayName?.takeIf { it.isNotBlank() } ?: "Sentinel Operator",
                            operatorEmail = user.email ?: "",
                            container = container,
                            onSignOut = {
                                ScreenShareForegroundService.stop(context)
                                signOutSafely(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    beforeSignOut = { container.preferencesRepository.clearSessionVault() }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedSentinelShell(
    userId: String,
    operatorName: String,
    operatorEmail: String,
    container: com.sentinel.android.di.SentinelContainer,
    onSignOut: () -> Unit
) {
    val strings = LocalSentinelStrings.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val opsViewModel: SentinelOperationsViewModel = viewModel(
        key = "ops_$userId",
        factory = SentinelOperationsViewModel.provideFactory(
            repository = container.firestoreRepository,
            preferencesRepository = container.preferencesRepository,
            networkMonitor = container.networkMonitor,
            crypto = container.crypto,
            currentUserId = userId
        )
    )
    val studioViewModel: MediaStudioViewModel = viewModel(
        key = "studio_$userId",
        factory = MediaStudioViewModel.provideFactory(
            mediaStudioRepository = container.mediaStudioRepository,
            auditRepository = container.firestoreRepository,
            currentUserId = userId
        )
    )

    val isOnline by opsViewModel.isOnline.collectAsStateWithLifecycle()
    val preferences by opsViewModel.preferences.collectAsStateWithLifecycle()
    val devicesState by opsViewModel.devicesState.collectAsStateWithLifecycle()
    val enrollmentsState by opsViewModel.enrollmentsState.collectAsStateWithLifecycle()
    val sessionsState by opsViewModel.sessionsState.collectAsStateWithLifecycle()
    val logsState by opsViewModel.activityLogsState.collectAsStateWithLifecycle()
    val sharedItems by opsViewModel.sharedMediaItems.collectAsStateWithLifecycle()
    val tokenInfo by opsViewModel.tokenInfo.collectAsStateWithLifecycle()
    val actionInProgress by opsViewModel.actionInProgress.collectAsStateWithLifecycle()
    val bannerMessage by opsViewModel.bannerMessage.collectAsStateWithLifecycle()

    val studioGenState by studioViewModel.genState.collectAsStateWithLifecycle()
    val studioSavedMedia by studioViewModel.savedMedia.collectAsStateWithLifecycle()
    val studioStatus by studioViewModel.statusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(bannerMessage) {
        bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            opsViewModel.clearBanner()
        }
    }
    LaunchedEffect(studioStatus) {
        studioStatus?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            studioViewModel.clearStatus()
        }
    }

    val topDestinations = listOf(
        NavDestination(
            route = SentinelRoutes.ROUTE_DASHBOARD,
            label = strings.navDashboard,
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
            testTag = "nav_dashboard"
        ),
        NavDestination(
            route = SentinelRoutes.ROUTE_DEVICES,
            label = strings.navDevices,
            selectedIcon = Icons.Filled.Devices,
            unselectedIcon = Icons.Outlined.Devices,
            testTag = "nav_devices"
        ),
        NavDestination(
            route = SentinelRoutes.ROUTE_COMMAND,
            label = strings.navCommand,
            selectedIcon = Icons.Filled.SettingsRemote,
            unselectedIcon = Icons.Outlined.SettingsRemote,
            testTag = "nav_command"
        ),
        NavDestination(
            route = SentinelRoutes.ROUTE_MEDIA_STUDIO,
            label = strings.navStudio,
            selectedIcon = Icons.Filled.AutoAwesome,
            unselectedIcon = Icons.Outlined.AutoAwesome,
            testTag = "nav_studio"
        ),
        NavDestination(
            route = SentinelRoutes.ROUTE_SECURITY,
            label = strings.navSecurity,
            selectedIcon = Icons.Filled.Security,
            unselectedIcon = Icons.Outlined.Security,
            testTag = "nav_security"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showPrimaryNav = topDestinations.any { it.route == currentRoute }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val allSessions = when (val s = sessionsState) {
        is UiState.Success -> s.data
        is UiState.Offline -> s.cachedData
        else -> emptyList()
    }
    val enrolledDeviceCount = when (val d = devicesState) {
        is UiState.Success -> d.data.size
        is UiState.Offline -> d.cachedData.size
        else -> 0
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showPrimaryNav && !useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showPrimaryNav && useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = SentinelRoutes.ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(SentinelRoutes.ROUTE_DASHBOARD) {
                        DashboardScreen(
                            operatorName = operatorName,
                            isOnline = isOnline,
                            devicesState = devicesState,
                            sessionsState = sessionsState,
                            logsState = logsState,
                            onNavigateToEnrollment = { navController.navigate(SentinelRoutes.ROUTE_ENROLLMENT) },
                            onNavigateToDevices = { navigateTopLevel(SentinelRoutes.ROUTE_DEVICES) },
                            onNavigateToCommand = { navigateTopLevel(SentinelRoutes.ROUTE_COMMAND) },
                            onNavigateToMediaStudio = { navigateTopLevel(SentinelRoutes.ROUTE_MEDIA_STUDIO) },
                            onNavigateToActivityLogs = { navController.navigate(SentinelRoutes.ROUTE_ACTIVITY_LOGS) },
                            onNavigateToSettings = { navController.navigate(SentinelRoutes.ROUTE_SETTINGS) },
                            onOpenLiveSession = { sid -> navController.navigate(SentinelRoutes.liveSessionRoute(sid)) }
                        )
                    }

                    composable(SentinelRoutes.ROUTE_DEVICES) {
                        DeviceManagerScreen(
                            isOnline = isOnline,
                            devicesState = devicesState,
                            actionInProgress = actionInProgress,
                            onOpenEnrollment = { navController.navigate(SentinelRoutes.ROUTE_ENROLLMENT) },
                            onEnrollCurrentDevice = { alias -> opsViewModel.enrollCurrentDevice(alias) },
                            onUpdatePermissions = { device, cam, screen, media, files, status ->
                                opsViewModel.updateDevicePermissions(device, cam, screen, media, files, status)
                            },
                            onRevokeDevice = { opsViewModel.revokeDevice(it) }
                        )
                    }

                    composable(SentinelRoutes.ROUTE_ENROLLMENT) {
                        DeviceEnrollmentScreen(
                            enrollmentsState = enrollmentsState,
                            actionInProgress = actionInProgress,
                            onBack = { navController.popBackStack() },
                            onCreateInvitation = { alias, perms ->
                                opsViewModel.createEnrollmentInvitation(alias, perms)
                            },
                            onApproveRequest = { opsViewModel.approveEnrollment(it) },
                            onRejectRequest = { opsViewModel.decideEnrollment(it, EnrollmentStatus.REJECTED) },
                            onCancelRequest = { opsViewModel.decideEnrollment(it, EnrollmentStatus.CANCELLED) }
                        )
                    }

                    composable(SentinelRoutes.ROUTE_COMMAND) {
                        CommandCenterScreen(
                            devicesState = devicesState,
                            sessionsState = sessionsState,
                            sharedItems = sharedItems,
                            onNavigateToEnrollment = { navController.navigate(SentinelRoutes.ROUTE_ENROLLMENT) },
                            onStartSession = { device, type, perms, summary, onStarted ->
                                opsViewModel.startAuthorizedSession(device, type, perms, summary, onStarted)
                            },
                            onOpenLiveSession = { sid -> navController.navigate(SentinelRoutes.liveSessionRoute(sid)) },
                            onTerminateSession = { opsViewModel.terminateSession(it) },
                            onRecordSharedItem = { device, descriptor, isPhotoVideo ->
                                opsViewModel.recordExplicitSharedItem(device, descriptor, isPhotoVideo)
                            },
                            onShowMessage = { opsViewModel.showBanner(it) }
                        )
                    }

                    composable(
                        route = "${SentinelRoutes.ROUTE_LIVE_SESSION}/{sessionId}",
                        arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                        val session = allSessions.firstOrNull { it.id == sessionId }
                        LiveSessionScreen(
                            session = session,
                            onBack = { navController.popBackStack() },
                            onTogglePause = { opsViewModel.togglePauseSession(it) },
                            onTerminate = { opsViewModel.terminateSession(it) }
                        )
                    }

                    composable(SentinelRoutes.ROUTE_MEDIA_STUDIO) {
                        MediaStudioScreen(
                            genState = studioGenState,
                            savedMedia = studioSavedMedia,
                            onGenerate = { studioViewModel.generateImage(it) },
                            onSaveCurrent = { studioViewModel.saveCurrentBitmap() },
                            onGetShareUri = { studioViewModel.getShareUri(it) },
                            onDeleteItem = { studioViewModel.deleteItem(it) }
                        )
                    }

                    composable(SentinelRoutes.ROUTE_SECURITY) {
                        SecurityCenterScreen(
                            operatorName = operatorName,
                            operatorEmail = operatorEmail,
                            operatorUid = userId,
                            tokenInfo = tokenInfo,
                            preferences = preferences,
                            enrolledDeviceCount = enrolledDeviceCount,
                            onRefreshToken = { opsViewModel.refreshIdTokenMetadata(forceRefresh = true) },
                            onUpdateSecurityPolicy = { bio, timeout ->
                                opsViewModel.updateSecuritySettings(bio, timeout)
                            },
                            onOpenActivityLogs = { navController.navigate(SentinelRoutes.ROUTE_ACTIVITY_LOGS) },
                            onOpenSettings = { navController.navigate(SentinelRoutes.ROUTE_SETTINGS) },
                            onSignOut = onSignOut
                        )
                    }

                    composable(SentinelRoutes.ROUTE_ACTIVITY_LOGS) {
                        ActivityLogsScreen(
                            isOnline = isOnline,
                            logsState = logsState,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(SentinelRoutes.ROUTE_SETTINGS) {
                        SettingsScreen(
                            preferences = preferences,
                            onSelectTheme = { opsViewModel.setThemeMode(it) },
                            onSelectLanguage = { opsViewModel.setLanguage(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

package com.firstlogistics.flproof.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.firstlogistics.flproof.FLProofApplication
import com.firstlogistics.flproof.export.BackupRestoreManager
import com.firstlogistics.flproof.export.CsvReportExporter
import com.firstlogistics.flproof.export.PdfReportGenerator
import com.firstlogistics.flproof.location.AddressResolver
import com.firstlogistics.flproof.location.CompassSensorManager
import com.firstlogistics.flproof.location.LocationTracker
import com.firstlogistics.flproof.ui.components.FLProofBottomBar
import com.firstlogistics.flproof.ui.components.PinLockDialog
import com.firstlogistics.flproof.ui.screens.camera.BeforeAfterCompareScreen
import com.firstlogistics.flproof.ui.screens.camera.CameraScreen
import com.firstlogistics.flproof.ui.screens.camera.CameraViewModel
import com.firstlogistics.flproof.ui.screens.dashboard.DashboardScreen
import com.firstlogistics.flproof.ui.screens.dashboard.DashboardViewModel
import com.firstlogistics.flproof.ui.screens.detail.EvidenceDetailScreen
import com.firstlogistics.flproof.ui.screens.detail.EvidenceDetailViewModel
import com.firstlogistics.flproof.ui.screens.gallery.GalleryScreen
import com.firstlogistics.flproof.ui.screens.gallery.GalleryViewModel
import com.firstlogistics.flproof.ui.screens.locations.LocationScreen
import com.firstlogistics.flproof.ui.screens.locations.LocationViewModel
import com.firstlogistics.flproof.ui.screens.reports.ReportsScreen
import com.firstlogistics.flproof.ui.screens.reports.ReportsViewModel
import com.firstlogistics.flproof.ui.screens.seabank.SeabankScreen
import com.firstlogistics.flproof.ui.screens.seabank.SeabankViewModel
import com.firstlogistics.flproof.ui.screens.settings.SettingsScreen
import com.firstlogistics.flproof.ui.screens.settings.SettingsViewModel
import com.firstlogistics.flproof.ui.theme.NavyBackground

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as FLProofApplication
    val navController = rememberNavController()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val baseRoute = currentRoute?.substringBefore('?')

    val shouldShowBottomBar = baseRoute in listOf(
        Screen.Dashboard.route,
        "camera",
        Screen.Gallery.route,
        Screen.Reports.route
    )

    // Security PIN Lock verification
    var isPinVerified by remember {
        mutableStateOf(!app.settingsRepository.isPinLockEnabled || app.settingsRepository.appPin.isEmpty())
    }

    if (!isPinVerified) {
        PinLockDialog(
            correctPin = app.settingsRepository.appPin,
            onSuccess = { isPinVerified = true },
            onDismiss = { /* Forced unlock requirement */ }
        )
        return
    }

    // Shared singletons / managers
    val locationTracker = remember { LocationTracker(app) }
    val compassSensorManager = remember { CompassSensorManager(app) }
    val addressResolver = remember { AddressResolver(app) }
    val pdfReportGenerator = remember { PdfReportGenerator(app, app.storageManager) }
    val csvReportExporter = remember { CsvReportExporter(app.storageManager) }
    val backupRestoreManager = remember { BackupRestoreManager(app, app.storageManager) }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                FLProofBottomBar(navController = navController)
            }
        },
        containerColor = NavyBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Dashboard Screen
            composable(Screen.Dashboard.route) {
                val viewModel = remember {
                    DashboardViewModel(app.evidenceRepository, app.settingsRepository)
                }
                DashboardScreen(
                    viewModel = viewModel,
                    onStartCamera = { navController.navigate(Screen.Camera.route) },
                    onOpenEvidenceDetail = { id -> navController.navigate(Screen.EvidenceDetail.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToBeforeAfter = { navController.navigate(Screen.BeforeAfter.route) },
                    onNavigateToSeaBank = { navController.navigate(Screen.SeaBank.route) }
                )
            }

            // Camera Screen (mendukung parameter opsional sessionId untuk multi-media)
            composable(
                route = "camera?sessionId={sessionId}",
                arguments = listOf(
                    navArgument("sessionId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val passedSessionId = backStackEntry.arguments?.getString("sessionId")
                val viewModel = remember(passedSessionId) {
                    CameraViewModel(
                        context = app,
                        evidenceRepository = app.evidenceRepository,
                        settingsRepository = app.settingsRepository,
                        storageManager = app.storageManager,
                        locationTracker = locationTracker,
                        compassSensorManager = compassSensorManager,
                        addressResolver = addressResolver
                    ).apply {
                        initSession(passedSessionId)
                    }
                }
                CameraScreen(
                    viewModel = viewModel,
                    onEvidenceSaved = { id ->
                        navController.navigate(Screen.EvidenceDetail.createRoute(id)) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    },
                    onOpenEvidenceDetail = { id ->
                        navController.navigate(Screen.EvidenceDetail.createRoute(id))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // SeaBank Allocation Screen
            composable(Screen.SeaBank.route) {
                val viewModel = remember {
                    SeabankViewModel(app.seabankPackageRepository)
                }
                SeabankScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Gallery Screen
            composable(Screen.Gallery.route) {
                val viewModel = remember {
                    GalleryViewModel(app.evidenceRepository)
                }
                GalleryScreen(
                    viewModel = viewModel,
                    onOpenEvidenceDetail = { id -> navController.navigate(Screen.EvidenceDetail.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Locations Screen
            composable(Screen.Locations.route) {
                val viewModel = remember {
                    LocationViewModel(
                        evidenceRepository = app.evidenceRepository,
                        locationTracker = locationTracker,
                        compassSensorManager = compassSensorManager
                    )
                }
                LocationScreen(
                    viewModel = viewModel,
                    onOpenEvidenceDetail = { id -> navController.navigate(Screen.EvidenceDetail.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Reports Screen
            composable(Screen.Reports.route) {
                val viewModel = remember {
                    ReportsViewModel(
                        context = app,
                        evidenceRepository = app.evidenceRepository,
                        settingsRepository = app.settingsRepository,
                        storageManager = app.storageManager,
                        pdfReportGenerator = pdfReportGenerator,
                        csvReportExporter = csvReportExporter
                    )
                }
                ReportsScreen(
                    viewModel = viewModel,
                    onOpenEvidenceDetail = { id -> navController.navigate(Screen.EvidenceDetail.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Evidence Detail Screen
            composable(
                route = Screen.EvidenceDetail.route,
                arguments = listOf(navArgument("evidenceId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://firstlogistics.flproof/evidence?id={evidenceId}" },
                    navDeepLink { uriPattern = "flproof://evidence?id={evidenceId}" },
                    navDeepLink { uriPattern = "flproof://evidence/{evidenceId}" }
                )
            ) { backStackEntry ->
                val evidenceId = backStackEntry.arguments?.getString("evidenceId") ?: ""
                val viewModel = remember {
                    EvidenceDetailViewModel(
                        context = app,
                        evidenceRepository = app.evidenceRepository,
                        settingsRepository = app.settingsRepository,
                        storageManager = app.storageManager,
                        pdfReportGenerator = pdfReportGenerator
                    )
                }
                EvidenceDetailScreen(
                    evidenceId = evidenceId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onAddMediaToSession = { sessId ->
                        navController.navigate(Screen.Camera.createRoute(sessId))
                    }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                val viewModel = remember {
                    SettingsViewModel(
                        context = app,
                        settingsRepository = app.settingsRepository,
                        storageManager = app.storageManager,
                        backupRestoreManager = backupRestoreManager
                    )
                }
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Before & After Screen
            composable(Screen.BeforeAfter.route) {
                BeforeAfterCompareScreen(
                    evidenceRepository = app.evidenceRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}


package com.firstlogistics.flproof.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Beranda", Icons.Default.Dashboard)
    object Camera : Screen("camera", "Kamera", Icons.Default.PhotoCamera) {
        fun createRoute(sessionId: String? = null) = if (!sessionId.isNullOrBlank()) "camera?sessionId=$sessionId" else "camera"
    }
    object SeaBank : Screen("seabank", "Alokasi SeaBank", Icons.Default.LocalShipping)
    object Gallery : Screen("gallery", "Evidence", Icons.Default.Collections)
    object Reports : Screen("reports", "Laporan", Icons.Default.Assessment)

    // Sub screens
    object EvidenceDetail : Screen("evidence_detail/{evidenceId}", "Detail Bukti") {
        fun createRoute(evidenceId: String) = "evidence_detail/$evidenceId"
    }
    object Locations : Screen("locations", "Lokasi", Icons.Default.LocationOn)
    object Settings : Screen("settings", "Pengaturan", Icons.Default.Settings)
    object BeforeAfter : Screen("before_after", "Before & After", Icons.Default.Compare)
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Camera,
    Screen.Gallery,
    Screen.Reports
)

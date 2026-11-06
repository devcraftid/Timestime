package com.firstlogistics.flproof.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandNavy,
    onPrimary = PureWhite,
    primaryContainer = PaleBlue,
    onPrimaryContainer = BrandNavy,
    secondary = BrandBlue,
    onSecondary = PureWhite,
    secondaryContainer = PaleBlue,
    onSecondaryContainer = BrandBlue,
    tertiary = GreenAccent,
    onTertiary = PureWhite,
    background = NavyBackground,
    onBackground = TextWhite,
    surface = NavySurface,
    onSurface = TextWhite,
    surfaceVariant = PaleBlue,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    error = StatusError,
    onError = PureWhite
)

@Composable
fun FLProofTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Status bar & nav bar light styling
                window.statusBarColor = NavyBackground.toArgb()
                window.navigationBarColor = NavySurface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

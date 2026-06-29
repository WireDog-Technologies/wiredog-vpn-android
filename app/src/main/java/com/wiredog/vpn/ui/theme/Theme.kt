package com.wiredog.vpn.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WireDogColorScheme = darkColorScheme(
    primary = VpnPrimary,
    onPrimary = VpnTextPrimary,
    primaryContainer = VpnPrimary,
    onPrimaryContainer = VpnTextPrimary,
    secondary = VpnTextSecondary,
    onSecondary = VpnTextPrimary,
    tertiary = VpnGreen,
    background = VpnBackground,
    onBackground = VpnTextPrimary,
    surface = VpnSurface,
    onSurface = VpnTextPrimary,
    surfaceVariant = VpnSurfaceVariant,
    onSurfaceVariant = VpnTextSecondary,
    outline = VpnBorderColor,
    outlineVariant = VpnDividerColor
)

@Composable
fun WireDogVPNAndroidTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = WireDogColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VpnBackground.toArgb()
            window.navigationBarColor = VpnBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.aura.glasschat.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BuddysLightMaterialScheme = lightColorScheme(
    primary = Color(0xFFF46A21),
    secondary = Color(0xFFFFE52E),
    tertiary = Color(0xFFFF8A3D),
    background = Color(0xFFFFF4DE),
    surface = Color(0xFFFFFDF5),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF171717),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF171717),
    onSurface = Color(0xFF171717),
    outline = Color(0xFF171717)
)

private val BuddysDarkMaterialScheme = darkColorScheme(
    primary = Color(0xFFF46A21),
    secondary = Color(0xFFFFE52E),
    tertiary = Color(0xFFFF8A3D),
    background = Color(0xFF171513),
    surface = Color(0xFF211E19),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF171717),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFF4DE),
    onSurface = Color(0xFFFFF4DE),
    outline = Color(0xFF4A4338)
)

@Composable
fun GlassChatTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ThemePreferences.init(context)
    }

    val themeMode by ThemePreferences.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val buddysColors = if (isDark) DarkBuddysColors else LightBuddysColors
    val materialScheme = if (isDark) BuddysDarkMaterialScheme else BuddysLightMaterialScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = buddysColors.background.toArgb()
                window.navigationBarColor = buddysColors.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalBuddysColors provides buddysColors
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography,
            content = content
        )
    }
}

package com.aura.glasschat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ====================================================================
// BUDDYS MASTER DESIGN SYSTEM — SEMANTIC COLOUR TOKENS
// ====================================================================

data class BuddysColorScheme(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val surfaceElevated: Color,
    val surfaceComposer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val divider: Color,
    val primaryRed: Color,
    val deepRed: Color,
    val softRed: Color,
    val spiderBlue: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val bubbleOutgoing: Color,
    val bubbleIncoming: Color,
    val textOnPrimary: Color,
    val webGeometryTint: Color
)

// --------------------------------------------------------------------
// LIGHT THEME (White + Black + BUDDYS Red + Deep Spider Navy)
// --------------------------------------------------------------------
val LightBuddysColors = BuddysColorScheme(
    isDark = false,
    background = Color(0xFFF8F9FB),
    surface = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFECEEF2),
    surfaceElevated = Color(0xFFF1F3F6),
    surfaceComposer = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF111318),
    textSecondary = Color(0xFF626873),
    textMuted = Color(0xFF969BA4),
    border = Color(0xFFE5E7EB),
    divider = Color(0xFFE5E7EB),
    primaryRed = Color(0xFFE3262E),
    deepRed = Color(0xFFB9151D),
    softRed = Color(0xFFFCEBED),
    spiderBlue = Color(0xFF17243A),
    success = Color(0xFF22A06B),
    warning = Color(0xFFD88900),
    error = Color(0xFFD92D35),
    bubbleOutgoing = Color(0xFFE3262E),
    bubbleIncoming = Color(0xFFF1F3F6),
    textOnPrimary = Color(0xFFFFFFFF),
    webGeometryTint = Color(0xFFE3262E).copy(alpha = 0.05f)
)

// --------------------------------------------------------------------
// DARK THEME (Cinematic OLED Near-Black + BUDDYS Red + Deep Spider Navy)
// --------------------------------------------------------------------
val DarkBuddysColors = BuddysColorScheme(
    isDark = true,
    background = Color(0xFF0B0C0F),
    surface = Color(0xFF141416),
    surfaceSecondary = Color(0xFF1A1A1E),
    surfaceElevated = Color(0xFF1C1C1F),
    surfaceComposer = Color(0xFF181A1F),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFA3A6AE),
    textMuted = Color(0xFF707075),
    border = Color(0xFF29292D),
    divider = Color(0xFF29292D),
    primaryRed = Color(0xFFFF3B40),
    deepRed = Color(0xFFC71920),
    softRed = Color(0xFF2B1215),
    spiderBlue = Color(0xFF253B61),
    success = Color(0xFF28B772),
    warning = Color(0xFFEAA11A),
    error = Color(0xFFE5454C),
    bubbleOutgoing = Color(0xFFFF3B40),
    bubbleIncoming = Color(0xFF1C1C1F),
    textOnPrimary = Color(0xFFFFFFFF),
    webGeometryTint = Color(0xFFFF3B40).copy(alpha = 0.08f)
)

val LocalBuddysColors = staticCompositionLocalOf { LightBuddysColors }

object BuddysTheme {
    val colors: BuddysColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalBuddysColors.current
}

// --------------------------------------------------------------------
// BACKWARD COMPATIBLE CONSTANTS / DYNAMIC GETTERS
// --------------------------------------------------------------------
val BuddysRed = Color(0xFFE31E24)
val BuddysDeepRed = Color(0xFFB51218)
val BuddysDarkBlue = Color(0xFF172A46)
val BuddysWhite = Color(0xFFFFFFFF)
val BuddysBlack = Color(0xFF111111)

val BackgroundCream: Color @Composable get() = BuddysTheme.colors.background
val PaperWhite: Color @Composable get() = BuddysTheme.colors.surface
val NotebookBorder: Color @Composable get() = BuddysTheme.colors.border
val TextPrimary: Color @Composable get() = BuddysTheme.colors.textPrimary
val TextSecondary: Color @Composable get() = BuddysTheme.colors.textSecondary
val TextMuted: Color @Composable get() = BuddysTheme.colors.textMuted
val AccentPrimary: Color @Composable get() = BuddysTheme.colors.primaryRed
val AccentDeep: Color @Composable get() = BuddysTheme.colors.deepRed
val AccentRose: Color @Composable get() = BuddysTheme.colors.primaryRed
val AccentSky: Color @Composable get() = BuddysTheme.colors.spiderBlue
val AccentSunny: Color @Composable get() = BuddysTheme.colors.warning
val AccentMint: Color @Composable get() = BuddysTheme.colors.success
val PastelSky: Color @Composable get() = BuddysTheme.colors.surfaceSecondary
val PastelYellow: Color @Composable get() = BuddysTheme.colors.surfaceSecondary
val PastelPink: Color @Composable get() = BuddysTheme.colors.softRed
val PastelMint: Color @Composable get() = BuddysTheme.colors.surfaceSecondary
val PastelSkyBorder: Color @Composable get() = BuddysTheme.colors.border

val BuddysRedGradient: Brush
    get() = Brush.linearGradient(
        listOf(Color(0xFFE31E24), Color(0xFFB51218))
    )

val StoryRingGradient: Brush
    get() = Brush.sweepGradient(
        listOf(
            Color(0xFFE31E24),
            Color(0xFFFF3366),
            Color(0xFFFF6B4A),
            Color(0xFFE31E24)
        )
    )

val SnapMediaColor: Color = Color(0xFFFF2A55)
val SnapChatColor: Color = Color(0xFF0084FF)
val SnapVoiceColor: Color = Color(0xFFA855F7)
val SeenReceiptBlue: Color = Color(0xFF3897F0)

val GlassCardBorder: Color
    @Composable get() = if (BuddysTheme.colors.isDark) Color(0xFF222225) else Color(0xFFE5E5E5)
package com.aura.glasschat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ====================================================================
// BUDDYS MASTER DESIGN SYSTEM — SEMANTIC COLOUR TOKENS (CALM & ELEGANT)
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
    val primaryAccent: Color,
    val deepAccent: Color,
    val softAccent: Color,
    val secondaryAccent: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val bubbleOutgoing: Color,
    val bubbleIncoming: Color,
    val textOnPrimary: Color,
    val webGeometryTint: Color
) {
    // Backward compatible aliases
    val primaryRed: Color get() = primaryAccent
    val deepRed: Color get() = deepAccent
    val softRed: Color get() = softAccent
    val spiderBlue: Color get() = secondaryAccent
}

// --------------------------------------------------------------------
// LIGHT THEME (Porcelain Light + Crisp Slate + Premium Rose Accent)
// --------------------------------------------------------------------
val LightBuddysColors = BuddysColorScheme(
    isDark = false,
    background = Color(0xFFFAFAFC),
    surface = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFF1F3F6),
    surfaceElevated = Color(0xFFE5E7EB),
    surfaceComposer = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    border = Color(0xFFE2E8F0),
    divider = Color(0xFFF1F5F9),
    primaryAccent = Color(0xFFE11D48),
    deepAccent = Color(0xFFBE123C),
    softAccent = Color(0xFFFFF1F2),
    secondaryAccent = Color(0xFF0284C7),
    success = Color(0xFF10B981),
    warning = Color(0xFFF59E0B),
    error = Color(0xFFEF4444),
    bubbleOutgoing = Color(0xFFE11D48),
    bubbleIncoming = Color(0xFFF1F3F6),
    textOnPrimary = Color(0xFFFFFFFF),
    webGeometryTint = Color.Transparent
)

// --------------------------------------------------------------------
// DARK THEME (Obsidian OLED + Sleek Satin + Refined Rose Accent)
// --------------------------------------------------------------------
val DarkBuddysColors = BuddysColorScheme(
    isDark = true,
    background = Color(0xFF090A0E),
    surface = Color(0xFF12141B),
    surfaceSecondary = Color(0xFF1A1D27),
    surfaceElevated = Color(0xFF222634),
    surfaceComposer = Color(0xFF151822),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    border = Color(0xFF232838),
    divider = Color(0xFF1E2332),
    primaryAccent = Color(0xFFF43F5E),
    deepAccent = Color(0xFFE11D48),
    softAccent = Color(0xFF2E1520),
    secondaryAccent = Color(0xFF38BDF8),
    success = Color(0xFF10B981),
    warning = Color(0xFFFBBF24),
    error = Color(0xFFF87171),
    bubbleOutgoing = Color(0xFFE11D48),
    bubbleIncoming = Color(0xFF1A1D27),
    textOnPrimary = Color(0xFFFFFFFF),
    webGeometryTint = Color.Transparent
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
val BuddysRed = Color(0xFFE11D48)
val BuddysDeepRed = Color(0xFFBE123C)
val BuddysDarkBlue = Color(0xFF0F172A)
val BuddysWhite = Color(0xFFFFFFFF)
val BuddysBlack = Color(0xFF090A0E)

val BackgroundCream: Color @Composable get() = BuddysTheme.colors.background
val PaperWhite: Color @Composable get() = BuddysTheme.colors.surface
val NotebookBorder: Color @Composable get() = BuddysTheme.colors.border
val TextPrimary: Color @Composable get() = BuddysTheme.colors.textPrimary
val TextSecondary: Color @Composable get() = BuddysTheme.colors.textSecondary
val TextMuted: Color @Composable get() = BuddysTheme.colors.textMuted
val AccentPrimary: Color @Composable get() = BuddysTheme.colors.primaryAccent
val AccentDeep: Color @Composable get() = BuddysTheme.colors.deepAccent
val AccentRose: Color @Composable get() = BuddysTheme.colors.primaryAccent
val AccentSky: Color @Composable get() = BuddysTheme.colors.secondaryAccent
val AccentSunny: Color @Composable get() = BuddysTheme.colors.warning
val AccentMint: Color @Composable get() = BuddysTheme.colors.success
val PastelSky: Color @Composable get() = BuddysTheme.colors.surfaceSecondary
val PastelYellow: Color @Composable get() = BuddysTheme.colors.surfaceSecondary
val PastelPink: Color @Composable get() = BuddysTheme.colors.softAccent
val PastelMint: Color @Composable get() = BuddysTheme.colors.surfaceSecondary
val PastelSkyBorder: Color @Composable get() = BuddysTheme.colors.border

val BuddysRedGradient: Brush
    get() = Brush.linearGradient(
        listOf(Color(0xFFF43F5E), Color(0xFFE11D48))
    )

val StoryRingGradient: Brush
    get() = Brush.sweepGradient(
        listOf(
            Color(0xFFF43F5E),
            Color(0xFFFB7185),
            Color(0xFFE11D48),
            Color(0xFFBE123C),
            Color(0xFFF43F5E)
        )
    )

val SnapMediaColor: Color = Color(0xFFF43F5E)
val SnapChatColor: Color = Color(0xFFE11D48)
val SnapVoiceColor: Color = Color(0xFF0284C7)
val SeenReceiptBlue: Color = Color(0xFF38BDF8)

val GlassCardBorder: Color
    @Composable get() = if (BuddysTheme.colors.isDark) Color(0xFF232838) else Color(0xFFE2E8F0)
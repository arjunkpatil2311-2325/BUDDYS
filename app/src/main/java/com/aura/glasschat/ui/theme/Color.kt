package com.aura.glasschat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ====================================================================
// BUDDYS MODERN CARTOON DESIGN SYSTEM COLOR TOKENS (REFERENCE INSPIRED)
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
    val primaryAccent: Color,      // Sunshine Yellow (#FDC827)
    val deepAccent: Color,         // Warm Amber (#F59E0B)
    val softAccent: Color,         // Soft Warm Tint (#FFF8E1)
    val secondaryAccent: Color,    // Sky Blue (#82C8FA)
    val skyHeader: Color,          // Sky Blue Header (#82C8FA)
    val yellowHeader: Color,       // Sunshine Yellow (#FDC827)
    val strongYellow: Color,       // Sunshine Yellow (#FDC827)
    val success: Color,            // Online Green (#22C55E)
    val warning: Color,            // Amber (#F59E0B)
    val error: Color,              // Cartoon Red (#EF4444)
    val bubbleOutgoing: Color,     // Sunshine Yellow (#FDC827)
    val bubbleIncoming: Color,     // Crisp White (#FFFFFF)
    val textOnPrimary: Color,      // Dark text on Sunshine Yellow (#18181B)
    val bottomDock: Color,         // Deep Black Floating Dock (#121316)
    val filterActive: Color,       // Dark Capsule Pill (#18181B)
    val filterInactive: Color,     // White Capsule Pill (#FFFFFF)
    val webGeometryTint: Color = Color.Transparent
) {
    // Backward compatible aliases
    val primaryRed: Color get() = primaryAccent
    val deepRed: Color get() = deepAccent
    val softRed: Color get() = softAccent
    val spiderBlue: Color get() = secondaryAccent
    val surfaceHeader: Color get() = skyHeader
}

// --------------------------------------------------------------------
// LIGHT THEME (PRIMARY: Sky Blue Header + Clean Cream Surface + Sunshine Yellow Bubbles)
// --------------------------------------------------------------------
val LightBuddysColors = BuddysColorScheme(
    isDark = false,
    background = Color(0xFF82C8FA),       // Sky blue canvas / header
    surface = Color(0xFFFFFDF9),          // Crisp warm cream main surface sheet
    surfaceSecondary = Color(0xFFF4F4F6), // Light neutral gray secondary surface
    surfaceElevated = Color(0xFFFFFFFF),  // Pure white elevated card
    surfaceComposer = Color(0xFFFFFFFF),  // Clean white composer capsule
    textPrimary = Color(0xFF18181B),      // Deep crisp dark text
    textSecondary = Color(0xFF64748B),    // Slate secondary text
    textMuted = Color(0xFF94A3B8),        // Faint muted timestamp text
    border = Color(0xFFE2E8F0),           // Subtle soft border
    divider = Color(0xFFF1F5F9),          // Subtle chat list divider
    primaryAccent = Color(0xFFFDC827),    // Sunshine golden yellow
    deepAccent = Color(0xFFF59E0B),       // Rich warm amber
    softAccent = Color(0xFFFFF9E6),       // Soft sunshine tint
    secondaryAccent = Color(0xFF82C8FA),  // Sky Blue accent
    skyHeader = Color(0xFF82C8FA),        // Reference Sky Blue Top Header
    yellowHeader = Color(0xFFFDC827),     // Yellow accent
    strongYellow = Color(0xFFFDC827),     // Yellow accent
    success = Color(0xFF22C55E),          // Vibrant online green
    warning = Color(0xFFF59E0B),          // Cartoon amber
    error = Color(0xFFEF4444),            // Stamp red
    bubbleOutgoing = Color(0xFFFDC827),   // Sunshine yellow speech bubble
    bubbleIncoming = Color(0xFFFFFFFF),   // Crisp white speech bubble
    textOnPrimary = Color(0xFF18181B),    // High-contrast dark text on yellow
    bottomDock = Color(0xFF121316),       // Deep black floating bottom dock
    filterActive = Color(0xFF18181B),     // Black active filter pill
    filterInactive = Color(0xFFFFFFFF),   // White inactive filter pill
    webGeometryTint = Color.Transparent
)

// --------------------------------------------------------------------
// DARK THEME (Modern Dark Mode with Sky Blue & Sunshine Accents)
// --------------------------------------------------------------------
val DarkBuddysColors = BuddysColorScheme(
    isDark = true,
    background = Color(0xFF0F172A),       // Deep twilight navy background
    surface = Color(0xFF1E293B),          // Dark surface
    surfaceSecondary = Color(0xFF334155), // Dark secondary surface
    surfaceElevated = Color(0xFF243044),  // Elevated dark layer
    surfaceComposer = Color(0xFF1E293B),  // Dark composer
    textPrimary = Color(0xFFF8FAFC),      // Crisp white text
    textSecondary = Color(0xFF94A3B8),    // Soft slate text
    textMuted = Color(0xFF64748B),        // Faint text
    border = Color(0xFF334155),           // Dark border
    divider = Color(0xFF243044),          // Dark divider
    primaryAccent = Color(0xFFFDC827),    // Sunshine yellow
    deepAccent = Color(0xFFF59E0B),       // Amber
    softAccent = Color(0xFF332F27),       // Dark tint
    secondaryAccent = Color(0xFF82C8FA),  // Sky blue
    skyHeader = Color(0xFF1E293B),        // Dark header
    yellowHeader = Color(0xFFFDC827),     // Yellow
    strongYellow = Color(0xFFFDC827),     // Yellow
    success = Color(0xFF22C55E),          // Green
    warning = Color(0xFFFBBF24),          // Yellow
    error = Color(0xFFEF4444),            // Red
    bubbleOutgoing = Color(0xFFFDC827),   // Sunshine yellow bubble
    bubbleIncoming = Color(0xFF243044),   // Dark incoming bubble
    textOnPrimary = Color(0xFF18181B),    // Dark text on yellow
    bottomDock = Color(0xFF090A0C),       // Deep black dock
    filterActive = Color(0xFFFDC827),     // Yellow active filter in dark mode
    filterInactive = Color(0xFF243044),   // Inactive filter
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
// MODERN CARTOON CONSTANTS
// --------------------------------------------------------------------
val BuddysSkyBlue = Color(0xFF82C8FA)
val BuddysSunshineYellow = Color(0xFFFDC827)
val BuddysDarkDock = Color(0xFF121316)
val BuddysCreamSurface = Color(0xFFFFFDF9)
val BuddysWhite = Color(0xFFFFFFFF)
val BuddysInk = Color(0xFF18181B)
val BuddysMuted = Color(0xFF94A3B8)

// Backward compatible aliases
val BuddysPrimaryPaper = Color(0xFFFFFDF9)
val BuddysSecondaryPaper = Color(0xFFF4F4F6)
val BuddysWhitePaper = Color(0xFFFFFFFF)
val BuddysPrimaryYellow = Color(0xFFFDC827)
val BuddysStrongYellow = Color(0xFFFDC827)
val BuddysPrimaryOrange = Color(0xFFFDC827)
val BuddysSecondaryOrange = Color(0xFFF59E0B)
val BuddysMutedInk = Color(0xFF64748B)
val BuddysLightDivider = Color(0xFFF1F5F9)
val BuddysRed = Color(0xFFFDC827)
val BuddysDeepRed = Color(0xFFF59E0B)
val BuddysDarkBlue = Color(0xFF121316)
val BuddysBlack = Color(0xFF121316)

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
val AccentSunny: Color @Composable get() = BuddysTheme.colors.primaryAccent
val AccentMint: Color @Composable get() = BuddysTheme.colors.success
val PastelSky: Color @Composable get() = BuddysTheme.colors.skyHeader
val PastelYellow: Color @Composable get() = BuddysTheme.colors.primaryAccent
val PastelPink: Color @Composable get() = BuddysTheme.colors.softAccent
val PastelMint: Color @Composable get() = BuddysTheme.colors.success
val PastelSkyBorder: Color @Composable get() = BuddysTheme.colors.border

// Gradients
val BuddysRedGradient: Brush
    get() = Brush.linearGradient(
        listOf(Color(0xFFFDC827), Color(0xFFF59E0B))
    )

val StoryRingGradient: Brush
    get() = Brush.sweepGradient(
        listOf(
            Color(0xFF82C8FA),
            Color(0xFFFDC827),
            Color(0xFF22C55E),
            Color(0xFF82C8FA)
        )
    )

val SnapMediaColor: Color = Color(0xFFFDC827)
val SnapChatColor: Color = Color(0xFF82C8FA)
val SnapVoiceColor: Color = Color(0xFF22C55E)
val SeenReceiptBlue: Color = Color(0xFF82C8FA)

val GlassCardBorder: Color
    @Composable get() = BuddysTheme.colors.border
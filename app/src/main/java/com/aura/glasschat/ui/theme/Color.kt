package com.aura.glasschat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ====================================================================
// BUDDYS MASTER DESIGN SYSTEM — OBSIDIAN & PRECISION GLASS TOKENS
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
    val primaryAccent: Color,      // Signature Electric Crimson (#FF3B40)
    val deepAccent: Color,         // Deep Crimson (#D92228)
    val softAccent: Color,         // Soft Glow Tint
    val secondaryAccent: Color,    // Electric Sky Blue (#38BDF8)
    val skyHeader: Color,          // Seamless Top Header Surface
    val yellowHeader: Color,       // Accent Tag
    val strongYellow: Color,       // Accent Tag
    val success: Color,            // Emerald Green (#10B981)
    val warning: Color,            // Warm Amber (#F59E0B)
    val error: Color,              // Crimson Red (#EF4444)
    val bubbleOutgoing: Color,     // Signature Crimson (#FF3B40)
    val bubbleIncoming: Color,     // Refined Neutral Surface Bubble
    val textOnPrimary: Color,      // High-contrast Pure White (#FFFFFF)
    val bottomDock: Color,         // Floating Glass Dock Surface
    val filterActive: Color,       // Active Filter Pill
    val filterInactive: Color,     // Inactive Filter Pill
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
// LIGHT THEME (Porcelain Canvas + Crisp Pure White Surface + Signature Crimson)
// --------------------------------------------------------------------
val LightBuddysColors = BuddysColorScheme(
    isDark = false,
    background = Color(0xFFF8F9FA),       // Clean airy porcelain canvas
    surface = Color(0xFFFFFFFF),          // Pure crisp white cards
    surfaceSecondary = Color(0xFFF1F3F6), // Smooth light slate secondary surface
    surfaceElevated = Color(0xFFFFFFFF),  // Pure white elevated card with shadow
    surfaceComposer = Color(0xFFFFFFFF),  // Clean white composer capsule
    textPrimary = Color(0xFF0F172A),      // Deep crisp dark slate
    textSecondary = Color(0xFF475569),    // Refined medium slate
    textMuted = Color(0xFF94A3B8),        // Soft muted timestamp text
    border = Color(0xFFE2E8F0),           // Subtle hairline border
    divider = Color(0xFFF1F5F9),          // Hairline divider
    primaryAccent = Color(0xFFFF3B40),    // Signature Electric Crimson
    deepAccent = Color(0xFFD92228),       // Deep Crimson
    softAccent = Color(0xFFFFF1F2),       // Soft rose glow tint
    secondaryAccent = Color(0xFF0284C7),  // Sky Blue accent
    skyHeader = Color(0xFFF8F9FA),        // Seamless clean header
    yellowHeader = Color(0xFFFF3B40),     // Vibrant brand accent
    strongYellow = Color(0xFFFF3B40),     // Vibrant brand accent
    success = Color(0xFF10B981),          // Emerald online green
    warning = Color(0xFFF59E0B),          // Amber
    error = Color(0xFFEF4444),            // Crimson error
    bubbleOutgoing = Color(0xFFFF3B40),   // Signature Crimson Bubble
    bubbleIncoming = Color(0xFFF1F3F6),   // Crisp light gray speech bubble
    textOnPrimary = Color(0xFFFFFFFF),    // High-contrast pure white text
    bottomDock = Color(0xFFFFFFFF),       // Pure white floating dock
    filterActive = Color(0xFF0F172A),     // Crisp dark active filter pill
    filterInactive = Color(0xFFF1F3F6),   // Light inactive filter pill
    webGeometryTint = Color.Transparent
)

// --------------------------------------------------------------------
// DARK THEME (Obsidian OLED Black + Graphite Surface + Glowing Crimson)
// --------------------------------------------------------------------
val DarkBuddysColors = BuddysColorScheme(
    isDark = true,
    background = Color(0xFF090A0E),       // Pure OLED Obsidian Black
    surface = Color(0xFF13141B),          // Deep dark graphite surface
    surfaceSecondary = Color(0xFF1A1C24), // Elevated dark secondary surface
    surfaceElevated = Color(0xFF222530),  // Floating modal / sheet layer
    surfaceComposer = Color(0xFF161821),  // Dark sleek composer capsule
    textPrimary = Color(0xFFF8FAFC),      // Crisp pure white text
    textSecondary = Color(0xFF94A3B8),    // Soft slate text
    textMuted = Color(0xFF64748B),        // Muted timestamp text
    border = Color(0xFF232734),           // Subtle 1px hairline dark border
    divider = Color(0xFF1B1E28),          // Subtle divider
    primaryAccent = Color(0xFFFF3B40),    // Signature Electric Crimson
    deepAccent = Color(0xFFD92228),       // Deep Crimson
    softAccent = Color(0xFF2D1418),       // Subtle crimson ambient tint
    secondaryAccent = Color(0xFF38BDF8),  // Electric Sky Blue
    skyHeader = Color(0xFF090A0E),        // Seamless dark header
    yellowHeader = Color(0xFFFF3B40),     // Vibrant brand accent
    strongYellow = Color(0xFFFF3B40),     // Vibrant brand accent
    success = Color(0xFF10B981),          // Emerald online green
    warning = Color(0xFFFBBF24),          // Warm Amber
    error = Color(0xFFEF4444),            // Crimson red
    bubbleOutgoing = Color(0xFFFF3B40),   // Signature Crimson Outgoing Bubble
    bubbleIncoming = Color(0xFF1A1C25),   // Refined dark graphite incoming bubble
    textOnPrimary = Color(0xFFFFFFFF),    // Pure white on crimson
    bottomDock = Color(0xFF111218),       // Deep graphite floating glass dock
    filterActive = Color(0xFFFF3B40),     // Vibrant crimson active pill
    filterInactive = Color(0xFF1A1C25),   // Subtle dark inactive pill
    webGeometryTint = Color.Transparent
)

val LocalBuddysColors = staticCompositionLocalOf { DarkBuddysColors }

object BuddysTheme {
    val colors: BuddysColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalBuddysColors.current
}

// --------------------------------------------------------------------
// DESIGN SYSTEM CONSTANTS & HELPERS
// --------------------------------------------------------------------
val BuddysSkyBlue = Color(0xFF38BDF8)
val BuddysSunshineYellow = Color(0xFFFF3B40)
val BuddysDarkDock = Color(0xFF111218)
val BuddysCreamSurface = Color(0xFF13141B)
val BuddysWhite = Color(0xFFFFFFFF)
val BuddysInk = Color(0xFFF8FAFC)
val BuddysMuted = Color(0xFF94A3B8)

// Backward compatible aliases
val BuddysPrimaryPaper = Color(0xFF13141B)
val BuddysSecondaryPaper = Color(0xFF1A1C24)
val BuddysWhitePaper = Color(0xFFFFFFFF)
val BuddysPrimaryYellow = Color(0xFFFF3B40)
val BuddysStrongYellow = Color(0xFFFF3B40)
val BuddysPrimaryOrange = Color(0xFFFF3B40)
val BuddysSecondaryOrange = Color(0xFFD92228)
val BuddysMutedInk = Color(0xFF64748B)
val BuddysLightDivider = Color(0xFF1B1E28)
val BuddysRed = Color(0xFFFF3B40)
val BuddysDeepRed = Color(0xFFD92228)
val BuddysDarkBlue = Color(0xFF090A0E)
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
val AccentSunny: Color @Composable get() = BuddysTheme.colors.primaryAccent
val AccentMint: Color @Composable get() = BuddysTheme.colors.success
val PastelSky: Color @Composable get() = BuddysTheme.colors.skyHeader
val PastelYellow: Color @Composable get() = BuddysTheme.colors.primaryAccent
val PastelPink: Color @Composable get() = BuddysTheme.colors.softAccent
val PastelMint: Color @Composable get() = BuddysTheme.colors.success
val PastelSkyBorder: Color @Composable get() = BuddysTheme.colors.border

// High-end gradients
val BuddysRedGradient: Brush
    get() = Brush.linearGradient(
        listOf(Color(0xFFFF3B40), Color(0xFFFF5E62))
    )

val StoryRingGradient: Brush
    get() = Brush.sweepGradient(
        listOf(
            Color(0xFFFF3B40),
            Color(0xFFFF6B6B),
            Color(0xFFE11D48),
            Color(0xFFFF5E62),
            Color(0xFFFF3B40)
        )
    )

val SnapMediaColor: Color = Color(0xFFFF3B40)
val SnapChatColor: Color = Color(0xFFFF3B40)
val SnapVoiceColor: Color = Color(0xFF38BDF8)
val SeenReceiptBlue: Color = Color(0xFF38BDF8)

val GlassCardBorder: Color
    @Composable get() = BuddysTheme.colors.border
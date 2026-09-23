package com.aura.glasschat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ====================================================================
// BUDDIES RETRO CARTOON DESIGN SYSTEM — PAPER & INK COLOR TOKENS
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
    val primaryAccent: Color,      // Primary Orange (#F46A21)
    val deepAccent: Color,         // Strong Orange (#E55610)
    val softAccent: Color,         // Warm Paper Tone (#F8EACB)
    val secondaryAccent: Color,    // Primary Yellow (#FFE52E)
    val yellowHeader: Color,       // Header Yellow (#FFE52E)
    val strongYellow: Color,       // Strong Yellow (#FFD91A)
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
    val surfaceHeader: Color get() = yellowHeader
}

// --------------------------------------------------------------------
// LIGHT THEME (DEFAULT: Warm Paper + Bold Ink + Sunny Yellow + Orange)
// --------------------------------------------------------------------
val LightBuddysColors = BuddysColorScheme(
    isDark = false,
    background = Color(0xFFFFF4DE),       // Warm primary cream paper
    surface = Color(0xFFFFFDF5),          // Crisp paper card surface
    surfaceSecondary = Color(0xFFF8EACB), // Secondary paper
    surfaceElevated = Color(0xFFF3E2BD),  // Elevated paper layer
    surfaceComposer = Color(0xFFFFFDF5),  // Composer paper background
    textPrimary = Color(0xFF171717),      // Deep dark ink
    textSecondary = Color(0xFF55504A),    // Muted ink
    textMuted = Color(0xFF8A8275),        // Faint ink / captions
    border = Color(0xFF171717),           // Bold 1.5-2dp dark ink border
    divider = Color(0xFFB8A98C),          // Light notebook rule divider
    primaryAccent = Color(0xFFF46A21),    // Primary illustrated orange
    deepAccent = Color(0xFFE0550E),       // Deep orange
    softAccent = Color(0xFFF8EACB),       // Soft paper tint
    secondaryAccent = Color(0xFFFFE52E),  // Primary yellow accent
    yellowHeader = Color(0xFFFFE52E),     // Section header yellow block
    strongYellow = Color(0xFFFFD91A),     // Strong yellow
    success = Color(0xFF16A34A),          // Playful green
    warning = Color(0xFFF59E0B),          // Cartoon amber
    error = Color(0xFFDC2626),            // Retro stamp red
    bubbleOutgoing = Color(0xFFF46A21),   // Vibrant orange speech bubble
    bubbleIncoming = Color(0xFFFFFDF5),   // Warm paper speech bubble
    textOnPrimary = Color(0xFFFFFFFF),    // Clean white on orange
    webGeometryTint = Color.Transparent
)

// --------------------------------------------------------------------
// DARK THEME (Dark Retro Paper + Light Ink + Yellow + Orange)
// --------------------------------------------------------------------
val DarkBuddysColors = BuddysColorScheme(
    isDark = true,
    background = Color(0xFF171513),       // Dark paper background
    surface = Color(0xFF211E19),          // Dark surface
    surfaceSecondary = Color(0xFF2B2720), // Dark secondary surface
    surfaceElevated = Color(0xFF332F27),  // Elevated dark paper
    surfaceComposer = Color(0xFF211E19),  // Dark composer
    textPrimary = Color(0xFFFFF4DE),      // Warm cream ink
    textSecondary = Color(0xFFB8A98C),    // Muted paper ink
    textMuted = Color(0xFF7A705E),        // Faint dark ink
    border = Color(0xFF4A4338),           // Dark ink border (light contrast)
    divider = Color(0xFF3A342B),          // Dark divider
    primaryAccent = Color(0xFFF46A21),    // Primary orange
    deepAccent = Color(0xFFFF8A3D),       // Secondary orange
    softAccent = Color(0xFF2B2720),       // Soft dark tint
    secondaryAccent = Color(0xFFFFE52E),  // Yellow accent
    yellowHeader = Color(0xFFFFE52E),     // Header yellow
    strongYellow = Color(0xFFFFD91A),     // Strong yellow
    success = Color(0xFF22C55E),          // Green
    warning = Color(0xFFFBBF24),          // Yellow
    error = Color(0xFFEF4444),            // Red
    bubbleOutgoing = Color(0xFFF46A21),   // Orange bubble
    bubbleIncoming = Color(0xFF211E19),   // Dark paper bubble
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
// RETRO CARTOON DESIGN SYSTEM CONSTANTS & DYNAMIC GETTERS
// --------------------------------------------------------------------
val BuddysPrimaryPaper = Color(0xFFFFF4DE)
val BuddysSecondaryPaper = Color(0xFFF8EACB)
val BuddysWhitePaper = Color(0xFFFFFDF5)
val BuddysPrimaryYellow = Color(0xFFFFE52E)
val BuddysStrongYellow = Color(0xFFFFD91A)
val BuddysPrimaryOrange = Color(0xFFF46A21)
val BuddysSecondaryOrange = Color(0xFFFF8A3D)
val BuddysInk = Color(0xFF171717)
val BuddysMutedInk = Color(0xFF55504A)
val BuddysLightDivider = Color(0xFFB8A98C)

// Backward compatible aliases
val BuddysRed = Color(0xFFF46A21)
val BuddysDeepRed = Color(0xFFE0550E)
val BuddysDarkBlue = Color(0xFF171717)
val BuddysWhite = Color(0xFFFFFDF5)
val BuddysBlack = Color(0xFF171717)

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
val PastelYellow: Color @Composable get() = BuddysTheme.colors.yellowHeader
val PastelPink: Color @Composable get() = BuddysTheme.colors.softAccent
val PastelMint: Color @Composable get() = BuddysTheme.colors.surfaceSecondary
val PastelSkyBorder: Color @Composable get() = BuddysTheme.colors.border

// Flat / illustrated story ring & brand brushes
val BuddysRedGradient: Brush
    get() = Brush.linearGradient(
        listOf(Color(0xFFF46A21), Color(0xFFFF8A3D))
    )

val StoryRingGradient: Brush
    get() = Brush.sweepGradient(
        listOf(
            Color(0xFFFFE52E),
            Color(0xFFF46A21),
            Color(0xFFFFD91A),
            Color(0xFFFF8A3D),
            Color(0xFFFFE52E)
        )
    )

val SnapMediaColor: Color = Color(0xFFF46A21)
val SnapChatColor: Color = Color(0xFFFFE52E)
val SnapVoiceColor: Color = Color(0xFFFF8A3D)
val SeenReceiptBlue: Color = Color(0xFF171717)

val GlassCardBorder: Color
    @Composable get() = BuddysTheme.colors.border
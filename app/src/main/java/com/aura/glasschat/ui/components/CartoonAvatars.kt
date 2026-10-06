package com.aura.glasschat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs

/**
 * Designer Mesh-Gradient Squircle Avatar Component.
 * Features ultra-clean, luxury gradient palettes, bold monograms,
 * specialized futuristic AI holographic orb, and subtle hairline rim borders.
 */
@Composable
fun CartoonAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    avatarUrl: String? = null,
    name: String = "",
    userId: String = "",
    isOnline: Boolean = false,
    showOnlineBadge: Boolean = false,
    isAi: Boolean = false,
    cornerRadius: Dp = 16.dp
) {
    val seed = (userId.ifBlank { name }).hashCode()
    val paletteIndex = abs(seed) % AvatarGradients.size

    val initial = when {
        isAi -> ""
        name.isNotBlank() -> name.trim().take(1).uppercase()
        userId.isNotBlank() -> userId.take(1).uppercase()
        else -> "B"
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(cornerRadius)),
                contentScale = ContentScale.Crop
            )
        } else if (isAi) {
            // Futuristic AI Holographic Orb Avatar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFF3B40),
                                Color(0xFF6366F1),
                                Color(0xFF38BDF8)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(cornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Buddys AI",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        } else {
            // High-End Designer Mesh Gradient Monogram Avatar
            val (gradStart, gradEnd) = AvatarGradients[paletteIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(gradStart, gradEnd),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(cornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.44f).sp,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // Online Status Indicator Dot
        if (showOnlineBadge && isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(2.dp, Color(0xFF090A0E), CircleShape)
            )
        }
    }
}

/**
 * Curated luxury gradient duos for avatar generation.
 */
private val AvatarGradients = listOf(
    Pair(Color(0xFFFF3B40), Color(0xFFFF7A59)), // Signature Crimson & Coral
    Pair(Color(0xFF6366F1), Color(0xFF8B5CF6)), // Royal Indigo & Violet
    Pair(Color(0xFF0EA5E9), Color(0xFF2563EB)), // Electric Azure & Sapphire
    Pair(Color(0xFF10B981), Color(0xFF059669)), // Emerald Mint
    Pair(Color(0xFFF59E0B), Color(0xFFD97706)), // Warm Amber & Gold
    Pair(Color(0xFFEC4899), Color(0xFFF43F5E)), // Hot Rose & Crimson
    Pair(Color(0xFF14B8A6), Color(0xFF0D9488)), // Cyan Teal
    Pair(Color(0xFF8B5CF6), Color(0xFFD946EF))  // Deep Purple & Fuchsia
)

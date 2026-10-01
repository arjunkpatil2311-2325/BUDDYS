package com.aura.glasschat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs

/**
 * Colorful Cartoon Squircle Avatar Component matching the reference design.
 * Features 9 distinctive cartoon characters with vibrant pastel squircle backdrops,
 * network image loading fallback, and optional online status indicator.
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
    val characterIndex = if (isAi) {
        8 // Dedicated Buddys AI Mascot
    } else {
        val seed = (userId.ifBlank { name }).hashCode()
        abs(seed) % 8
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
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(cornerRadius)),
                contentScale = ContentScale.Crop
            )
        } else {
            // Draw Hand-Crafted Cartoon Character Squircle
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                drawCartoonCharacter(characterIndex, size = this.size)
            }
        }

        // Online Status Dot
        if (showOnlineBadge && isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Draws one of 9 cute cartoon characters on a vibrant squircle backdrop.
 */
private fun DrawScope.drawCartoonCharacter(index: Int, size: Size) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    when (index) {
        0 -> {
            // 🐯 Tiger (fabulousNooby) - Warm Amber Backdrop
            drawRect(Color(0xFFF97316))
            // Body / Shirt (Green)
            drawRoundRect(Color(0xFF22C55E), Offset(w * 0.22f, h * 0.62f), Size(w * 0.56f, h * 0.45f), CornerRadius(12f, 12f))
            // Head
            drawCircle(Color(0xFFFFA500), radius = w * 0.28f, center = Offset(cx, h * 0.44f))
            // Ears
            drawCircle(Color(0xFFFFA500), radius = w * 0.09f, center = Offset(w * 0.28f, h * 0.23f))
            drawCircle(Color(0xFFFFD180), radius = w * 0.05f, center = Offset(w * 0.28f, h * 0.23f))
            drawCircle(Color(0xFFFFA500), radius = w * 0.09f, center = Offset(w * 0.72f, h * 0.23f))
            drawCircle(Color(0xFFFFD180), radius = w * 0.05f, center = Offset(w * 0.72f, h * 0.23f))
            // Muzzle
            drawOval(Color(0xFFFFE0B2), Offset(w * 0.35f, h * 0.42f), Size(w * 0.30f, h * 0.20f))
            // Eyes
            drawCircle(Color(0xFF1E293B), radius = w * 0.035f, center = Offset(w * 0.38f, h * 0.40f))
            drawCircle(Color(0xFF1E293B), radius = w * 0.035f, center = Offset(w * 0.62f, h * 0.40f))
            // Nose
            drawCircle(Color(0xFF991B1B), radius = w * 0.025f, center = Offset(cx, h * 0.48f))
            // Stripes
            drawLine(Color(0xFF9A3412), Offset(cx, h * 0.24f), Offset(cx, h * 0.32f), strokeWidth = 3f)
        }
        1 -> {
            // 🦝 Raccoon (Study Squad) - Forest Green Backdrop
            drawRect(Color(0xFF16A34A))
            // Body (Dark Gray)
            drawRoundRect(Color(0xFF475569), Offset(w * 0.22f, h * 0.62f), Size(w * 0.56f, h * 0.45f), CornerRadius(12f, 12f))
            // Head (Gray)
            drawCircle(Color(0xFF94A3B8), radius = w * 0.28f, center = Offset(cx, h * 0.45f))
            // Mask
            drawRoundRect(Color(0xFF1E293B), Offset(w * 0.24f, h * 0.36f), Size(w * 0.52f, h * 0.16f), CornerRadius(8f, 8f))
            // Eyes
            drawCircle(Color.White, radius = w * 0.045f, center = Offset(w * 0.38f, h * 0.44f))
            drawCircle(Color(0xFF0F172A), radius = w * 0.025f, center = Offset(w * 0.38f, h * 0.44f))
            drawCircle(Color.White, radius = w * 0.045f, center = Offset(w * 0.62f, h * 0.44f))
            drawCircle(Color(0xFF0F172A), radius = w * 0.025f, center = Offset(w * 0.62f, h * 0.44f))
            // Cap (Graduation cap style)
            drawRoundRect(Color(0xFF0F172A), Offset(w * 0.25f, h * 0.18f), Size(w * 0.50f, h * 0.10f), CornerRadius(4f, 4f))
        }
        2 -> {
            // 🐧 Penguin (Mango) - Purple/Golden Backdrop
            drawRect(Color(0xFF8B5CF6))
            // Body (Black / White)
            drawCircle(Color(0xFF0F172A), radius = w * 0.30f, center = Offset(cx, h * 0.50f))
            drawOval(Color.White, Offset(w * 0.30f, h * 0.38f), Size(w * 0.40f, h * 0.38f))
            // Knitted Beanie Hat (Yellow)
            drawRoundRect(Color(0xFFFDC827), Offset(w * 0.25f, h * 0.18f), Size(w * 0.50f, h * 0.20f), CornerRadius(8f, 8f))
            drawCircle(Color(0xFFF59E0B), radius = w * 0.06f, center = Offset(cx, h * 0.16f))
            // Eyes
            drawCircle(Color(0xFF0F172A), radius = w * 0.035f, center = Offset(w * 0.40f, h * 0.46f))
            drawCircle(Color(0xFF0F172A), radius = w * 0.035f, center = Offset(w * 0.60f, h * 0.46f))
            // Orange Beak
            drawOval(Color(0xFFF97316), Offset(w * 0.44f, h * 0.52f), Size(w * 0.12f, h * 0.09f))
        }
        3 -> {
            // 🐘 Elephant (Weekend Plans) - Lilac/Lavender Backdrop
            drawRect(Color(0xFFA855F7))
            // Ears
            drawCircle(Color(0xFF93C5FD), radius = w * 0.20f, center = Offset(w * 0.24f, h * 0.40f))
            drawCircle(Color(0xFF93C5FD), radius = w * 0.20f, center = Offset(w * 0.76f, h * 0.40f))
            // Head
            drawCircle(Color(0xFF60A5FA), radius = w * 0.26f, center = Offset(cx, h * 0.45f))
            // Trunk
            drawRoundRect(Color(0xFF3B82F6), Offset(w * 0.42f, h * 0.48f), Size(w * 0.16f, h * 0.28f), CornerRadius(8f, 8f))
            // Eyes
            drawCircle(Color(0xFF0F172A), radius = w * 0.035f, center = Offset(w * 0.36f, h * 0.40f))
            drawCircle(Color(0xFF0F172A), radius = w * 0.035f, center = Offset(w * 0.64f, h * 0.40f))
        }
        4 -> {
            // 🦁 Lion (Jenny) - Sky Blue Backdrop
            drawRect(Color(0xFF38BDF8))
            // Mane (Orange)
            drawCircle(Color(0xFFF97316), radius = w * 0.34f, center = Offset(cx, h * 0.45f))
            // Face (Yellow)
            drawCircle(Color(0xFFFDE047), radius = w * 0.22f, center = Offset(cx, h * 0.45f))
            // Eyes
            drawCircle(Color(0xFF0F172A), radius = w * 0.035f, center = Offset(w * 0.40f, h * 0.42f))
            drawCircle(Color(0xFF0F172A), radius = w * 0.035f, center = Offset(w * 0.60f, h * 0.42f))
            // Pink Nose
            drawCircle(Color(0xFFEC4899), radius = w * 0.03f, center = Offset(cx, h * 0.50f))
        }
        5 -> {
            // 🐼 Panda (Game On) - Vibrant Pink Backdrop
            drawRect(Color(0xFFF43F5E))
            // Body (Green Hoodie)
            drawRoundRect(Color(0xFF10B981), Offset(w * 0.22f, h * 0.62f), Size(w * 0.56f, h * 0.45f), CornerRadius(12f, 12f))
            // Ears (Black)
            drawCircle(Color(0xFF1E293B), radius = w * 0.09f, center = Offset(w * 0.28f, h * 0.25f))
            drawCircle(Color(0xFF1E293B), radius = w * 0.09f, center = Offset(w * 0.72f, h * 0.25f))
            // Head (White)
            drawCircle(Color.White, radius = w * 0.26f, center = Offset(cx, h * 0.45f))
            // Eye Patches
            drawOval(Color(0xFF1E293B), Offset(w * 0.30f, h * 0.38f), Size(w * 0.16f, h * 0.14f))
            drawOval(Color(0xFF1E293B), Offset(w * 0.54f, h * 0.38f), Size(w * 0.16f, h * 0.14f))
            // Eyes
            drawCircle(Color.White, radius = w * 0.025f, center = Offset(w * 0.38f, h * 0.44f))
            drawCircle(Color.White, radius = w * 0.025f, center = Offset(w * 0.62f, h * 0.44f))
            // Nose
            drawOval(Color(0xFF0F172A), Offset(w * 0.44f, h * 0.52f), Size(w * 0.12f, h * 0.07f))
        }
        6 -> {
            // 🐻 Bear (Teddy) - Sunny Sky Blue Backdrop
            drawRect(Color(0xFF0284C7))
            // Body (Yellow Tee)
            drawRoundRect(Color(0xFFFDC827), Offset(w * 0.22f, h * 0.62f), Size(w * 0.56f, h * 0.45f), CornerRadius(12f, 12f))
            // Ears (Brown)
            drawCircle(Color(0xFF92400E), radius = w * 0.09f, center = Offset(w * 0.28f, h * 0.25f))
            drawCircle(Color(0xFFD97706), radius = w * 0.05f, center = Offset(w * 0.28f, h * 0.25f))
            drawCircle(Color(0xFF92400E), radius = w * 0.09f, center = Offset(w * 0.72f, h * 0.25f))
            drawCircle(Color(0xFFD97706), radius = w * 0.05f, center = Offset(w * 0.72f, h * 0.25f))
            // Head (Brown)
            drawCircle(Color(0xFFB45309), radius = w * 0.26f, center = Offset(cx, h * 0.45f))
            // Muzzle
            drawOval(Color(0xFFFDE68A), Offset(w * 0.35f, h * 0.44f), Size(w * 0.30f, h * 0.18f))
            // Eyes
            drawCircle(Color(0xFF0F172A), radius = w * 0.035f, center = Offset(w * 0.38f, h * 0.40f))
            drawCircle(Color(0xFF0F172A), radius = w * 0.035f, center = Offset(w * 0.62f, h * 0.40f))
            // Nose
            drawCircle(Color(0xFF451A03), radius = w * 0.03f, center = Offset(cx, h * 0.49f))
        }
        7 -> {
            // 🐊 Crocodile / Buddy (Mint Green Backdrop)
            drawRect(Color(0xFF14B8A6))
            // Head
            drawRoundRect(Color(0xFF15803D), Offset(w * 0.22f, h * 0.30f), Size(w * 0.56f, h * 0.45f), CornerRadius(16f, 16f))
            // Sunglasses (Cool Black Shades)
            drawRoundRect(Color(0xFF0F172A), Offset(w * 0.26f, h * 0.38f), Size(w * 0.48f, h * 0.16f), CornerRadius(6f, 6f))
            drawLine(Color.White, Offset(w * 0.30f, h * 0.40f), Offset(w * 0.42f, h * 0.50f), strokeWidth = 2f)
            // Big Smile & Teeth
            drawArc(Color.White, 0f, 180f, true, Offset(w * 0.36f, h * 0.56f), Size(w * 0.28f, h * 0.12f))
        }
        8 -> {
            // 🤖 Buddys AI Mascot (Deep Indigo & Neon Yellow Sparkles)
            drawRect(Color(0xFF4F46E5))
            // Antenna
            drawLine(Color(0xFFFDC827), Offset(cx, h * 0.15f), Offset(cx, h * 0.28f), strokeWidth = 4f)
            drawCircle(Color(0xFFFDC827), radius = w * 0.06f, center = Offset(cx, h * 0.15f))
            // Robot Head
            drawRoundRect(Color(0xFFEEF2FF), Offset(w * 0.22f, h * 0.28f), Size(w * 0.56f, h * 0.45f), CornerRadius(16f, 16f))
            // Visor (Dark Navy)
            drawRoundRect(Color(0xFF1E1B4B), Offset(w * 0.28f, h * 0.36f), Size(w * 0.44f, h * 0.20f), CornerRadius(8f, 8f))
            // Glow Eyes (Cyan & Yellow)
            drawCircle(Color(0xFF38BDF8), radius = w * 0.04f, center = Offset(w * 0.40f, h * 0.46f))
            drawCircle(Color(0xFFFDC827), radius = w * 0.04f, center = Offset(w * 0.60f, h * 0.46f))
            // Cute Robot Smile
            drawArc(Color(0xFF6366F1), 0f, 180f, false, Offset(w * 0.42f, h * 0.58f), Size(w * 0.16f, h * 0.08f), style = Stroke(width = 3f))
        }
    }
}

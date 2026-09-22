package com.aura.glasschat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.ui.theme.BuddysTheme

/**
 * Modern Official BUDDIES Logo Vector Composable.
 * Beautifully synthesizes:
 * 1. Rounded Camera / Story frame
 * 2. Chat speech bubble pointer
 * 3. Camera lens / notification dot
 * 4. Two interlinked buddy silhouettes inside
 */
@Composable
fun BuddiesLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color = BuddysTheme.colors.primaryAccent,
    filled: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = (w * 0.085f).coerceAtLeast(2.5f)

        // 1. Camera / Chat Bubble Outer Boundary Path
        val framePath = Path().apply {
            val r = w * 0.24f
            // Start at top-left curve
            moveTo(r, 0f)
            // Top edge to top-right
            lineTo(w - r, 0f)
            quadraticTo(w, 0f, w, r)
            // Right edge down towards chat tail
            lineTo(w, h * 0.62f)
            // Speech tail point
            lineTo(w * 0.98f, h * 0.88f)
            lineTo(w * 0.72f, h * 0.82f)
            // Bottom edge to bottom-left
            lineTo(r, h * 0.82f)
            quadraticTo(0f, h * 0.82f, 0f, h * 0.82f - r)
            // Left edge to top-left
            lineTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            close()
        }

        if (filled) {
            drawPath(path = framePath, color = tint, style = Fill)
        } else {
            drawPath(
                path = framePath,
                color = tint,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 2. Camera Lens / Flash Dot (Top Right)
            drawCircle(
                color = tint,
                radius = w * 0.055f,
                center = Offset(w * 0.80f, h * 0.18f)
            )

            // 3. Buddy 1 (Left Silhouette)
            // Head
            drawCircle(
                color = tint,
                radius = w * 0.11f,
                center = Offset(w * 0.34f, h * 0.32f),
                style = Fill
            )
            // Body / Torso
            val bodyLeftPath = Path().apply {
                arcTo(
                    rect = Rect(
                        offset = Offset(w * 0.18f, h * 0.44f),
                        size = Size(w * 0.32f, h * 0.32f)
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
            }
            drawPath(
                path = bodyLeftPath,
                color = tint,
                style = Stroke(width = strokeWidth * 0.95f, cap = StrokeCap.Round)
            )

            // 4. Buddy 2 (Right Silhouette)
            // Head
            drawCircle(
                color = tint,
                radius = w * 0.11f,
                center = Offset(w * 0.62f, h * 0.28f),
                style = Fill
            )
            // Body / Torso
            val bodyRightPath = Path().apply {
                arcTo(
                    rect = Rect(
                        offset = Offset(w * 0.46f, h * 0.40f),
                        size = Size(w * 0.32f, h * 0.32f)
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
            }
            drawPath(
                path = bodyRightPath,
                color = tint,
                style = Stroke(width = strokeWidth * 0.95f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Backward compatibility alias for the logo.
 */
@Composable
fun BuddysLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color = BuddysTheme.colors.primaryAccent,
    filled: Boolean = false
) {
    BuddiesLogo(modifier = modifier, size = size, tint = tint, filled = filled)
}

/**
 * Backward compatibility alias for the emblem.
 */
@Composable
fun BuddysSpiderEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = BuddysTheme.colors.primaryAccent
) {
    BuddiesLogo(modifier = modifier, size = size, tint = tint)
}

/**
 * Canonical Top Branding Wordmark for Buddies with modern refined typography and logo.
 */
@Composable
fun BuddiesBrandHeader(
    modifier: Modifier = Modifier,
    showEmblem: Boolean = true,
    fontSize: Int = 22
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (showEmblem) {
            BuddiesLogo(size = (fontSize + 4).dp, tint = BuddysTheme.colors.primaryAccent)
            Spacer(modifier = Modifier.width(9.dp))
        }
        Text(
            text = "Buddies",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = BuddysTheme.colors.textPrimary,
                fontSize = fontSize.sp
            )
        )
    }
}

/**
 * Backward compatibility alias for brand header.
 */
@Composable
fun BuddysBrandHeader(
    modifier: Modifier = Modifier,
    showEmblem: Boolean = true
) {
    BuddiesBrandHeader(modifier = modifier, showEmblem = showEmblem)
}

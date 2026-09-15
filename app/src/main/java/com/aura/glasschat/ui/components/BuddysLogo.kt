package com.aura.glasschat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.ui.theme.BuddysTheme

/**
 * Original Abstract BUDDYS Spider/Web Emblem Vector.
 * Minimalist geometric mark featuring 8 angular web legs extending from a central diamond node.
 */
@Composable
fun BuddysSpiderEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = BuddysTheme.colors.primaryRed
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // Central Diamond Core
        val corePath = Path().apply {
            moveTo(cx, cy - h * 0.22f)
            lineTo(cx + w * 0.16f, cy)
            lineTo(cx, cy + h * 0.24f)
            lineTo(cx - w * 0.16f, cy)
            close()
        }
        drawPath(path = corePath, color = tint)

        val strokeWidth = (w * 0.055f).coerceAtLeast(1.5f)

        // Upper Angled Web Legs (Left & Right)
        val upperLeft = Path().apply {
            moveTo(cx - w * 0.10f, cy - h * 0.10f)
            lineTo(cx - w * 0.32f, cy - h * 0.36f)
            lineTo(cx - w * 0.44f, cy - h * 0.30f)
        }
        drawPath(upperLeft, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))

        val upperRight = Path().apply {
            moveTo(cx + w * 0.10f, cy - h * 0.10f)
            lineTo(cx + w * 0.32f, cy - h * 0.36f)
            lineTo(cx + w * 0.44f, cy - h * 0.30f)
        }
        drawPath(upperRight, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))

        // Mid Web Legs (Left & Right)
        val midLeft = Path().apply {
            moveTo(cx - w * 0.14f, cy)
            lineTo(cx - w * 0.38f, cy - h * 0.06f)
            lineTo(cx - w * 0.46f, cy + h * 0.08f)
        }
        drawPath(midLeft, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))

        val midRight = Path().apply {
            moveTo(cx + w * 0.14f, cy)
            lineTo(cx + w * 0.38f, cy - h * 0.06f)
            lineTo(cx + w * 0.46f, cy + h * 0.08f)
        }
        drawPath(midRight, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))

        // Lower Long Web Legs (Left & Right)
        val lowerLeft = Path().apply {
            moveTo(cx - w * 0.08f, cy + h * 0.12f)
            lineTo(cx - w * 0.30f, cy + h * 0.34f)
            lineTo(cx - w * 0.22f, cy + h * 0.45f)
        }
        drawPath(lowerLeft, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))

        val lowerRight = Path().apply {
            moveTo(cx + w * 0.08f, cy + h * 0.12f)
            lineTo(cx + w * 0.30f, cy + h * 0.34f)
            lineTo(cx + w * 0.22f, cy + h * 0.45f)
        }
        drawPath(lowerRight, color = tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))
    }
}

/**
 * Top Branding Wordmark for BUDDYS with the abstract emblem.
 */
@Composable
fun BuddysBrandHeader(
    modifier: Modifier = Modifier,
    showEmblem: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (showEmblem) {
            BuddysSpiderEmblem(size = 20.dp, tint = BuddysTheme.colors.primaryRed)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = "BUDDYS",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 20.sp
            )
        )
    }
}

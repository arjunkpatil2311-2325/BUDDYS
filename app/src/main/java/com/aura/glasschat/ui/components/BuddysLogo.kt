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
 * Modern Sleek BUDDYS Connection Emblem Vector.
 * Minimalist geometric mark representing two connected buds/circles (Buddies).
 */
@Composable
fun BuddysSpiderEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = BuddysTheme.colors.primaryAccent
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = w * 0.22f
        val strokeWidth = (w * 0.10f).coerceAtLeast(2f)

        // Left Buddy Ring
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(w * 0.35f, h * 0.5f),
            style = Stroke(width = strokeWidth)
        )

        // Right Buddy Ring (Interlinked)
        drawCircle(
            color = tint.copy(alpha = 0.85f),
            radius = r,
            center = Offset(w * 0.65f, h * 0.5f),
            style = Stroke(width = strokeWidth)
        )

        // Central connection pulse dot
        drawCircle(
            color = tint,
            radius = strokeWidth * 0.7f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
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

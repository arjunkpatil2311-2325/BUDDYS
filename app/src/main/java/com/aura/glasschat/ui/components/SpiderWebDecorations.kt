package com.aura.glasschat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.glasschat.ui.theme.BuddysTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Subtle geometric web decoration for header corners.
 * Renders understated radial lines and concentric arcs.
 */
@Composable
fun BuddysWebDecoration(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    tint: Color = BuddysTheme.colors.webGeometryTint
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 1.dp.toPx()

        val angles = listOf(0.0, 22.5, 45.0, 67.5, 90.0)
        val radii = listOf(0.25f, 0.50f, 0.75f, 1.0f)

        // 1. Radial Spoke Lines from Top-Left Origin (0,0)
        for (deg in angles) {
            val rad = Math.toRadians(deg)
            val ex = (w * cos(rad)).toFloat()
            val ey = (h * sin(rad)).toFloat()
            drawLine(
                color = tint,
                start = Offset.Zero,
                end = Offset(ex, ey),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // 2. Connecting Curved Web Arcs
        for (rRatio in radii) {
            val rX = w * rRatio
            val rY = h * rRatio
            val arcPath = Path()

            for (i in 0 until angles.size) {
                val deg = angles[i]
                val rad = Math.toRadians(deg)
                val px = (rX * cos(rad)).toFloat()
                val py = (rY * sin(rad)).toFloat()

                if (i == 0) {
                    arcPath.moveTo(px, py)
                } else {
                    val prevDeg = angles[i - 1]
                    val prevRad = Math.toRadians(prevDeg)
                    val prevX = (rX * cos(prevRad)).toFloat()
                    val prevY = (rY * sin(prevRad)).toFloat()

                    // Subtle inward curved control point
                    val midDeg = (prevDeg + deg) / 2.0
                    val midRad = Math.toRadians(midDeg)
                    val ctrlDist = rRatio * 0.88f
                    val ctrlX = (w * ctrlDist * cos(midRad)).toFloat()
                    val ctrlY = (h * ctrlDist * sin(midRad)).toFloat()

                    arcPath.quadraticTo(ctrlX, ctrlY, px, py)
                }
            }
            drawPath(path = arcPath, color = tint, style = Stroke(width = strokeWidth))
        }
    }
}

/**
 * Top-Right Geometric Web Accent
 */
@Composable
fun BuddysWebTopRight(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    tint: Color = BuddysTheme.colors.webGeometryTint
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 1.dp.toPx()

        val angles = listOf(90.0, 112.5, 135.0, 157.5, 180.0)
        val radii = listOf(0.25f, 0.50f, 0.75f, 1.0f)
        val origin = Offset(w, 0f)

        // Radial Spokes from Top-Right Origin (w, 0)
        for (deg in angles) {
            val rad = Math.toRadians(deg)
            val ex = origin.x + (w * cos(rad)).toFloat()
            val ey = origin.y + (h * sin(rad)).toFloat()
            drawLine(
                color = tint,
                start = origin,
                end = Offset(ex, ey),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Connecting Curved Web Arcs
        for (rRatio in radii) {
            val rX = w * rRatio
            val rY = h * rRatio
            val arcPath = Path()

            for (i in 0 until angles.size) {
                val deg = angles[i]
                val rad = Math.toRadians(deg)
                val px = origin.x + (rX * cos(rad)).toFloat()
                val py = origin.y + (rY * sin(rad)).toFloat()

                if (i == 0) {
                    arcPath.moveTo(px, py)
                } else {
                    val prevDeg = angles[i - 1]
                    val midDeg = (prevDeg + deg) / 2.0
                    val midRad = Math.toRadians(midDeg)
                    val ctrlDist = rRatio * 0.88f
                    val ctrlX = origin.x + (w * ctrlDist * cos(midRad)).toFloat()
                    val ctrlY = origin.y + (h * ctrlDist * sin(midRad)).toFloat()

                    arcPath.quadraticTo(ctrlX, ctrlY, px, py)
                }
            }
            drawPath(path = arcPath, color = tint, style = Stroke(width = strokeWidth))
        }
    }
}

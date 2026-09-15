package com.aura.glasschat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.util.ChatUtils
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun VoiceMessageBubble(
    messageId: String,
    durationMs: Long?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    isOutgoing: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeek: ((Long) -> Unit)? = null,
    playbackSpeed: Float = 1.0f,
    onToggleSpeed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalDuration = (durationMs ?: 0L).coerceAtLeast(1000L)
    val progressFraction = if (isPlaying || currentPositionMs > 0) {
        (currentPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Generate stable waveform bar heights for this messageId
    val barHeights = remember(messageId) {
        val rand = Random(abs(messageId.hashCode()))
        val count = 26
        List(count) {
            0.25f + rand.nextFloat() * 0.75f
        }
    }

    // Subtle breathing animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave_pulse")
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val activeColor = if (isOutgoing) Color.White else BuddysTheme.colors.primaryRed
    val inactiveColor = if (isOutgoing) Color.White.copy(alpha = 0.35f) else BuddysTheme.colors.textMuted.copy(alpha = 0.25f)

    Row(
        modifier = modifier
            .widthIn(min = 230.dp, max = 290.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause Circle Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isOutgoing) {
                        if (isPlaying) Color.White else Color.White.copy(alpha = 0.22f)
                    } else {
                        if (isPlaying) BuddysTheme.colors.primaryRed else BuddysTheme.colors.surfaceElevated
                    }
                )
                .clickable { onPlayPauseClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = if (isOutgoing) Color.White else BuddysTheme.colors.primaryRed
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isOutgoing) {
                        if (isPlaying) BuddysTheme.colors.primaryRed else Color.White
                    } else {
                        if (isPlaying) BuddysTheme.colors.textOnPrimary else BuddysTheme.colors.textPrimary
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Waveform + Time + Speed Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Interactive Waveform Canvas with tap and drag seeking
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(totalDuration) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (fraction * totalDuration).toLong()
                            onSeek?.invoke(targetMs)
                        }
                    }
                    .pointerInput(totalDuration) {
                        detectDragGestures { change, _ ->
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (fraction * totalDuration).toLong()
                            onSeek?.invoke(targetMs)
                        }
                    }
            ) {
                val totalBars = barHeights.size
                val barSpacing = 2.2.dp.toPx()
                val totalSpacing = barSpacing * (totalBars - 1)
                val barWidth = ((size.width - totalSpacing) / totalBars).coerceAtLeast(2.dp.toPx())
                val maxBarHeight = size.height * 0.92f

                for (i in 0 until totalBars) {
                    val x = i * (barWidth + barSpacing)
                    val rawHeight = barHeights[i] * maxBarHeight
                    val height = if (isPlaying && (i.toFloat() / totalBars.toFloat()) <= progressFraction) {
                        (rawHeight * wavePulse).coerceAtMost(size.height)
                    } else {
                        rawHeight
                    }
                    val y = (size.height - height) / 2f
                    val barFraction = i.toFloat() / totalBars.toFloat()
                    val isBarActive = barFraction <= progressFraction

                    drawRoundRect(
                        color = if (isBarActive) activeColor else inactiveColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time & Speed Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayTime = if (isPlaying || currentPositionMs > 0) {
                    ChatUtils.formatDuration(currentPositionMs)
                } else {
                    ChatUtils.formatDuration(totalDuration)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isOutgoing) Color.White.copy(alpha = 0.95f) else if (isPlaying) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textSecondary,
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )

                    if (isPlaying) {
                        Text(
                            text = "/ " + ChatUtils.formatDuration(totalDuration),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isOutgoing) Color.White.copy(alpha = 0.7f) else BuddysTheme.colors.textMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Playback speed toggle pill (1x / 1.5x / 2x)
                if (onToggleSpeed != null) {
                    val speedText = when (playbackSpeed) {
                        1.5f -> "1.5x"
                        2.0f -> "2x"
                        else -> "1x"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isOutgoing) Color.White.copy(alpha = 0.2f) else BuddysTheme.colors.surfaceElevated
                            )
                            .clickable { onToggleSpeed() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = speedText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isOutgoing) Color.White else BuddysTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}


package com.aura.glasschat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.ui.theme.BuddysTheme

/**
 * Android-Native Dynamic Island Quick-Response Overlay.
 * Floats elegantly at the top of the screen with smooth spring physics.
 */
@Composable
fun DynamicIslandOverlay(
    event: IslandEvent?,
    onOpenChat: (chatId: String, otherUserId: String) -> Unit = { _, _ -> },
    onOpenAi: () -> Unit = {},
    onAnswerCall: (callerId: String, callerName: String, isVideo: Boolean) -> Unit = { _, _, _ -> },
    onDeclineCall: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = event != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + fadeOut(animationSpec = tween(150))
    ) {
        if (event == null) return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -15f) {
                            onDismiss()
                        }
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        when (event) {
                            is IslandEvent.NewMessage -> {
                                onOpenChat(event.chatId, event.senderId)
                                onDismiss()
                            }
                            is IslandEvent.VoiceMessage -> {
                                onOpenChat(event.chatId, event.senderId)
                                onDismiss()
                            }
                            is IslandEvent.AiThinking, is IslandEvent.AiResponseReady -> {
                                onOpenAi()
                                onDismiss()
                            }
                            is IslandEvent.IncomingCall -> {
                                onAnswerCall(event.callerId, event.callerName, event.isVideo)
                                onDismiss()
                            }
                            is IslandEvent.UploadProgress -> {
                                onDismiss()
                            }
                        }
                    },
                color = Color(0xFF121316),
                shape = RoundedCornerShape(26.dp)
            ) {
                when (event) {
                    is IslandEvent.NewMessage -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CartoonAvatar(
                                size = 42.dp,
                                avatarUrl = event.avatarUrl,
                                name = event.senderName,
                                userId = event.senderId,
                                cornerRadius = 14.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = event.senderName,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(BuddysTheme.colors.primaryAccent)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = event.messagePreview,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Quick Reply Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BuddysTheme.colors.primaryAccent,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = "Reply",
                                    color = Color(0xFF18181B),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    is IslandEvent.VoiceMessage -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CartoonAvatar(
                                size = 42.dp,
                                avatarUrl = event.avatarUrl,
                                name = event.senderName,
                                userId = event.senderId,
                                cornerRadius = 14.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.senderName,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🎙️ Voice note",
                                        color = BuddysTheme.colors.primaryAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = " • ${event.durationText}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BuddysTheme.colors.primaryAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF18181B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    is IslandEvent.IncomingCall -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CartoonAvatar(
                                size = 44.dp,
                                avatarUrl = event.avatarUrl,
                                name = event.callerName,
                                userId = event.callerId,
                                cornerRadius = 14.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.callerName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )

                                Text(
                                    text = if (event.isVideo) "Incoming Video Call 📹" else "Incoming Voice Call 📞",
                                    color = Color(0xFF22C55E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Decline Action
                            IconButton(
                                onClick = {
                                    onDeclineCall()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Decline",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Accept Action
                            IconButton(
                                onClick = {
                                    onAnswerCall(event.callerId, event.callerName, event.isVideo)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Answer",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    is IslandEvent.AiThinking -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "AiThinkingTransition")
                        val dotOffset by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "AiDotOffset"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CartoonAvatar(
                                size = 40.dp,
                                isAi = true,
                                name = "Buddys AI",
                                cornerRadius = 14.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Buddys AI",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.5.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = event.status,
                                    color = BuddysTheme.colors.primaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // Thinking Pulse Indicator
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4F46E5).copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = BuddysTheme.colors.primaryAccent,
                                    trackColor = Color.Transparent
                                )
                            }
                        }
                    }

                    is IslandEvent.AiResponseReady -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CartoonAvatar(
                                size = 40.dp,
                                isAi = true,
                                name = "Buddys AI",
                                cornerRadius = 14.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Buddys AI",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "✨ Response Ready",
                                        color = Color(0xFF22C55E),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = event.previewText,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = "View",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    is IslandEvent.UploadProgress -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BuddysTheme.colors.primaryAccent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = if (event.isCompleted) Color(0xFF22C55E) else BuddysTheme.colors.primaryAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = event.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (event.isCompleted) "Done!" else "${(event.progress * 100).toInt()}%",
                                        color = BuddysTheme.colors.primaryAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { event.progress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = BuddysTheme.colors.primaryAccent,
                                    trackColor = Color(0xFF243044)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.aura.glasschat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.data.model.CallSession
import com.aura.glasschat.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun FloatingCallOverlay(
    callerName: String,
    avatarUrl: String?,
    isVideo: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onExpand: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(20f) }
    var offsetY by remember { mutableFloatStateOf(100f) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .width(140.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(20.dp))
            .background(BuddysTheme.colors.surface)
            .border(1.5.dp, BuddysTheme.colors.primaryAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { onExpand() }
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp)
            ) {
                AvatarView(
                    imageUrl = avatarUrl,
                    displayName = callerName,
                    size = 48.dp
                )
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(BuddysTheme.colors.primaryAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = callerName,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = BuddysTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (isVideo) "Video Call" else "Audio Call",
                fontSize = 10.sp,
                color = BuddysTheme.colors.primaryAccent,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) BuddysTheme.colors.error.copy(alpha = 0.2f) else BuddysTheme.colors.surfaceSecondary)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) BuddysTheme.colors.error else BuddysTheme.colors.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.error)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IncomingCallOverlay(
    incomingCall: CallSession?,
    onAccept: (CallSession) -> Unit,
    onDecline: (CallSession) -> Unit
) {
    AnimatedVisibility(
        visible = incomingCall != null,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it }
    ) {
        if (incomingCall != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(PaperWhite)
                        .border(1.5.dp, AccentPrimary, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarView(
                            imageUrl = incomingCall.callerAvatarUrl,
                            displayName = incomingCall.callerName,
                            size = 46.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = incomingCall.callerName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (incomingCall.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (incomingCall.isVideo) "Incoming Video Call..." else "Incoming Voice Call...",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Decline (Red)
                        IconButton(
                            onClick = { onDecline(incomingCall) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentRose)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Accept (Green)
                        IconButton(
                            onClick = { onAccept(incomingCall) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentMint)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

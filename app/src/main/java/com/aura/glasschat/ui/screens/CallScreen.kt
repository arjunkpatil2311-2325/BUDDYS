package com.aura.glasschat.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.data.model.CallStatus
import com.aura.glasschat.data.model.CallType
import com.aura.glasschat.ui.components.AvatarView
import com.aura.glasschat.ui.components.BuddysSpiderEmblem
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.CallViewModel
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(
    onEndCall: () -> Unit,
    viewModel: CallViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission Check
    val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    val hasPermissions = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(uiState.isEnded) {
        if (uiState.isEnded) {
            onEndCall()
        }
    }

    // Ripple Pulse Animation for Voice Calls
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val session = uiState.callSession
    val otherName = if (session?.callerUid == uiState.currentUserId) {
        session?.receiverName ?: "Buddy"
    } else {
        session?.callerName ?: "Buddy"
    }
    val otherAvatar = if (session?.callerUid == uiState.currentUserId) {
        session?.receiverAvatarUrl
    } else {
        session?.callerAvatarUrl
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF121215), Color(0xFF070708))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (uiState.callType == CallType.VIDEO && uiState.isVideoEnabled) {
            // VIDEO CALL VIEW
            // Remote Video Stream (Full-screen)
            if (uiState.remoteVideoTrack != null) {
                val eglContext = viewModel.eglBaseContext ?: org.webrtc.EglBase.create().eglBaseContext
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(eglContext, null)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setEnableHardwareScaler(true)
                            uiState.remoteVideoTrack?.addSink(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Connecting Video Placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AvatarView(imageUrl = otherAvatar, displayName = otherName, size = 110.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting video with $otherName...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            // VOICE CALL VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Wordmark with spider emblem
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BuddysSpiderEmblem(
                        modifier = Modifier.size(24.dp),
                        tint = BuddysTheme.colors.primaryRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buddies Call",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(50.dp))

                // Pulsing Avatar Ring
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(if (uiState.callStatus == CallStatus.ACCEPTED) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(BuddysTheme.colors.primaryRed.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .border(3.dp, BuddysTheme.colors.primaryRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarView(
                            imageUrl = otherAvatar,
                            displayName = otherName,
                            size = 120.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buddy Name
                Text(
                    text = otherName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 26.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Call Status / Duration
                val statusText = when (uiState.callStatus) {
                    CallStatus.CALLING -> "Calling..."
                    CallStatus.RINGING -> "Ringing..."
                    CallStatus.ACCEPTED -> uiState.durationFormatted
                    CallStatus.ENDED -> "Call Ended"
                    CallStatus.REJECTED -> "Declined"
                    CallStatus.MISSED -> "Missed"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (uiState.callStatus == CallStatus.ACCEPTED) BuddysTheme.colors.success else Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
        }

        // Floating Call Status Header (Top Overlay in Video)
        if (uiState.callType == CallType.VIDEO) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = otherName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = if (uiState.callStatus == CallStatus.ACCEPTED) uiState.durationFormatted else "Calling...",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                    )
                }

                IconButton(
                    onClick = { viewModel.switchCamera() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip camera", tint = Color.White)
                }
            }
        }

        // Bottom Action Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Mute Mic
            IconButton(
                onClick = { viewModel.toggleMic() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isMicMuted) BuddysTheme.colors.primaryRed else Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (uiState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // 2. Speakerphone Toggle
            IconButton(
                onClick = { viewModel.toggleSpeakerphone() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isSpeakerOn) Color.White else Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (uiState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    contentDescription = "Speaker",
                    tint = if (uiState.isSpeakerOn) Color.Black else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // 3. Toggle Video Camera
            IconButton(
                onClick = { viewModel.toggleVideo() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isVideoEnabled) BuddysTheme.colors.primaryRed else Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (uiState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Camera",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // 4. End Call Button (Red)
            IconButton(
                onClick = { viewModel.endCall() },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.primaryRed)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

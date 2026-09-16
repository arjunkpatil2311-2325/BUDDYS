package com.aura.glasschat.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.ui.viewmodel.TapToBuddyStatus
import com.aura.glasschat.ui.viewmodel.TapToBuddyViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TapToBuddyScreen(
    onBack: () -> Unit,
    onNavigateToPairingCode: () -> Unit,
    onNavigateToChat: (chatId: String, otherUserId: String) -> Unit,
    viewModel: TapToBuddyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendMediaToPeer(context, it) }
    }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        hasPermissions = allGranted
        if (allGranted) {
            viewModel.startNearbySearch()
        }
    }

    LaunchedEffect(uiState.status) {
        when (uiState.status) {
            TapToBuddyStatus.DEVICE_FOUND -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            TapToBuddyStatus.SUCCESS -> {
                triggerSuccessHaptic(context)
            }
            else -> {}
        }
    }

    LaunchedEffect(uiState.mediaSaveSuccessMessage) {
        uiState.mediaSaveSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMediaSuccessMessage()
        }
    }

    if (uiState.showMediaPreviewDialog && uiState.incomingMediaBytes != null) {
        MediaPreviewDialog(
            imageBytes = uiState.incomingMediaBytes!!,
            senderName = uiState.incomingMediaMetadata?.displayName ?: "Nearby Buddy",
            mediaSize = uiState.incomingMediaMetadata?.mediaSize ?: 0L,
            onSave = { viewModel.saveReceivedMedia(context) },
            onDismiss = { viewModel.dismissReceivedMedia() }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    ThreeDTopBar(
                        title = "Tap to Buddy",
                        onBack = {
                            viewModel.cancelPairing()
                            onBack()
                        }
                    )

                    AnimatedVisibility(
                        visible = uiState.isTransferringMedia,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        TransferProgressBanner(
                            progress = uiState.mediaTransferProgress,
                            bytesTransferred = uiState.mediaBytesTransferred,
                            totalBytes = uiState.mediaTotalBytes
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = uiState.status,
                            transitionSpec = {
                                (slideInVertically { -it / 2 } + fadeIn(animationSpec = tween(300)))
                                    .togetherWith(slideOutVertically { it / 2 } + fadeOut(animationSpec = tween(200)))
                            },
                            label = "tapToBuddyState"
                        ) { targetStatus ->
                            when (targetStatus) {
                                TapToBuddyStatus.READY -> {
                                    ReadyStateView(
                                        onStartClick = {
                                            if (hasPermissions) {
                                                viewModel.startNearbySearch()
                                            } else {
                                                permissionLauncher.launch(requiredPermissions)
                                            }
                                        },
                                        onUseCodeClick = onNavigateToPairingCode
                                    )
                                }

                                TapToBuddyStatus.SEARCHING -> {
                                    SearchingStateView(
                                        onCancelClick = { viewModel.cancelPairing() }
                                    )
                                }

                                TapToBuddyStatus.DEVICE_FOUND -> {
                                    DeviceFoundStateView(
                                        peerUser = uiState.peerUser,
                                        onConnectClick = { viewModel.confirmPairing() },
                                        onSharePhotoClick = { mediaPickerLauncher.launch("image/*") },
                                        onCancelClick = { viewModel.cancelPairing() }
                                    )
                                }

                                TapToBuddyStatus.WAITING_CONFIRMATION -> {
                                    WaitingStateView(
                                        peerUser = uiState.peerUser,
                                        onCancelClick = { viewModel.cancelPairing() }
                                    )
                                }

                                TapToBuddyStatus.SUCCESS -> {
                                    SuccessStateView(
                                        currentUser = uiState.currentUser,
                                        peerUser = uiState.peerUser,
                                        onStartChatClick = {
                                            val chatId = uiState.createdChatId
                                            val peerUid = uiState.peerUser?.uid
                                            if (!chatId.isNullOrBlank() && !peerUid.isNullOrBlank()) {
                                                onNavigateToChat(chatId, peerUid)
                                            }
                                        },
                                        onSharePhotoClick = { mediaPickerLauncher.launch("image/*") }
                                    )
                                }

                                TapToBuddyStatus.ERROR -> {
                                    ErrorStateView(
                                        errorMessage = uiState.errorMessage ?: "Could not connect to nearby phone.",
                                        onRetryClick = {
                                            if (hasPermissions) {
                                                viewModel.startNearbySearch()
                                            } else {
                                                permissionLauncher.launch(requiredPermissions)
                                            }
                                        },
                                        onUseCodeClick = onNavigateToPairingCode
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyStateView(
    onStartClick: () -> Unit,
    onUseCodeClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.primaryRed.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.surfaceSecondary)
                    .border(1.5.dp, BuddysTheme.colors.primaryRed.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                BuddysSpiderEmblem(size = 46.dp, tint = BuddysTheme.colors.primaryRed)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Bring Phones Together",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 22.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Hold your phone close to your friend's phone to connect and share media instantly via Buddies Nearby.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        ThreeDButton(
            text = "Start Nearby Discovery",
            onClick = onStartClick,
            leadingIcon = Icons.Default.Sensors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        ThreeDOutlinedButton(
            text = "Use Pairing Code instead",
            onClick = onUseCodeClick,
            leadingIcon = Icons.Default.Key,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SearchingStateView(
    onCancelClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            val radarColor = BuddysTheme.colors.primaryRed

            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f

                drawCircle(
                    color = radarColor.copy(alpha = (1f - pulse1).coerceIn(0f, 0.5f)),
                    radius = maxRadius * pulse1,
                    center = centerOffset,
                    style = Stroke(width = 2.dp.toPx())
                )

                drawCircle(
                    color = radarColor.copy(alpha = (1f - pulse2).coerceIn(0f, 0.5f)),
                    radius = maxRadius * pulse2,
                    center = centerOffset,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.surface)
                    .border(2.dp, BuddysTheme.colors.primaryRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                BuddysSpiderEmblem(size = 38.dp, tint = BuddysTheme.colors.primaryRed)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Looking for Buddies...",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bring both phones together and keep this screen open.",
            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        ThreeDOutlinedButton(
            text = "Cancel",
            onClick = onCancelClick,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}

@Composable
private fun DeviceFoundStateView(
    peerUser: com.aura.glasschat.data.model.User?,
    onConnectClick: () -> Unit,
    onSharePhotoClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        ThreeDCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            elevation = 5.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BuddysTheme.colors.primaryRed.copy(alpha = 0.12f),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "BUDDY DETECTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BuddysTheme.colors.primaryRed,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(2.5.dp, BuddysTheme.colors.primaryRed, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarView(
                        imageUrl = peerUser?.avatarUrl,
                        displayName = peerUser?.displayName ?: "Buddy",
                        size = 84.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = peerUser?.displayName?.ifBlank { peerUser.username } ?: "Buddy",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 20.sp
                    )
                )

                if (!peerUser?.username.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "@${peerUser?.username}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BuddysTheme.colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Nearby • Active now",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.primaryRed,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThreeDOutlinedButton(
                        text = "Cancel",
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f)
                    )

                    ThreeDButton(
                        text = "Connect",
                        onClick = onConnectClick,
                        leadingIcon = Icons.Default.Check,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onSharePhotoClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BuddysTheme.colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = BuddysTheme.colors.primaryRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Photo Directly",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingStateView(
    peerUser: com.aura.glasschat.data.model.User?,
    onCancelClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            color = BuddysTheme.colors.primaryRed,
            strokeWidth = 3.dp,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Waiting for confirmation...",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 19.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Waiting for ${peerUser?.displayName ?: "your buddy"} to tap Connect.",
            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        ThreeDOutlinedButton(
            text = "Cancel",
            onClick = onCancelClick,
            modifier = Modifier.fillMaxWidth(0.5f)
        )
    }
}

@Composable
private fun SuccessStateView(
    currentUser: com.aura.glasschat.data.model.User?,
    peerUser: com.aura.glasschat.data.model.User?,
    onStartChatClick: () -> Unit,
    onSharePhotoClick: () -> Unit
) {
    val scaleAnim by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "successScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.surfaceSecondary)
                    .border(2.dp, BuddysTheme.colors.primaryRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AvatarView(
                    imageUrl = currentUser?.avatarUrl,
                    displayName = currentUser?.displayName ?: "Me",
                    size = 76.dp
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = (-20).dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.surfaceSecondary)
                    .border(2.dp, BuddysTheme.colors.primaryRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AvatarView(
                    imageUrl = peerUser?.avatarUrl,
                    displayName = peerUser?.displayName ?: "Buddy",
                    size = 76.dp
                )
            }
        }

        Text(
            text = "You're Buddies!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 26.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You and ${peerUser?.displayName?.ifBlank { peerUser.username } ?: "your friend"} are now connected.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.5.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        ThreeDButton(
            text = "Start Chat",
            onClick = onStartChatClick,
            leadingIcon = Icons.AutoMirrored.Filled.Chat,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        ThreeDOutlinedButton(
            text = "Send Photo Directly",
            onClick = onSharePhotoClick,
            leadingIcon = Icons.Default.Send,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorStateView(
    errorMessage: String,
    onRetryClick: () -> Unit,
    onUseCodeClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.softRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = BuddysTheme.colors.error,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Couldn't Connect",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ThreeDButton(
            text = "Try Again",
            onClick = onRetryClick,
            leadingIcon = Icons.Default.Refresh,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        ThreeDOutlinedButton(
            text = "Use Pairing Code",
            onClick = onUseCodeClick,
            leadingIcon = Icons.Default.Key,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TransferProgressBanner(
    progress: Float,
    bytesTransferred: Long,
    totalBytes: Long
) {
    Surface(
        color = BuddysTheme.colors.surfaceSecondary,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.primaryRed.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(20.dp),
                        color = BuddysTheme.colors.primaryRed,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Transferring Media...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textPrimary
                        )
                    )
                }

                val percent = (progress * 100).toInt()
                Text(
                    text = "${percent}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.primaryRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BuddysTheme.colors.primaryRed,
                trackColor = BuddysTheme.colors.surface
            )

            if (totalBytes > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val formattedTransferred = formatBytes(bytesTransferred)
                val formattedTotal = formatBytes(totalBytes)
                Text(
                    text = "${formattedTransferred} / ${formattedTotal}",
                    style = MaterialTheme.typography.labelSmall.copy(color = BuddysTheme.colors.textSecondary),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun MediaPreviewDialog(
    imageBytes: ByteArray,
    senderName: String,
    mediaSize: Long,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val bitmap = remember(imageBytes) {
        try {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (_: Exception) {
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        ThreeDCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Incoming Photo",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.textPrimary
                            )
                        )
                        Text(
                            text = "From ${senderName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BuddysTheme.colors.textSecondary
                            )
                        )
                    }

                    if (mediaSize > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BuddysTheme.colors.surfaceSecondary
                        ) {
                            Text(
                                text = formatBytes(mediaSize),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BuddysTheme.colors.textSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BuddysTheme.colors.surfaceSecondary)
                            .border(1.dp, BuddysTheme.colors.primaryRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Received photo preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BuddysTheme.colors.surfaceSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Preview unavailable",
                            style = MaterialTheme.typography.bodyMedium.copy(color = BuddysTheme.colors.textSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThreeDOutlinedButton(
                        text = "Dismiss",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    ThreeDButton(
                        text = "Save to Photos",
                        onClick = onSave,
                        leadingIcon = Icons.Default.SaveAlt,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val df = DecimalFormat("#,##0.#")
    return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}

private fun triggerSuccessHaptic(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(120L)
    }
}

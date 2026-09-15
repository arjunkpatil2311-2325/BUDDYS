package com.aura.glasschat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aura.glasschat.ui.viewmodel.UpdateUiState
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// BUDDYS Brand Color Palette
private val BuddysBlack = Color(0xFF0A0A0B)
private val BuddysSurface = Color(0xFF131316)
private val BuddysSurfaceElevated = Color(0xFF1B1B1F)
private val BuddysSurfaceCard = Color(0xFF161619)
private val BuddysBorder = Color(0xFF29292D)
private val BuddysBorderActive = Color(0xFF3E3E44)
private val BuddysRed = Color(0xFFFF3B40)
private val BuddysRedDeep = Color(0xFFB9151D)
private val BuddysNavy = Color(0xFF253B61)
private val BuddysNavyDeep = Color(0xFF172A46)
private val BuddysTextPrimary = Color(0xFFF5F5F5)
private val BuddysTextSecondary = Color(0xFFA1A1A6)
private val BuddysTextMuted = Color(0xFF707075)

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val isVisible = state !is UpdateUiState.Idle && state !is UpdateUiState.Checking

    if (!isVisible) return

    val isMandatory = when (state) {
        is UpdateUiState.UpdateAvailable -> state.isMandatory
        is UpdateUiState.Downloading -> state.isMandatory
        is UpdateUiState.ReadyToInstall -> state.isMandatory
        is UpdateUiState.Error -> state.isMandatory
        else -> false
    }

    // Entrance Animation State
    var dialogEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        dialogEntered = true
    }

    Dialog(
        onDismissRequest = {
            if (!isMandatory) {
                onDismissClick()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = dialogEntered,
                enter = scaleIn(
                    initialScale = 0.90f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(300)
                ),
                exit = scaleOut(targetScale = 0.95f) + fadeOut(animationSpec = tween(200))
            ) {
                // Main 3D Modal Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .wrapContentHeight()
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color.Black,
                            spotColor = BuddysRed.copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    BuddysBorderActive,
                                    BuddysBorder,
                                    BuddysNavy.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        ),
                    color = BuddysSurface,
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // 1. Animated Spider-Web / Network Loading Visual
                        NetworkUpdateGraphic(isDownloading = state is UpdateUiState.Downloading)

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Animated Scanning Line (Idle) OR Real Progress Bar (Downloading)
                        when (state) {
                            is UpdateUiState.Downloading -> {
                                DownloadProgressBar(
                                    progress = state.progress,
                                    downloadedBytes = state.downloadedBytes,
                                    totalBytes = state.totalBytes
                                )
                            }
                            else -> {
                                ShimmerScanningLine()
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. Header Section (Sequenced Reveal)
                        Text(
                            text = "NEW UPDATE AVAILABLE",
                            color = BuddysRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val targetVersion = when (state) {
                            is UpdateUiState.UpdateAvailable -> state.manifest.latestVersion
                            is UpdateUiState.Downloading -> state.manifest.latestVersion
                            is UpdateUiState.ReadyToInstall -> state.manifest.latestVersion
                            is UpdateUiState.Error -> state.manifest?.latestVersion ?: "0.2.0"
                            else -> "0.2.0"
                        }

                        val fileSizeText = when (state) {
                            is UpdateUiState.UpdateAvailable -> state.manifest.fileSize
                            is UpdateUiState.Downloading -> state.manifest.fileSize
                            is UpdateUiState.ReadyToInstall -> state.manifest.fileSize
                            is UpdateUiState.Error -> state.manifest?.fileSize ?: ""
                            else -> ""
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Buddies $targetVersion",
                                color = BuddysTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )

                            if (fileSizeText.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BuddysNavyDeep,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysNavy.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = fileSizeText,
                                        color = Color(0xFF93C5FD),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Dynamic Middle Content (Release Notes / Status)
                        when (state) {
                            is UpdateUiState.UpdateAvailable -> {
                                if (state.manifest.releaseNotes.isNotEmpty()) {
                                    ReleaseNotesCard(notes = state.manifest.releaseNotes)
                                }
                            }

                            is UpdateUiState.Downloading -> {
                                val percent = (state.progress * 100).toInt()
                                Text(
                                    text = "Streaming update package ($percent%)...",
                                    color = BuddysTextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            is UpdateUiState.ReadyToInstall -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0D2818),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "READY TO INSTALL",
                                                color = Color(0xFF34D399),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "APK verified. Tap below to launch installer.",
                                                color = BuddysTextPrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            is UpdateUiState.Error -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF281113),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysRed.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = BuddysRed,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = state.message,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            else -> {}
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // 5. Action Buttons (UPDATE NOW & LATER)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (state) {
                                is UpdateUiState.UpdateAvailable -> {
                                    PremiumGlowButton(
                                        text = "UPDATE NOW",
                                        icon = Icons.Default.Download,
                                        onClick = onUpdateClick
                                    )

                                    if (!isMandatory) {
                                        OutlinedButton(
                                            onClick = onDismissClick,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BuddysBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = BuddysTextSecondary
                                            )
                                        ) {
                                            Text(
                                                text = "LATER",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }

                                is UpdateUiState.Downloading -> {
                                    OutlinedButton(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BuddysBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            disabledContentColor = BuddysTextMuted
                                        )
                                    ) {
                                        Text(
                                            text = "DOWNLOADING PACKAGE...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                is UpdateUiState.ReadyToInstall -> {
                                    Button(
                                        onClick = onInstallClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .shadow(
                                                elevation = 12.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                spotColor = Color(0xFF10B981).copy(alpha = 0.5f)
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "INSTALL NOW",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                is UpdateUiState.Error -> {
                                    PremiumGlowButton(
                                        text = "RETRY DOWNLOAD",
                                        icon = Icons.Default.Download,
                                        onClick = onUpdateClick
                                    )

                                    if (!isMandatory) {
                                        OutlinedButton(
                                            onClick = onDismissClick,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BuddysBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = BuddysTextSecondary
                                            )
                                        ) {
                                            Text(
                                                text = "DISMISS",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }

                    }
                }
            }
        }
    }
}

/**
 * Animated Spider-Web / Geometric Network Visual around the Update Emblem
 */
@Composable
private fun NetworkUpdateGraphic(isDownloading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "NetworkAnim")
    
    // Web line pulse & rotation
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val nodeScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NodeScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SlowRotate"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Geometric Network Web Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width / 2 - 4.dp.toPx()
            val midRadius = outerRadius * 0.65f
            val nodeCount = 6

            // Draw concentric web rings
            drawCircle(
                color = BuddysNavy.copy(alpha = pulseAlpha * 0.5f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )

            drawCircle(
                color = BuddysBorder.copy(alpha = 0.6f),
                radius = midRadius,
                center = center,
                style = Stroke(width = 0.8.dp.toPx())
            )

            // Draw radial network spokes & pulsing nodes
            for (i in 0 until nodeCount) {
                val angle = Math.toRadians((i * (360.0 / nodeCount) + rotationAngle).toDouble())
                val outerPoint = Offset(
                    (center.x + outerRadius * cos(angle)).toFloat(),
                    (center.y + outerRadius * sin(angle)).toFloat()
                )
                val midPoint = Offset(
                    (center.x + midRadius * cos(angle)).toFloat(),
                    (center.y + midRadius * sin(angle)).toFloat()
                )

                // Network Spoke Line
                drawLine(
                    color = if (i % 2 == 0) BuddysRed.copy(alpha = pulseAlpha * 0.4f) else BuddysNavy.copy(alpha = 0.5f),
                    start = center,
                    end = outerPoint,
                    strokeWidth = 0.8.dp.toPx()
                )

                // Interconnecting chord lines (Web Geometry)
                val nextAngle = Math.toRadians(((i + 1) * (360.0 / nodeCount) + rotationAngle).toDouble())
                val nextOuterPoint = Offset(
                    (center.x + outerRadius * cos(nextAngle)).toFloat(),
                    (center.y + outerRadius * sin(nextAngle)).toFloat()
                )
                drawLine(
                    color = BuddysBorder.copy(alpha = pulseAlpha * 0.7f),
                    start = outerPoint,
                    end = nextOuterPoint,
                    strokeWidth = 0.6.dp.toPx()
                )

                // Outer Pulsing Node
                drawCircle(
                    color = if (i % 2 == 0) BuddysRed else BuddysNavy,
                    radius = (2.2.dp.toPx()) * (if (i % 2 == 0) nodeScale else 1f),
                    center = outerPoint
                )
            }
        }

        // Center Emblem Core
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BuddysSurfaceElevated, BuddysBlack)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(BuddysRed, BuddysNavyDeep)
                    ),
                    shape = CircleShape
                )
                .shadow(elevation = 8.dp, shape = CircleShape, spotColor = BuddysRed.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDownloading) Icons.Default.Download else Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = BuddysRed,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Continuous Shimmer / Radar Scanning Line
 */
@Composable
private fun ShimmerScanningLine() {
    val infiniteTransition = rememberInfiniteTransition(label = "ShimmerAnim")
    val sweepPosition by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepPos"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(BuddysBorder.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BuddysNavy.copy(alpha = 0.6f),
                            BuddysRed,
                            Color.Transparent
                        ),
                        startX = (sweepPosition - 0.4f) * 800,
                        endX = (sweepPosition + 0.4f) * 800
                    )
                )
        )
    }
}

/**
 * Real Download Progress Bar & Live Byte Counter
 */
@Composable
private fun DownloadProgressBar(progress: Float, downloadedBytes: Long, totalBytes: Long) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = BuddysRed,
            trackColor = BuddysBorder
        )

        if (totalBytes > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            val downloadedMb = downloadedBytes.toDouble() / (1024 * 1024)
            val totalMb = totalBytes.toDouble() / (1024 * 1024)
            Text(
                text = String.format(Locale.US, "%.1f MB / %.1f MB", downloadedMb, totalMb),
                color = BuddysTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Release Notes Card with Custom Red Node Bullets
 */
@Composable
private fun ReleaseNotesCard(notes: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp)
            .background(BuddysSurfaceCard, RoundedCornerShape(18.dp))
            .border(1.dp, BuddysBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "HIGHLIGHTS",
            color = BuddysTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        notes.forEach { note ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.5.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 10.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(BuddysRed)
                )
                Text(
                    text = note,
                    color = BuddysTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Premium 3D Glow Button with Tactile Press Animation
 */
@Composable
private fun PremiumGlowButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "ButtonPress"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = BuddysRed.copy(alpha = 0.55f),
                ambientColor = Color.Black
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BuddysRed,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

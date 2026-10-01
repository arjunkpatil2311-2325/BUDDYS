package com.aura.glasschat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aura.glasschat.BuildConfig
import com.aura.glasschat.data.update.UpdateManifest
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.ui.viewmodel.UpdateUiState
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Minimal Doodle / Cartoon Style In-App Update Dialog.
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val isVisible = state !is UpdateUiState.Idle && state !is UpdateUiState.Checking && state !is UpdateUiState.UpToDate

    if (!isVisible) return

    val isMandatory = when (state) {
        is UpdateUiState.UpdateAvailable -> state.isMandatory
        is UpdateUiState.Downloading -> state.isMandatory
        is UpdateUiState.ReadyToInstall -> state.isMandatory
        is UpdateUiState.Error -> state.isMandatory
        is UpdateUiState.DownloadError -> state.isMandatory
        is UpdateUiState.ValidationError -> state.isMandatory
        else -> false
    }

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
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = dialogEntered,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(220)),
                exit = scaleOut(targetScale = 0.92f) + fadeOut(animationSpec = tween(150))
            ) {
                // Outer Cartoon Box with Doodle Drop-Shadow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp)
                ) {
                    // Tactile Cartoon Drop Shadow Layer
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .offset(x = 6.dp, y = 6.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = BuddysTheme.colors.primaryAccent.copy(alpha = 0.35f)
                    ) {}

                    // Main Modal Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        color = Color(0xFF14151B),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Floating Cartoon Mascot Header
                            DoodleMascotHeader(state = state)

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sticker Tag: "✨ NEW DROP"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BuddysTheme.colors.primaryAccent.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.5.dp,
                                    color = BuddysTheme.colors.primaryAccent.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.rotate(-2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✨",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = if (isMandatory) "CRITICAL UPDATE" else "NEW UPDATE READY",
                                        color = BuddysTheme.colors.primaryAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val targetVersion = when (state) {
                                is UpdateUiState.UpdateAvailable -> state.manifest.latestVersion
                                is UpdateUiState.Downloading -> state.manifest.latestVersion
                                is UpdateUiState.ReadyToInstall -> state.manifest.latestVersion
                                is UpdateUiState.Error -> state.manifest?.latestVersion ?: "0.2.0"
                                is UpdateUiState.DownloadError -> state.manifest?.latestVersion ?: "0.2.0"
                                is UpdateUiState.ValidationError -> state.manifest?.latestVersion ?: "0.2.0"
                                else -> "0.2.0"
                            }

                            val fileSizeText = when (state) {
                                is UpdateUiState.UpdateAvailable -> state.manifest.fileSize
                                is UpdateUiState.Downloading -> state.manifest.fileSize
                                is UpdateUiState.ReadyToInstall -> state.manifest.fileSize
                                is UpdateUiState.Error -> state.manifest?.fileSize ?: ""
                                is UpdateUiState.DownloadError -> state.manifest?.fileSize ?: ""
                                is UpdateUiState.ValidationError -> state.manifest?.fileSize ?: ""
                                else -> ""
                            }

                            // Title
                            Text(
                                text = "BUDDYS $targetVersion",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Version Badge Pill
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF20222B), RoundedCornerShape(20.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    color = Color(0xFF9E9EA7),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "➔",
                                    color = BuddysTheme.colors.primaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "v$targetVersion",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (fileSizeText.isNotBlank()) {
                                    Text(
                                        text = "•",
                                        color = Color(0xFF6B6B76),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = fileSizeText,
                                        color = BuddysTheme.colors.primaryAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Middle Dynamic Content Section
                            when (state) {
                                is UpdateUiState.UpdateAvailable -> {
                                    if (state.manifest.releaseNotes.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 140.dp)
                                                .background(Color(0xFF1B1C24), RoundedCornerShape(20.dp))
                                                .border(1.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                                                .padding(14.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Text(
                                                    text = "🎨",
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                )
                                                Text(
                                                    text = "WHAT'S COOKING",
                                                    color = Color(0xFFA1A1B0),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.8.sp
                                                )
                                            }

                                            val bulletIcons = listOf("✦", "⚡", "🚀", "💬", "✨")
                                            state.manifest.releaseNotes.forEachIndexed { index, note ->
                                                val bullet = bulletIcons[index % bulletIcons.size]
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = bullet,
                                                        color = BuddysTheme.colors.primaryAccent,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                    Text(
                                                        text = note,
                                                        color = Color(0xFFECECF0),
                                                        fontSize = 12.5.sp,
                                                        lineHeight = 16.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                is UpdateUiState.Downloading -> {
                                    val percent = (state.progress * 100).toInt()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1B1C24), RoundedCornerShape(20.dp))
                                            .border(1.5.dp, BuddysTheme.colors.primaryAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Animated Doodle Progress Bar
                                        DoodleProgressBar(progress = state.progress)

                                        Spacer(modifier = Modifier.height(10.dp))

                                        val downloadedMb = state.downloadedBytes.toDouble() / (1024 * 1024)
                                        val totalMb = state.totalBytes.toDouble() / (1024 * 1024)
                                        val byteText = if (state.totalBytes > 0) {
                                            String.format(Locale.US, "%.1f / %.1f MB  (%d%%)", downloadedMb, totalMb, percent)
                                        } else {
                                            String.format(Locale.US, "%.1f MB downloaded...", downloadedMb)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚡ Fetching update...",
                                                color = Color(0xFFA1A1B0),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = byteText,
                                                color = BuddysTheme.colors.primaryAccent,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                is UpdateUiState.ReadyToInstall -> {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF0F291E),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF22C55E).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🎉",
                                                fontSize = 20.sp,
                                                modifier = Modifier.padding(end = 10.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Download Complete!",
                                                    color = Color(0xFF4ADE80),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "Verified and ready to launch.",
                                                    color = Color(0xFFD1FAE5),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                is UpdateUiState.Error, is UpdateUiState.DownloadError, is UpdateUiState.ValidationError -> {
                                    val errorMsg = when (state) {
                                        is UpdateUiState.Error -> state.message
                                        is UpdateUiState.DownloadError -> state.message
                                        is UpdateUiState.ValidationError -> state.message
                                        else -> "Something went wrong"
                                    }
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF2B1214),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🙈",
                                                fontSize = 20.sp,
                                                modifier = Modifier.padding(end = 10.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Oops! Update hiccup",
                                                    color = Color(0xFFF87171),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = errorMsg,
                                                    color = Color(0xFFFEE2E2),
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 15.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> {}
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Tactile Doodle Action Buttons
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                when (state) {
                                    is UpdateUiState.UpdateAvailable -> {
                                        DoodleCartoonButton(
                                            text = "UPDATE NOW 🚀",
                                            containerColor = BuddysTheme.colors.primaryAccent,
                                            contentColor = Color.White,
                                            shadowColor = Color(0xFF8B1217),
                                            onClick = onUpdateClick
                                        )

                                        if (!isMandatory) {
                                            TextButton(
                                                onClick = onDismissClick,
                                                modifier = Modifier.fillMaxWidth().height(40.dp)
                                            ) {
                                                Text(
                                                    text = "Maybe Later",
                                                    color = Color(0xFF8E8E9A),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }

                                    is UpdateUiState.Downloading -> {
                                        DoodleCartoonButton(
                                            text = "DOWNLOADING... ⏳",
                                            containerColor = Color(0xFF262833),
                                            contentColor = Color(0xFFA1A1B0),
                                            shadowColor = Color(0xFF16171E),
                                            enabled = false,
                                            onClick = {}
                                        )
                                    }

                                    is UpdateUiState.ReadyToInstall -> {
                                        DoodleCartoonButton(
                                            text = "INSTALL NOW ⚡",
                                            containerColor = Color(0xFF16A34A),
                                            contentColor = Color.White,
                                            shadowColor = Color(0xFF0F5B29),
                                            onClick = onInstallClick
                                        )
                                    }

                                    is UpdateUiState.Error, is UpdateUiState.DownloadError, is UpdateUiState.ValidationError -> {
                                        DoodleCartoonButton(
                                            text = "TRY AGAIN 🔄",
                                            containerColor = BuddysTheme.colors.primaryAccent,
                                            contentColor = Color.White,
                                            shadowColor = Color(0xFF8B1217),
                                            onClick = onUpdateClick
                                        )

                                        if (!isMandatory) {
                                            TextButton(
                                                onClick = onDismissClick,
                                                modifier = Modifier.fillMaxWidth().height(40.dp)
                                            ) {
                                                Text(
                                                    text = "Dismiss",
                                                    color = Color(0xFF8E8E9A),
                                                    fontWeight = FontWeight.Bold,
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
}

/**
 * Animated Doodle Mascot Header with Rocket, Sparkles, and Floating Doodle Stars.
 */
@Composable
private fun DoodleMascotHeader(state: UpdateUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "DoodleFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MascotY"
    )

    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarScale"
    )

    val primaryColor = BuddysTheme.colors.primaryAccent

    Box(
        modifier = Modifier
            .size(80.dp)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Sparkle 1 (Top Right)
            drawDoodleStar(
                center = Offset(size.width * 0.85f, size.height * 0.18f),
                radius = 8f * starScale,
                color = Color(0xFFFFD54F)
            )

            // Sparkle 2 (Top Left)
            drawDoodleStar(
                center = Offset(size.width * 0.15f, size.height * 0.25f),
                radius = 6f * (2f - starScale),
                color = Color(0xFFFF80AB)
            )

            // Sparkle 3 (Bottom Right)
            drawDoodleStar(
                center = Offset(size.width * 0.88f, size.height * 0.75f),
                radius = 5f * starScale,
                color = Color(0xFF80D8FF)
            )

            // Cute Circular Mascot Backdrop
            drawCircle(
                color = Color(0xFF22242F),
                radius = size.width * 0.38f,
                center = center
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.8f),
                radius = size.width * 0.38f,
                center = center,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Inner Mascot Icon
        Icon(
            imageVector = when (state) {
                is UpdateUiState.ReadyToInstall -> Icons.Default.CheckCircle
                is UpdateUiState.Downloading -> Icons.Default.Download
                is UpdateUiState.Error, is UpdateUiState.DownloadError, is UpdateUiState.ValidationError -> Icons.Default.Info
                else -> Icons.Default.RocketLaunch
            },
            contentDescription = null,
            tint = when (state) {
                is UpdateUiState.ReadyToInstall -> Color(0xFF4ADE80)
                is UpdateUiState.Error, is UpdateUiState.DownloadError, is UpdateUiState.ValidationError -> Color(0xFFF87171)
                else -> Color.White
            },
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * Draws a hand-drawn 4-point doodle sparkle star.
 */
private fun DrawScope.drawDoodleStar(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path = path, color = color)
}

/**
 * Tactile Cartoon Push Button with 3D Drop Shadow effect.
 */
@Composable
private fun DoodleCartoonButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    shadowColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetY by animateDpAsState(
        targetValue = if (isPressed && enabled) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "BtnOffset"
    )

    val shadowHeight = 4.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        // Bottom Shadow Block (Cartoon 3D Depth)
        if (enabled) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .offset(y = shadowHeight),
                shape = RoundedCornerShape(16.dp),
                color = shadowColor
            ) {}
        }

        // Top Button Face
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .offset(y = offsetY)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = Color.White.copy(alpha = if (enabled) 0.25f else 0.08f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = contentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Doodle Progress Bar with Cute Running Dot/Spark.
 */
@Composable
private fun DoodleProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "DoodleProgress"
    )

    val primaryColor = BuddysTheme.colors.primaryAccent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = 8.dp.toPx()
            val yOffset = (size.height - trackHeight) / 2f

            // Background Track
            drawRoundRect(
                color = Color(0xFF2B2D3A),
                topLeft = Offset(0f, yOffset),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )

            // Progress Fill
            if (animatedProgress > 0.01f) {
                val fillWidth = size.width * animatedProgress
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(primaryColor, Color(0xFFFF6064))
                    ),
                    topLeft = Offset(0f, yOffset),
                    size = Size(fillWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )

                // Head Sparkle Dot
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(fillWidth, size.height / 2f)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 3.dp.toPx(),
                    center = Offset(fillWidth, size.height / 2f)
                )
            }
        }
    }
}

/**
 * Cartoon Doodle In-App Update Banner for Profile and HomeScreen sections.
 */
@Composable
fun UpdateAvailableBanner(
    manifest: UpdateManifest,
    onUpdateClick: (UpdateManifest) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // Tactile Cartoon Shadow Layer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp),
            shape = RoundedCornerShape(22.dp),
            color = BuddysTheme.colors.primaryAccent.copy(alpha = 0.25f)
        ) {}

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable { onUpdateClick(manifest) },
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF181922),
            border = androidx.compose.foundation.BorderStroke(2.dp, BuddysTheme.colors.primaryAccent.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BuddysTheme.colors.primaryAccent.copy(alpha = 0.18f))
                                .border(1.5.dp, BuddysTheme.colors.primaryAccent.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🚀", fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Update Ready",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = BuddysTheme.colors.primaryAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.primaryAccent.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "v${manifest.latestVersion}",
                                        color = BuddysTheme.colors.primaryAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (manifest.fileSize.isNotBlank()) "Tap to install • ${manifest.fileSize}" else "Tap to install latest improvements",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFA1A1B0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BuddysTheme.colors.primaryAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpdateClick(manifest) }
                    ) {
                        Text(
                            text = "UPDATE",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                if (manifest.releaseNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    manifest.releaseNotes.take(2).forEach { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "✦",
                                color = BuddysTheme.colors.primaryAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFD4D4DC),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Activity / Notifications Feed Update Card (Doodle Style).
 */
@Composable
fun UpdateNotificationCard(
    manifest: UpdateManifest,
    onUpdateClick: (UpdateManifest) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onUpdateClick(manifest) },
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF181922),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BuddysTheme.colors.primaryAccent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.primaryAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚀", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "BUDDYS Update Available",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = BuddysTheme.colors.primaryAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "v${manifest.latestVersion}",
                            color = BuddysTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val noteSummary = manifest.releaseNotes.firstOrNull() ?: "New performance enhancements and bug fixes"
                Text(
                    text = noteSummary,
                    color = Color(0xFFA1A1B0),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BuddysTheme.colors.primaryAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onUpdateClick(manifest) }
            ) {
                Text(
                    text = "Update",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

package com.aura.glasschat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aura.glasschat.BuildConfig
import com.aura.glasschat.data.update.UpdateManifest
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.ui.viewmodel.UpdateUiState
import java.util.Locale

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
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = dialogEntered,
                enter = scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(250)),
                exit = scaleOut(targetScale = 0.95f) + fadeOut(animationSpec = tween(180))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp)
                        .wrapContentHeight()
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = BuddysTheme.colors.primaryAccent.copy(alpha = 0.25f)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 1.dp,
                            color = BuddysTheme.colors.border,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    color = BuddysTheme.colors.surface,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Modern Emblem Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(BuddysTheme.colors.surfaceSecondary)
                                .border(1.dp, BuddysTheme.colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (state) {
                                    is UpdateUiState.ReadyToInstall -> Icons.Default.CheckCircle
                                    is UpdateUiState.Downloading -> Icons.Default.Download
                                    is UpdateUiState.Error -> Icons.Default.Info
                                    else -> Icons.Default.SystemUpdate
                                },
                                contentDescription = null,
                                tint = when (state) {
                                    is UpdateUiState.ReadyToInstall -> BuddysTheme.colors.success
                                    is UpdateUiState.Error -> BuddysTheme.colors.error
                                    else -> BuddysTheme.colors.primaryAccent
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val targetVersion = when (state) {
                            is UpdateUiState.UpdateAvailable -> state.manifest.latestVersion
                            is UpdateUiState.Downloading -> state.manifest.latestVersion
                            is UpdateUiState.ReadyToInstall -> state.manifest.latestVersion
                            is UpdateUiState.Error -> state.manifest?.latestVersion ?: "0.4.0"
                            else -> "0.4.0"
                        }

                        val fileSizeText = when (state) {
                            is UpdateUiState.UpdateAvailable -> state.manifest.fileSize
                            is UpdateUiState.Downloading -> state.manifest.fileSize
                            is UpdateUiState.ReadyToInstall -> state.manifest.fileSize
                            is UpdateUiState.Error -> state.manifest?.fileSize ?: ""
                            else -> ""
                        }

                        Text(
                            text = "Buddies $targetVersion is available",
                            color = BuddysTheme.colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current: Version ${BuildConfig.VERSION_NAME}",
                                color = BuddysTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = "•",
                                color = BuddysTheme.colors.textMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "New: Version $targetVersion",
                                color = BuddysTheme.colors.primaryAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (fileSizeText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Package Size: $fileSizeText",
                                color = BuddysTheme.colors.textMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Middle Content State
                        when (state) {
                            is UpdateUiState.UpdateAvailable -> {
                                if (state.manifest.releaseNotes.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 140.dp)
                                            .background(BuddysTheme.colors.surfaceSecondary, RoundedCornerShape(16.dp))
                                            .padding(14.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = "WHAT'S NEW",
                                            color = BuddysTheme.colors.textMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        state.manifest.releaseNotes.forEach { note ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "•",
                                                    color = BuddysTheme.colors.primaryAccent,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                )
                                                Text(
                                                    text = note,
                                                    color = BuddysTheme.colors.textPrimary,
                                                    fontSize = 13.sp,
                                                    lineHeight = 17.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            is UpdateUiState.Downloading -> {
                                val percent = (state.progress * 100).toInt()
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = BuddysTheme.colors.primaryAccent,
                                        trackColor = BuddysTheme.colors.border
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val downloadedMb = state.downloadedBytes.toDouble() / (1024 * 1024)
                                    val totalMb = state.totalBytes.toDouble() / (1024 * 1024)
                                    val byteText = if (state.totalBytes > 0) {
                                        String.format(Locale.US, "%.1f MB / %.1f MB (%d%%)", downloadedMb, totalMb, percent)
                                    } else {
                                        String.format(Locale.US, "%.1f MB downloaded...", downloadedMb)
                                    }
                                    Text(
                                        text = byteText,
                                        color = BuddysTheme.colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            is UpdateUiState.ReadyToInstall -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = BuddysTheme.colors.success.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.success.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = BuddysTheme.colors.success,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Download verified. Ready to install.",
                                            color = BuddysTheme.colors.textPrimary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            is UpdateUiState.Error -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = BuddysTheme.colors.error.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.error.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = BuddysTheme.colors.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = state.message,
                                            color = BuddysTheme.colors.textPrimary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            else -> {}
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Action Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (state) {
                                is UpdateUiState.UpdateAvailable -> {
                                    CalmActionButton(
                                        text = "Update Now",
                                        icon = Icons.Default.Download,
                                        containerColor = BuddysTheme.colors.primaryAccent,
                                        contentColor = Color.White,
                                        onClick = onUpdateClick
                                    )

                                    if (!isMandatory) {
                                        TextButton(
                                            onClick = onDismissClick,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Text(
                                                text = "Later",
                                                color = BuddysTheme.colors.textSecondary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.5.sp
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
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
                                    ) {
                                        Text(
                                            text = "Downloading...",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = BuddysTheme.colors.textMuted
                                        )
                                    }
                                }

                                is UpdateUiState.ReadyToInstall -> {
                                    CalmActionButton(
                                        text = "Install Now",
                                        icon = Icons.Default.SystemUpdate,
                                        containerColor = BuddysTheme.colors.success,
                                        contentColor = Color.White,
                                        onClick = onInstallClick
                                    )
                                }

                                is UpdateUiState.Error -> {
                                    CalmActionButton(
                                        text = "Retry",
                                        icon = Icons.Default.Download,
                                        containerColor = BuddysTheme.colors.primaryAccent,
                                        contentColor = Color.White,
                                        onClick = onUpdateClick
                                    )

                                    if (!isMandatory) {
                                        TextButton(
                                            onClick = onDismissClick,
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Text(
                                                text = "Dismiss",
                                                color = BuddysTheme.colors.textSecondary,
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

@Composable
private fun CalmActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "BtnScale"
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Premium in-app Update Banner for Profile and HomeScreen sections.
 */
@Composable
fun UpdateAvailableBanner(
    manifest: UpdateManifest,
    onUpdateClick: (UpdateManifest) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onUpdateClick(manifest) },
        shape = RoundedCornerShape(20.dp),
        color = BuddysTheme.colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BuddysTheme.colors.primaryAccent.copy(alpha = 0.6f))
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
                            .background(BuddysTheme.colors.primaryAccent.copy(alpha = 0.15f))
                            .border(1.dp, BuddysTheme.colors.primaryAccent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = BuddysTheme.colors.primaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Update Available",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BuddysTheme.colors.textPrimary,
                                    fontSize = 15.sp
                                )
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
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (manifest.fileSize.isNotBlank()) "Tap to install • ${manifest.fileSize}" else "Tap to install latest improvements",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BuddysTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Button(
                    onClick = { onUpdateClick(manifest) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BuddysTheme.colors.primaryAccent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Update",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (manifest.releaseNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = BuddysTheme.colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))

                manifest.releaseNotes.take(2).forEach { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = BuddysTheme.colors.primaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BuddysTheme.colors.textPrimary.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Activity / Notifications Feed Update Card.
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
            .clip(RoundedCornerShape(16.dp))
            .clickable { onUpdateClick(manifest) },
        shape = RoundedCornerShape(16.dp),
        color = BuddysTheme.colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.primaryAccent.copy(alpha = 0.4f))
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
                    .background(BuddysTheme.colors.primaryAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = BuddysTheme.colors.primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Buddies Update Available",
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = BuddysTheme.colors.primaryAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "v${manifest.latestVersion}",
                            color = BuddysTheme.colors.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val noteSummary = manifest.releaseNotes.firstOrNull() ?: "New performance enhancements and bug fixes"
                Text(
                    text = noteSummary,
                    color = BuddysTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onUpdateClick(manifest) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BuddysTheme.colors.primaryAccent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Update",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

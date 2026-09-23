package com.aura.glasschat.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.BuildConfig
import com.aura.glasschat.ui.components.BuddysButton
import com.aura.glasschat.ui.components.BuddysCard
import com.aura.glasschat.ui.components.BuddysOutlinedButton
import com.aura.glasschat.ui.components.BuddysTopBar
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.ui.viewmodel.UpdateUiState
import com.aura.glasschat.ui.viewmodel.UpdateViewModel
import java.util.Locale

@Composable
fun AppUpdatesScreen(
    onBack: () -> Unit,
    viewModel: UpdateViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState is UpdateUiState.Idle || uiState is UpdateUiState.UpToDate) {
            viewModel.checkForUpdates(force = true)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = BuddysTheme.colors.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BuddysTopBar(
                title = "App Updates",
                onBack = onBack
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "UpdateScreenState"
                ) { state ->
                    when (state) {
                        is UpdateUiState.Idle -> {
                            IdleUpdateContent(
                                lastCheckedTimestamp = state.lastCheckTimestamp,
                                onCheckClick = { viewModel.checkForUpdates(force = true) }
                            )
                        }

                        is UpdateUiState.Checking -> {
                            CheckingUpdateContent()
                        }

                        is UpdateUiState.UpToDate -> {
                            UpToDateContent(
                                currentVersion = state.version,
                                onCheckAgainClick = { viewModel.checkForUpdates(force = true) }
                            )
                        }

                        is UpdateUiState.UpdateAvailable -> {
                            val targetAsset = remember(state.manifest) { state.manifest.getAssetForDevice() }
                            UpdateAvailableContent(
                                latestVersion = state.manifest.latestVersion,
                                fileSize = targetAsset.fileSize.ifBlank { state.manifest.fileSize },
                                releaseNotes = state.manifest.releaseNotes,
                                onUpdateClick = {
                                    viewModel.startDownload(state.manifest, state.isMandatory)
                                }
                            )
                        }

                        is UpdateUiState.Downloading -> {
                            DownloadingUpdateContent(
                                version = state.manifest.latestVersion,
                                progress = state.progress,
                                downloadedBytes = state.downloadedBytes,
                                totalBytes = state.totalBytes
                            )
                        }

                        is UpdateUiState.ReadyToInstall -> {
                            ReadyToInstallContent(
                                onInstallClick = {
                                    viewModel.installApk(state.apkFile)
                                }
                            )
                        }

                        is UpdateUiState.DownloadError, is UpdateUiState.Error -> {
                            val manifest = when (state) {
                                is UpdateUiState.DownloadError -> state.manifest
                                is UpdateUiState.Error -> state.manifest
                                else -> null
                            }
                            DownloadErrorContent(
                                onRetryClick = {
                                    if (manifest != null) {
                                        viewModel.startDownload(manifest, isMandatory = false)
                                    } else {
                                        viewModel.checkForUpdates(force = true)
                                    }
                                }
                            )
                        }

                        is UpdateUiState.ValidationError -> {
                            ValidationErrorContent(
                                onTryAgainClick = {
                                    if (state.manifest != null) {
                                        viewModel.startDownload(state.manifest, state.isMandatory)
                                    } else {
                                        viewModel.checkForUpdates(force = true)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleUpdateContent(
    lastCheckedTimestamp: Long,
    onCheckClick: () -> Unit
) {
    val lastCheckedText = remember(lastCheckedTimestamp) {
        if (lastCheckedTimestamp > 0) {
            val now = System.currentTimeMillis()
            val diff = now - lastCheckedTimestamp
            if (diff < 60_000L) {
                "Just now"
            } else {
                DateUtils.getRelativeTimeSpanString(
                    lastCheckedTimestamp,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString()
            }
        } else {
            "Not checked yet"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary)
                .border(1.dp, BuddysTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = BuddysTheme.colors.primaryRed,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Buddies",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                letterSpacing = (-0.3).sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Current version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.5.sp
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        BuddysButton(
            text = "Check for updates",
            onClick = onCheckClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Last checked:\n$lastCheckedText",
            style = MaterialTheme.typography.bodySmall.copy(
                color = BuddysTheme.colors.textMuted,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CheckingUpdateContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = BuddysTheme.colors.primaryRed,
            strokeWidth = 3.dp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Checking for updates...",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
private fun UpToDateContent(
    currentVersion: String,
    onCheckAgainClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary)
                .border(1.dp, BuddysTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF22A06B),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "You're up to date",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Buddies $currentVersion",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.5.sp
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        BuddysOutlinedButton(
            text = "Check Again",
            onClick = onCheckAgainClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Composable
private fun UpdateAvailableContent(
    latestVersion: String,
    fileSize: String,
    releaseNotes: List<String>,
    onUpdateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary)
                .border(1.dp, BuddysTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = BuddysTheme.colors.primaryRed,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Update available",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Buddies $latestVersion",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = BuddysTheme.colors.primaryRed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Current version: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 12.sp
            )
        )

        if (fileSize.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Size: $fileSize",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(0.5.dp, BuddysTheme.colors.border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⚡ Size optimized",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BuddysTheme.colors.primaryRed,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        if (releaseNotes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            BuddysCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "What's new",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textPrimary,
                            fontSize = 13.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    releaseNotes.forEach { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.5.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                color = BuddysTheme.colors.primaryRed,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textSecondary,
                                    fontSize = 13.5.sp,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        BuddysButton(
            text = "Update Now",
            leadingIcon = Icons.Default.Download,
            onClick = onUpdateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Composable
private fun DownloadingUpdateContent(
    version: String,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long
) {
    val percent = (progress * 100).toInt().coerceIn(0, 100)
    val downloadedMb = (downloadedBytes.toDouble() / (1024 * 1024))
    val totalMb = (totalBytes.toDouble() / (1024 * 1024))
    val sizeText = if (totalBytes > 0) {
        String.format(Locale.US, "%.0f MB / %.0f MB", downloadedMb, totalMb)
    } else {
        String.format(Locale.US, "%.1f MB", downloadedMb)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Downloading update",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Buddies $version",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.5.sp
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = BuddysTheme.colors.primaryRed,
            trackColor = BuddysTheme.colors.surfaceSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sizeText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BuddysTheme.colors.textSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 14.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Downloading...",
            style = MaterialTheme.typography.bodySmall.copy(
                color = BuddysTheme.colors.textMuted,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun ReadyToInstallContent(
    onInstallClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary)
                .border(1.dp, BuddysTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF22A06B),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Download complete",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Update is ready to install.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.5.sp
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        BuddysButton(
            text = "Install Update",
            leadingIcon = Icons.Default.SystemUpdate,
            onClick = onInstallClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Composable
private fun DownloadErrorContent(
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary)
                .border(1.dp, BuddysTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = BuddysTheme.colors.error,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Couldn't download update",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        BuddysButton(
            text = "Retry",
            onClick = onRetryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Composable
private fun ValidationErrorContent(
    onTryAgainClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary)
                .border(1.dp, BuddysTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = BuddysTheme.colors.error,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Update couldn't be verified",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "The downloaded update was rejected for safety.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        BuddysButton(
            text = "Try Again",
            onClick = onTryAgainClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

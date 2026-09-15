package com.aura.glasschat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.BuddysTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = BuddysTheme.colors

    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    var imageCacheBytes by remember { mutableLongStateOf(0L) }
    var voiceCacheBytes by remember { mutableLongStateOf(0L) }
    var isClearing by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    fun calculateSizes() {
        coroutineScope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            var total = 0L
            var img = 0L
            var voice = 0L

            fun scan(file: File) {
                if (file.isDirectory) {
                    file.listFiles()?.forEach { scan(it) }
                } else {
                    val length = file.length()
                    total += length
                    val name = file.name.lowercase()
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || file.parent?.contains("image_cache") == true) {
                        img += length
                    } else if (name.endsWith(".m4a") || name.endsWith(".mp3") || name.endsWith(".aac") || file.parent?.contains("voice") == true) {
                        voice += length
                    }
                }
            }

            if (cacheDir != null && cacheDir.exists()) {
                scan(cacheDir)
            }

            val codeCacheDir = context.codeCacheDir
            if (codeCacheDir != null && codeCacheDir.exists()) {
                scan(codeCacheDir)
            }

            withContext(Dispatchers.Main) {
                cacheSizeBytes = total
                imageCacheBytes = img
                voiceCacheBytes = voice
            }
        }
    }

    LaunchedEffect(Unit) {
        calculateSizes()
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            BuddysTopBar(
                title = "Storage & Data",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total cache summary card
                BuddysCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = colors.primaryRed,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Cached App Data",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatSize(cacheSizeBytes),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Free up storage without losing your account or saved messages.",
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Breakdown card
                BuddysCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "STORAGE BREAKDOWN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StorageRow(
                            title = "Images & Thumbnails",
                            subtitle = "Cached photos, profile avatars, story previews",
                            sizeStr = formatSize(imageCacheBytes)
                        )
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 8.dp))

                        StorageRow(
                            title = "Voice Messages & Audio",
                            subtitle = "Downloaded audio cache",
                            sizeStr = formatSize(voiceCacheBytes)
                        )
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 8.dp))

                        StorageRow(
                            title = "Temporary Cache & Metadata",
                            subtitle = "Network responses and app cache",
                            sizeStr = formatSize((cacheSizeBytes - imageCacheBytes - voiceCacheBytes).coerceAtLeast(0L))
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                BuddysButton(
                    text = if (isClearing) "Clearing Cache..." else "Clear All Cache (${formatSize(cacheSizeBytes)})",
                    onClick = { showConfirmDialog = true },
                    leadingIcon = Icons.Default.CleaningServices,
                    enabled = !isClearing && cacheSizeBytes > 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                containerColor = colors.surface,
                shape = RoundedCornerShape(18.dp),
                title = {
                    Text(
                        "Clear App Cache?",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "This will remove temporary cached images and voice files from your device storage. Your chats, account, and online messages will remain intact.",
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    BuddysButton(
                        text = "Clear",
                        onClick = {
                            showConfirmDialog = false
                            isClearing = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    context.cacheDir?.deleteRecursively()
                                    context.codeCacheDir?.deleteRecursively()
                                } catch (_: Exception) {}
                                calculateSizes()
                                withContext(Dispatchers.Main) {
                                    isClearing = false
                                }
                            }
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun StorageRow(
    title: String,
    subtitle: String,
    sizeStr: String
) {
    val colors = BuddysTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = sizeStr,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = colors.primaryRed
        )
    }
}

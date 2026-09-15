package com.aura.glasschat.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.StoryViewModel
import java.io.File
import java.io.FileOutputStream

private fun saveBitmapToStoryTempUri(context: android.content.Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "story_camera_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun CreateStoryScreen(
    onBack: () -> Unit,
    onStoryPosted: () -> Unit,
    viewModel: StoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var captionText by rememberSaveable { mutableStateOf("") }
    var selectedAudience by rememberSaveable { mutableStateOf("EVERYONE") } // "EVERYONE" or "CLOSE_FRIENDS"

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val uri = saveBitmapToStoryTempUri(context, bitmap)
            if (uri != null) {
                selectedImageUri = uri
            }
        }
    }

    LaunchedEffect(uiState.uploadSuccess) {
        if (uiState.uploadSuccess) {
            Toast.makeText(context, "Story shared with your buddys", Toast.LENGTH_SHORT).show()
            viewModel.clearUploadStatus()
            onStoryPosted()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearUploadStatus()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top Bar
            BuddysTopBar(
                title = "New Story",
                onBack = onBack,
                actions = {
                    BuddysButton(
                        text = if (selectedAudience == "CLOSE_FRIENDS") "Share (⭐)" else "Share",
                        onClick = {
                            val uri = selectedImageUri
                            if (uri != null) {
                                viewModel.uploadStory(context, uri, captionText, selectedAudience)
                            } else {
                                Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = selectedImageUri != null && !uiState.isUploading,
                        isLoading = uiState.isUploading,
                        modifier = Modifier.height(36.dp)
                    )
                }
            )

            // Audience Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audience:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textSecondary
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selectedAudience == "EVERYONE") BuddysTheme.colors.surfaceSecondary else BuddysTheme.colors.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedAudience == "EVERYONE") BuddysTheme.colors.primaryRed else BuddysTheme.colors.border
                    ),
                    modifier = Modifier.clickable { selectedAudience = "EVERYONE" }
                ) {
                    Text(
                        text = "🌐 Everyone",
                        fontSize = 12.sp,
                        fontWeight = if (selectedAudience == "EVERYONE") FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedAudience == "EVERYONE") BuddysTheme.colors.primaryRed else BuddysTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                val isCloseSelected = selectedAudience == "CLOSE_FRIENDS"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isCloseSelected) {
                        if (BuddysTheme.colors.isDark) Color(0xFF1B3828) else Color(0xFFE8F5E9)
                    } else BuddysTheme.colors.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCloseSelected) Color(0xFF2E7D32) else BuddysTheme.colors.border
                    ),
                    modifier = Modifier.clickable { selectedAudience = "CLOSE_FRIENDS" }
                ) {
                    Text(
                        text = "⭐ Close Friends",
                        fontSize = 12.sp,
                        fontWeight = if (isCloseSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCloseSelected) {
                            if (BuddysTheme.colors.isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32)
                        } else BuddysTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            // Main Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BuddysTheme.colors.surface)
                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Story Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Caption Banner
                    if (captionText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = captionText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Empty Story Prompt
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        BuddysSpiderEmblem(
                            modifier = Modifier.size(56.dp),
                            tint = BuddysTheme.colors.primaryRed
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Share a moment with your buddys",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.textPrimary
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Stories disappear automatically after 24 hours",
                            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            BuddysButton(
                                text = "Gallery",
                                onClick = { galleryLauncher.launch("image/*") },
                                leadingIcon = Icons.Default.PhotoLibrary
                            )

                            BuddysOutlinedButton(
                                text = "Camera",
                                onClick = { cameraLauncher.launch(null) },
                                leadingIcon = Icons.Default.CameraAlt
                            )
                        }
                    }
                }
            }

            // Bottom Caption & Source Bar (When image selected)
            if (selectedImageUri != null) {
                BuddysCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Change photo",
                                tint = BuddysTheme.colors.primaryRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        BasicTextField(
                            value = captionText,
                            onValueChange = { if (it.length <= 150) captionText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = BuddysTheme.colors.textPrimary),
                            decorationBox = { innerTextField ->
                                if (captionText.isEmpty()) {
                                    Text("Add a caption...", style = MaterialTheme.typography.bodyMedium.copy(color = BuddysTheme.colors.textMuted))
                                }
                                innerTextField()
                            },
                            singleLine = true
                        )

                        IconButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Retake photo",
                                tint = BuddysTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

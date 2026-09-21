package com.aura.glasschat.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aura.glasschat.data.model.StoryDraft
import com.aura.glasschat.data.model.StoryStickerItem
import com.aura.glasschat.data.model.StoryTextOverlay
import com.aura.glasschat.ui.components.BuddysButton
import com.aura.glasschat.ui.components.BuddysSpiderEmblem
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.ui.viewmodel.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private fun saveBitmapToStoryTempUri(context: android.content.Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "story_studio_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

val PALETTE_COLORS = listOf(
    0xFFFFFFFF, // White
    0xFFFF2A42, // Spider Neon Red
    0xFF00E676, // Neon Green
    0xFF00E5FF, // Neon Cyan
    0xFFFFEB3B, // Neon Yellow
    0xFFD500F9, // Neon Purple
    0xFFFF6D00, // Neon Orange
    0xFF121216  // Obsidian Dark
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryStudioScreen(
    onBack: () -> Unit,
    onStoryPosted: () -> Unit,
    viewModel: StoryStudioViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showStickersSheet by remember { mutableStateOf(false) }
    var showDraftsSheet by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        viewModel.loadDrafts(context)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setImageUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val uri = saveBitmapToStoryTempUri(context, bitmap)
            if (uri != null) {
                viewModel.setImageUri(uri)
            }
        }
    }

    // Intercept back button when photo is selected
    BackHandler(enabled = state.selectedImageUri != null) {
        showExitConfirmation = true
    }

    LaunchedEffect(state.uploadSuccess) {
        if (state.uploadSuccess) {
            Toast.makeText(context, "Story published! ✨", Toast.LENGTH_SHORT).show()
            viewModel.clearUploadStatus()
            onStoryPosted()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearUploadStatus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (state.selectedImageUri == null) {
            // Intake Screen: Camera / Gallery Selector / Drafts
            StoryIntakeView(
                draftsCount = state.drafts.size,
                onGalleryClick = { galleryLauncher.launch("image/*") },
                onCameraClick = { cameraLauncher.launch(null) },
                onDraftsClick = { showDraftsSheet = true },
                onBack = onBack
            )
        } else {
            // Main Canvas Studio
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { canvasSize = it.size }
            ) {
                // Layer 0: Background Media with Transformations (Pan / Zoom / Rotate / Double-tap reset)
                val colorMatrix = remember(state.selectedFilter) {
                    when (state.selectedFilter) {
                        StoryFilter.WARM -> ColorMatrix(floatArrayOf(
                            1.15f, 0f, 0f, 0f, 15f,
                            0f, 1.05f, 0f, 0f, 10f,
                            0f, 0f, 0.85f, 0f, -10f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        StoryFilter.MONO -> ColorMatrix().apply { setToSaturation(0f) }
                        StoryFilter.CYBER_NEON -> ColorMatrix(floatArrayOf(
                            1.3f, 0f, 0f, 0f, 30f,
                            0f, 0.9f, 0f, 0f, -10f,
                            0f, 0f, 1.4f, 0f, 35f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        StoryFilter.VINTAGE -> ColorMatrix(floatArrayOf(
                            0.95f, 0f, 0f, 0f, 20f,
                            0f, 0.85f, 0f, 0f, 15f,
                            0f, 0f, 0.75f, 0f, 5f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        StoryFilter.VIVID -> ColorMatrix().apply { setToSaturation(1.45f) }
                        StoryFilter.NORMAL -> ColorMatrix()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(state.activeTool) {
                            if (state.activeTool == StudioTool.NONE || state.activeTool == StudioTool.FILTERS) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        viewModel.resetPhotoTransform()
                                        Toast.makeText(context, "Photo reset", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                        .pointerInput(state.activeTool) {
                            if (state.activeTool == StudioTool.NONE || state.activeTool == StudioTool.FILTERS) {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    viewModel.updatePhotoTransform(pan, zoom, rotation)
                                }
                            }
                        }
                ) {
                    AsyncImage(
                        model = state.selectedImageUri,
                        contentDescription = "Story Background",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = state.photoOffsetX
                                translationY = state.photoOffsetY
                                scaleX = state.photoScale
                                scaleY = state.photoScale
                                rotationZ = state.photoRotation
                            },
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(colorMatrix)
                    )
                }

                // Layer 1: Drawing Canvas (Strokes)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(state.activeTool) {
                            if (state.activeTool == StudioTool.DRAWING) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        viewModel.startStroke(Offset(offset.x / w, offset.y / h))
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        viewModel.continueStroke(Offset(change.position.x / w, change.position.y / h))
                                    },
                                    onDragEnd = {
                                        viewModel.endStroke()
                                    },
                                    onDragCancel = {
                                        viewModel.endStroke()
                                    }
                                )
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Draw completed strokes
                    state.strokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            val path = Path()
                            stroke.points.forEachIndexed { i, pt ->
                                val px = pt.x * w
                                val py = pt.y * h
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }

                            when (stroke.brushType) {
                                BrushType.PEN -> {
                                    drawPath(
                                        path = path,
                                        color = Color(stroke.color),
                                        style = Stroke(width = stroke.strokeWidth * (w / 400f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                                BrushType.NEON -> {
                                    // Outer neon glow
                                    drawPath(
                                        path = path,
                                        color = Color(stroke.color).copy(alpha = 0.5f),
                                        style = Stroke(width = stroke.strokeWidth * (w / 280f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                    // Inner white core
                                    drawPath(
                                        path = path,
                                        color = Color.White,
                                        style = Stroke(width = stroke.strokeWidth * (w / 600f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                                BrushType.HIGHLIGHTER -> {
                                    drawPath(
                                        path = path,
                                        color = Color(stroke.color).copy(alpha = 0.4f),
                                        style = Stroke(width = stroke.strokeWidth * (w / 220f), cap = StrokeCap.Square, join = StrokeJoin.Round)
                                    )
                                }
                                BrushType.ERASER -> {}
                            }
                        }
                    }

                    // Draw in-progress stroke
                    if (state.currentStrokePoints.size > 1) {
                        val activePath = Path()
                        state.currentStrokePoints.forEachIndexed { i, pt ->
                            val px = pt.x * w
                            val py = pt.y * h
                            if (i == 0) activePath.moveTo(px, py) else activePath.lineTo(px, py)
                        }
                        drawPath(
                            path = activePath,
                            color = Color(state.brushColor),
                            style = Stroke(width = state.brushSize * (w / 400f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                // Layer 2: Transformable Stickers
                state.stickers.forEach { sticker ->
                    DraggableStickerView(
                        sticker = sticker,
                        canvasSize = canvasSize,
                        onUpdatePosition = { x, y, scale, rot ->
                            viewModel.updateStickerPosition(sticker.id, x, y, scale, rot)
                        },
                        onDelete = { viewModel.deleteSticker(sticker.id) }
                    )
                }

                // Layer 3: Transformable Text Overlays
                state.textOverlays.forEach { overlay ->
                    DraggableTextView(
                        overlay = overlay,
                        canvasSize = canvasSize,
                        onUpdatePosition = { x, y, scale, rot ->
                            viewModel.updateTextOverlayPosition(overlay.id, x, y, scale, rot)
                        },
                        onTap = {
                            viewModel.openTextEditor(overlay)
                        },
                        onDelete = { viewModel.deleteTextOverlay(overlay.id) }
                    )
                }

                // Layer 4: Top Studio Toolbar
                if (state.activeTool != StudioTool.DRAWING) {
                    StudioTopBar(
                        onBack = { showExitConfirmation = true },
                        onDraftsClick = { showDraftsSheet = true },
                        onTextClick = { viewModel.openTextEditor() },
                        onStickerClick = { showStickersSheet = true },
                        onDrawClick = { viewModel.setActiveTool(StudioTool.DRAWING) },
                        onFilterClick = {
                            val nextFilter = when (state.selectedFilter) {
                                StoryFilter.NORMAL -> StoryFilter.WARM
                                StoryFilter.WARM -> StoryFilter.MONO
                                StoryFilter.MONO -> StoryFilter.CYBER_NEON
                                StoryFilter.CYBER_NEON -> StoryFilter.VINTAGE
                                StoryFilter.VINTAGE -> StoryFilter.VIVID
                                StoryFilter.VIVID -> StoryFilter.NORMAL
                            }
                            viewModel.setFilter(nextFilter)
                            Toast.makeText(context, "Filter: ${nextFilter.displayName}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Layer 5: Drawing Toolbar (When Drawing is active)
                if (state.activeTool == StudioTool.DRAWING) {
                    DrawingToolHud(
                        brushType = state.brushType,
                        brushColor = state.brushColor,
                        onBrushTypeSelect = { viewModel.setBrushType(it) },
                        onColorSelect = { viewModel.setBrushColor(it) },
                        onUndo = { viewModel.undoStroke() },
                        onClear = { viewModel.clearStrokes() },
                        onDone = { viewModel.setActiveTool(StudioTool.NONE) }
                    )
                }

                // Layer 6: Bottom Footer (Caption, Audience, Share)
                if (state.activeTool == StudioTool.NONE) {
                    StudioBottomBar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        audience = state.audience,
                        caption = state.caption,
                        isUploading = state.isBakingAndUploading,
                        onAudienceChange = { viewModel.setAudience(it) },
                        onCaptionChange = { viewModel.setCaption(it) },
                        onShare = { viewModel.shareStory(context) }
                    )
                }
            }
        }

        // Text Overlay Editor Dialog
        if (state.activeTool == StudioTool.TEXT && state.editingTextOverlay != null) {
            StoryTextEditorDialog(
                initialOverlay = state.editingTextOverlay!!,
                onDismiss = { viewModel.setActiveTool(StudioTool.NONE) },
                onSave = { overlay -> viewModel.saveTextOverlay(overlay) }
            )
        }

        // Sticker Picker Bottom Sheet
        if (showStickersSheet) {
            ModalBottomSheet(
                onDismissRequest = { showStickersSheet = false },
                containerColor = Color(0xFF16161E),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                StickerPickerSheetContent(
                    onSelectLocation = { name ->
                        viewModel.addSticker("LOCATION", mapOf("name" to name))
                        showStickersSheet = false
                    },
                    onSelectTime = {
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                        viewModel.addSticker("TIME", mapOf("time" to timeFormat))
                        showStickersSheet = false
                    },
                    onSelectMention = { username ->
                        viewModel.addSticker("MENTION", mapOf("username" to username))
                        showStickersSheet = false
                    },
                    onSelectPoll = {
                        showStickersSheet = false
                        showPollDialog = true
                    },
                    onSelectQuestion = {
                        showStickersSheet = false
                        showQuestionDialog = true
                    },
                    onSelectEmoji = { emoji ->
                        viewModel.addSticker("EMOJI", mapOf("emoji" to emoji))
                        showStickersSheet = false
                    }
                )
            }
        }

        // Poll Creation Dialog
        if (showPollDialog) {
            var pollQuestion by remember { mutableStateOf("Ask a question...") }
            AlertDialog(
                onDismissRequest = { showPollDialog = false },
                containerColor = Color(0xFF1E1E28),
                title = { Text("Create Poll Sticker", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        label = { Text("Poll Question", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BuddysTheme.colors.primaryRed
                        ),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addSticker("POLL", mapOf("question" to pollQuestion.trim().ifBlank { "Ask a question..." }))
                            showPollDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BuddysTheme.colors.primaryRed)
                    ) {
                        Text("Add Poll", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPollDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }

        // Question Creation Dialog
        if (showQuestionDialog) {
            var questionPrompt by remember { mutableStateOf("Ask me a question") }
            AlertDialog(
                onDismissRequest = { showQuestionDialog = false },
                containerColor = Color(0xFF1E1E28),
                title = { Text("Create Question Sticker", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = questionPrompt,
                        onValueChange = { questionPrompt = it },
                        label = { Text("Prompt Title", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BuddysTheme.colors.primaryRed
                        ),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addSticker("QUESTION", mapOf("prompt" to questionPrompt.trim().ifBlank { "Ask me a question" }))
                            showQuestionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BuddysTheme.colors.primaryRed)
                    ) {
                        Text("Add Sticker", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuestionDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }

        // Saved Drafts Sheet
        if (showDraftsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDraftsSheet = false },
                containerColor = Color(0xFF16161E),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                StoryDraftsSheetContent(
                    drafts = state.drafts,
                    onResumeDraft = { draft ->
                        viewModel.resumeDraft(draft)
                        showDraftsSheet = false
                    },
                    onDeleteDraft = { draftId ->
                        viewModel.deleteDraft(context, draftId)
                    }
                )
            }
        }

        // Exit / Save Draft Confirmation Dialog
        if (showExitConfirmation) {
            AlertDialog(
                onDismissRequest = { showExitConfirmation = false },
                containerColor = Color(0xFF1E1E28),
                title = { Text("Save this story?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "You can save this story as a draft and finish editing it later, or discard your current edits.",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveCurrentDraft(context)
                            Toast.makeText(context, "Draft saved! 📝", Toast.LENGTH_SHORT).show()
                            showExitConfirmation = false
                            viewModel.setImageUri(Uri.EMPTY)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BuddysTheme.colors.primaryRed)
                    ) {
                        Text("Save Draft", color = Color.White)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showExitConfirmation = false
                                viewModel.setImageUri(Uri.EMPTY)
                            }
                        ) {
                            Text("Discard", color = Color(0xFFFF5252))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showExitConfirmation = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// INTAKE VIEW (Empty state before photo is selected)
// -------------------------------------------------------------
@Composable
private fun StoryIntakeView(
    draftsCount: Int,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDraftsClick: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12))
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        if (draftsCount > 0) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onDraftsClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Drafts, contentDescription = "Drafts", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Drafts ($draftsCount)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BuddysSpiderEmblem(
                modifier = Modifier.size(80.dp),
                tint = BuddysTheme.colors.primaryRed
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Story Studio",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Capture a photo or pick from gallery to design your 24h story with typography, interactive stickers & doodles.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BuddysTheme.colors.primaryRed,
                    modifier = Modifier
                        .size(120.dp, 100.dp)
                        .clickable { onCameraClick() }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Camera", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }

                // Gallery Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E1E28),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .size(120.dp, 100.dp)
                        .clickable { onGalleryClick() }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Gallery", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STUDIO TOP BAR
// -------------------------------------------------------------
@Composable
private fun StudioTopBar(
    onBack: () -> Unit,
    onDraftsClick: () -> Unit,
    onTextClick: () -> Unit,
    onStickerClick: () -> Unit,
    onDrawClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Tool
            IconButton(
                onClick = onTextClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Text("Aa", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }

            // Stickers Tool
            IconButton(
                onClick = onStickerClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.EmojiEmotions, contentDescription = "Stickers", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            // Drawing Tool
            IconButton(
                onClick = onDrawClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Doodle", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Filters Tool
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Filters", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// DRAWING HUD
// -------------------------------------------------------------
@Composable
private fun DrawingToolHud(
    brushType: BrushType,
    brushColor: Long,
    onBrushTypeSelect: (BrushType) -> Unit,
    onColorSelect: (Long) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                )
            )
            .padding(14.dp)
    ) {
        // Top brush selector + Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onUndo,
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // Brush mode pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrushType.entries.forEach { type ->
                    val isSelected = brushType == type
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) BuddysTheme.colors.primaryRed else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.clickable { onBrushTypeSelect(type) }
                    ) {
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onDone,
                modifier = Modifier.size(36.dp).background(BuddysTheme.colors.primaryRed, CircleShape)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Color Palette Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(PALETTE_COLORS) { colorLong ->
                val isSelected = brushColor == colorLong
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelect(colorLong) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STUDIO BOTTOM BAR
// -------------------------------------------------------------
@Composable
private fun StudioBottomBar(
    modifier: Modifier = Modifier,
    audience: String,
    caption: String,
    isUploading: Boolean,
    onAudienceChange: (String) -> Unit,
    onCaptionChange: (String) -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Caption Input Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = caption,
                onValueChange = { if (it.length <= 140) onCaptionChange(it) },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                decorationBox = { innerTextField ->
                    if (caption.isEmpty()) {
                        Text("Add a story caption...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                    innerTextField()
                },
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Audience + Share Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audience Selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isEveryone = audience == "EVERYONE"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isEveryone) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (isEveryone) BuddysTheme.colors.primaryRed else Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { onAudienceChange("EVERYONE") }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("🌐 Your Story", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                val isClose = audience == "CLOSE_FRIENDS"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isClose) Color(0xFF00E676).copy(alpha = 0.25f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (isClose) Color(0xFF00E676) else Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { onAudienceChange("CLOSE_FRIENDS") }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("⭐ Close Friends", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isClose) Color(0xFF00E676) else Color.White)
                    }
                }
            }

            // Publish / Share Button
            Surface(
                shape = CircleShape,
                color = if (audience == "CLOSE_FRIENDS") Color(0xFF00E676) else BuddysTheme.colors.primaryRed,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(enabled = !isUploading) { onShare() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Share Story", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DRAGGABLE STICKER VIEW
// -------------------------------------------------------------
@Composable
private fun DraggableStickerView(
    sticker: StoryStickerItem,
    canvasSize: IntSize,
    onUpdatePosition: (x: Float, y: Float, scale: Float, rotation: Float) -> Unit,
    onDelete: () -> Unit
) {
    if (canvasSize.width == 0 || canvasSize.height == 0) return

    var currentX by remember(sticker.id) { mutableFloatStateOf(sticker.x) }
    var currentY by remember(sticker.id) { mutableFloatStateOf(sticker.y) }
    var currentScale by remember(sticker.id) { mutableFloatStateOf(sticker.scale) }
    var currentRotation by remember(sticker.id) { mutableFloatStateOf(sticker.rotation) }

    val pxX = currentX * canvasSize.width
    val pxY = currentY * canvasSize.height

    Box(
        modifier = Modifier
            .offset(x = (pxX - 100f).dp / 2.75f, y = (pxY - 100f).dp / 2.75f)
            .scale(currentScale)
            .rotate(currentRotation)
            .pointerInput(sticker.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    currentX = ((currentX * canvasSize.width + pan.x) / canvasSize.width).coerceIn(0.05f, 0.95f)
                    currentY = ((currentY * canvasSize.height + pan.y) / canvasSize.height).coerceIn(0.05f, 0.95f)
                    currentScale = (currentScale * zoom).coerceIn(0.4f, 4.0f)
                    currentRotation += rotation
                    onUpdatePosition(currentX, currentY, currentScale, currentRotation)
                }
            }
    ) {
        when (sticker.type) {
            "LOCATION" -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BuddysTheme.colors.primaryRed,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = "📍 " + (sticker.data["name"] ?: "Buddies HQ"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            "TIME" -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1A24),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = sticker.data["time"] ?: "12:00 PM",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            "MENTION" -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFD500F9),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = "@" + (sticker.data["username"] ?: "buddy"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            "POLL" -> {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E1E28),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(200.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = sticker.data["question"] ?: "Ask a question...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF00E676),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "YES",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BuddysTheme.colors.primaryRed,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "NO",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
            "QUESTION" -> {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(210.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = sticker.data["prompt"] ?: "Ask me a question",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0F0F5),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Type something...",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            "EMOJI" -> {
                Text(
                    text = sticker.data["emoji"] ?: "🔥",
                    fontSize = 48.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DRAGGABLE TEXT VIEW
// -------------------------------------------------------------
@Composable
private fun DraggableTextView(
    overlay: StoryTextOverlay,
    canvasSize: IntSize,
    onUpdatePosition: (x: Float, y: Float, scale: Float, rotation: Float) -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    if (canvasSize.width == 0 || canvasSize.height == 0) return

    var currentX by remember(overlay.id) { mutableFloatStateOf(overlay.x) }
    var currentY by remember(overlay.id) { mutableFloatStateOf(overlay.y) }
    var currentScale by remember(overlay.id) { mutableFloatStateOf(overlay.scale) }
    var currentRotation by remember(overlay.id) { mutableFloatStateOf(overlay.rotation) }

    val pxX = currentX * canvasSize.width
    val pxY = currentY * canvasSize.height

    val fontFamily = when (overlay.style) {
        "TYPEWRITER" -> FontFamily.Monospace
        "STRONG" -> FontFamily.Serif
        "MINIMAL" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
    val fontStyle = if (overlay.style == "STRONG") FontStyle.Italic else FontStyle.Normal
    val fontWeight = when (overlay.style) {
        "TYPEWRITER" -> FontWeight.Normal
        "MINIMAL" -> FontWeight.Light
        else -> FontWeight.Black
    }
    val textAlign = when (overlay.alignment) {
        "LEFT" -> TextAlign.Start
        "RIGHT" -> TextAlign.End
        else -> TextAlign.Center
    }

    Box(
        modifier = Modifier
            .offset(x = (pxX - 120f).dp / 2.75f, y = (pxY - 60f).dp / 2.75f)
            .scale(currentScale)
            .rotate(currentRotation)
            .pointerInput(overlay.id) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(overlay.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    currentX = ((currentX * canvasSize.width + pan.x) / canvasSize.width).coerceIn(0.05f, 0.95f)
                    currentY = ((currentY * canvasSize.height + pan.y) / canvasSize.height).coerceIn(0.05f, 0.95f)
                    currentScale = (currentScale * zoom).coerceIn(0.4f, 4.0f)
                    currentRotation += rotation
                    onUpdatePosition(currentX, currentY, currentScale, currentRotation)
                }
            }
    ) {
        val hasBg = overlay.backgroundColor != 0L && overlay.backgroundColor != 0x00000000L
        val bgAlpha = overlay.backgroundOpacity.coerceIn(0f, 1f)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (hasBg) Color(overlay.backgroundColor).copy(alpha = bgAlpha) else Color.Transparent,
            shadowElevation = if (hasBg) 6.dp else 0.dp
        ) {
            Text(
                text = overlay.text,
                color = Color(overlay.color),
                fontSize = overlay.fontSize.sp,
                fontFamily = fontFamily,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                textAlign = textAlign,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// TEXT OVERLAY EDITOR DIALOG
// -------------------------------------------------------------
@Composable
private fun StoryTextEditorDialog(
    initialOverlay: StoryTextOverlay,
    onDismiss: () -> Unit,
    onSave: (StoryTextOverlay) -> Unit
) {
    var text by remember { mutableStateOf(initialOverlay.text) }
    var style by remember { mutableStateOf(initialOverlay.style) }
    var colorLong by remember { mutableLongStateOf(initialOverlay.color) }
    var alignment by remember { mutableStateOf(initialOverlay.alignment) }
    var fontSize by remember { mutableFloatStateOf(initialOverlay.fontSize) }
    // bgMode: 0 = none, 1 = semi-transparent, 2 = solid
    var bgMode by remember {
        mutableIntStateOf(
            when {
                initialOverlay.backgroundColor == 0L || initialOverlay.backgroundColor == 0x00000000L -> 0
                initialOverlay.backgroundOpacity < 0.9f -> 1
                else -> 2
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Header Controls: Close, Alignment, Background Pill Mode, Done
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Alignment Toggle
                    IconButton(
                        onClick = {
                            alignment = when (alignment) {
                                "LEFT" -> "CENTER"
                                "CENTER" -> "RIGHT"
                                else -> "LEFT"
                            }
                        }
                    ) {
                        @Suppress("DEPRECATION")
                        val alignIcon = when (alignment) {
                            "LEFT" -> Icons.Default.FormatAlignLeft
                            "RIGHT" -> Icons.Default.FormatAlignRight
                            else -> Icons.Default.FormatAlignCenter
                        }
                        Icon(alignIcon, contentDescription = "Alignment", tint = Color.White)
                    }

                    // Background Pill Mode Toggle (Cycle: None -> Translucent -> Solid)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (bgMode) {
                            1 -> Color.White.copy(alpha = 0.5f)
                            2 -> Color.White
                            else -> Color.White.copy(alpha = 0.2f)
                        },
                        modifier = Modifier.clickable {
                            bgMode = (bgMode + 1) % 3
                        }
                    ) {
                        Text(
                            text = "A",
                            color = if (bgMode == 2) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                BuddysButton(
                    text = "Done",
                    onClick = {
                        val bgColor = when (bgMode) {
                            1, 2 -> if (colorLong == 0xFFFFFFFF) 0xFF121216 else 0xFFFFFFFF
                            else -> 0x00000000L
                        }
                        val opacity = when (bgMode) {
                            1 -> 0.65f
                            2 -> 1.0f
                            else -> 0.0f
                        }

                        onSave(
                            initialOverlay.copy(
                                text = text.trim(),
                                style = style,
                                color = colorLong,
                                backgroundColor = bgColor,
                                backgroundOpacity = opacity,
                                alignment = alignment,
                                fontSize = fontSize
                            )
                        )
                    },
                    modifier = Modifier.height(36.dp)
                )
            }

            // Center Text Input with live typography preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                val fontFamily = when (style) {
                    "TYPEWRITER" -> FontFamily.Monospace
                    "STRONG" -> FontFamily.Serif
                    "MINIMAL" -> FontFamily.SansSerif
                    else -> FontFamily.Default
                }
                val fontStyle = if (style == "STRONG") FontStyle.Italic else FontStyle.Normal
                val fontWeight = when (style) {
                    "TYPEWRITER" -> FontWeight.Normal
                    "MINIMAL" -> FontWeight.Light
                    else -> FontWeight.Black
                }
                val textAlign = when (alignment) {
                    "LEFT" -> TextAlign.Start
                    "RIGHT" -> TextAlign.End
                    else -> TextAlign.Center
                }

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        color = Color(colorLong),
                        fontSize = fontSize.sp,
                        fontFamily = fontFamily,
                        fontStyle = fontStyle,
                        fontWeight = fontWeight,
                        textAlign = textAlign
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                "Type something...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = fontSize.sp,
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Bottom Style & Color Selectors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding()
            ) {
                // Style Selector (Classic, Modern, Strong, Typewriter, Minimal, Neon)
                val styles = listOf("CLASSIC", "MODERN", "STRONG", "TYPEWRITER", "MINIMAL", "NEON")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    items(styles) { st ->
                        val isSelected = style == st
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) BuddysTheme.colors.primaryRed else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { style = st }
                        ) {
                            Text(
                                text = st,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Color Palette
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PALETTE_COLORS) { clr ->
                        val isSelected = colorLong == clr
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(clr))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { colorLong = clr }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STICKER PICKER SHEET
// -------------------------------------------------------------
@Composable
private fun StickerPickerSheetContent(
    onSelectLocation: (String) -> Unit,
    onSelectTime: () -> Unit,
    onSelectMention: (String) -> Unit,
    onSelectPoll: () -> Unit,
    onSelectQuestion: () -> Unit,
    onSelectEmoji: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Widgets & Stickers", "Emojis")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = BuddysTheme.colors.primaryRed
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.5f)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Interactive Widgets & Chips
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Poll Sticker
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1E28),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f).clickable { onSelectPoll() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📊 Poll", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }

                    // Question Sticker
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1E28),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f).clickable { onSelectQuestion() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("❓ Question", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                // Time Widget
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1A24),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectTime() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🕒 Current Time", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }

                // Locations
                Text("Locations", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                val locations = listOf("Spider HQ", "Neon City", "New York", "Home Base", "Buddies Lab")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(locations) { loc ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BuddysTheme.colors.primaryRed.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BuddysTheme.colors.primaryRed),
                            modifier = Modifier.clickable { onSelectLocation(loc) }
                        ) {
                            Text("📍 $loc", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                }

                // Mentions
                Spacer(modifier = Modifier.height(4.dp))
                Text("Mentions", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                val mentions = listOf("buddy", "spiderman", "aura", "squad", "bestie")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mentions) { men ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFD500F9).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFD500F9)),
                            modifier = Modifier.clickable { onSelectMention(men) }
                        ) {
                            Text("@$men", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        } else {
            // Emojis Grid
            val emojis = listOf(
                "🔥", "⭐", "🕷️", "❤️", "💀", "🚀",
                "⚡", "💯", "🎨", "🎵", "🕶️", "👑",
                "🥳", "🍕", "👾", "🕹️", "🎉", "✨"
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (row in emojis.chunked(6)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        row.forEach { em ->
                            Text(
                                text = em,
                                fontSize = 32.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onSelectEmoji(em) }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// STORY DRAFTS SHEET
// -------------------------------------------------------------
@Composable
private fun StoryDraftsSheetContent(
    drafts: List<StoryDraft>,
    onResumeDraft: (StoryDraft) -> Unit,
    onDeleteDraft: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved Story Drafts (${drafts.size})",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (drafts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved drafts yet.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(drafts) { draft ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1E28),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Thumbnail
                                AsyncImage(
                                    model = draft.imageUriString,
                                    contentDescription = "Draft Thumbnail",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = draft.caption.ifBlank { "Story Draft" },
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    val formattedTime = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(draft.updatedAt))
                                    Text(
                                        text = formattedTime,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onResumeDraft(draft) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BuddysTheme.colors.primaryRed),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(onClick = { onDeleteDraft(draft.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

package com.aura.glasschat.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

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
            // Intake Screen: Camera / Gallery Selector
            StoryIntakeView(
                onGalleryClick = { galleryLauncher.launch("image/*") },
                onCameraClick = { cameraLauncher.launch(null) },
                onBack = onBack
            )
        } else {
            // Main Canvas Studio
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { canvasSize = it.size }
            ) {
                // Layer 0: Background Media with ColorMatrix Filter
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

                AsyncImage(
                    model = state.selectedImageUri,
                    contentDescription = "Story Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                )

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
                        onBack = { viewModel.setImageUri(Uri.EMPTY) },
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
                    onSelectEmoji = { emoji ->
                        viewModel.addSticker("EMOJI", mapOf("emoji" to emoji))
                        showStickersSheet = false
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// INTAKE VIEW (Empty state before photo is selected)
// -------------------------------------------------------------
@Composable
private fun StoryIntakeView(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
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
                text = "Capture a photo or choose from your gallery to design your 24h story with shaders, doodles & stickers.",
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
            Icon(Icons.Default.Close, contentDescription = "Discard", tint = Color.White)
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
                    currentScale = (currentScale * zoom).coerceIn(0.5f, 3.0f)
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
        else -> FontFamily.Default
    }
    val fontStyle = if (overlay.style == "STRONG") FontStyle.Italic else FontStyle.Normal
    val fontWeight = when (overlay.style) {
        "TYPEWRITER" -> FontWeight.Normal
        else -> FontWeight.Black
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
                    currentScale = (currentScale * zoom).coerceIn(0.5f, 3.0f)
                    currentRotation += rotation
                    onUpdatePosition(currentX, currentY, currentScale, currentRotation)
                }
            }
    ) {
        val hasBg = overlay.backgroundColor != 0L && overlay.backgroundColor != 0x00000000L
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (hasBg) Color(overlay.backgroundColor) else Color.Transparent,
            shadowElevation = if (hasBg) 6.dp else 0.dp
        ) {
            Text(
                text = overlay.text,
                color = Color(overlay.color),
                fontSize = 22.sp,
                fontFamily = fontFamily,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center,
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
    var hasBackground by remember { mutableStateOf(initialOverlay.backgroundColor != 0L && initialOverlay.backgroundColor != 0x00000000L) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }

                // Background badge toggle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (hasBackground) Color.White else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { hasBackground = !hasBackground }
                ) {
                    Text(
                        text = "A",
                        color = if (hasBackground) Color.Black else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                BuddysButton(
                    text = "Done",
                    onClick = {
                        val bgColor = if (hasBackground) {
                            if (colorLong == 0xFFFFFFFF) 0xCC121216 else 0xCCFFFFFF
                        } else 0x00000000L

                        onSave(
                            initialOverlay.copy(
                                text = text.trim(),
                                style = style,
                                color = colorLong,
                                backgroundColor = bgColor
                            )
                        )
                    },
                    modifier = Modifier.height(36.dp)
                )
            }

            // Center Text Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                val fontFamily = when (style) {
                    "TYPEWRITER" -> FontFamily.Monospace
                    "STRONG" -> FontFamily.Serif
                    else -> FontFamily.Default
                }
                val fontStyle = if (style == "STRONG") FontStyle.Italic else FontStyle.Normal
                val fontWeight = if (style == "TYPEWRITER") FontWeight.Normal else FontWeight.Black

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        color = Color(colorLong),
                        fontSize = 28.sp,
                        fontFamily = fontFamily,
                        fontStyle = fontStyle,
                        fontWeight = fontWeight,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                "Type something...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 28.sp,
                                textAlign = TextAlign.Center,
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
                // Style Selector
                val styles = listOf("CLASSIC", "MODERN", "NEON", "TYPEWRITER", "STRONG")
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
    onSelectEmoji: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Widgets", "Emojis")

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
            // Interactive Widgets
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Time Widget
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E28),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectTime() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🕒 Current Time", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    }
                }

                // Locations
                val locations = listOf("Spider HQ", "Neon City", "New York", "Home Base")
                Text("Locations", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
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
                val mentions = listOf("buddy", "spiderman", "aura", "squad")
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

package com.aura.glasschat.ui.viewmodel

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.StoryStickerItem
import com.aura.glasschat.data.model.StoryTextOverlay
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.MediaRepository
import com.aura.glasschat.data.repository.StoryRepository
import com.aura.glasschat.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

enum class StudioTool {
    NONE,
    DRAWING,
    TEXT,
    STICKERS,
    FILTERS
}

enum class BrushType {
    PEN,
    NEON,
    HIGHLIGHTER,
    ERASER
}

enum class StoryFilter(val displayName: String) {
    NORMAL("Normal"),
    WARM("Warm"),
    MONO("Mono"),
    CYBER_NEON("Cyber Neon"),
    VINTAGE("Vintage"),
    VIVID("Vivid")
}

data class DrawnStroke(
    val points: List<Offset>,
    val color: Long,
    val strokeWidth: Float,
    val brushType: BrushType
)

data class StoryStudioState(
    val selectedImageUri: Uri? = null,
    val activeTool: StudioTool = StudioTool.NONE,
    val selectedFilter: StoryFilter = StoryFilter.NORMAL,
    val brushType: BrushType = BrushType.PEN,
    val brushColor: Long = 0xFFFF2A42, // Neon Red default
    val brushSize: Float = 12f,
    val strokes: List<DrawnStroke> = emptyList(),
    val currentStrokePoints: List<Offset> = emptyList(),
    val textOverlays: List<StoryTextOverlay> = emptyList(),
    val editingTextOverlay: StoryTextOverlay? = null,
    val stickers: List<StoryStickerItem> = emptyList(),
    val caption: String = "",
    val audience: String = "EVERYONE", // "EVERYONE" or "CLOSE_FRIENDS"
    val isBakingAndUploading: Boolean = false,
    val uploadSuccess: Boolean = false,
    val errorMessage: String? = null
)

class StoryStudioViewModel @JvmOverloads constructor(
    private val storyRepository: StoryRepository = StoryRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val mediaRepository: MediaRepository = MediaRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(StoryStudioState())
    val state: StateFlow<StoryStudioState> = _state.asStateFlow()

    fun setImageUri(uri: Uri) {
        _state.update { it.copy(selectedImageUri = uri) }
    }

    fun setActiveTool(tool: StudioTool) {
        _state.update { it.copy(activeTool = tool) }
    }

    fun setFilter(filter: StoryFilter) {
        _state.update { it.copy(selectedFilter = filter) }
    }

    fun setBrushType(type: BrushType) {
        _state.update { it.copy(brushType = type) }
    }

    fun setBrushColor(color: Long) {
        _state.update { it.copy(brushColor = color) }
    }

    fun setBrushSize(size: Float) {
        _state.update { it.copy(brushSize = size) }
    }

    fun setAudience(audience: String) {
        _state.update { it.copy(audience = audience) }
    }

    fun setCaption(caption: String) {
        _state.update { it.copy(caption = caption) }
    }

    // --- Drawing methods ---

    fun startStroke(point: Offset) {
        _state.update { it.copy(currentStrokePoints = listOf(point)) }
    }

    fun continueStroke(point: Offset) {
        _state.update { it.copy(currentStrokePoints = it.currentStrokePoints + point) }
    }

    fun endStroke() {
        val currentPoints = _state.value.currentStrokePoints
        if (currentPoints.isNotEmpty()) {
            val stroke = DrawnStroke(
                points = currentPoints,
                color = _state.value.brushColor,
                strokeWidth = _state.value.brushSize,
                brushType = _state.value.brushType
            )
            _state.update {
                it.copy(
                    strokes = it.strokes + stroke,
                    currentStrokePoints = emptyList()
                )
            }
        }
    }

    fun undoStroke() {
        _state.update {
            if (it.strokes.isNotEmpty()) {
                it.copy(strokes = it.strokes.dropLast(1))
            } else it
        }
    }

    fun clearStrokes() {
        _state.update { it.copy(strokes = emptyList(), currentStrokePoints = emptyList()) }
    }

    // --- Text Overlay methods ---

    fun openTextEditor(overlay: StoryTextOverlay? = null) {
        val target = overlay ?: StoryTextOverlay(
            id = UUID.randomUUID().toString(),
            text = "",
            style = "CLASSIC",
            color = 0xFFFFFFFF,
            backgroundColor = 0x00000000,
            x = 0.5f,
            y = 0.5f
        )
        _state.update {
            it.copy(
                editingTextOverlay = target,
                activeTool = StudioTool.TEXT
            )
        }
    }

    fun saveTextOverlay(overlay: StoryTextOverlay) {
        if (overlay.text.isBlank()) {
            // Remove if empty
            _state.update { state ->
                state.copy(
                    textOverlays = state.textOverlays.filter { it.id != overlay.id },
                    editingTextOverlay = null,
                    activeTool = StudioTool.NONE
                )
            }
            return
        }

        _state.update { state ->
            val existingIndex = state.textOverlays.indexOfFirst { it.id == overlay.id }
            val updated = if (existingIndex != -1) {
                state.textOverlays.toMutableList().apply { set(existingIndex, overlay) }
            } else {
                state.textOverlays + overlay
            }
            state.copy(
                textOverlays = updated,
                editingTextOverlay = null,
                activeTool = StudioTool.NONE
            )
        }
    }

    fun updateTextOverlayPosition(id: String, x: Float, y: Float, scale: Float = 1.0f, rotation: Float = 0.0f) {
        _state.update { state ->
            val updated = state.textOverlays.map {
                if (it.id == id) it.copy(x = x.coerceIn(0.05f, 0.95f), y = y.coerceIn(0.05f, 0.95f), scale = scale, rotation = rotation)
                else it
            }
            state.copy(textOverlays = updated)
        }
    }

    fun deleteTextOverlay(id: String) {
        _state.update { state ->
            state.copy(textOverlays = state.textOverlays.filter { it.id != id })
        }
    }

    // --- Sticker methods ---

    fun addSticker(type: String, data: Map<String, String>) {
        val sticker = StoryStickerItem(
            id = UUID.randomUUID().toString(),
            type = type,
            data = data,
            x = 0.5f,
            y = 0.5f,
            scale = 1.0f,
            rotation = 0.0f
        )
        _state.update {
            it.copy(
                stickers = it.stickers + sticker,
                activeTool = StudioTool.NONE
            )
        }
    }

    fun updateStickerPosition(id: String, x: Float, y: Float, scale: Float = 1.0f, rotation: Float = 0.0f) {
        _state.update { state ->
            val updated = state.stickers.map {
                if (it.id == id) it.copy(x = x.coerceIn(0.05f, 0.95f), y = y.coerceIn(0.05f, 0.95f), scale = scale, rotation = rotation)
                else it
            }
            state.copy(stickers = updated)
        }
    }

    fun deleteSticker(id: String) {
        _state.update { state ->
            state.copy(stickers = state.stickers.filter { it.id != id })
        }
    }

    // --- Compositing & Baking Pipeline ---

    fun shareStory(context: Context) {
        val currentUri = _state.value.selectedImageUri
        if (currentUri == null) {
            _state.update { it.copy(errorMessage = "Please select or capture a photo first") }
            return
        }

        val uid = authRepository.currentUserId
        if (uid.isBlank()) {
            _state.update { it.copy(errorMessage = "Please log in to publish stories") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isBakingAndUploading = true, errorMessage = null, uploadSuccess = false) }

            try {
                val bakedBytes = bakeStoryBitmap(context, currentUri, _state.value)

                val user = userRepository.getUser(uid) ?: run {
                    val fbUser = authRepository.currentUser
                    User(
                        uid = uid,
                        email = fbUser?.email ?: "",
                        displayName = fbUser?.displayName ?: "Buddy",
                        username = fbUser?.displayName?.replace(" ", "")?.lowercase() ?: "buddy",
                        avatarUrl = fbUser?.photoUrl?.toString()
                    )
                }

                val uploadResult = storyRepository.uploadBakedStory(
                    imageBytes = bakedBytes,
                    caption = _state.value.caption,
                    user = user,
                    audience = _state.value.audience,
                    textOverlays = _state.value.textOverlays,
                    stickers = _state.value.stickers,
                    filterName = _state.value.selectedFilter.name,
                    drawingPathData = null
                )

                uploadResult.fold(
                    onSuccess = {
                        _state.update { it.copy(isBakingAndUploading = false, uploadSuccess = true) }
                    },
                    onFailure = { err ->
                        _state.update { it.copy(isBakingAndUploading = false, errorMessage = err.localizedMessage ?: "Failed to post story") }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(isBakingAndUploading = false, errorMessage = e.localizedMessage ?: "Error processing story image") }
            }
        }
    }

    private suspend fun bakeStoryBitmap(context: Context, imageUri: Uri, state: StoryStudioState): ByteArray = withContext(Dispatchers.IO) {
        val targetWidth = 1080
        val targetHeight = 1920

        // 1. Decode original image
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(imageUri)
            ?: throw IllegalArgumentException("Cannot open image input stream")
        val rawBytes = inputStream.readBytes()
        inputStream.close()

        val rawBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: throw IllegalArgumentException("Failed to decode image data")

        // 2. Create Target 9:16 vertical canvas
        val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // 3. Draw background image with center-crop fill
        val srcRatio = rawBitmap.width.toFloat() / rawBitmap.height.toFloat()
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
        val srcRect: Rect
        if (srcRatio > targetRatio) {
            val cropWidth = (rawBitmap.height * targetRatio).toInt()
            val left = (rawBitmap.width - cropWidth) / 2
            srcRect = Rect(left, 0, left + cropWidth, rawBitmap.height)
        } else {
            val cropHeight = (rawBitmap.width / targetRatio).toInt()
            val top = (rawBitmap.height - cropHeight) / 2
            srcRect = Rect(0, top, rawBitmap.width, top + cropHeight)
        }

        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Apply Color Filter Matrix
        when (state.selectedFilter) {
            StoryFilter.WARM -> {
                val cm = ColorMatrix(floatArrayOf(
                    1.15f, 0f, 0f, 0f, 15f,
                    0f, 1.05f, 0f, 0f, 10f,
                    0f, 0f, 0.85f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                bgPaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            StoryFilter.MONO -> {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                bgPaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            StoryFilter.CYBER_NEON -> {
                val cm = ColorMatrix(floatArrayOf(
                    1.3f, 0f, 0f, 0f, 30f,
                    0f, 0.9f, 0f, 0f, -10f,
                    0f, 0f, 1.4f, 0f, 35f,
                    0f, 0f, 0f, 1f, 0f
                ))
                bgPaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            StoryFilter.VINTAGE -> {
                val cm = ColorMatrix(floatArrayOf(
                    0.95f, 0f, 0f, 0f, 20f,
                    0f, 0.85f, 0f, 0f, 15f,
                    0f, 0f, 0.75f, 0f, 5f,
                    0f, 0f, 0f, 1f, 0f
                ))
                bgPaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            StoryFilter.VIVID -> {
                val cm = ColorMatrix().apply { setSaturation(1.45f) }
                bgPaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            StoryFilter.NORMAL -> {}
        }

        canvas.drawBitmap(rawBitmap, srcRect, dstRect, bgPaint)
        rawBitmap.recycle()

        // 4. Draw strokes
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        state.strokes.forEach { stroke ->
            if (stroke.points.size > 1) {
                val path = Path()
                stroke.points.forEachIndexed { i, pt ->
                    val x = pt.x * targetWidth
                    val y = pt.y * targetHeight
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                when (stroke.brushType) {
                    BrushType.PEN -> {
                        strokePaint.color = stroke.color.toInt()
                        strokePaint.strokeWidth = stroke.strokeWidth * (targetWidth / 400f)
                        strokePaint.maskFilter = null
                        strokePaint.xfermode = null
                        canvas.drawPath(path, strokePaint)
                    }
                    BrushType.NEON -> {
                        strokePaint.color = stroke.color.toInt()
                        strokePaint.strokeWidth = stroke.strokeWidth * (targetWidth / 300f)
                        strokePaint.maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
                        strokePaint.xfermode = null
                        canvas.drawPath(path, strokePaint)

                        strokePaint.color = android.graphics.Color.WHITE
                        strokePaint.strokeWidth = stroke.strokeWidth * (targetWidth / 600f)
                        strokePaint.maskFilter = null
                        canvas.drawPath(path, strokePaint)
                    }
                    BrushType.HIGHLIGHTER -> {
                        val baseColor = stroke.color.toInt()
                        val alphaColor = android.graphics.Color.argb(
                            90,
                            android.graphics.Color.red(baseColor),
                            android.graphics.Color.green(baseColor),
                            android.graphics.Color.blue(baseColor)
                        )
                        strokePaint.color = alphaColor
                        strokePaint.strokeWidth = stroke.strokeWidth * (targetWidth / 250f)
                        strokePaint.maskFilter = null
                        strokePaint.xfermode = null
                        canvas.drawPath(path, strokePaint)
                    }
                    BrushType.ERASER -> { }
                }
            }
        }

        // 5. Draw Stickers
        val stickerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 58f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val stickerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        state.stickers.forEach { sticker ->
            val cx = sticker.x * targetWidth
            val cy = sticker.y * targetHeight
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(sticker.rotation)
            canvas.scale(sticker.scale, sticker.scale)

            when (sticker.type) {
                "LOCATION" -> {
                    val label = "📍 " + (sticker.data["name"] ?: "Buddies HQ")
                    stickerBgPaint.color = android.graphics.Color.parseColor("#FF2A42")
                    val textBounds = Rect()
                    stickerTextPaint.textSize = 46f
                    stickerTextPaint.getTextBounds(label, 0, label.length, textBounds)
                    val padX = 36f
                    val padY = 22f
                    val r = RectF(
                        -textBounds.width() / 2f - padX,
                        -textBounds.height() / 2f - padY,
                        textBounds.width() / 2f + padX,
                        textBounds.height() / 2f + padY
                    )
                    canvas.drawRoundRect(r, 32f, 32f, stickerBgPaint)
                    canvas.drawText(label, 0f, textBounds.height() / 3f, stickerTextPaint)
                }
                "TIME" -> {
                    val time = sticker.data["time"] ?: "12:00 PM"
                    stickerBgPaint.color = android.graphics.Color.parseColor("#1A1A24")
                    val textBounds = Rect()
                    stickerTextPaint.textSize = 52f
                    stickerTextPaint.getTextBounds(time, 0, time.length, textBounds)
                    val padX = 36f
                    val padY = 24f
                    val r = RectF(
                        -textBounds.width() / 2f - padX,
                        -textBounds.height() / 2f - padY,
                        textBounds.width() / 2f + padX,
                        textBounds.height() / 2f + padY
                    )
                    canvas.drawRoundRect(r, 28f, 28f, stickerBgPaint)
                    canvas.drawText(time, 0f, textBounds.height() / 3f, stickerTextPaint)
                }
                "MENTION" -> {
                    val mention = "@" + (sticker.data["username"] ?: "buddy")
                    stickerBgPaint.color = android.graphics.Color.parseColor("#D500F9")
                    val textBounds = Rect()
                    stickerTextPaint.textSize = 48f
                    stickerTextPaint.getTextBounds(mention, 0, mention.length, textBounds)
                    val padX = 34f
                    val padY = 22f
                    val r = RectF(
                        -textBounds.width() / 2f - padX,
                        -textBounds.height() / 2f - padY,
                        textBounds.width() / 2f + padX,
                        textBounds.height() / 2f + padY
                    )
                    canvas.drawRoundRect(r, 30f, 30f, stickerBgPaint)
                    canvas.drawText(mention, 0f, textBounds.height() / 3f, stickerTextPaint)
                }
                "EMOJI" -> {
                    val emoji = sticker.data["emoji"] ?: "🔥"
                    val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 120f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(emoji, 0f, 40f, emojiPaint)
                }
            }
            canvas.restore()
        }

        // 6. Draw Text Overlays
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }
        val overlayBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        state.textOverlays.forEach { overlay ->
            val cx = overlay.x * targetWidth
            val cy = overlay.y * targetHeight
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(overlay.rotation)
            canvas.scale(overlay.scale, overlay.scale)

            overlayPaint.color = overlay.color.toInt()
            overlayPaint.textSize = 62f

            when (overlay.style) {
                "MODERN" -> {
                    overlayPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                "NEON" -> {
                    overlayPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    overlayPaint.setShadowLayer(16f, 0f, 0f, overlay.color.toInt())
                }
                "TYPEWRITER" -> {
                    overlayPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                }
                "STRONG" -> {
                    overlayPaint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                }
                else -> {
                    overlayPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }

            val textBounds = Rect()
            overlayPaint.getTextBounds(overlay.text, 0, overlay.text.length, textBounds)

            if (overlay.backgroundColor != 0L && overlay.backgroundColor != 0x00000000L) {
                overlayBgPaint.color = overlay.backgroundColor.toInt()
                val padX = 36f
                val padY = 20f
                val r = RectF(
                    -textBounds.width() / 2f - padX,
                    -textBounds.height() / 2f - padY,
                    textBounds.width() / 2f + padX,
                    textBounds.height() / 2f + padY
                )
                canvas.drawRoundRect(r, 20f, 20f, overlayBgPaint)
            }

            canvas.drawText(overlay.text, 0f, textBounds.height() / 3f, overlayPaint)
            canvas.restore()
        }

        // 7. Compress into JPEG
        val outStream = ByteArrayOutputStream()
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 88, outStream)
        resultBitmap.recycle()
        outStream.toByteArray()
    }

    fun clearUploadStatus() {
        _state.update { it.copy(uploadSuccess = false, errorMessage = null) }
    }
}

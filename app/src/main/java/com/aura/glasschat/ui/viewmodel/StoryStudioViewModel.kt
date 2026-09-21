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
    FILTERS,
    DRAFTS
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
    // Photo transformations (Pinch to zoom, pan, rotate)
    val photoScale: Float = 1.0f,
    val photoOffsetX: Float = 0f,
    val photoOffsetY: Float = 0f,
    val photoRotation: Float = 0f,
    // Text overlays & Stickers
    val textOverlays: List<StoryTextOverlay> = emptyList(),
    val editingTextOverlay: StoryTextOverlay? = null,
    val stickers: List<StoryStickerItem> = emptyList(),
    val selectedObjectId: String? = null, // text or sticker ID selected for transforms
    // Drafts
    val drafts: List<com.aura.glasschat.data.model.StoryDraft> = emptyList(),
    val isDraftSaved: Boolean = false,
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
        _state.update {
            it.copy(
                selectedImageUri = uri,
                photoScale = 1.0f,
                photoOffsetX = 0f,
                photoOffsetY = 0f,
                photoRotation = 0f
            )
        }
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

    fun selectObject(id: String?) {
        _state.update { it.copy(selectedObjectId = id) }
    }

    // --- Photo Transformation Methods ---

    fun updatePhotoTransform(pan: Offset, zoom: Float, rotation: Float) {
        _state.update { state ->
            val newScale = (state.photoScale * zoom).coerceIn(0.5f, 5.0f)
            val newRotation = (state.photoRotation + rotation) % 360f
            state.copy(
                photoScale = newScale,
                photoOffsetX = state.photoOffsetX + pan.x,
                photoOffsetY = state.photoOffsetY + pan.y,
                photoRotation = newRotation
            )
        }
    }

    fun resetPhotoTransform() {
        _state.update {
            it.copy(
                photoScale = 1.0f,
                photoOffsetX = 0f,
                photoOffsetY = 0f,
                photoRotation = 0f
            )
        }
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
            backgroundOpacity = 1.0f,
            alignment = "CENTER",
            fontSize = 28f,
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

    fun updateTextOverlayTransform(id: String, deltaX: Float, deltaY: Float, scaleFactor: Float, rotationDelta: Float) {
        _state.update { state ->
            val updated = state.textOverlays.map { overlay ->
                if (overlay.id == id) {
                    val newX = (overlay.x + deltaX).coerceIn(0.05f, 0.95f)
                    val newY = (overlay.y + deltaY).coerceIn(0.05f, 0.95f)
                    val newScale = (overlay.scale * scaleFactor).coerceIn(0.4f, 4.0f)
                    val newRotation = (overlay.rotation + rotationDelta) % 360f
                    overlay.copy(x = newX, y = newY, scale = newScale, rotation = newRotation)
                } else overlay
            }
            state.copy(textOverlays = updated)
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
            state.copy(
                textOverlays = state.textOverlays.filter { it.id != id },
                selectedObjectId = if (state.selectedObjectId == id) null else state.selectedObjectId
            )
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

    fun updateStickerTransform(id: String, deltaX: Float, deltaY: Float, scaleFactor: Float, rotationDelta: Float) {
        _state.update { state ->
            val updated = state.stickers.map { sticker ->
                if (sticker.id == id) {
                    val newX = (sticker.x + deltaX).coerceIn(0.05f, 0.95f)
                    val newY = (sticker.y + deltaY).coerceIn(0.05f, 0.95f)
                    val newScale = (sticker.scale * scaleFactor).coerceIn(0.4f, 4.0f)
                    val newRotation = (sticker.rotation + rotationDelta) % 360f
                    sticker.copy(x = newX, y = newY, scale = newScale, rotation = newRotation)
                } else sticker
            }
            state.copy(stickers = updated)
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
            state.copy(
                stickers = state.stickers.filter { it.id != id },
                selectedObjectId = if (state.selectedObjectId == id) null else state.selectedObjectId
            )
        }
    }

    // --- Story Drafts Persistence ---

    fun loadDrafts(context: Context) {
        val uid = authRepository.currentUserId
        if (uid.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("buddys_story_drafts_$uid", Context.MODE_PRIVATE)
            val draftsJson = prefs.getString("saved_drafts", "[]") ?: "[]"
            try {
                val jsonArray = org.json.JSONArray(draftsJson)
                val list = mutableListOf<com.aura.glasschat.data.model.StoryDraft>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawText = obj.optJSONArray("textOverlays")
                    val textList = mutableListOf<StoryTextOverlay>()
                    if (rawText != null) {
                        for (t in 0 until rawText.length()) {
                            val tObj = rawText.getJSONObject(t)
                            val tMap = mutableMapOf<String, Any?>()
                            tObj.keys().forEach { k -> tMap[k] = tObj.get(k) }
                            textList.add(StoryTextOverlay.fromMap(tMap))
                        }
                    }

                    val rawStickers = obj.optJSONArray("stickers")
                    val stickerList = mutableListOf<StoryStickerItem>()
                    if (rawStickers != null) {
                        for (s in 0 until rawStickers.length()) {
                            val sObj = rawStickers.getJSONObject(s)
                            val sMap = mutableMapOf<String, Any?>()
                            sObj.keys().forEach { k -> sMap[k] = sObj.get(k) }
                            stickerList.add(StoryStickerItem.fromMap(sMap))
                        }
                    }

                    list.add(
                        com.aura.glasschat.data.model.StoryDraft(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = uid,
                            imageUriString = obj.optString("imageUriString", ""),
                            caption = obj.optString("caption", ""),
                            filterName = obj.optString("filterName", "NORMAL"),
                            textOverlays = textList,
                            stickers = stickerList,
                            audience = obj.optString("audience", "EVERYONE"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                _state.update { it.copy(drafts = list.sortedByDescending { d -> d.updatedAt }) }
            } catch (_: Exception) {}
        }
    }

    fun saveCurrentDraft(context: Context) {
        val uid = authRepository.currentUserId
        val uri = _state.value.selectedImageUri ?: return
        if (uid.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val draft = com.aura.glasschat.data.model.StoryDraft(
                id = UUID.randomUUID().toString(),
                userId = uid,
                imageUriString = uri.toString(),
                caption = _state.value.caption,
                filterName = _state.value.selectedFilter.name,
                textOverlays = _state.value.textOverlays,
                stickers = _state.value.stickers,
                audience = _state.value.audience,
                updatedAt = System.currentTimeMillis()
            )

            val currentDrafts = _state.value.drafts.filter { it.imageUriString != uri.toString() } + draft
            val jsonArray = org.json.JSONArray()
            for (d in currentDrafts) {
                val obj = org.json.JSONObject()
                obj.put("id", d.id)
                obj.put("imageUriString", d.imageUriString)
                obj.put("caption", d.caption)
                obj.put("filterName", d.filterName)
                obj.put("audience", d.audience)
                obj.put("updatedAt", d.updatedAt)

                val textArr = org.json.JSONArray()
                d.textOverlays.forEach { t ->
                    val tObj = org.json.JSONObject(t.toMap())
                    textArr.put(tObj)
                }
                obj.put("textOverlays", textArr)

                val stickerArr = org.json.JSONArray()
                d.stickers.forEach { s ->
                    val sObj = org.json.JSONObject(s.toMap())
                    stickerArr.put(sObj)
                }
                obj.put("stickers", stickerArr)
                jsonArray.put(obj)
            }

            val prefs = context.getSharedPreferences("buddys_story_drafts_$uid", Context.MODE_PRIVATE)
            prefs.edit().putString("saved_drafts", jsonArray.toString()).apply()

            _state.update { it.copy(drafts = currentDrafts.sortedByDescending { d -> d.updatedAt }, isDraftSaved = true) }
        }
    }

    fun resumeDraft(draft: com.aura.glasschat.data.model.StoryDraft) {
        try {
            val uri = Uri.parse(draft.imageUriString)
            val filter = StoryFilter.values().find { it.name == draft.filterName } ?: StoryFilter.NORMAL
            _state.update {
                it.copy(
                    selectedImageUri = uri,
                    selectedFilter = filter,
                    caption = draft.caption,
                    textOverlays = draft.textOverlays,
                    stickers = draft.stickers,
                    audience = draft.audience,
                    activeTool = StudioTool.NONE
                )
            }
        } catch (_: Exception) {}
    }

    fun deleteDraft(context: Context, draftId: String) {
        val uid = authRepository.currentUserId
        if (uid.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedList = _state.value.drafts.filter { it.id != draftId }
            val jsonArray = org.json.JSONArray()
            for (d in updatedList) {
                val obj = org.json.JSONObject()
                obj.put("id", d.id)
                obj.put("imageUriString", d.imageUriString)
                obj.put("caption", d.caption)
                obj.put("filterName", d.filterName)
                obj.put("audience", d.audience)
                obj.put("updatedAt", d.updatedAt)
                jsonArray.put(obj)
            }
            val prefs = context.getSharedPreferences("buddys_story_drafts_$uid", Context.MODE_PRIVATE)
            prefs.edit().putString("saved_drafts", jsonArray.toString()).apply()
            _state.update { it.copy(drafts = updatedList) }
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

        // 3. Draw background image with center-crop fill + Photo Transformations (pan/zoom/rotate)
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

        canvas.save()
        val photoTransX = state.photoOffsetX * (targetWidth / 360f)
        val photoTransY = state.photoOffsetY * (targetHeight / 640f)
        canvas.translate(targetWidth / 2f + photoTransX, targetHeight / 2f + photoTransY)
        canvas.rotate(state.photoRotation)
        canvas.scale(state.photoScale, state.photoScale)
        canvas.translate(-targetWidth / 2f, -targetHeight / 2f)
        canvas.drawBitmap(rawBitmap, srcRect, dstRect, bgPaint)
        canvas.restore()
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
                "POLL" -> {
                    val question = sticker.data["question"] ?: "Ask a question..."
                    stickerBgPaint.color = android.graphics.Color.parseColor("#1E1E28")
                    val cardWidth = 460f
                    val cardHeight = 240f
                    val r = RectF(-cardWidth / 2f, -cardHeight / 2f, cardWidth / 2f, cardHeight / 2f)
                    canvas.drawRoundRect(r, 36f, 36f, stickerBgPaint)

                    // Question text
                    stickerTextPaint.textSize = 42f
                    stickerTextPaint.color = android.graphics.Color.WHITE
                    canvas.drawText(question, 0f, -40f, stickerTextPaint)

                    // Option A: YES (Green Pill)
                    stickerBgPaint.color = android.graphics.Color.parseColor("#00E676")
                    val yesRect = RectF(-cardWidth / 2f + 24f, 10f, -12f, 80f)
                    canvas.drawRoundRect(yesRect, 24f, 24f, stickerBgPaint)
                    stickerTextPaint.textSize = 34f
                    stickerTextPaint.color = android.graphics.Color.BLACK
                    canvas.drawText("YES", (-cardWidth / 2f + 24f - 12f) / 2f, 56f, stickerTextPaint)

                    // Option B: NO (Red Pill)
                    stickerBgPaint.color = android.graphics.Color.parseColor("#FF2A42")
                    val noRect = RectF(12f, 10f, cardWidth / 2f - 24f, 80f)
                    canvas.drawRoundRect(noRect, 24f, 24f, stickerBgPaint)
                    stickerTextPaint.color = android.graphics.Color.WHITE
                    canvas.drawText("NO", (12f + cardWidth / 2f - 24f) / 2f, 56f, stickerTextPaint)
                }
                "QUESTION" -> {
                    val prompt = sticker.data["prompt"] ?: "Ask me a question"
                    stickerBgPaint.color = android.graphics.Color.WHITE
                    val cardWidth = 480f
                    val cardHeight = 200f
                    val r = RectF(-cardWidth / 2f, -cardHeight / 2f, cardWidth / 2f, cardHeight / 2f)
                    canvas.drawRoundRect(r, 36f, 36f, stickerBgPaint)

                    // Top header
                    stickerTextPaint.textSize = 38f
                    stickerTextPaint.color = android.graphics.Color.BLACK
                    canvas.drawText(prompt, 0f, -20f, stickerTextPaint)

                    // Sub-pill
                    stickerBgPaint.color = android.graphics.Color.parseColor("#F0F0F5")
                    val subRect = RectF(-cardWidth / 2f + 30f, 15f, cardWidth / 2f - 30f, 75f)
                    canvas.drawRoundRect(subRect, 20f, 20f, stickerBgPaint)
                    stickerTextPaint.textSize = 28f
                    stickerTextPaint.color = android.graphics.Color.GRAY
                    canvas.drawText("Type something...", 0f, 52f, stickerTextPaint)
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
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
            val computedSize = (overlay.fontSize * (targetWidth / 440f)).coerceAtLeast(36f)
            overlayPaint.textSize = computedSize

            when (overlay.alignment) {
                "LEFT" -> overlayPaint.textAlign = Paint.Align.LEFT
                "RIGHT" -> overlayPaint.textAlign = Paint.Align.RIGHT
                else -> overlayPaint.textAlign = Paint.Align.CENTER
            }

            when (overlay.style) {
                "MODERN" -> {
                    overlayPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    overlayPaint.letterSpacing = 0.08f
                    overlayPaint.maskFilter = null
                    overlayPaint.clearShadowLayer()
                }
                "NEON" -> {
                    overlayPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    overlayPaint.letterSpacing = 0f
                    overlayPaint.setShadowLayer(24f, 0f, 0f, overlay.color.toInt())
                }
                "TYPEWRITER" -> {
                    overlayPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                    overlayPaint.letterSpacing = 0f
                    overlayPaint.clearShadowLayer()
                }
                "STRONG" -> {
                    overlayPaint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                    overlayPaint.letterSpacing = 0f
                    overlayPaint.clearShadowLayer()
                }
                "MINIMAL" -> {
                    overlayPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    overlayPaint.letterSpacing = 0.05f
                    overlayPaint.clearShadowLayer()
                }
                else -> { // CLASSIC
                    overlayPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    overlayPaint.letterSpacing = 0f
                    overlayPaint.clearShadowLayer()
                }
            }

            val textBounds = Rect()
            overlayPaint.getTextBounds(overlay.text, 0, overlay.text.length, textBounds)

            if (overlay.backgroundColor != 0L && overlay.backgroundColor != 0x00000000L) {
                val rawColor = overlay.backgroundColor.toInt()
                val alpha = (overlay.backgroundOpacity * 255).toInt().coerceIn(0, 255)
                val finalBgColor = android.graphics.Color.argb(
                    alpha,
                    android.graphics.Color.red(rawColor),
                    android.graphics.Color.green(rawColor),
                    android.graphics.Color.blue(rawColor)
                )
                overlayBgPaint.color = finalBgColor
                val padX = 40f
                val padY = 24f
                val left = when (overlay.alignment) {
                    "LEFT" -> -padX
                    "RIGHT" -> -textBounds.width() - padX
                    else -> -textBounds.width() / 2f - padX
                }
                val r = RectF(
                    left,
                    -textBounds.height() - padY / 2f,
                    left + textBounds.width() + (padX * 2),
                    padY / 2f + 8f
                )
                canvas.drawRoundRect(r, 24f, 24f, overlayBgPaint)
            }

            val drawX = when (overlay.alignment) {
                "LEFT" -> 0f
                "RIGHT" -> 0f
                else -> 0f
            }
            canvas.drawText(overlay.text, drawX, 0f, overlayPaint)
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

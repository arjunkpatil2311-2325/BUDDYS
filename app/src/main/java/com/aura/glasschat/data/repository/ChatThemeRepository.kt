package com.aura.glasschat.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Dedicated local repository for managing custom per-chat wallpaper backgrounds.
 * Stores compressed JPEG wallpaper files in internal app storage (isolated per chatId).
 * Never uploads wallpapers to Firebase.
 */
class ChatThemeRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("buddys_chat_themes", Context.MODE_PRIVATE)
    private val wallpapersDir = File(context.filesDir, "chat_wallpapers").apply {
        if (!exists()) mkdirs()
    }

    private val _backgroundFlows = mutableMapOf<String, MutableStateFlow<String?>>()

    fun getBackgroundFlow(chatId: String): StateFlow<String?> {
        synchronized(_backgroundFlows) {
            return _backgroundFlows.getOrPut(chatId) {
                MutableStateFlow(getSavedBackgroundPath(chatId))
            }.asStateFlow()
        }
    }

    fun getSavedBackgroundPath(chatId: String): String? {
        val savedPath = prefs.getString("bg_$chatId", null) ?: return null
        val file = File(savedPath)
        return if (file.exists() && file.length() > 0) {
            savedPath
        } else {
            // Clean up invalid reference
            prefs.edit().remove("bg_$chatId").apply()
            null
        }
    }

    /**
     * Efficiently decodes, rotates, scales down, and saves the image to internal app storage.
     * Prevents loading huge 4K/8K images into memory and ensures smooth scrolling.
     */
    suspend fun saveChatBackground(chatId: String, sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(wallpapersDir, "bg_${chatId}.jpg")
            
            // Read stream once into memory to safely handle PhotoPicker URIs
            val rawBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("Cannot read image from URI"))

            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(Exception("Selected image is empty"))
            }

            // 1. Decode bounds to calculate inSampleSize (Max 1080x2400 for crisp yet fast display)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

            val reqWidth = 1080
            val reqHeight = 2400
            val width = options.outWidth
            val height = options.outHeight

            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            // 2. Decode bitmap with downsampling
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = max(1, inSampleSize)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return@withContext Result.failure(Exception("Failed to decode image bitmap"))

            // 3. Handle EXIF rotation
            val rotationDegrees = getExifOrientation(rawBytes)
            val finalBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
            } else {
                decodedBitmap
            }

            // 4. Save as high-quality compressed JPEG
            FileOutputStream(targetFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }

            if (finalBitmap != decodedBitmap) {
                decodedBitmap.recycle()
            }
            finalBitmap.recycle()

            val absolutePath = targetFile.absolutePath
            prefs.edit().putString("bg_$chatId", absolutePath).apply()

            synchronized(_backgroundFlows) {
                _backgroundFlows[chatId]?.value = absolutePath
            }

            Result.success(absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes the custom wallpaper for the chat and deletes the local file.
     */
    suspend fun removeChatBackground(chatId: String): Unit = withContext(Dispatchers.IO) {
        val targetFile = File(wallpapersDir, "bg_${chatId}.jpg")
        if (targetFile.exists()) {
            targetFile.delete()
        }
        prefs.edit().remove("bg_$chatId").apply()
        synchronized(_backgroundFlows) {
            _backgroundFlows[chatId]?.value = null
        }
    }

    private fun getExifOrientation(rawBytes: ByteArray): Int {
        return try {
            ByteArrayInputStream(rawBytes).use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {
            0
        }
    }
}

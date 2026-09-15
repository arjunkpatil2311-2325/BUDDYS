package com.aura.glasschat.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.math.max

class MediaRepository(
    private val mediaStorageRepository: MediaStorageRepository = SupabaseMediaStorageRepository.getInstance()
) {

    companion object {
        private const val MAX_IMAGE_DIMENSION = 1280
        private const val MAX_FILE_SIZE_BYTES = 800 * 1024L // 800 KB max target for images
        private const val ABSOLUTE_MAX_MEDIA_BYTES = 10 * 1024 * 1024L // 10 MB limit
    }

    /**
     * Efficiently decodes and compresses an image from Uri with downsampling and rotation correction.
     * Safely reads input stream once into memory to support single-use PhotoPicker URIs.
     */
    suspend fun compressImage(context: Context, uri: Uri): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // Read the stream once into memory to avoid security/closure issues with PhotoPicker URIs
            val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalArgumentException("Cannot read image data from URI"))

            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Selected image is empty"))
            }

            // 1. Read bounds only
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid image dimensions"))
            }

            // 2. Compute sample size
            var inSampleSize = 1
            val maxDim = max(originalWidth, originalHeight)
            while (maxDim / inSampleSize > MAX_IMAGE_DIMENSION) {
                inSampleSize *= 2
            }

            // 3. Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Efficient memory footprint
            }
            var bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return@withContext Result.failure(IllegalArgumentException("Failed to decode image"))

            // 4. Correct EXIF orientation
            try {
                java.io.ByteArrayInputStream(rawBytes).use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    }
                    if (!matrix.isIdentity) {
                        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        if (rotated != bitmap) {
                            bitmap.recycle()
                            bitmap = rotated
                        }
                    }
                }
            } catch (_: Exception) {}

            // 5. Compress to JPEG
            var quality = 85
            var stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            var bytes = stream.toByteArray()

            // If still too large, further reduce quality
            while (bytes.size > MAX_FILE_SIZE_BYTES && quality > 40) {
                quality -= 15
                stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                bytes = stream.toByteArray()
            }

            bitmap.recycle()
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads compressed image bytes to Supabase Storage:
     * chat_media/{chatId}/{messageId}/image.jpg
     */
    suspend fun uploadChatImage(
        chatId: String,
        messageId: String,
        imageBytes: ByteArray,
        senderId: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Image data is empty"))
        }
        if (imageBytes.size > ABSOLUTE_MAX_MEDIA_BYTES) {
            return@withContext Result.failure(IllegalArgumentException("Image exceeds 10MB limit (${imageBytes.size / 1024} KB)"))
        }

        val resolvedSender = if (senderId.isNotBlank()) senderId else (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
        android.util.Log.d("BUDDYS_STORAGE", "Uploading chat image via MediaStorageRepository: path=chat_media/$chatId/$messageId/image.jpg, size=${imageBytes.size} bytes, sender=$resolvedSender")

        val result = mediaStorageRepository.uploadChatImage(chatId, messageId, imageBytes, resolvedSender)
        result.fold(
            onSuccess = { downloadUrl ->
                android.util.Log.d("BUDDYS_STORAGE", "Chat image uploaded successfully: $downloadUrl")
                Result.success(downloadUrl)
            },
            onFailure = { e ->
                android.util.Log.e("BUDDYS_STORAGE", "Chat image upload failed: ${e.message}", e)
                val friendlyMsg = com.aura.glasschat.util.ChatUtils.getFriendlyStorageErrorMessage(e)
                Result.failure(Exception(friendlyMsg, e))
            }
        )
    }

    /**
     * Uploads an audio voice recording to Supabase Storage:
     * voice_messages/{chatId}/{messageId}/voice.m4a
     */
    suspend fun uploadChatVoice(
        chatId: String,
        messageId: String,
        audioFile: File,
        senderId: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Recorded audio file is empty or missing"))
        }
        if (audioFile.length() > ABSOLUTE_MAX_MEDIA_BYTES) {
            return@withContext Result.failure(IllegalArgumentException("Voice message exceeds size limit"))
        }

        val resolvedSender = if (senderId.isNotBlank()) senderId else (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
        android.util.Log.d(
            "BUDDYS_STORAGE",
            "Uploading voice recording: path=voice_messages/$chatId/$messageId/voice.m4a, localPath=${audioFile.absolutePath}, size=${audioFile.length()} bytes, MIME=audio/mp4, sender=$resolvedSender"
        )

        val result = mediaStorageRepository.uploadChatVoice(chatId, messageId, audioFile, resolvedSender)
        result.fold(
            onSuccess = { downloadUrl ->
                android.util.Log.d("BUDDYS_STORAGE", "Voice note uploaded successfully: $downloadUrl")
                Result.success(downloadUrl)
            },
            onFailure = { e ->
                android.util.Log.e("BUDDYS_STORAGE", "Voice note upload failed: ${e.message}", e)
                val friendlyMsg = com.aura.glasschat.util.ChatUtils.getFriendlyStorageErrorMessage(e)
                Result.failure(Exception(friendlyMsg, e))
            }
        )
    }

    /**
     * Deletes media associated with a message when it is unsent.
     */
    suspend fun deleteChatMedia(chatId: String, messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        mediaStorageRepository.deleteChatMedia(chatId, messageId)
    }
}

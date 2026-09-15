package com.aura.glasschat.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object MediaStoreUtils {

    /**
     * Saves an image byte array to the Android MediaStore under the dedicated 'Buddies' album ("Pictures/Buddies").
     * Returns the Uri of the saved image, or null on failure.
     */
    fun saveImageToBuddiesAlbum(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "BUDDIES_" + System.currentTimeMillis() + ".jpg",
        mimeType: String = "image/jpeg"
    ): Uri? {
        return try {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Buddies")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    outputStream.write(imageBytes)
                    outputStream.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                imageUri
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val buddiesDir = File(picturesDir, "Buddies")
                if (!buddiesDir.exists()) {
                    buddiesDir.mkdirs()
                }

                val imageFile = File(buddiesDir, filename)
                FileOutputStream(imageFile).use { outputStream ->
                    outputStream.write(imageBytes)
                    outputStream.flush()
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                }
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

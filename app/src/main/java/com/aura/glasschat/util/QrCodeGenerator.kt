package com.aura.glasschat.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrCodeGenerator {

    /**
     * Generates a high-contrast QR code bitmap for a given content string.
     * Uses error correction level M and zero-margin encoding for crisp rendering.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }

            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates standard safe BUDDYS profile URI: buddys://profile/{username}
     */
    fun getProfileDeepLink(username: String): String {
        val clean = UsernameUtils.normalize(username)
        return "buddys://profile/$clean"
    }

    /**
     * Extracts username from a scanned BUDDYS QR code payload.
     * Supports:
     * - buddys://profile/{username}
     * - https://buddys.app/{username}
     * - @{username}
     * - {username}
     */
    fun parseScannedPayload(payload: String): String? {
        val trimmed = payload.trim()
        if (trimmed.isBlank()) return null

        return when {
            trimmed.startsWith("buddys://profile/") -> {
                trimmed.removePrefix("buddys://profile/").trim().removePrefix("@")
            }
            trimmed.startsWith("https://buddys.app/") -> {
                trimmed.removePrefix("https://buddys.app/").trim().removePrefix("@")
            }
            trimmed.startsWith("@") -> {
                trimmed.removePrefix("@").trim()
            }
            trimmed.length in 3..30 && !trimmed.contains(" ") && !trimmed.contains("/") -> {
                trimmed
            }
            else -> null
        }
    }
}

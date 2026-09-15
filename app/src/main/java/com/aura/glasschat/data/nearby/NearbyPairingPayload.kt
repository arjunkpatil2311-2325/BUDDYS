package com.aura.glasschat.data.nearby

import org.json.JSONObject
import java.security.SecureRandom
import java.util.UUID

enum class NearbyPayloadType {
    OFFER,            // Initial discovery announcement
    CONFIRM_REQUEST,  // User tapped "Connect"
    CONFIRM_ACCEPT,   // Both sides accepted connection
    CANCEL,           // User tapped "Cancel" or left screen
    REJECT,           // Explicitly declined pairing
    MEDIA_METADATA    // Metadata header before direct P2P media transfer
}

data class NearbyPairingPayload(
    val type: NearbyPayloadType,
    val sessionId: String,
    val senderUid: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val nonce: String = generateNonce(),
    val fileName: String? = null,
    val mimeType: String? = null,
    val mediaSize: Long = 0L
) {
    companion object {
        private const val MAX_PAYLOAD_AGE_MS = 2 * 60 * 1000L // 2 minutes

        private fun generateNonce(): String {
            val random = SecureRandom()
            val bytes = ByteArray(16)
            random.nextBytes(bytes)
            return bytes.joinToString("") { java.lang.String.format("%02x", it) }
        }

        fun createOffer(
            senderUid: String,
            displayName: String,
            username: String,
            avatarUrl: String?,
            sessionId: String = UUID.randomUUID().toString()
        ): NearbyPairingPayload {
            return NearbyPairingPayload(
                type = NearbyPayloadType.OFFER,
                sessionId = sessionId,
                senderUid = senderUid,
                displayName = displayName.trim(),
                username = username.trim(),
                avatarUrl = avatarUrl
            )
        }

        fun createMediaMetadata(
            senderUid: String,
            displayName: String,
            username: String,
            fileName: String,
            mimeType: String,
            mediaSize: Long,
            sessionId: String = UUID.randomUUID().toString()
        ): NearbyPairingPayload {
            return NearbyPairingPayload(
                type = NearbyPayloadType.MEDIA_METADATA,
                sessionId = sessionId,
                senderUid = senderUid,
                displayName = displayName.trim(),
                username = username.trim(),
                fileName = fileName,
                mimeType = mimeType,
                mediaSize = mediaSize
            )
        }

        fun fromJson(jsonStr: String): NearbyPairingPayload? {
            return try {
                val json = JSONObject(jsonStr)
                val typeStr = json.optString("type")
                val type = NearbyPayloadType.valueOf(typeStr)
                val sessionId = json.getString("sessionId")
                val senderUid = json.getString("senderUid")
                val displayName = json.optString("displayName", "Buddy")
                val username = json.optString("username", "")
                val avatarUrl = json.optString("avatarUrl").takeIf { it.isNotBlank() && it != "null" }
                val timestamp = json.getLong("timestamp")
                val nonce = json.optString("nonce", "")
                val fileName = json.optString("fileName").takeIf { it.isNotBlank() && it != "null" }
                val mimeType = json.optString("mimeType").takeIf { it.isNotBlank() && it != "null" }
                val mediaSize = json.optLong("mediaSize", 0L)

                // Validate sanity constraints
                if (sessionId.isBlank() || senderUid.isBlank()) return null
                if (System.currentTimeMillis() - timestamp > MAX_PAYLOAD_AGE_MS) return null

                NearbyPairingPayload(
                    type = type,
                    sessionId = sessionId,
                    senderUid = senderUid,
                    displayName = displayName,
                    username = username,
                    avatarUrl = avatarUrl,
                    timestamp = timestamp,
                    nonce = nonce,
                    fileName = fileName,
                    mimeType = mimeType,
                    mediaSize = mediaSize
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type.name)
        json.put("sessionId", sessionId)
        json.put("senderUid", senderUid)
        json.put("displayName", displayName)
        json.put("username", username)
        json.put("avatarUrl", avatarUrl ?: "")
        json.put("timestamp", timestamp)
        json.put("nonce", nonce)
        fileName?.let { json.put("fileName", it) }
        mimeType?.let { json.put("mimeType", it) }
        if (mediaSize > 0) json.put("mediaSize", mediaSize)
        return json.toString()
    }

    fun toByteArray(): ByteArray = toJson().toByteArray(Charsets.UTF_8)
}

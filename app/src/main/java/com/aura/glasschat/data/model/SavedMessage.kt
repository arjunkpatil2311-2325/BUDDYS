package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class SavedMessage(
    val id: String = "",
    val messageId: String = "",
    val chatId: String = "",
    val otherUserId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String? = null,
    val content: String = "",
    val type: String = "TEXT",
    val mediaUrl: String? = null,
    val durationMs: Long? = null,
    val messageTimestamp: Timestamp? = null,
    @ServerTimestamp
    val savedAt: Timestamp? = null
) {
    val isImageMessage: Boolean get() = type == "IMAGE"
    val isVoiceMessage: Boolean get() = type == "VOICE"

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "messageId" to messageId,
        "chatId" to chatId,
        "otherUserId" to otherUserId,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderAvatarUrl" to senderAvatarUrl,
        "content" to content,
        "type" to type,
        "mediaUrl" to mediaUrl,
        "durationMs" to durationMs,
        "messageTimestamp" to (messageTimestamp ?: Timestamp.now()),
        "savedAt" to (savedAt ?: Timestamp.now())
    )
}

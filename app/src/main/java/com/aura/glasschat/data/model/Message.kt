package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val type: String = "TEXT", // "TEXT", "IMAGE", "VOICE"
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    
    // Status tracking: "SENDING", "SENT", "DELIVERED", "SEEN"
    val status: String = "SENT",
    @ServerTimestamp
    val deliveredAt: Timestamp? = null,
    @ServerTimestamp
    val seenAt: Timestamp? = null,

    // Editing & Unsending
    val isEdited: Boolean = false,
    @ServerTimestamp
    val editedAt: Timestamp? = null,
    val isUnsent: Boolean = false,

    // Reply support
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val replyToSenderId: String? = null,

    // Reactions: Map of userId -> emoji (e.g. "uid123" -> "❤️")
    val reactions: Map<String, String> = emptyMap(),

    // Delete for me: List of user IDs who deleted this message for themselves
    val deletedForUsers: List<String> = emptyList(),

    // Phase 3 Upgrades:
    val isForwarded: Boolean = false,
    val storyReplyPreviewUrl: String? = null,
    val failedToSend: Boolean = false
) {
    val text: String get() = content
    val messageId: String get() = id
    val isImageMessage: Boolean get() = type == "IMAGE"
    val isVoiceMessage: Boolean get() = type == "VOICE"
    val isMissedCallMessage: Boolean get() = type == "CALL_MISSED_VOICE" || type == "CALL_MISSED_VIDEO"
    val isStoryReplyMessage: Boolean get() = type == "STORY_REPLY"

    fun isVisibleToUser(userId: String): Boolean {
        return !isUnsent && !deletedForUsers.contains(userId)
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "chatId" to chatId,
        "senderId" to senderId,
        "senderName" to senderName,
        "content" to content,
        "type" to type,
        "timestamp" to (timestamp ?: Timestamp.now()),
        "mediaUrl" to mediaUrl,
        "thumbnailUrl" to thumbnailUrl,
        "durationMs" to durationMs,
        "status" to status,
        "deliveredAt" to deliveredAt,
        "seenAt" to seenAt,
        "isEdited" to isEdited,
        "editedAt" to editedAt,
        "isUnsent" to isUnsent,
        "replyToMessageId" to replyToMessageId,
        "replyToText" to replyToText,
        "replyToSenderName" to replyToSenderName,
        "replyToSenderId" to replyToSenderId,
        "reactions" to reactions,
        "deletedForUsers" to deletedForUsers,
        "isForwarded" to isForwarded,
        "storyReplyPreviewUrl" to storyReplyPreviewUrl
    )
}

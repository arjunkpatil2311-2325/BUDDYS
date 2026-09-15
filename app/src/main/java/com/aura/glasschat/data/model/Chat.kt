package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ParticipantInfo(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "username" to username,
        "avatarUrl" to avatarUrl
    )
}

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val participantInfo: Map<String, Map<String, Any?>> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp
    val lastMessageTimestamp: Timestamp? = null,
    val lastReadAt: Map<String, Timestamp> = emptyMap(),
    val typingUsers: Map<String, Boolean> = emptyMap(),
    val pinnedBy: List<String> = emptyList(),
    val mutedBy: List<String> = emptyList(),
    val pinnedMessageIds: List<String> = emptyList(),
    val lockedBy: List<String> = emptyList(),
    val hiddenBy: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun getOtherParticipantUid(currentUid: String): String {
        return participants.firstOrNull { it != currentUid } ?: ""
    }

    fun getOtherParticipantInfo(currentUid: String): ParticipantInfo {
        val otherUid = getOtherParticipantUid(currentUid)
        val map = participantInfo[otherUid] ?: return ParticipantInfo(uid = otherUid)
        return ParticipantInfo(
            uid = otherUid,
            displayName = map["displayName"] as? String ?: "User",
            username = map["username"] as? String ?: "",
            avatarUrl = map["avatarUrl"] as? String
        )
    }

    fun isUserTyping(userId: String): Boolean {
        return typingUsers[userId] == true
    }

    fun isPinned(userId: String): Boolean {
        return pinnedBy.contains(userId)
    }

    fun isMuted(userId: String): Boolean {
        return mutedBy.contains(userId)
    }

    fun isLocked(userId: String): Boolean {
        return lockedBy.contains(userId)
    }

    fun isHidden(userId: String): Boolean {
        return hiddenBy.contains(userId)
    }

    fun isMessagePinned(messageId: String): Boolean {
        return pinnedMessageIds.contains(messageId)
    }

    fun hasUnread(currentUid: String): Boolean {
        val lastTimestamp = lastMessageTimestamp ?: return false
        if (lastMessageSenderId == currentUid) return false
        val userReadTime = lastReadAt[currentUid] ?: return true
        return lastTimestamp.seconds > userReadTime.seconds
    }
}

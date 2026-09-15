package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Story(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userDisplayName: String = "",
    val userAvatarUrl: String? = null,
    val mediaUrl: String = "",
    val caption: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val viewedBy: List<String> = emptyList(),

    // Phase 3 Upgrades:
    val audience: String = "EVERYONE", // "EVERYONE", "FRIENDS", "CLOSE_FRIENDS"
    val closeFriends: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap() // userId -> emoji
) {
    val isExpired: Boolean
        get() {
            val exp = expiresAt ?: return false
            return Timestamp.now().seconds > exp.seconds
        }

    val isCloseFriendsOnly: Boolean
        get() = audience == "CLOSE_FRIENDS"

    fun isViewedBy(uid: String): Boolean {
        return viewedBy.contains(uid)
    }

    fun isVisibleTo(uid: String, isFriend: Boolean = true, isCloseFriend: Boolean = false): Boolean {
        if (userId == uid) return true
        if (isExpired) return false
        return when (audience) {
            "CLOSE_FRIENDS" -> isCloseFriend || closeFriends.contains(uid)
            "FRIENDS" -> isFriend
            else -> true
        }
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "username" to username,
        "userDisplayName" to userDisplayName,
        "userAvatarUrl" to userAvatarUrl,
        "mediaUrl" to mediaUrl,
        "caption" to caption,
        "createdAt" to (createdAt ?: Timestamp.now()),
        "expiresAt" to (expiresAt ?: Timestamp(Timestamp.now().seconds + 86400, 0)),
        "viewedBy" to viewedBy,
        "audience" to audience,
        "closeFriends" to closeFriends,
        "reactions" to reactions
    )
}

data class UserStories(
    val userId: String,
    val username: String,
    val userDisplayName: String,
    val userAvatarUrl: String?,
    val stories: List<Story>
) {
    val hasUnread: Boolean
        get() = stories.any { it.viewedBy.isEmpty() }

    fun hasUnreadFor(currentUid: String): Boolean {
        return stories.any { !it.isViewedBy(currentUid) }
    }
}

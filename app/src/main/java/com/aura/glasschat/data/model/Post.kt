package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userDisplayName: String = "",
    val userAvatarUrl: String? = null,
    val mediaUrl: String = "",
    val mediaType: String = "IMAGE",
    val caption: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likes: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val pinnedAt: Timestamp? = null
) {
    fun isLikedBy(currentUid: String): Boolean = likes.contains(currentUid)

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "username" to username,
        "userDisplayName" to userDisplayName,
        "userAvatarUrl" to userAvatarUrl,
        "mediaUrl" to mediaUrl,
        "mediaType" to mediaType,
        "caption" to caption,
        "createdAt" to (createdAt ?: Timestamp.now()),
        "likeCount" to likeCount,
        "commentCount" to commentCount,
        "likes" to likes,
        "isPinned" to isPinned,
        "pinnedAt" to pinnedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Post {
            @Suppress("UNCHECKED_CAST")
            val likesList = map["likes"] as? List<String> ?: emptyList()
            return Post(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                username = map["username"] as? String ?: "",
                userDisplayName = map["userDisplayName"] as? String ?: "",
                userAvatarUrl = map["userAvatarUrl"] as? String,
                mediaUrl = map["mediaUrl"] as? String ?: "",
                mediaType = map["mediaType"] as? String ?: "IMAGE",
                caption = map["caption"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp,
                likeCount = (map["likeCount"] as? Number)?.toInt() ?: likesList.size,
                commentCount = (map["commentCount"] as? Number)?.toInt() ?: 0,
                likes = likesList,
                isPinned = map["isPinned"] as? Boolean ?: false,
                pinnedAt = map["pinnedAt"] as? Timestamp
            )
        }
    }
}

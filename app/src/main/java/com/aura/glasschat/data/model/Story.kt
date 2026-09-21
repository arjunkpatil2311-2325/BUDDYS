package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class StoryTextOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val style: String = "CLASSIC", // CLASSIC, MODERN, NEON, TYPEWRITER, STRONG, MINIMAL
    val color: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0x00000000,
    val backgroundOpacity: Float = 1.0f,
    val alignment: String = "CENTER", // LEFT, CENTER, RIGHT
    val fontSize: Float = 28f,
    val shadow: Boolean = false,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "text" to text,
        "style" to style,
        "color" to color,
        "backgroundColor" to backgroundColor,
        "backgroundOpacity" to backgroundOpacity,
        "alignment" to alignment,
        "fontSize" to fontSize,
        "shadow" to shadow,
        "x" to x,
        "y" to y,
        "scale" to scale,
        "rotation" to rotation
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): StoryTextOverlay {
            return StoryTextOverlay(
                id = map["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                text = map["text"] as? String ?: "",
                style = map["style"] as? String ?: "CLASSIC",
                color = (map["color"] as? Number)?.toLong() ?: 0xFFFFFFFF,
                backgroundColor = (map["backgroundColor"] as? Number)?.toLong() ?: 0x00000000,
                backgroundOpacity = (map["backgroundOpacity"] as? Number)?.toFloat() ?: 1.0f,
                alignment = map["alignment"] as? String ?: "CENTER",
                fontSize = (map["fontSize"] as? Number)?.toFloat() ?: 28f,
                shadow = map["shadow"] as? Boolean ?: false,
                x = (map["x"] as? Number)?.toFloat() ?: 0.5f,
                y = (map["y"] as? Number)?.toFloat() ?: 0.5f,
                scale = (map["scale"] as? Number)?.toFloat() ?: 1.0f,
                rotation = (map["rotation"] as? Number)?.toFloat() ?: 0.0f
            )
        }
    }
}

data class StoryDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "",
    val imageUriString: String = "",
    val caption: String = "",
    val filterName: String = "NORMAL",
    val textOverlays: List<StoryTextOverlay> = emptyList(),
    val stickers: List<StoryStickerItem> = emptyList(),
    val audience: String = "EVERYONE",
    val updatedAt: Long = System.currentTimeMillis()
)

data class StoryStickerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String = "EMOJI", // LOCATION, MENTION, TIME, POLL, MUSIC, EMOJI
    val data: Map<String, String> = emptyMap(),
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type,
        "data" to data,
        "x" to x,
        "y" to y,
        "scale" to scale,
        "rotation" to rotation
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): StoryStickerItem {
            @Suppress("UNCHECKED_CAST")
            val rawData = map["data"] as? Map<String, String> ?: emptyMap()
            return StoryStickerItem(
                id = map["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                type = map["type"] as? String ?: "EMOJI",
                data = rawData,
                x = (map["x"] as? Number)?.toFloat() ?: 0.5f,
                y = (map["y"] as? Number)?.toFloat() ?: 0.5f,
                scale = (map["scale"] as? Number)?.toFloat() ?: 1.0f,
                rotation = (map["rotation"] as? Number)?.toFloat() ?: 0.0f
            )
        }
    }
}

data class StoryViewerEntry(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val viewedAt: Timestamp = Timestamp.now(),
    val reaction: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "username" to username,
        "displayName" to displayName,
        "avatarUrl" to avatarUrl,
        "viewedAt" to viewedAt,
        "reaction" to reaction
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): StoryViewerEntry {
            return StoryViewerEntry(
                uid = map["uid"] as? String ?: "",
                username = map["username"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                avatarUrl = map["avatarUrl"] as? String,
                viewedAt = map["viewedAt"] as? Timestamp ?: Timestamp.now(),
                reaction = map["reaction"] as? String
            )
        }
    }
}

data class Story(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userDisplayName: String = "",
    val userAvatarUrl: String? = null,
    val mediaUrl: String = "",
    val mediaType: String = "IMAGE", // IMAGE, VIDEO
    val caption: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val viewedBy: List<String> = emptyList(),

    // Studio Overlays & Shaders
    val textOverlays: List<StoryTextOverlay> = emptyList(),
    val stickers: List<StoryStickerItem> = emptyList(),
    val filterName: String = "NORMAL",
    val drawingPathData: String? = null,

    // Audience & Privacy
    val audience: String = "EVERYONE", // "EVERYONE", "FRIENDS", "CLOSE_FRIENDS"
    val closeFriends: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val viewerDetails: Map<String, StoryViewerEntry> = emptyMap()
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
        "mediaType" to mediaType,
        "caption" to caption,
        "createdAt" to (createdAt ?: Timestamp.now()),
        "expiresAt" to (expiresAt ?: Timestamp(Timestamp.now().seconds + 86400, 0)),
        "viewedBy" to viewedBy,
        "textOverlays" to textOverlays.map { it.toMap() },
        "stickers" to stickers.map { it.toMap() },
        "filterName" to filterName,
        "drawingPathData" to drawingPathData,
        "audience" to audience,
        "closeFriends" to closeFriends,
        "reactions" to reactions,
        "viewerDetails" to viewerDetails.mapValues { it.value.toMap() }
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

package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ProfileHighlight(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val storyIds: List<String> = emptyList(),
    val stories: List<Story> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "title" to title,
        "coverUrl" to coverUrl,
        "storyIds" to storyIds,
        "stories" to stories.map { it.toMap() },
        "createdAt" to (createdAt ?: Timestamp.now()),
        "updatedAt" to (updatedAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ProfileHighlight {
            @Suppress("UNCHECKED_CAST")
            val rawStoryIds = map["storyIds"] as? List<String> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val rawStories = map["stories"] as? List<Map<String, Any?>> ?: emptyList()
            val storiesList = rawStories.mapNotNull { storyMap ->
                try {
                    Story(
                        id = storyMap["id"] as? String ?: "",
                        userId = storyMap["userId"] as? String ?: "",
                        username = storyMap["username"] as? String ?: "",
                        userDisplayName = storyMap["userDisplayName"] as? String ?: "",
                        userAvatarUrl = storyMap["userAvatarUrl"] as? String,
                        mediaUrl = storyMap["mediaUrl"] as? String ?: "",
                        caption = storyMap["caption"] as? String ?: "",
                        createdAt = storyMap["createdAt"] as? Timestamp,
                        expiresAt = storyMap["expiresAt"] as? Timestamp
                    )
                } catch (_: Exception) {
                    null
                }
            }

            return ProfileHighlight(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                title = map["title"] as? String ?: "",
                coverUrl = map["coverUrl"] as? String ?: "",
                storyIds = rawStoryIds,
                stories = storiesList,
                createdAt = map["createdAt"] as? Timestamp,
                updatedAt = map["updatedAt"] as? Timestamp
            )
        }
    }
}

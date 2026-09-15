package com.aura.glasschat.data.repository

import android.content.Context
import android.net.Uri
import com.aura.glasschat.data.model.Story
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.model.UserStories
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val mediaStorageRepository: MediaStorageRepository = SupabaseMediaStorageRepository.getInstance(),
    private val mediaRepository: MediaRepository = MediaRepository()
) {
    companion object {
        const val STORIES_COLLECTION = "stories"
        private const val TAG = "BUDDYS_STORY"
    }

    /**
     * Uploads a new 24-hour story with compressed image, caption, and audience.
     */
    suspend fun uploadStory(
        context: Context,
        imageUri: Uri,
        caption: String,
        user: User,
        audience: String = "EVERYONE"
    ): Result<Story> {
        val storyId = UUID.randomUUID().toString()
        val compressResult = mediaRepository.compressImage(context, imageUri)
        val imageBytes = compressResult.getOrNull()
            ?: return Result.failure(compressResult.exceptionOrNull() ?: Exception("Failed to process story image"))

        android.util.Log.d(TAG, "Starting story upload via MediaStorageRepository: id=$storyId, uid=${user.uid}, size=${imageBytes.size} bytes, audience=$audience")

        val uploadResult = mediaStorageRepository.uploadStoryMedia(
            storyId = storyId,
            userId = user.uid,
            imageBytes = imageBytes,
            audience = audience
        )

        val downloadUrl: String = uploadResult.fold(
            onSuccess = { url ->
                android.util.Log.d(TAG, "Story media uploaded successfully: $url")
                url
            },
            onFailure = { e ->
                android.util.Log.e(TAG, "Story media upload failed: message=${e.message}", e)
                val friendlyMsg = com.aura.glasschat.util.ChatUtils.getFriendlyStorageErrorMessage(e)
                return Result.failure(Exception(friendlyMsg, e))
            }
        )

        return try {
            val now = Timestamp.now()
            val expiresAt = Timestamp(now.seconds + 86400, 0) // 24 hours

            val story = Story(
                id = storyId,
                userId = user.uid,
                username = user.username,
                userDisplayName = user.displayName,
                userAvatarUrl = user.avatarUrl,
                mediaUrl = downloadUrl,
                caption = caption.trim(),
                createdAt = now,
                expiresAt = expiresAt,
                viewedBy = emptyList(),
                audience = audience,
                closeFriends = if (audience == "CLOSE_FRIENDS") user.closeFriends else emptyList(),
                reactions = emptyMap()
            )

            firestore.collection(STORIES_COLLECTION)
                .document(storyId)
                .set(story.toMap())
                .await()

            android.util.Log.d("BUDDYS_STORY", "Story document saved to Firestore: $storyId")
            Result.success(story)
        } catch (e: Exception) {
            android.util.Log.e("BUDDYS_STORY", "Failed to write story document to Firestore", e)
            val friendlyMsg = com.aura.glasschat.util.ChatUtils.getFriendlyFirestoreErrorMessage(e)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    /**
     * Observes all active stories for a given list of user IDs (self + friends/following).
     */
    fun observeActiveStories(currentUid: String): Flow<List<UserStories>> = callbackFlow {
        val now = Timestamp.now()
        val listener = firestore.collection(STORIES_COLLECTION)
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val allStories = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val userId = doc.getString("userId") ?: ""
                        val username = doc.getString("username") ?: ""
                        val userDisplayName = doc.getString("userDisplayName") ?: ""
                        val userAvatarUrl = doc.getString("userAvatarUrl")
                        val mediaUrl = doc.getString("mediaUrl") ?: ""
                        val caption = doc.getString("caption") ?: ""
                        val createdAt = doc.getTimestamp("createdAt")
                        val expiresAt = doc.getTimestamp("expiresAt")
                        @Suppress("UNCHECKED_CAST")
                        val viewedBy = doc.get("viewedBy") as? List<String> ?: emptyList()
                        val audience = doc.getString("audience") ?: "EVERYONE"
                        @Suppress("UNCHECKED_CAST")
                        val closeFriends = doc.get("closeFriends") as? List<String> ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val reactions = doc.get("reactions") as? Map<String, String> ?: emptyMap()

                        val story = Story(
                            id = id,
                            userId = userId,
                            username = username,
                            userDisplayName = userDisplayName,
                            userAvatarUrl = userAvatarUrl,
                            mediaUrl = mediaUrl,
                            caption = caption,
                            createdAt = createdAt,
                            expiresAt = expiresAt,
                            viewedBy = viewedBy,
                            audience = audience,
                            closeFriends = closeFriends,
                            reactions = reactions
                        )

                        if (story.isVisibleTo(currentUid)) story else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                // Group by user
                val grouped = allStories
                    .groupBy { it.userId }
                    .map { (userId, storiesList) ->
                        val first = storiesList.first()
                        UserStories(
                            userId = userId,
                            username = first.username,
                            userDisplayName = first.userDisplayName,
                            userAvatarUrl = first.userAvatarUrl,
                            stories = storiesList.sortedBy { it.createdAt?.seconds ?: 0 }
                        )
                    }
                    .sortedWith(compareBy(
                        { if (it.userId == currentUid) 0 else 1 }, // Self first
                        { if (it.hasUnreadFor(currentUid)) 0 else 1 } // Unread next
                    ))

                trySend(grouped)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Adds an emoji reaction to a story.
     */
    suspend fun addStoryReaction(storyId: String, userId: String, emoji: String): Result<Unit> {
        return try {
            firestore.collection(STORIES_COLLECTION)
                .document(storyId)
                .update("reactions.$userId", emoji)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes the user's private story archive (all stories ever uploaded by the user).
     */
    fun observeStoryArchive(userId: String): Flow<List<Story>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(STORIES_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val stories = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val username = doc.getString("username") ?: ""
                        val userDisplayName = doc.getString("userDisplayName") ?: ""
                        val userAvatarUrl = doc.getString("userAvatarUrl")
                        val mediaUrl = doc.getString("mediaUrl") ?: ""
                        val caption = doc.getString("caption") ?: ""
                        val createdAt = doc.getTimestamp("createdAt")
                        val expiresAt = doc.getTimestamp("expiresAt")
                        @Suppress("UNCHECKED_CAST")
                        val viewedBy = doc.get("viewedBy") as? List<String> ?: emptyList()
                        val audience = doc.getString("audience") ?: "EVERYONE"
                        @Suppress("UNCHECKED_CAST")
                        val closeFriends = doc.get("closeFriends") as? List<String> ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val reactions = doc.get("reactions") as? Map<String, String> ?: emptyMap()

                        Story(
                            id = id,
                            userId = userId,
                            username = username,
                            userDisplayName = userDisplayName,
                            userAvatarUrl = userAvatarUrl,
                            mediaUrl = mediaUrl,
                            caption = caption,
                            createdAt = createdAt,
                            expiresAt = expiresAt,
                            viewedBy = viewedBy,
                            audience = audience,
                            closeFriends = closeFriends,
                            reactions = reactions
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(stories)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Marks a story as viewed by the current user.
     */
    suspend fun markStoryAsViewed(storyId: String, currentUid: String): Result<Unit> {
        if (currentUid.isBlank()) return Result.success(Unit)
        return try {
            firestore.collection(STORIES_COLLECTION)
                .document(storyId)
                .update("viewedBy", FieldValue.arrayUnion(currentUid))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a story document and its storage media.
     */
    suspend fun deleteStory(storyId: String): Result<Unit> {
        return try {
            firestore.collection(STORIES_COLLECTION).document(storyId).delete().await()
            try {
                mediaStorageRepository.deleteStoryMedia(storyId)
            } catch (_: Exception) { }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

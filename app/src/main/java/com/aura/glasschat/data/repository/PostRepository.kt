package com.aura.glasschat.data.repository

import android.content.Context
import android.net.Uri
import com.aura.glasschat.data.model.Post
import com.aura.glasschat.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val mediaStorageRepository: MediaStorageRepository = SupabaseMediaStorageRepository.getInstance(),
    private val mediaRepository: MediaRepository = MediaRepository()
) {
    companion object {
        const val POSTS_COLLECTION = "posts"
        const val USERS_COLLECTION = "users"
        const val MAX_PINNED_POSTS = 3
    }

    /**
     * Creates and uploads a new Post.
     */
    suspend fun createPost(
        context: Context,
        imageUri: Uri,
        caption: String,
        user: User
    ): Result<Post> {
        val compressResult = mediaRepository.compressImage(context, imageUri)
        val imageBytes = compressResult.getOrNull()
            ?: return Result.failure(compressResult.exceptionOrNull() ?: Exception("Failed to process image"))

        val postId = UUID.randomUUID().toString()

        val uploadResult = mediaStorageRepository.uploadPostMedia(
            postId = postId,
            userId = user.uid,
            imageBytes = imageBytes
        )

        val downloadUrl = uploadResult.fold(
            onSuccess = { it },
            onFailure = { return Result.failure(it) }
        )

        return try {
            val now = Timestamp.now()
            val post = Post(
                id = postId,
                userId = user.uid,
                username = user.username,
                userDisplayName = user.displayName,
                userAvatarUrl = user.avatarUrl,
                mediaUrl = downloadUrl,
                mediaType = "IMAGE",
                caption = caption.trim(),
                createdAt = now,
                likeCount = 0,
                commentCount = 0,
                likes = emptyList(),
                isPinned = false,
                pinnedAt = null
            )

            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .set(post.toMap())
                .await()

            // Increment user postsCount
            try {
                firestore.collection(USERS_COLLECTION)
                    .document(user.uid)
                    .update("postsCount", FieldValue.increment(1))
                    .await()
            } catch (_: Exception) { }

            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes all posts for a user, sorted with pinned posts first.
     */
    fun observeUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(POSTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        val map = data.toMutableMap()
                        map["id"] = doc.id
                        Post.fromMap(map)
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()

                // Sort: pinned posts first (by pinnedAt desc), then regular posts (by createdAt desc)
                val sortedPosts = posts.sortedWith(
                    compareByDescending<Post> { it.isPinned }
                        .thenByDescending { it.pinnedAt?.seconds ?: 0L }
                        .thenByDescending { it.createdAt?.seconds ?: 0L }
                )

                trySend(sortedPosts)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Pins a post to the user profile (up to MAX_PINNED_POSTS allowed).
     */
    suspend fun pinPost(userId: String, postId: String): Result<Unit> {
        if (userId.isBlank() || postId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid IDs"))
        }

        return try {
            val pinnedDocs = firestore.collection(POSTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isPinned", true)
                .get()
                .await()

            if (pinnedDocs.size() >= MAX_PINNED_POSTS) {
                // Check if this post is already one of the pinned posts
                val alreadyPinned = pinnedDocs.documents.any { it.id == postId }
                if (!alreadyPinned) {
                    return Result.failure(
                        IllegalStateException("You can only pin up to $MAX_PINNED_POSTS posts on your profile.")
                    )
                }
            }

            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .update(
                    mapOf(
                        "isPinned" to true,
                        "pinnedAt" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unpins a post from user profile.
     */
    suspend fun unpinPost(userId: String, postId: String): Result<Unit> {
        if (userId.isBlank() || postId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid IDs"))
        }

        return try {
            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .update(
                    mapOf(
                        "isPinned" to false,
                        "pinnedAt" to null
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a post.
     */
    suspend fun deletePost(postId: String, userId: String): Result<Unit> {
        if (postId.isBlank()) return Result.failure(IllegalArgumentException("Invalid post ID"))

        return try {
            firestore.collection(POSTS_COLLECTION).document(postId).delete().await()
            try {
                mediaStorageRepository.deletePostMedia(postId)
            } catch (_: Exception) { }

            if (userId.isNotBlank()) {
                try {
                    firestore.collection(USERS_COLLECTION)
                        .document(userId)
                        .update("postsCount", FieldValue.increment(-1))
                        .await()
                } catch (_: Exception) { }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggles like on a post.
     */
    suspend fun toggleLikePost(postId: String, currentUid: String): Result<Unit> {
        if (postId.isBlank() || currentUid.isBlank()) return Result.failure(IllegalArgumentException("Invalid arguments"))

        return try {
            val doc = firestore.collection(POSTS_COLLECTION).document(postId).get().await()
            @Suppress("UNCHECKED_CAST")
            val likes = doc.get("likes") as? List<String> ?: emptyList()

            if (likes.contains(currentUid)) {
                firestore.collection(POSTS_COLLECTION).document(postId).update(
                    mapOf(
                        "likes" to FieldValue.arrayRemove(currentUid),
                        "likeCount" to FieldValue.increment(-1)
                    )
                ).await()
            } else {
                firestore.collection(POSTS_COLLECTION).document(postId).update(
                    mapOf(
                        "likes" to FieldValue.arrayUnion(currentUid),
                        "likeCount" to FieldValue.increment(1)
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

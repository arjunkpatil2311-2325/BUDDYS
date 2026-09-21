package com.aura.glasschat.data.repository

import com.aura.glasschat.data.model.ProfileHighlight
import com.aura.glasschat.data.model.Story
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class HighlightRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        const val USERS_COLLECTION = "users"
        const val HIGHLIGHTS_SUBCOLLECTION = "highlights"
    }

    /**
     * Observes highlights for a specific user.
     */
    fun observeUserHighlights(userId: String): Flow<List<ProfileHighlight>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(HIGHLIGHTS_SUBCOLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val highlights = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        val map = data.toMutableMap()
                        map["id"] = doc.id
                        ProfileHighlight.fromMap(map)
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(highlights)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Creates a new highlight for the specified user.
     */
    suspend fun createHighlight(
        userId: String,
        title: String,
        coverUrl: String,
        storyIds: List<String>,
        stories: List<Story>
    ): Result<ProfileHighlight> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        val highlightId = UUID.randomUUID().toString()
        val now = Timestamp.now()

        val highlight = ProfileHighlight(
            id = highlightId,
            userId = userId,
            title = title.trim().ifBlank { "Highlight" },
            coverUrl = coverUrl,
            storyIds = storyIds,
            stories = stories,
            createdAt = now,
            updatedAt = now
        )

        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(HIGHLIGHTS_SUBCOLLECTION)
                .document(highlightId)
                .set(highlight.toMap())
                .await()
            Result.success(highlight)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing highlight's title, cover, and stories.
     */
    suspend fun updateHighlight(
        userId: String,
        highlightId: String,
        title: String,
        coverUrl: String,
        storyIds: List<String>,
        stories: List<Story>
    ): Result<Unit> {
        if (userId.isBlank() || highlightId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid IDs"))
        }

        return try {
            val updates = mapOf(
                "title" to title.trim().ifBlank { "Highlight" },
                "coverUrl" to coverUrl,
                "storyIds" to storyIds,
                "stories" to stories.map { it.toMap() },
                "updatedAt" to Timestamp.now()
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(HIGHLIGHTS_SUBCOLLECTION)
                .document(highlightId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a highlight.
     */
    suspend fun deleteHighlight(userId: String, highlightId: String): Result<Unit> {
        if (userId.isBlank() || highlightId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid IDs"))
        }

        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(HIGHLIGHTS_SUBCOLLECTION)
                .document(highlightId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

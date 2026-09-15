package com.aura.glasschat.data.repository

import com.aura.glasschat.data.model.SavedMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SavedMessagesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val SAVED_MESSAGES_SUBCOLLECTION = "saved_messages"
    }

    /**
     * Saves a message to the user's private saved messages subcollection.
     */
    suspend fun saveMessage(userId: String, savedMessage: SavedMessage): Result<Unit> {
        if (userId.isBlank() || savedMessage.messageId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID and message ID required"))
        }
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SAVED_MESSAGES_SUBCOLLECTION)
                .document(savedMessage.messageId)
                .set(savedMessage.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves a Chat Message directly to saved messages.
     */
    suspend fun saveMessage(userId: String, message: com.aura.glasschat.data.model.Message, otherUserId: String = ""): Result<Unit> {
        val saved = SavedMessage(
            id = message.id,
            messageId = message.id,
            chatId = message.chatId,
            otherUserId = otherUserId,
            senderId = message.senderId,
            senderName = message.senderName,
            content = message.content,
            type = message.type,
            mediaUrl = message.mediaUrl,
            durationMs = message.durationMs,
            messageTimestamp = message.timestamp
        )
        return saveMessage(userId, saved)
    }

    /**
     * Removes a saved message by its message ID.
     */
    suspend fun removeSavedMessage(userId: String, messageId: String): Result<Unit> {
        if (userId.isBlank() || messageId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID and message ID required"))
        }
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SAVED_MESSAGES_SUBCOLLECTION)
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time flow of all saved messages for a user, sorted newest first.
     */
    fun observeSavedMessages(userId: String): Flow<List<SavedMessage>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(SAVED_MESSAGES_SUBCOLLECTION)
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SavedMessage::class.java)
                } ?: emptyList()
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }
}

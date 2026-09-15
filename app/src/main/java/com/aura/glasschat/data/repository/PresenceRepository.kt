package com.aura.glasschat.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PresenceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        const val USERS_COLLECTION = "users"
        const val CHATS_COLLECTION = "chats"
        const val PRESENCE_SUBCOLLECTION = "presence"
    }

    /**
     * Updates global user online/offline status with debounce/lifecycle awareness.
     */
    suspend fun setUserGlobalPresence(uid: String, isOnline: Boolean) {
        if (uid.isBlank()) return
        try {
            val updates = mapOf(
                "isOnline" to isOnline,
                "lastSeen" to FieldValue.serverTimestamp()
            )
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(updates, SetOptions.merge())
                .await()
        } catch (_: Exception) {}
    }

    /**
     * Updates chat-specific presence ("In this chat" status).
     */
    suspend fun setChatPresence(chatId: String, uid: String, isInChat: Boolean) {
        if (chatId.isBlank() || uid.isBlank()) return
        try {
            val data = mapOf(
                "uid" to uid,
                "isInChat" to isInChat,
                "lastActiveAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(PRESENCE_SUBCOLLECTION)
                .document(uid)
                .set(data, SetOptions.merge())
                .await()
        } catch (_: Exception) {}
    }

    /**
     * Observes whether the other user is actively present inside the specific chat right now.
     */
    fun observeOtherUserInChat(chatId: String, otherUid: String): Flow<Boolean> = callbackFlow {
        if (chatId.isBlank() || otherUid.isBlank()) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(PRESENCE_SUBCOLLECTION)
            .document(otherUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(false)
                    return@addSnapshotListener
                }

                val isInChat = snapshot.getBoolean("isInChat") ?: false
                val lastActive = snapshot.getTimestamp("lastActiveAt")
                
                // If presence has timed out (> 60 seconds without activity), treat as not in chat
                val isRecent = if (lastActive != null) {
                    val diffSec = (Timestamp.now().seconds - lastActive.seconds)
                    diffSec < 60
                } else true

                trySend(isInChat && isRecent)
            }

        awaitClose { listener.remove() }
    }
}

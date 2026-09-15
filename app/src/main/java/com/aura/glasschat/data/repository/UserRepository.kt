package com.aura.glasschat.data.repository

import com.aura.glasschat.data.model.Friend
import com.aura.glasschat.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val mediaStorageRepository: MediaStorageRepository = SupabaseMediaStorageRepository.getInstance()
) {

    /**
     * Observes current user profile in real-time.
     */
    fun observeUserProfile(userId: String): Flow<User?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)?.copy(uid = snapshot.id)
                    trySend(user)
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Observes the subcollection of friends: /users/{userId}/friends
     */
    fun observeFriends(userId: String): Flow<List<Friend>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(userId)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val friends = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Friend::class.java)?.copy(friendUid = doc.id)
                    }
                    trySend(friends)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun getUser(userId: String): User? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        statusMessage: String,
        avatarUrl: String? = null,
        bio: String? = null,
        pronouns: String? = null,
        link: String? = null,
        gender: String? = null,
        isPrivate: Boolean? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName.trim(),
                "statusMessage" to statusMessage.trim()
            )
            if (avatarUrl != null) updates["avatarUrl"] = avatarUrl
            if (bio != null) updates["bio"] = bio.trim()
            if (pronouns != null) updates["pronouns"] = pronouns.trim()
            if (link != null) updates["link"] = link.trim()
            if (gender != null) updates["gender"] = gender.trim()
            if (isPrivate != null) updates["isPrivate"] = isPrivate

            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAccountPrivacy(userId: String, isPrivate: Boolean): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        return try {
            firestore.collection("users")
                .document(userId)
                .update("isPrivate", isPrivate)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePicture(userId: String, imageBytes: ByteArray): Result<String> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        return try {
            val uploadResult = mediaStorageRepository.uploadProfilePicture(userId, imageBytes)
            val downloadUrl = uploadResult.getOrThrow()

            firestore.collection("users")
                .document(userId)
                .update("avatarUrl", downloadUrl)
                .await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeProfilePicture(userId: String): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        return try {
            mediaStorageRepository.deleteProfilePicture(userId)
            firestore.collection("users")
                .document(userId)
                .update("avatarUrl", null)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePresence(userId: String, isOnline: Boolean) {
        if (userId.isBlank()) return
        try {
            firestore.collection("users")
                .document(userId)
                .set(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to Timestamp.now()
                    ),
                    SetOptions.merge()
                ).await()
        } catch (_: Exception) {}
    }

    suspend fun updateNote(userId: String, note: String): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        return try {
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "note" to note.trim(),
                        "noteCreatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        return try {
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "note" to null,
                        "noteCreatedAt" to null
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(
        userId: String,
        onlinePrivacy: String? = null,
        lastSeenPrivacy: String? = null,
        notificationPrivacy: String? = null
    ): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        return try {
            val updates = mutableMapOf<String, Any>()
            if (onlinePrivacy != null) updates["onlinePrivacy"] = onlinePrivacy
            if (lastSeenPrivacy != null) updates["lastSeenPrivacy"] = lastSeenPrivacy
            if (notificationPrivacy != null) updates["notificationPrivacy"] = notificationPrivacy

            if (updates.isNotEmpty()) {
                firestore.collection("users")
                    .document(userId)
                    .update(updates)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCloseFriends(userId: String, closeFriends: List<String>): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        return try {
            firestore.collection("users")
                .document(userId)
                .update("closeFriends", closeFriends)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCloseFriend(userId: String, friendId: String, isCurrentlyClose: Boolean): Result<Unit> {
        if (userId.isBlank() || friendId.isBlank()) return Result.failure(IllegalArgumentException("IDs cannot be blank"))
        return try {
            val op = if (isCurrentlyClose) com.google.firebase.firestore.FieldValue.arrayRemove(friendId) else com.google.firebase.firestore.FieldValue.arrayUnion(friendId)
            firestore.collection("users")
                .document(userId)
                .update("closeFriends", op)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

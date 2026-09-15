package com.aura.glasschat.data.repository

import com.aura.glasschat.data.model.Chat
import com.aura.glasschat.data.model.Friend
import com.aura.glasschat.data.model.PairingCode
import com.aura.glasschat.data.model.PairingRequest
import com.aura.glasschat.data.model.User
import com.aura.glasschat.util.ChatUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

sealed class PairingResult {
    data class Success(val friendUser: User, val chatId: String) : PairingResult()
    data class Error(val message: String) : PairingResult()
}

/**
 * Secure Firestore-Only Pairing Repository ($0 Spark Plan Compatible).
 * Each user performs ONLY the writes they are authorized to make under Firestore Security Rules.
 * User B writes ONLY to /users/B/friends/A and /pairing_requests
 * User A writes ONLY to /users/A/friends/B and accepts /pairing_requests
 * No Cloud Functions or Blaze billing required.
 */
class PairingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val CODE_EXPIRATION_MINUTES = 15
        private const val PAIRING_CODES_COLLECTION = "pairing_codes"
        private const val PAIRING_REQUESTS_COLLECTION = "pairing_requests"
        private const val USERS_COLLECTION = "users"
        private const val CHATS_COLLECTION = "chats"
        private const val FRIENDS_SUBCOLLECTION = "friends"
    }

    private var activeRequestListener: ListenerRegistration? = null

    /**
     * User A: Generates a cryptographically random, 15-minute temporary pairing code
     * and listens for incoming pairing requests to complete mutual pairing.
     */
    suspend fun generatePairingCode(user: User): Result<PairingCode> {
        return try {
            val codeString = ChatUtils.generateSecurePairingCode()
            val now = Date()
            val expiresAtDate = Date(now.time + CODE_EXPIRATION_MINUTES * 60 * 1000)

            val codeDocRef = firestore.collection(PAIRING_CODES_COLLECTION).document()
            val pairingCode = PairingCode(
                codeId = codeDocRef.id,
                code = codeString,
                ownerUid = user.uid,
                ownerDisplayName = user.displayName,
                ownerUsername = user.username,
                ownerAvatarUrl = user.avatarUrl,
                createdAt = Timestamp(now),
                expiresAt = Timestamp(expiresAtDate),
                used = false,
                usedBy = null
            )

            codeDocRef.set(
                mapOf(
                    "code" to pairingCode.code,
                    "ownerUid" to pairingCode.ownerUid,
                    "ownerDisplayName" to pairingCode.ownerDisplayName,
                    "ownerUsername" to pairingCode.ownerUsername,
                    "ownerAvatarUrl" to pairingCode.ownerAvatarUrl,
                    "createdAt" to pairingCode.createdAt,
                    "expiresAt" to pairingCode.expiresAt,
                    "used" to false,
                    "usedBy" to null
                )
            ).await()

            // Start listening for incoming pairing requests to accept them automatically
            listenAndAcceptRequests(user, pairingCode.code)

            Result.success(pairingCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * User A: Automatically listens to incoming pairing requests for their code,
     * accepts the request, and writes User A's authorized friendship document.
     */
    private fun listenAndAcceptRequests(currentUser: User, activeCode: String) {
        activeRequestListener?.remove()
        activeRequestListener = firestore.collection(PAIRING_REQUESTS_COLLECTION)
            .whereEqualTo("targetUid", currentUser.uid)
            .whereEqualTo("code", activeCode)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val request = doc.toObject(PairingRequest::class.java)?.copy(requestId = doc.id)
                        ?: continue

                    val requesterUid = request.requesterUid
                    val deterministicChatId = ChatUtils.getDeterministicChatId(currentUser.uid, requesterUid)
                    val now = Timestamp.now()

                    // 1. User A writes to /users/A/friends/B (Authorized under A's security rule)
                    val friendRecordForA = Friend(
                        friendUid = requesterUid,
                        chatId = deterministicChatId,
                        displayName = request.requesterDisplayName,
                        username = request.requesterUsername,
                        avatarUrl = request.requesterAvatarUrl,
                        createdAt = now
                    )

                    firestore.collection(USERS_COLLECTION)
                        .document(currentUser.uid)
                        .collection(FRIENDS_SUBCOLLECTION)
                        .document(requesterUid)
                        .set(friendRecordForA.toMap(), SetOptions.merge())

                    // 2. User A updates the pairing request status to ACCEPTED
                    doc.reference.update(mapOf("status" to "ACCEPTED"))

                    // 3. User A creates/ensures the deterministic chat
                    val initialChat = mapOf(
                        "chatId" to deterministicChatId,
                        "participants" to listOf(currentUser.uid, requesterUid),
                        "participantInfo" to mapOf(
                            currentUser.uid to mapOf(
                                "uid" to currentUser.uid,
                                "displayName" to currentUser.displayName,
                                "username" to currentUser.username,
                                "avatarUrl" to currentUser.avatarUrl
                            ),
                            requesterUid to mapOf(
                                "uid" to requesterUid,
                                "displayName" to request.requesterDisplayName,
                                "username" to request.requesterUsername,
                                "avatarUrl" to request.requesterAvatarUrl
                            )
                        ),
                        "lastMessage" to "✨ Connected on Buddies",
                        "lastMessageSenderId" to "",
                        "lastMessageTimestamp" to now,
                        "lastReadAt" to mapOf(
                            currentUser.uid to now,
                            requesterUid to now
                        ),
                        "createdAt" to now
                    )

                    firestore.collection(CHATS_COLLECTION)
                        .document(deterministicChatId)
                        .set(initialChat, SetOptions.merge())
                }
            }
    }

    /**
     * User B: Enters User A's code, validates constraints, marks the code used,
     * writes User B's authorized friendship document, and sends a pairing request to User A.
     */
    suspend fun pairWithCode(inputCode: String, currentUser: User): PairingResult {
        val normalizedCode = ChatUtils.normalizePairingCode(inputCode)

        return try {
            // 1. Query for the active pairing code
            val querySnapshot = firestore.collection(PAIRING_CODES_COLLECTION)
                .whereEqualTo("code", normalizedCode)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return PairingResult.Error("Invalid pairing code. Please verify and try again.")
            }

            val codeDoc = querySnapshot.documents[0]
            val pairingCode = codeDoc.toObject(PairingCode::class.java)?.copy(codeId = codeDoc.id)
                ?: return PairingResult.Error("Could not parse pairing code.")

            // 2. Validate code constraints
            if (pairingCode.used) {
                return PairingResult.Error("This pairing code has already been used.")
            }

            if (pairingCode.isExpired()) {
                return PairingResult.Error("This pairing code has expired. Please ask your friend for a new one.")
            }

            if (pairingCode.ownerUid == currentUser.uid) {
                return PairingResult.Error("You cannot pair with your own code.")
            }

            val ownerUid = pairingCode.ownerUid

            // 3. Fetch owner profile for friendship record
            val ownerDoc = firestore.collection(USERS_COLLECTION).document(ownerUid).get().await()
            val ownerUser = ownerDoc.toObject(User::class.java)?.copy(uid = ownerDoc.id)
                ?: return PairingResult.Error("Friend account not found.")

            val deterministicChatId = ChatUtils.getDeterministicChatId(currentUser.uid, ownerUid)
            val now = Timestamp.now()

            // 4. Mark code as used
            codeDoc.reference.update(
                mapOf(
                    "used" to true,
                    "usedBy" to currentUser.uid
                )
            ).await()

            // 5. User B writes to /users/B/friends/A (Authorized under B's security rule)
            val friendRecordForB = Friend(
                friendUid = ownerUid,
                chatId = deterministicChatId,
                displayName = ownerUser.displayName,
                username = ownerUser.username,
                avatarUrl = ownerUser.avatarUrl,
                createdAt = now
            )

            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .collection(FRIENDS_SUBCOLLECTION)
                .document(ownerUid)
                .set(friendRecordForB.toMap(), SetOptions.merge())
                .await()

            // 6. User B submits pairing request for User A
            val requestId = "req_${currentUser.uid}_${ownerUid}"
            val pairingRequest = PairingRequest(
                requestId = requestId,
                requesterUid = currentUser.uid,
                requesterDisplayName = currentUser.displayName,
                requesterUsername = currentUser.username,
                requesterAvatarUrl = currentUser.avatarUrl,
                targetUid = ownerUid,
                code = normalizedCode,
                status = "PENDING",
                createdAt = now
            )

            firestore.collection(PAIRING_REQUESTS_COLLECTION)
                .document(requestId)
                .set(pairingRequest.toMap(), SetOptions.merge())
                .await()

            // 7. User B creates/ensures the deterministic chat
            val initialChat = mapOf(
                "chatId" to deterministicChatId,
                "participants" to listOf(currentUser.uid, ownerUid),
                "participantInfo" to mapOf(
                    currentUser.uid to mapOf(
                        "uid" to currentUser.uid,
                        "displayName" to currentUser.displayName,
                        "username" to currentUser.username,
                        "avatarUrl" to currentUser.avatarUrl
                    ),
                    ownerUid to mapOf(
                        "uid" to ownerUid,
                        "displayName" to ownerUser.displayName,
                        "username" to ownerUser.username,
                        "avatarUrl" to ownerUser.avatarUrl
                    )
                ),
                "lastMessage" to "✨ Connected on Buddies",
                "lastMessageSenderId" to "",
                "lastMessageTimestamp" to now,
                "lastReadAt" to mapOf(
                    currentUser.uid to now,
                    ownerUid to now
                ),
                "createdAt" to now
            )

            firestore.collection(CHATS_COLLECTION)
                .document(deterministicChatId)
                .set(initialChat, SetOptions.merge())
                .await()

            PairingResult.Success(ownerUser, deterministicChatId)
        } catch (e: Exception) {
            PairingResult.Error(e.localizedMessage ?: "Failed to pair with friend.")
        }
    }

    /**
     * Creates an authorized friendship and 1-to-1 chat channel for the current user.
     * Designed for "Tap to Buddy" nearby pairing where each peer writes their own authorized friend document.
     */
    suspend fun createMutualFriendship(currentUser: User, targetUser: User): Result<String> {
        return try {
            if (currentUser.uid == targetUser.uid) {
                return Result.failure(IllegalArgumentException("Cannot pair with yourself"))
            }

            val deterministicChatId = ChatUtils.getDeterministicChatId(currentUser.uid, targetUser.uid)
            val now = Timestamp.now()

            // 1. Write to /users/{currentUser.uid}/friends/{targetUser.uid}
            val friendRecord = Friend(
                friendUid = targetUser.uid,
                chatId = deterministicChatId,
                displayName = targetUser.displayName.ifBlank { targetUser.username },
                username = targetUser.username,
                avatarUrl = targetUser.avatarUrl,
                createdAt = now
            )

            firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .collection(FRIENDS_SUBCOLLECTION)
                .document(targetUser.uid)
                .set(friendRecord.toMap(), SetOptions.merge())
                .await()

            // 2. Ensure /chats/{deterministicChatId} exists
            val initialChat = mapOf(
                "chatId" to deterministicChatId,
                "participants" to listOf(currentUser.uid, targetUser.uid),
                "participantInfo" to mapOf(
                    currentUser.uid to mapOf(
                        "uid" to currentUser.uid,
                        "displayName" to currentUser.displayName,
                        "username" to currentUser.username,
                        "avatarUrl" to currentUser.avatarUrl
                    ),
                    targetUser.uid to mapOf(
                        "uid" to targetUser.uid,
                        "displayName" to targetUser.displayName,
                        "username" to targetUser.username,
                        "avatarUrl" to targetUser.avatarUrl
                    )
                ),
                "lastMessage" to "✨ Connected on Buddies",
                "lastMessageSenderId" to "",
                "lastMessageTimestamp" to now,
                "lastReadAt" to mapOf(
                    currentUser.uid to now,
                    targetUser.uid to now
                ),
                "createdAt" to now
            )

            firestore.collection(CHATS_COLLECTION)
                .document(deterministicChatId)
                .set(initialChat, SetOptions.merge())
                .await()

            Result.success(deterministicChatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cleanUp() {
        activeRequestListener?.remove()
        activeRequestListener = null
    }
}

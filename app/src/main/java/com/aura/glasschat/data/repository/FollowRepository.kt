package com.aura.glasschat.data.repository

import com.aura.glasschat.data.model.*
import com.aura.glasschat.util.UsernameUtils
import com.aura.glasschat.util.UsernameValidationResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

enum class RelationshipState {
    SELF,
    NOT_FOLLOWING,
    FOLLOWING,
    REQUESTED,
    FOLLOWED_BY,
    MUTUAL, // "Buddys ✨"
    BLOCKED
}

// Backward-compatible alias
typealias FollowStatus = RelationshipState

class FollowRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Efficient, case-insensitive prefix search on /users collection by username.
     * Guaranteed Spark tier compatibility (bounded limit, no full-collection scan).
     */
    suspend fun searchUsers(query: String, currentUid: String): Result<List<User>> {
        val cleanQuery = UsernameUtils.normalize(query)
        if (cleanQuery.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            val snapshot = firestore.collection("users")
                .orderBy("username")
                .startAt(cleanQuery)
                .endAt(cleanQuery + "\uf8ff")
                .limit(25)
                .get()
                .await()

            val blockedUids = getBlockedUserIds(currentUid)

            val users = snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)?.copy(uid = doc.id)
                if (user != null && user.uid != currentUid && !blockedUids.contains(user.uid)) {
                    user
                } else null
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Looks up user profile by username (used for QR code resolution and deep linking).
     */
    suspend fun getUserByUsername(username: String): Result<User?> {
        val clean = UsernameUtils.normalize(username)
        if (clean.isBlank()) return Result.success(null)
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", clean)
                .limit(1)
                .get()
                .await()
            val user = snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes real-time relationship status between two users:
     * - SELF
     * - BLOCKED
     * - MUTUAL (Buddys ✨: I follow them AND they follow me)
     * - FOLLOWING (I follow them)
     * - FOLLOWED_BY (They follow me, but I don't follow them)
     * - REQUESTED (My request is pending)
     * - NOT_FOLLOWING
     */
    fun observeRelationshipState(currentUid: String, targetUid: String): Flow<RelationshipState> = callbackFlow {
        if (currentUid.isBlank() || targetUid.isBlank() || currentUid == targetUid) {
            trySend(if (currentUid == targetUid) RelationshipState.SELF else RelationshipState.NOT_FOLLOWING)
            close()
            return@callbackFlow
        }

        val iFollowThemRef = firestore.collection("users")
            .document(targetUid)
            .collection("followers")
            .document(currentUid)

        val theyFollowMeRef = firestore.collection("users")
            .document(currentUid)
            .collection("followers")
            .document(targetUid)

        val requestQuery = firestore.collection("follow_requests")
            .whereEqualTo("requesterUid", currentUid)
            .whereEqualTo("targetUid", targetUid)
            .whereEqualTo("status", "PENDING")

        val blockRef = firestore.collection("blocks").document("${currentUid}_${targetUid}")

        var iFollowThem = false
        var theyFollowMe = false
        var isRequested = false
        var isBlocked = false

        fun evaluate() {
            val state = when {
                isBlocked -> RelationshipState.BLOCKED
                iFollowThem && theyFollowMe -> RelationshipState.MUTUAL
                iFollowThem -> RelationshipState.FOLLOWING
                isRequested -> RelationshipState.REQUESTED
                theyFollowMe -> RelationshipState.FOLLOWED_BY
                else -> RelationshipState.NOT_FOLLOWING
            }
            trySend(state)
        }

        val l1 = iFollowThemRef.addSnapshotListener { snap, _ ->
            iFollowThem = snap != null && snap.exists()
            evaluate()
        }

        val l2 = theyFollowMeRef.addSnapshotListener { snap, _ ->
            theyFollowMe = snap != null && snap.exists()
            evaluate()
        }

        val l3 = requestQuery.addSnapshotListener { snap, _ ->
            isRequested = snap != null && !snap.isEmpty
            evaluate()
        }

        val l4 = blockRef.addSnapshotListener { snap, _ ->
            isBlocked = snap != null && snap.exists()
            evaluate()
        }

        awaitClose {
            l1.remove()
            l2.remove()
            l3.remove()
            l4.remove()
        }
    }

    // Alias for backward compatibility
    fun observeFollowStatus(currentUid: String, targetUid: String): Flow<RelationshipState> =
        observeRelationshipState(currentUid, targetUid)

    /**
     * Follows a user (immediate for public, follow request for private).
     */
    suspend fun followUser(currentUser: User, targetUser: User): Result<RelationshipState> {
        if (currentUser.uid == targetUser.uid) {
            return Result.failure(IllegalArgumentException("You cannot follow yourself"))
        }

        return try {
            val now = Timestamp.now()

            if (targetUser.isPrivate) {
                // Private account -> Send Follow Request
                val requestId = "${currentUser.uid}_${targetUser.uid}"
                val requestRef = firestore.collection("follow_requests").document(requestId)

                val followRequest = FollowRequest(
                    requestId = requestId,
                    requesterUid = currentUser.uid,
                    requesterUsername = currentUser.username,
                    requesterDisplayName = currentUser.displayName,
                    requesterAvatarUrl = currentUser.avatarUrl,
                    targetUid = targetUser.uid,
                    status = "PENDING",
                    createdAt = now
                )

                val notifRef = firestore.collection("notifications").document()
                val notification = AppNotification(
                    id = notifRef.id,
                    recipientUid = targetUser.uid,
                    actorUid = currentUser.uid,
                    actorUsername = currentUser.username,
                    actorDisplayName = currentUser.displayName,
                    actorAvatarUrl = currentUser.avatarUrl,
                    type = "FOLLOW_REQUEST",
                    requestId = requestId,
                    isRead = false,
                    createdAt = now
                )

                val batch = firestore.batch()
                batch.set(requestRef, followRequest.toMap(), SetOptions.merge())
                batch.set(notifRef, notification.toMap())
                batch.commit().await()

                Result.success(RelationshipState.REQUESTED)
            } else {
                // Public account -> Follow immediately
                val followerRef = firestore.collection("users")
                    .document(targetUser.uid)
                    .collection("followers")
                    .document(currentUser.uid)

                val followingRef = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("following")
                    .document(targetUser.uid)

                val followerRecord = Follow(
                    uid = currentUser.uid,
                    username = currentUser.username,
                    displayName = currentUser.displayName,
                    avatarUrl = currentUser.avatarUrl,
                    followedAt = now
                )

                val followingRecord = Follow(
                    uid = targetUser.uid,
                    username = targetUser.username,
                    displayName = targetUser.displayName,
                    avatarUrl = targetUser.avatarUrl,
                    followedAt = now
                )

                val targetUserRef = firestore.collection("users").document(targetUser.uid)
                val currentUserRef = firestore.collection("users").document(currentUser.uid)

                val notifRef = firestore.collection("notifications").document()
                val notification = AppNotification(
                    id = notifRef.id,
                    recipientUid = targetUser.uid,
                    actorUid = currentUser.uid,
                    actorUsername = currentUser.username,
                    actorDisplayName = currentUser.displayName,
                    actorAvatarUrl = currentUser.avatarUrl,
                    type = "FOLLOW",
                    isRead = false,
                    createdAt = now
                )

                val batch = firestore.batch()
                batch.set(followerRef, followerRecord.toMap(), SetOptions.merge())
                batch.set(followingRef, followingRecord.toMap(), SetOptions.merge())
                batch.update(targetUserRef, "followerCount", FieldValue.increment(1))
                batch.update(currentUserRef, "followingCount", FieldValue.increment(1))
                batch.set(notifRef, notification.toMap())
                batch.commit().await()

                Result.success(RelationshipState.FOLLOWING)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unfollows a user or cancels a pending request.
     */
    suspend fun unfollowUser(currentUid: String, targetUid: String): Result<Unit> {
        if (currentUid == targetUid) return Result.success(Unit)

        return try {
            val followerRef = firestore.collection("users")
                .document(targetUid)
                .collection("followers")
                .document(currentUid)

            val followingRef = firestore.collection("users")
                .document(currentUid)
                .collection("following")
                .document(targetUid)

            val requestId = "${currentUid}_${targetUid}"
            val requestRef = firestore.collection("follow_requests").document(requestId)

            val targetUserRef = firestore.collection("users").document(targetUid)
            val currentUserRef = firestore.collection("users").document(currentUid)

            val followerDoc = followerRef.get().await()
            val batch = firestore.batch()

            if (followerDoc.exists()) {
                batch.delete(followerRef)
                batch.delete(followingRef)
                batch.update(targetUserRef, "followerCount", FieldValue.increment(-1))
                batch.update(currentUserRef, "followingCount", FieldValue.increment(-1))
            }

            batch.delete(requestRef)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Accepts a follow request from another user.
     */
    suspend fun acceptFollowRequest(request: FollowRequest, currentTargetUser: User): Result<Unit> {
        return try {
            val now = Timestamp.now()

            val followerRef = firestore.collection("users")
                .document(currentTargetUser.uid)
                .collection("followers")
                .document(request.requesterUid)

            val followingRef = firestore.collection("users")
                .document(request.requesterUid)
                .collection("following")
                .document(currentTargetUser.uid)

            val followerRecord = Follow(
                uid = request.requesterUid,
                username = request.requesterUsername,
                displayName = request.requesterDisplayName,
                avatarUrl = request.requesterAvatarUrl,
                followedAt = now
            )

            val followingRecord = Follow(
                uid = currentTargetUser.uid,
                username = currentTargetUser.username,
                displayName = currentTargetUser.displayName,
                avatarUrl = currentTargetUser.avatarUrl,
                followedAt = now
            )

            val targetUserRef = firestore.collection("users").document(currentTargetUser.uid)
            val requesterUserRef = firestore.collection("users").document(request.requesterUid)
            val requestRef = firestore.collection("follow_requests").document(request.requestId)

            val notifRef = firestore.collection("notifications").document()
            val notification = AppNotification(
                id = notifRef.id,
                recipientUid = request.requesterUid,
                actorUid = currentTargetUser.uid,
                actorUsername = currentTargetUser.username,
                actorDisplayName = currentTargetUser.displayName,
                actorAvatarUrl = currentTargetUser.avatarUrl,
                type = "FOLLOW_ACCEPT",
                isRead = false,
                createdAt = now
            )

            val batch = firestore.batch()
            batch.set(followerRef, followerRecord.toMap(), SetOptions.merge())
            batch.set(followingRef, followingRecord.toMap(), SetOptions.merge())
            batch.update(targetUserRef, "followerCount", FieldValue.increment(1))
            batch.update(requesterUserRef, "followingCount", FieldValue.increment(1))
            batch.update(requestRef, "status", "ACCEPTED")
            batch.set(notifRef, notification.toMap())
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Declines a follow request.
     */
    suspend fun declineFollowRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("follow_requests")
                .document(requestId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically changes username in /usernames index and /users/{uid}.
     */
    suspend fun changeUsername(currentUid: String, oldUsername: String, newUsername: String): Result<Unit> {
        val validation = UsernameUtils.validate(newUsername)
        if (validation is UsernameValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validation.reason))
        }
        val cleanNew = (validation as UsernameValidationResult.Valid).normalized
        val cleanOld = UsernameUtils.normalize(oldUsername)

        if (cleanNew == cleanOld) {
            return Result.success(Unit)
        }

        return try {
            val now = Timestamp.now()
            val newUsernameDocRef = firestore.collection("usernames").document(cleanNew)
            val oldUsernameDocRef = firestore.collection("usernames").document(cleanOld)
            val userDocRef = firestore.collection("users").document(currentUid)

            firestore.runTransaction { transaction ->
                val newSnapshot = transaction.get(newUsernameDocRef)
                if (newSnapshot.exists()) {
                    val existingUid = newSnapshot.getString("uid")
                    if (existingUid != currentUid) {
                        throw IllegalStateException("Username '@$cleanNew' is already taken.")
                    }
                }

                // 1. Claim new username
                transaction.set(
                    newUsernameDocRef,
                    mapOf(
                        "uid" to currentUid,
                        "username" to cleanNew,
                        "createdAt" to now
                    )
                )

                // 2. Update user profile
                transaction.update(
                    userDocRef,
                    mapOf(
                        "username" to cleanNew,
                        "usernameChangedAt" to now
                    )
                )

                // 3. Delete old username claim if different
                if (cleanOld.isNotBlank() && cleanOld != cleanNew) {
                    transaction.delete(oldUsernameDocRef)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reports a user for safety/moderation.
     */
    suspend fun reportUser(
        reporterUid: String,
        reportedUid: String,
        reportedUsername: String,
        reason: String,
        details: String
    ): Result<Unit> {
        return try {
            val reportRef = firestore.collection("reports").document()
            val report = Report(
                reportId = reportRef.id,
                reporterUid = reporterUid,
                reportedUid = reportedUid,
                reportedUsername = reportedUsername,
                reason = reason,
                details = details,
                createdAt = Timestamp.now()
            )
            reportRef.set(report.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes followers subcollection: /users/{userId}/followers
     */
    fun observeFollowers(userId: String): Flow<List<Follow>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("followers")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val followers = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Follow::class.java)?.copy(uid = doc.id)
                }.sortedByDescending { it.followedAt?.seconds ?: 0L }
                trySend(followers)
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Observes following subcollection: /users/{userId}/following
     */
    fun observeFollowing(userId: String): Flow<List<Follow>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val following = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Follow::class.java)?.copy(uid = doc.id)
                }.sortedByDescending { it.followedAt?.seconds ?: 0L }
                trySend(following)
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Observes in-app activity notifications feed for current user.
     */
    fun observeNotifications(userId: String): Flow<List<AppNotification>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("notifications")
            .whereEqualTo("recipientUid", userId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notifs = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                trySend(notifs)
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Observes unread count for notifications.
     */
    fun observeUnreadNotificationCount(userId: String): Flow<Int> = callbackFlow {
        if (userId.isBlank()) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientUid", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot.size())
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun markNotificationAsRead(notifId: String) {
        if (notifId.isBlank()) return
        try {
            firestore.collection("notifications").document(notifId).update("isRead", true).await()
        } catch (_: Exception) {}
    }

    /**
     * Blocks a user and severs any follow relationship.
     */
    suspend fun blockUser(currentUid: String, targetUid: String): Result<Unit> {
        if (currentUid.isBlank() || targetUid.isBlank() || currentUid == targetUid) {
            return Result.failure(IllegalArgumentException("Invalid block operation"))
        }

        return try {
            val blockId = "${currentUid}_${targetUid}"
            val blockRef = firestore.collection("blocks").document(blockId)
            val block = Block(blockId = blockId, blockerUid = currentUid, blockedUid = targetUid, createdAt = Timestamp.now())

            // Unfollow both directions
            unfollowUser(currentUid, targetUid)
            unfollowUser(targetUid, currentUid)

            blockRef.set(block.toMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockUser(currentUid: String, targetUid: String): Result<Unit> {
        return try {
            val blockId = "${currentUid}_${targetUid}"
            firestore.collection("blocks").document(blockId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isUserBlocked(currentUid: String, targetUid: String): Boolean {
        if (currentUid.isBlank() || targetUid.isBlank()) return false
        return try {
            val b1 = firestore.collection("blocks").document("${currentUid}_${targetUid}").get().await().exists()
            val b2 = firestore.collection("blocks").document("${targetUid}_${currentUid}").get().await().exists()
            b1 || b2
        } catch (_: Exception) {
            false
        }
    }

    fun observeBlockedUsers(currentUid: String): Flow<List<Block>> = callbackFlow {
        if (currentUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("blocks")
            .whereEqualTo("blockerUid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val blocks = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Block::class.java)?.copy(blockId = doc.id)
                }
                trySend(blocks)
            }

        awaitClose {
            listener.remove()
        }
    }

    private suspend fun getBlockedUserIds(currentUid: String): Set<String> {
        if (currentUid.isBlank()) return emptySet()
        return try {
            val outgoing = firestore.collection("blocks").whereEqualTo("blockerUid", currentUid).get().await()
            val incoming = firestore.collection("blocks").whereEqualTo("blockedUid", currentUid).get().await()

            val blocked = mutableSetOf<String>()
            outgoing.documents.forEach { doc ->
                doc.getString("blockedUid")?.let { blocked.add(it) }
            }
            incoming.documents.forEach { doc ->
                doc.getString("blockerUid")?.let { blocked.add(it) }
            }
            blocked
        } catch (_: Exception) {
            emptySet()
        }
    }
}

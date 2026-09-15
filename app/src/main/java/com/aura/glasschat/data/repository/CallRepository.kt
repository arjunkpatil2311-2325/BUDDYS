package com.aura.glasschat.data.repository

import com.aura.glasschat.data.model.CallSession
import com.aura.glasschat.data.model.IceCandidateModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CallRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        const val CALLS_COLLECTION = "calls"
        const val CANDIDATES_SUBCOLLECTION = "candidates"
    }

    /**
     * Initiates an outgoing call with SDP Offer.
     */
    suspend fun startCall(callSession: CallSession): Result<Unit> {
        return try {
            firestore.collection(CALLS_COLLECTION)
                .document(callSession.callId)
                .set(callSession.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Listens for incoming ringing calls for the current user.
     */
    fun observeIncomingCalls(currentUid: String): Flow<CallSession?> = callbackFlow {
        if (currentUid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CALLS_COLLECTION)
            .whereEqualTo("receiverUid", currentUid)
            .whereIn("status", listOf("CALLING", "RINGING"))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val doc = snapshot?.documents?.firstOrNull()
                if (doc != null) {
                    val session = CallSession(
                        callId = doc.getString("callId") ?: doc.id,
                        callerUid = doc.getString("callerUid") ?: "",
                        callerName = doc.getString("callerName") ?: "",
                        callerAvatarUrl = doc.getString("callerAvatarUrl"),
                        receiverUid = doc.getString("receiverUid") ?: "",
                        receiverName = doc.getString("receiverName") ?: "",
                        receiverAvatarUrl = doc.getString("receiverAvatarUrl"),
                        type = doc.getString("type") ?: "AUDIO",
                        status = doc.getString("status") ?: "CALLING",
                        sdpOffer = doc.getString("sdpOffer"),
                        sdpAnswer = doc.getString("sdpAnswer"),
                        createdAt = doc.getTimestamp("createdAt"),
                        endedAt = doc.getTimestamp("endedAt"),
                        durationSec = (doc.getLong("durationSec") ?: 0).toInt()
                    )
                    trySend(session)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observes the active call session document for status changes or answer SDP.
     */
    fun observeCallSession(callId: String): Flow<CallSession?> = callbackFlow {
        if (callId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CALLS_COLLECTION)
            .document(callId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val session = CallSession(
                    callId = doc.getString("callId") ?: doc.id,
                    callerUid = doc.getString("callerUid") ?: "",
                    callerName = doc.getString("callerName") ?: "",
                    callerAvatarUrl = doc.getString("callerAvatarUrl"),
                    receiverUid = doc.getString("receiverUid") ?: "",
                    receiverName = doc.getString("receiverName") ?: "",
                    receiverAvatarUrl = doc.getString("receiverAvatarUrl"),
                    type = doc.getString("type") ?: "AUDIO",
                    status = doc.getString("status") ?: "CALLING",
                    sdpOffer = doc.getString("sdpOffer"),
                    sdpAnswer = doc.getString("sdpAnswer"),
                    createdAt = doc.getTimestamp("createdAt"),
                    endedAt = doc.getTimestamp("endedAt"),
                    durationSec = (doc.getLong("durationSec") ?: 0).toInt()
                )
                trySend(session)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Accepts an incoming call by providing SDP Answer.
     */
    suspend fun acceptCall(callId: String, answerSdp: String): Result<Unit> {
        return try {
            firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .update(
                    mapOf(
                        "status" to "ACCEPTED",
                        "sdpAnswer" to answerSdp
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rejects an incoming call.
     */
    suspend fun rejectCall(callId: String): Result<Unit> {
        return try {
            firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .update(
                    mapOf(
                        "status" to "REJECTED",
                        "endedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ends an active call.
     */
    suspend fun endCall(callId: String, durationSec: Int): Result<Unit> {
        return try {
            firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .update(
                    mapOf(
                        "status" to "ENDED",
                        "endedAt" to FieldValue.serverTimestamp(),
                        "durationSec" to durationSec
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends an ICE Candidate.
     */
    suspend fun sendIceCandidate(callId: String, candidate: IceCandidateModel): Result<Unit> {
        return try {
            firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .collection(CANDIDATES_SUBCOLLECTION)
                .document()
                .set(candidate.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes incoming ICE candidates from the other participant.
     */
    fun observeIceCandidates(callId: String, otherUid: String): Flow<List<IceCandidateModel>> = callbackFlow {
        if (callId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CALLS_COLLECTION)
            .document(callId)
            .collection(CANDIDATES_SUBCOLLECTION)
            .whereEqualTo("senderUid", otherUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val candidates = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        IceCandidateModel(
                            sdpMid = doc.getString("sdpMid") ?: "",
                            sdpMLineIndex = (doc.getLong("sdpMLineIndex") ?: 0).toInt(),
                            sdp = doc.getString("sdp") ?: "",
                            senderUid = doc.getString("senderUid") ?: ""
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(candidates)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observes call history where user is either caller or receiver.
     */
    fun observeCallHistory(userId: String): Flow<List<CallSession>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CALLS_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Fallback to callerUid / receiverUid query
                    firestore.collection(CALLS_COLLECTION)
                        .whereEqualTo("receiverUid", userId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(20)
                        .addSnapshotListener { subSnapshot, _ ->
                            val calls = subSnapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(CallSession::class.java)?.copy(callId = doc.id)
                            } ?: emptyList()
                            trySend(calls)
                        }
                    return@addSnapshotListener
                }

                val calls = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CallSession::class.java)?.copy(callId = doc.id)
                } ?: emptyList()
                trySend(calls)
            }

        awaitClose { listener.remove() }
    }
}

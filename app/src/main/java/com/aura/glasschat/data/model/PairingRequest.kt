package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class PairingRequest(
    val requestId: String = "",
    val requesterUid: String = "",
    val requesterDisplayName: String = "",
    val requesterUsername: String = "",
    val requesterAvatarUrl: String? = null,
    val targetUid: String = "",
    val code: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "requestId" to requestId,
        "requesterUid" to requesterUid,
        "requesterDisplayName" to requesterDisplayName,
        "requesterUsername" to requesterUsername,
        "requesterAvatarUrl" to requesterAvatarUrl,
        "targetUid" to targetUid,
        "code" to code,
        "status" to status,
        "createdAt" to (createdAt ?: Timestamp.now())
    )
}

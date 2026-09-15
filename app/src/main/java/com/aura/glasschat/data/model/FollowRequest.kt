package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class FollowRequest(
    val requestId: String = "",
    val requesterUid: String = "",
    val requesterUsername: String = "",
    val requesterDisplayName: String = "",
    val requesterAvatarUrl: String? = null,
    val targetUid: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED, CANCELLED
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "requestId" to requestId,
        "requesterUid" to requesterUid,
        "requesterUsername" to requesterUsername,
        "requesterDisplayName" to requesterDisplayName,
        "requesterAvatarUrl" to requesterAvatarUrl,
        "targetUid" to targetUid,
        "status" to status,
        "createdAt" to (createdAt ?: Timestamp.now())
    )
}

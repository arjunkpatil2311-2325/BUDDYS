package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Report(
    val reportId: String = "",
    val reporterUid: String = "",
    val reportedUid: String = "",
    val reportedUsername: String = "",
    val reason: String = "", // Spam, Harassment, Impersonation, Inappropriate content, Other
    val details: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "reportId" to reportId,
        "reporterUid" to reporterUid,
        "reportedUid" to reportedUid,
        "reportedUsername" to reportedUsername,
        "reason" to reason,
        "details" to details,
        "createdAt" to (createdAt ?: Timestamp.now())
    )
}

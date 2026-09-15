package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class AppNotification(
    val id: String = "",
    val recipientUid: String = "",
    val actorUid: String = "",
    val actorUsername: String = "",
    val actorDisplayName: String = "",
    val actorAvatarUrl: String? = null,
    val type: String = "FOLLOW", // FOLLOW, FOLLOW_REQUEST, FOLLOW_ACCEPT
    val requestId: String? = null,
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "recipientUid" to recipientUid,
        "actorUid" to actorUid,
        "actorUsername" to actorUsername,
        "actorDisplayName" to actorDisplayName,
        "actorAvatarUrl" to actorAvatarUrl,
        "type" to type,
        "requestId" to requestId,
        "isRead" to isRead,
        "createdAt" to (createdAt ?: Timestamp.now())
    )
}

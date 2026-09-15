package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Friend(
    val friendUid: String = "",
    val chatId: String = "",
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "friendUid" to friendUid,
        "chatId" to chatId,
        "displayName" to displayName,
        "username" to username,
        "avatarUrl" to avatarUrl,
        "isOnline" to isOnline,
        "createdAt" to (createdAt ?: Timestamp.now())
    )
}

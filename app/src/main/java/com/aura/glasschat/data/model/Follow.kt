package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Follow(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    @ServerTimestamp
    val followedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "username" to username,
        "displayName" to displayName,
        "avatarUrl" to avatarUrl,
        "followedAt" to (followedAt ?: Timestamp.now())
    )
}

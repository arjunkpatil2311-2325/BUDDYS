package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class PairingCode(
    val codeId: String = "",
    val code: String = "",
    val ownerUid: String = "",
    val ownerDisplayName: String = "",
    val ownerUsername: String = "",
    val ownerAvatarUrl: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val used: Boolean = false,
    val usedBy: String? = null
) {
    fun isExpired(): Boolean {
        val expiry = expiresAt ?: return true
        return expiry.seconds < Timestamp.now().seconds
    }

    fun isValid(): Boolean {
        return !used && !isExpired()
    }
}

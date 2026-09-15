package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Block(
    val blockId: String = "",
    val blockerUid: String = "",
    val blockedUid: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "blockId" to blockId,
        "blockerUid" to blockerUid,
        "blockedUid" to blockedUid,
        "createdAt" to (createdAt ?: Timestamp.now())
    )
}

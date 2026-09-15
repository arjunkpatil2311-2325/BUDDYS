package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallStatus {
    CALLING,
    RINGING,
    ACCEPTED,
    REJECTED,
    ENDED,
    MISSED
}

data class IceCandidateModel(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdp: String = "",
    val senderUid: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "sdpMid" to sdpMid,
        "sdpMLineIndex" to sdpMLineIndex,
        "sdp" to sdp,
        "senderUid" to senderUid
    )
}

data class CallSession(
    val callId: String = "",
    val callerUid: String = "",
    val callerName: String = "",
    val callerAvatarUrl: String? = null,
    val receiverUid: String = "",
    val receiverName: String = "",
    val receiverAvatarUrl: String? = null,
    val type: String = "AUDIO", // "AUDIO" or "VIDEO"
    val status: String = "CALLING", // "CALLING", "RINGING", "ACCEPTED", "REJECTED", "ENDED", "MISSED"
    val sdpOffer: String? = null,
    val sdpAnswer: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val endedAt: Timestamp? = null,
    val durationSec: Int = 0
) {
    val isVideo: Boolean get() = type == "VIDEO"
    val isAudio: Boolean get() = type == "AUDIO"
    val isActive: Boolean get() = status == "CALLING" || status == "RINGING" || status == "ACCEPTED"

    fun toMap(): Map<String, Any?> = mapOf(
        "callId" to callId,
        "callerUid" to callerUid,
        "callerName" to callerName,
        "callerAvatarUrl" to callerAvatarUrl,
        "receiverUid" to receiverUid,
        "receiverName" to receiverName,
        "receiverAvatarUrl" to receiverAvatarUrl,
        "type" to type,
        "status" to status,
        "sdpOffer" to sdpOffer,
        "sdpAnswer" to sdpAnswer,
        "createdAt" to (createdAt ?: Timestamp.now()),
        "endedAt" to endedAt,
        "durationSec" to durationSec
    )
}

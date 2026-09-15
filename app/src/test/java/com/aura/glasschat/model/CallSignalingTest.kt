package com.aura.glasschat.model

import com.aura.glasschat.data.model.CallSession
import com.aura.glasschat.data.model.CallStatus
import com.aura.glasschat.data.model.CallType
import com.aura.glasschat.data.model.IceCandidateModel
import com.aura.glasschat.ui.viewmodel.CallUiState
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class CallSignalingTest {

    @Test
    fun testCallSessionModelCreationAndSerialization() {
        val now = Timestamp.now()
        val session = CallSession(
            callId = "call_abc_123",
            callerUid = "user_caller",
            callerName = "Rahul",
            callerAvatarUrl = "https://example.com/rahul.jpg",
            receiverUid = "user_receiver",
            receiverName = "Priya",
            receiverAvatarUrl = null,
            type = "AUDIO",
            status = "CALLING",
            sdpOffer = "v=0\r\no=- 12345 2 IN IP4 127.0.0.1",
            sdpAnswer = null,
            createdAt = now,
            durationSec = 0
        )

        assertTrue(session.isAudio)
        assertFalse(session.isVideo)
        assertTrue(session.isActive)

        val map = session.toMap()
        assertEquals("call_abc_123", map["callId"])
        assertEquals("user_caller", map["callerUid"])
        assertEquals("user_receiver", map["receiverUid"])
        assertEquals("AUDIO", map["type"])
        assertEquals("CALLING", map["status"])
        assertEquals("v=0\r\no=- 12345 2 IN IP4 127.0.0.1", map["sdpOffer"])
    }

    @Test
    fun testVideoCallStateAndDurationFormatting() {
        val session = CallSession(
            callId = "call_video_456",
            callerUid = "user_1",
            callerName = "Amit",
            receiverUid = "user_2",
            receiverName = "Vikram",
            type = "VIDEO",
            status = "ACCEPTED",
            durationSec = 135 // 2 min 15 sec
        )

        assertTrue(session.isVideo)
        assertFalse(session.isAudio)

        val uiState = CallUiState(
            callSession = session,
            callType = CallType.VIDEO,
            callStatus = CallStatus.ACCEPTED,
            callDurationSec = 135
        )

        assertEquals("02:15", uiState.durationFormatted)
    }

    @Test
    fun testIceCandidateSerialization() {
        val candidate = IceCandidateModel(
            sdpMid = "audio",
            sdpMLineIndex = 0,
            sdp = "candidate:123456 1 udp 2122260223 192.168.1.100 50000 typ host",
            senderUid = "user_caller"
        )

        val map = candidate.toMap()
        assertEquals("audio", map["sdpMid"])
        assertEquals(0, map["sdpMLineIndex"])
        assertEquals("candidate:123456 1 udp 2122260223 192.168.1.100 50000 typ host", map["sdp"])
        assertEquals("user_caller", map["senderUid"])
    }
}

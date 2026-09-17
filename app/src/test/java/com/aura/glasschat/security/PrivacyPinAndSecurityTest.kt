package com.aura.glasschat.security

import com.aura.glasschat.data.model.Chat
import com.aura.glasschat.data.model.ParticipantInfo
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.util.Date

class PrivacyPinAndSecurityTest {

    @Test
    fun `test LockTimeout enum durations and mappings`() {
        assertEquals(0L, LockTimeout.IMMEDIATELY.durationMs)
        assertEquals(60_000L, LockTimeout.AFTER_1_MIN.durationMs)
        assertEquals(300_000L, LockTimeout.AFTER_5_MIN.durationMs)
        assertEquals(900_000L, LockTimeout.AFTER_15_MIN.durationMs)

        assertEquals(LockTimeout.IMMEDIATELY, LockTimeout.fromName("IMMEDIATELY"))
        assertEquals(LockTimeout.AFTER_1_MIN, LockTimeout.fromName("AFTER_1_MIN"))
        assertEquals(LockTimeout.AFTER_5_MIN, LockTimeout.fromName("AFTER_5_MIN"))
        assertEquals(LockTimeout.AFTER_15_MIN, LockTimeout.fromName("AFTER_15_MIN"))
        assertEquals(LockTimeout.IMMEDIATELY, LockTimeout.fromName("INVALID_UNKNOWN"))
        assertEquals(LockTimeout.IMMEDIATELY, LockTimeout.fromName(null))
    }

    @Test
    fun `test SHA-256 PIN hashing with salt produces distinct hashes`() {
        val pin = "123456"
        val salt1 = ByteArray(16) { 1 }
        val salt2 = ByteArray(16) { 2 }

        val md1 = MessageDigest.getInstance("SHA-256")
        md1.update(salt1)
        val hash1 = md1.digest(pin.toByteArray(Charsets.UTF_8))

        val md2 = MessageDigest.getInstance("SHA-256")
        md2.update(salt2)
        val hash2 = md2.digest(pin.toByteArray(Charsets.UTF_8))

        assertFalse("Different salts must produce different hashes", hash1.contentEquals(hash2))

        // Same salt produces identical hash
        val md3 = MessageDigest.getInstance("SHA-256")
        md3.update(salt1)
        val hash3 = md3.digest(pin.toByteArray(Charsets.UTF_8))
        assertTrue("Same salt must produce identical hash", hash1.contentEquals(hash3))
    }

    @Test
    fun `test incorrect PIN produces different hash`() {
        val salt = ByteArray(16) { 42 }
        val correctPin = "987654"
        val wrongPin = "987655"

        val md1 = MessageDigest.getInstance("SHA-256")
        md1.update(salt)
        val correctHash = md1.digest(correctPin.toByteArray(Charsets.UTF_8))

        val md2 = MessageDigest.getInstance("SHA-256")
        md2.update(salt)
        val wrongHash = md2.digest(wrongPin.toByteArray(Charsets.UTF_8))

        assertFalse("Wrong PIN must never match correct hash", correctHash.contentEquals(wrongHash))
    }

    @Test
    fun `test PinVerificationResult sealed class hierarchy`() {
        val success: PinVerificationResult = PinVerificationResult.Success
        val incorrect: PinVerificationResult = PinVerificationResult.Incorrect(attemptsRemaining = 3)
        val throttled: PinVerificationResult = PinVerificationResult.Throttled(remainingSeconds = 30)
        val noPinSet: PinVerificationResult = PinVerificationResult.NoPinSet

        assertTrue(success is PinVerificationResult.Success)
        assertEquals(3, (incorrect as PinVerificationResult.Incorrect).attemptsRemaining)
        assertEquals(30L, (throttled as PinVerificationResult.Throttled).remainingSeconds)
        assertTrue(noPinSet is PinVerificationResult.NoPinSet)
    }

    @Test
    fun `test Chat model hidden and locked status per user`() {
        val currentUid = "user_me"
        val otherUid = "user_friend"
        val chat = Chat(
            chatId = "chat_123",
            participants = listOf(currentUid, otherUid),
            participantInfo = mapOf(
                currentUid to ParticipantInfo("user_me", "Me", "me_user", null).toMap(),
                otherUid to ParticipantInfo("user_friend", "Friend", "friend_user", null).toMap()
            ),
            lastMessage = "Secret message",
            lastMessageTimestamp = Timestamp(Date()),
            lastMessageSenderId = otherUid,
            lockedBy = listOf(currentUid),
            hiddenBy = listOf(currentUid)
        )

        assertTrue("Chat must be locked for user_me", chat.isLocked(currentUid))
        assertFalse("Chat must NOT be locked for user_friend", chat.isLocked(otherUid))
        assertTrue("Chat must be hidden for user_me", chat.isHidden(currentUid))
        assertFalse("Chat must NOT be hidden for user_friend", chat.isHidden(otherUid))
    }

    @Test
    fun `test hidden and locked chat filtering logic`() {
        val currentUid = "user_me"
        val chatNormal = Chat(chatId = "chat_normal", participants = listOf(currentUid, "p1"))
        val chatHidden = Chat(
            chatId = "chat_hidden",
            participants = listOf(currentUid, "p2"),
            hiddenBy = listOf(currentUid)
        )
        val chatLocked = Chat(
            chatId = "chat_locked",
            participants = listOf(currentUid, "p3"),
            lockedBy = listOf(currentUid)
        )

        val allChats = listOf(chatNormal, chatHidden, chatLocked)

        val visibleChats = allChats.filter { !it.isHidden(currentUid) && !it.isLocked(currentUid) }

        assertEquals(1, visibleChats.size)
        assertEquals("chat_normal", visibleChats.first().chatId)
    }
}

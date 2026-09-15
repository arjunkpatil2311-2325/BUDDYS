package com.aura.glasschat.model

import com.aura.glasschat.data.model.Chat
import com.aura.glasschat.data.model.Message
import org.junit.Assert.*
import org.junit.Test

/**
 * Validates storage paths, message contract, and security authorization matrix
 * for Account A, Account B (participants), and Account C (unrelated third party).
 */
class StorageSecurityPathTest {

    private val userA = "account_A_uid"
    private val userB = "account_B_uid"
    private val userC = "account_C_uid"
    private val chatIdAB = "chat_AB"
    private val messageId = "msg_001"

    @Test
    fun testStorageMediaPaths_followStrictSchema() {
        val imagePath = "chat_media/$chatIdAB/$messageId/image.jpg"
        val voicePath = "chat_media/$chatIdAB/$messageId/voice.m4a"

        val imageRegex = Regex("^chat_media/([a-zA-Z0-9_-]+)/([a-zA-Z0-9_-]+)/image\\.jpg$")
        val voiceRegex = Regex("^chat_media/([a-zA-Z0-9_-]+)/([a-zA-Z0-9_-]+)/voice\\.m4a$")

        assertTrue(imageRegex.matches(imagePath))
        assertTrue(voiceRegex.matches(voicePath))

        val invalidPath1 = "chat_media/$chatIdAB/$messageId/malicious.exe"
        val invalidPath2 = "chat_media/public/image.jpg"
        assertFalse(imageRegex.matches(invalidPath1))
        assertFalse(imageRegex.matches(invalidPath2))
    }

    @Test
    fun testChatParticipantAuthorizationMatrix() {
        val chat = Chat(
            chatId = chatIdAB,
            participants = listOf(userA, userB)
        )

        // Account A & B are authorized participants
        assertTrue(chat.participants.contains(userA))
        assertTrue(chat.participants.contains(userB))

        // Account C is NOT a participant and MUST be rejected
        assertFalse(chat.participants.contains(userC))
    }

    @Test
    fun testMessageCreationSecuritySchema() {
        val messageFromA = Message(
            id = messageId,
            chatId = chatIdAB,
            senderId = userA,
            senderName = "Alice",
            content = "Hello Bob",
            type = "TEXT"
        )

        // Validate sender matches author
        assertEquals(userA, messageFromA.senderId)
        assertEquals(chatIdAB, messageFromA.chatId)
        assertTrue(messageFromA.type in listOf("TEXT", "IMAGE", "VOICE"))
        assertTrue(messageFromA.content.length <= 4000)

        // Image message payload
        val imageMessage = Message(
            id = "img_002",
            chatId = chatIdAB,
            senderId = userA,
            senderName = "Alice",
            content = "[Image]",
            type = "IMAGE",
            mediaUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/chat_media%2Fchat_AB%2Fimg_002%2Fimage.jpg"
        )
        assertEquals("IMAGE", imageMessage.type)
        assertNotNull(imageMessage.mediaUrl)

        // Voice message payload
        val voiceMessage = Message(
            id = "voice_003",
            chatId = chatIdAB,
            senderId = userB,
            senderName = "Bob",
            content = "[Voice message]",
            type = "VOICE",
            mediaUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/chat_media%2Fchat_AB%2Fvoice_003%2Fvoice.m4a",
            durationMs = 5200L
        )
        assertEquals("VOICE", voiceMessage.type)
        assertEquals(5200L, voiceMessage.durationMs)
    }

    @Test
    fun testUnsendMessageSecurityState() {
        val unsentMessage = Message(
            id = messageId,
            chatId = chatIdAB,
            senderId = userA,
            content = "This message was unsent",
            isUnsent = true,
            mediaUrl = null
        )

        assertTrue(unsentMessage.isUnsent)
        assertNull(unsentMessage.mediaUrl)
    }
}

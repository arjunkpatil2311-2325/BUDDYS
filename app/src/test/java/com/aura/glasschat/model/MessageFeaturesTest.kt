package com.aura.glasschat.model

import com.aura.glasschat.data.model.Chat
import com.aura.glasschat.data.model.Message
import com.aura.glasschat.util.ChatUtils
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class MessageFeaturesTest {

    @Test
    fun message_isVisibleToUser_respectsDeletedForUsers() {
        val userA = "user_A"
        val userB = "user_B"

        val message = Message(
            id = "msg_1",
            content = "Secret plan",
            senderId = userA,
            deletedForUsers = listOf(userA)
        )

        assertFalse(message.isVisibleToUser(userA))
        assertTrue(message.isVisibleToUser(userB))
    }

    @Test
    fun chat_pinAndMute_helpersWorkCorrectly() {
        val userA = "user_A"
        val userB = "user_B"

        val chat = Chat(
            chatId = "chat_1",
            participants = listOf(userA, userB),
            pinnedBy = listOf(userA),
            mutedBy = listOf(userB)
        )

        assertTrue(chat.isPinned(userA))
        assertFalse(chat.isPinned(userB))

        assertFalse(chat.isMuted(userA))
        assertTrue(chat.isMuted(userB))
    }

    @Test
    fun formatSeenStatus_formatsRelativeCorrectly() {
        val now = Timestamp.now()
        assertEquals("Seen just now", ChatUtils.formatSeenStatus(now))

        val fiveMinutesAgo = Timestamp(Date(System.currentTimeMillis() - 5 * 60 * 1000))
        assertEquals("Seen 5m ago", ChatUtils.formatSeenStatus(fiveMinutesAgo))

        val twoHoursAgo = Timestamp(Date(System.currentTimeMillis() - 2 * 3600 * 1000))
        assertEquals("Seen 2h ago", ChatUtils.formatSeenStatus(twoHoursAgo))
    }

    @Test
    fun message_defaultValues_areBackwardCompatible() {
        val textMessage = Message(
            id = "msg_legacy",
            content = "Hello there",
            senderId = "user_1"
        )

        assertEquals("TEXT", textMessage.type)
        assertFalse(textMessage.isImageMessage)
        assertFalse(textMessage.isVoiceMessage)
        assertNull(textMessage.mediaUrl)
        assertNull(textMessage.durationMs)
    }

    @Test
    fun message_imageAndVoice_propertiesAndSerialization() {
        val imageMsg = Message(
            id = "img_1",
            content = "Look at this view!",
            type = "IMAGE",
            mediaUrl = "https://storage.googleapis.com/test/image.jpg",
            senderId = "user_1"
        )
        assertTrue(imageMsg.isImageMessage)
        assertFalse(imageMsg.isVoiceMessage)
        val imageMap = imageMsg.toMap()
        assertEquals("IMAGE", imageMap["type"])
        assertEquals("https://storage.googleapis.com/test/image.jpg", imageMap["mediaUrl"])

        val voiceMsg = Message(
            id = "voice_1",
            content = "[Voice message]",
            type = "VOICE",
            mediaUrl = "https://storage.googleapis.com/test/voice.m4a",
            durationMs = 7400L,
            senderId = "user_1"
        )
        assertTrue(voiceMsg.isVoiceMessage)
        assertFalse(voiceMsg.isImageMessage)
        val voiceMap = voiceMsg.toMap()
        assertEquals("VOICE", voiceMap["type"])
        assertEquals(7400L, voiceMap["durationMs"])
    }

    @Test
    fun formatDuration_formatsCorrectly() {
        assertEquals("0:00", ChatUtils.formatDuration(null))
        assertEquals("0:00", ChatUtils.formatDuration(0L))
        assertEquals("0:07", ChatUtils.formatDuration(7000L))
        assertEquals("1:05", ChatUtils.formatDuration(65000L))
        assertEquals("2:30", ChatUtils.formatDuration(150000L))
    }
}

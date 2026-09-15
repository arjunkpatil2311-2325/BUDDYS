package com.aura.glasschat.model

import com.aura.glasschat.data.model.Chat
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class ReadReceiptTest {

    @Test
    fun hasUnread_returnsTrue_whenLastMessageNewerThanUserReadTimestamp() {
        val userA = "user_A"
        val userB = "user_B"

        val readTime = Timestamp(Date(1000000))
        val messageTime = Timestamp(Date(1005000)) // 5 seconds later

        val chat = Chat(
            chatId = "chat_user_A_user_B",
            participants = listOf(userA, userB),
            lastMessage = "Hey there!",
            lastMessageSenderId = userB,
            lastMessageTimestamp = messageTime,
            lastReadAt = mapOf(
                userA to readTime,
                userB to messageTime
            )
        )

        assertTrue(chat.hasUnread(userA))
        assertFalse(chat.hasUnread(userB)) // sender should not see unread
    }

    @Test
    fun hasUnread_returnsFalse_whenUserHasReadLatestMessage() {
        val userA = "user_A"
        val userB = "user_B"

        val messageTime = Timestamp(Date(1000000))
        val readTime = Timestamp(Date(1005000)) // user read after message was sent

        val chat = Chat(
            chatId = "chat_user_A_user_B",
            participants = listOf(userA, userB),
            lastMessage = "Hey there!",
            lastMessageSenderId = userB,
            lastMessageTimestamp = messageTime,
            lastReadAt = mapOf(
                userA to readTime,
                userB to messageTime
            )
        )

        assertFalse(chat.hasUnread(userA))
    }
}

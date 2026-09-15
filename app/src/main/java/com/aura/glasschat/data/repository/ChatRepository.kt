package com.aura.glasschat.data.repository

import com.aura.glasschat.data.model.Chat
import com.aura.glasschat.data.model.Message
import com.aura.glasschat.util.ChatUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val CHATS_COLLECTION = "chats"
        private const val MESSAGES_SUBCOLLECTION = "messages"
    }

    /**
     * Observes all active conversations where current user is a participant.
     */
    fun observeUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)?.copy(chatId = doc.id)
                    }.sortedByDescending { it.lastMessageTimestamp?.seconds ?: 0L }
                    trySend(chats)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Observes a single chat document in real time for typing status, read receipts, and metadata.
     */
    fun observeChat(chatId: String): Flow<Chat?> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val chat = snapshot.toObject(Chat::class.java)?.copy(chatId = snapshot.id)
                trySend(chat)
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Observes real-time message stream for an active chat thread.
     */
    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                    trySend(messages)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Updates the user's typing status for real-time typing indicators.
     */
    suspend fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        if (chatId.isBlank() || userId.isBlank()) return
        try {
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("typingUsers.$userId", isTyping)
                .await()
        } catch (_: Exception) {}
    }

    /**
     * Sends a real-time message and updates the chat metadata atomically.
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String,
        replyToMessage: Message? = null
    ): Result<Message> {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message content cannot be empty"))
        }

        return try {
            val now = Timestamp.now()
            val msgRef = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document()

            val message = Message(
                id = msgRef.id,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                content = trimmedContent,
                type = "TEXT",
                timestamp = now,
                mediaUrl = null,
                status = "SENT",
                replyToMessageId = replyToMessage?.id,
                replyToText = when {
                    replyToMessage == null -> null
                    replyToMessage.isImageMessage -> "🖼️ Image"
                    replyToMessage.isVoiceMessage -> "🎤 Voice message · ${ChatUtils.formatDuration(replyToMessage.durationMs)}"
                    else -> replyToMessage.content
                },
                replyToSenderName = replyToMessage?.senderName,
                replyToSenderId = replyToMessage?.senderId
            )

            val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)

            val batch = firestore.batch()
            batch.set(msgRef, message.toMap())
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to trimmedContent,
                    "lastMessageSenderId" to senderId,
                    "lastMessageTimestamp" to now,
                    "lastReadAt.$senderId" to now,
                    "typingUsers.$senderId" to false
                )
            )

            batch.commit().await()
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends an image message and updates the chat metadata.
     */
    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        imageUrl: String,
        caption: String = "",
        replyToMessage: Message? = null,
        messageId: String? = null
    ): Result<Message> {
        if (imageUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Image URL cannot be empty"))
        }

        return try {
            val now = Timestamp.now()
            val msgRef = if (!messageId.isNullOrBlank()) {
                firestore.collection(CHATS_COLLECTION)
                    .document(chatId)
                    .collection(MESSAGES_SUBCOLLECTION)
                    .document(messageId)
            } else {
                firestore.collection(CHATS_COLLECTION)
                    .document(chatId)
                    .collection(MESSAGES_SUBCOLLECTION)
                    .document()
            }

            val displayContent = caption.trim().ifEmpty { "[Image]" }
            val lastMsgText = if (caption.isNotBlank()) "📷 $caption" else "📷 Photo"

            val message = Message(
                id = msgRef.id,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                content = displayContent,
                type = "IMAGE",
                timestamp = now,
                mediaUrl = imageUrl,
                status = "SENT",
                replyToMessageId = replyToMessage?.id,
                replyToText = when {
                    replyToMessage == null -> null
                    replyToMessage.isImageMessage -> "🖼️ Image"
                    replyToMessage.isVoiceMessage -> "🎤 Voice message · ${ChatUtils.formatDuration(replyToMessage.durationMs)}"
                    else -> replyToMessage.content
                },
                replyToSenderName = replyToMessage?.senderName,
                replyToSenderId = replyToMessage?.senderId
            )

            val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)

            val batch = firestore.batch()
            batch.set(msgRef, message.toMap())
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to lastMsgText,
                    "lastMessageSenderId" to senderId,
                    "lastMessageTimestamp" to now,
                    "lastReadAt.$senderId" to now,
                    "typingUsers.$senderId" to false
                )
            )

            batch.commit().await()
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a voice message and updates the chat metadata.
     */
    suspend fun sendVoiceMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        voiceUrl: String,
        durationMs: Long,
        replyToMessage: Message? = null,
        messageId: String? = null
    ): Result<Message> {
        if (voiceUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Voice URL cannot be empty"))
        }

        return try {
            val now = Timestamp.now()
            val msgRef = if (!messageId.isNullOrBlank()) {
                firestore.collection(CHATS_COLLECTION)
                    .document(chatId)
                    .collection(MESSAGES_SUBCOLLECTION)
                    .document(messageId)
            } else {
                firestore.collection(CHATS_COLLECTION)
                    .document(chatId)
                    .collection(MESSAGES_SUBCOLLECTION)
                    .document()
            }

            val lastMsgText = "🎤 Voice message (${ChatUtils.formatDuration(durationMs)})"

            val message = Message(
                id = msgRef.id,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                content = "[Voice message]",
                type = "VOICE",
                timestamp = now,
                mediaUrl = voiceUrl,
                durationMs = durationMs,
                status = "SENT",
                replyToMessageId = replyToMessage?.id,
                replyToText = when {
                    replyToMessage == null -> null
                    replyToMessage.isImageMessage -> "🖼️ Image"
                    replyToMessage.isVoiceMessage -> "🎤 Voice message · ${ChatUtils.formatDuration(replyToMessage.durationMs)}"
                    else -> replyToMessage.content
                },
                replyToSenderName = replyToMessage?.senderName,
                replyToSenderId = replyToMessage?.senderId
            )

            val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)

            val batch = firestore.batch()
            batch.set(msgRef, message.toMap())
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to lastMsgText,
                    "lastMessageSenderId" to senderId,
                    "lastMessageTimestamp" to now,
                    "lastReadAt.$senderId" to now,
                    "typingUsers.$senderId" to false
                )
            )

            batch.commit().await()
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Edits a message sent by the current user.
     */
    suspend fun editMessage(
        chatId: String,
        messageId: String,
        senderId: String,
        newContent: String
    ): Result<Unit> {
        val trimmed = newContent.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message content cannot be empty"))
        }

        return try {
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document(messageId)
                .update(
                    mapOf(
                        "content" to trimmed,
                        "isEdited" to true,
                        "editedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unsends a message sent by current user.
     */
    suspend fun unsendMessage(
        chatId: String,
        messageId: String,
        senderId: String
    ): Result<Unit> {
        return try {
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document(messageId)
                .update(
                    mapOf(
                        "content" to "This message was unsent",
                        "isUnsent" to true,
                        "mediaUrl" to null,
                        "thumbnailUrl" to null
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Hides a message only for the current user (Delete for Me).
     */
    suspend fun deleteMessageForMe(
        chatId: String,
        messageId: String,
        userId: String
    ): Result<Unit> {
        return try {
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document(messageId)
                .update("deletedForUsers", FieldValue.arrayUnion(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggles reaction on a message (adds or removes if already set to same emoji).
     */
    suspend fun toggleReaction(
        chatId: String,
        messageId: String,
        userId: String,
        emoji: String,
        currentReaction: String?
    ): Result<Unit> {
        return try {
            val msgRef = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document(messageId)

            if (currentReaction == emoji) {
                msgRef.update("reactions.$userId", FieldValue.delete()).await()
            } else {
                msgRef.update("reactions.$userId", emoji).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marks incoming messages as DELIVERED when received by recipient device.
     */
    suspend fun markMessagesAsDelivered(chatId: String, messageIds: List<String>) {
        if (chatId.isBlank() || messageIds.isEmpty()) return
        try {
            val now = Timestamp.now()
            val batch = firestore.batch()
            for (msgId in messageIds.take(50)) {
                val docRef = firestore.collection(CHATS_COLLECTION)
                    .document(chatId)
                    .collection(MESSAGES_SUBCOLLECTION)
                    .document(msgId)
                batch.update(
                    docRef,
                    mapOf(
                        "status" to "DELIVERED",
                        "deliveredAt" to now
                    )
                )
            }
            batch.commit().await()
            android.util.Log.d("BUDDYS_RECEIPTS", "Marked ${messageIds.size} messages as DELIVERED in chat $chatId")
        } catch (e: Exception) {
            android.util.Log.w("BUDDYS_RECEIPTS", "Failed to mark messages as DELIVERED", e)
        }
    }

    /**
     * Acknowledges incoming delivery for a chat when the recipient is on HomeScreen or elsewhere in the app.
     */
    suspend fun acknowledgeChatDelivery(chatId: String, currentUid: String) {
        if (chatId.isBlank() || currentUid.isBlank()) return
        try {
            val recentDocs = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()

            val undeliveredIds = recentDocs.documents.filter { doc ->
                val senderId = doc.getString("senderId")
                val status = doc.getString("status")
                senderId != currentUid && status == "SENT"
            }.map { it.id }

            if (undeliveredIds.isNotEmpty()) {
                markMessagesAsDelivered(chatId, undeliveredIds)
            }
        } catch (e: Exception) {
            android.util.Log.w("BUDDYS_RECEIPTS", "acknowledgeChatDelivery error", e)
        }
    }

    /**
     * Updates the user's read timestamp for the chat and marks unread incoming messages as SEEN.
     * Takes explicit list of message IDs to avoid requiring compound Firestore indexes.
     */
    suspend fun markMessagesAsSeen(chatId: String, messageIds: List<String>, userId: String) {
        if (chatId.isBlank() || userId.isBlank()) return
        try {
            val now = Timestamp.now()
            val batch = firestore.batch()

            // Update chat lastReadAt
            val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)
            batch.update(chatRef, "lastReadAt.$userId", now)

            // Update individual messages
            for (msgId in messageIds.take(50)) {
                val docRef = firestore.collection(CHATS_COLLECTION)
                    .document(chatId)
                    .collection(MESSAGES_SUBCOLLECTION)
                    .document(msgId)
                batch.update(
                    docRef,
                    mapOf(
                        "status" to "SEEN",
                        "seenAt" to now
                    )
                )
            }
            batch.commit().await()
            android.util.Log.d("BUDDYS_RECEIPTS", "Marked ${messageIds.size} messages as SEEN in chat $chatId by $userId")
        } catch (e: Exception) {
            android.util.Log.w("BUDDYS_RECEIPTS", "Failed to mark messages as SEEN", e)
        }
    }

    /**
     * Pins or unpins a chat for the current user.
     */
    suspend fun togglePinChat(chatId: String, userId: String, isCurrentlyPinned: Boolean) {
        if (chatId.isBlank() || userId.isBlank()) return
        try {
            val op = if (isCurrentlyPinned) FieldValue.arrayRemove(userId) else FieldValue.arrayUnion(userId)
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("pinnedBy", op)
                .await()
        } catch (_: Exception) {}
    }

    /**
     * Pins a message in the conversation.
     */
    suspend fun pinMessage(chatId: String, messageId: String): Result<Unit> {
        if (chatId.isBlank() || messageId.isBlank()) return Result.failure(IllegalArgumentException("IDs cannot be blank"))
        return try {
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("pinnedMessageIds", FieldValue.arrayUnion(messageId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unpins a message in the conversation.
     */
    suspend fun unpinMessage(chatId: String, messageId: String): Result<Unit> {
        if (chatId.isBlank() || messageId.isBlank()) return Result.failure(IllegalArgumentException("IDs cannot be blank"))
        return try {
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("pinnedMessageIds", FieldValue.arrayRemove(messageId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mutes or unmutes a chat for the current user.
     */
    suspend fun toggleMuteChat(chatId: String, userId: String, isCurrentlyMuted: Boolean) {
        if (chatId.isBlank() || userId.isBlank()) return
        try {
            val op = if (isCurrentlyMuted) FieldValue.arrayRemove(userId) else FieldValue.arrayUnion(userId)
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("mutedBy", op)
                .await()
        } catch (_: Exception) {}
    }

    /**
     * Forwards a list of messages to another chat thread.
     */
    suspend fun forwardMessages(
        targetChatId: String,
        senderId: String,
        senderName: String,
        messages: List<Message>
    ): Result<Unit> {
        return try {
            for (msg in messages) {
                if (msg.isUnsent) continue
                when {
                    msg.isImageMessage && !msg.mediaUrl.isNullOrBlank() -> {
                        sendImageMessage(
                            chatId = targetChatId,
                            senderId = senderId,
                            senderName = senderName,
                            imageUrl = msg.mediaUrl,
                            caption = if (msg.content != "[Image]") msg.content else ""
                        )
                    }
                    msg.isVoiceMessage && !msg.mediaUrl.isNullOrBlank() -> {
                        sendVoiceMessage(
                            chatId = targetChatId,
                            senderId = senderId,
                            senderName = senderName,
                            voiceUrl = msg.mediaUrl,
                            durationMs = msg.durationMs ?: 0L
                        )
                    }
                    else -> {
                        if (msg.content.isNotBlank()) {
                            sendMessage(
                                chatId = targetChatId,
                                senderId = senderId,
                                senderName = senderName,
                                content = msg.content
                            )
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggles lock status for the current user.
     */
    suspend fun toggleLockChat(chatId: String, userId: String, isCurrentlyLocked: Boolean) {
        if (chatId.isBlank() || userId.isBlank()) return
        try {
            val op = if (isCurrentlyLocked) FieldValue.arrayRemove(userId) else FieldValue.arrayUnion(userId)
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("lockedBy", op)
                .await()
        } catch (_: Exception) {}
    }

    /**
     * Toggles hidden status for the current user.
     */
    suspend fun toggleHideChat(chatId: String, userId: String, isCurrentlyHidden: Boolean) {
        if (chatId.isBlank() || userId.isBlank()) return
        try {
            val op = if (isCurrentlyHidden) FieldValue.arrayRemove(userId) else FieldValue.arrayUnion(userId)
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("hiddenBy", op)
                .await()
        } catch (_: Exception) {}
    }

    /**
     * Logs a missed call system message into the conversation thread.
     */
    suspend fun logMissedCallMessage(
        chatId: String,
        callerId: String,
        callerName: String,
        callType: String = "VOICE"
    ): Result<Message> {
        return try {
            val now = Timestamp.now()
            val msgRef = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document()

            val text = "📞 Missed $callType Call"
            val message = Message(
                id = msgRef.id,
                chatId = chatId,
                senderId = callerId,
                senderName = callerName,
                content = text,
                type = "MISSED_CALL",
                timestamp = now,
                status = "SENT"
            )

            val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)
            val batch = firestore.batch()
            batch.set(msgRef, message.toMap())
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to text,
                    "lastMessageSenderId" to callerId,
                    "lastMessageTimestamp" to now
                )
            )
            batch.commit().await()
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a story reply message with media preview.
     */
    suspend fun sendStoryReplyMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        replyText: String,
        storyId: String,
        storyImageUrl: String?
    ): Result<Message> {
        return try {
            val now = Timestamp.now()
            val msgRef = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document()

            val message = Message(
                id = msgRef.id,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                content = replyText,
                type = "STORY_REPLY",
                timestamp = now,
                mediaUrl = storyImageUrl,
                status = "SENT",
                replyToMessageId = storyId,
                replyToText = "Replied to story",
                storyReplyPreviewUrl = storyImageUrl
            )

            val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)
            val batch = firestore.batch()
            batch.set(msgRef, message.toMap())
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to "Replied to story: $replyText",
                    "lastMessageSenderId" to senderId,
                    "lastMessageTimestamp" to now,
                    "lastReadAt.$senderId" to now,
                    "typingUsers.$senderId" to false
                )
            )
            batch.commit().await()
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets or creates a direct conversation between two users.
     */
    suspend fun getOrCreateDirectChat(
        currentUserId: String,
        otherUserId: String,
        otherUserName: String = "",
        otherUserAvatar: String? = null
    ): Result<String> {
        return try {
            val query = firestore.collection(CHATS_COLLECTION)
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            val existingChat = query.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*>
                val isGroup = doc.getBoolean("isGroup") ?: false
                !isGroup && participants?.contains(otherUserId) == true
            }

            if (existingChat != null) {
                Result.success(existingChat.id)
            } else {
                val newChatRef = firestore.collection(CHATS_COLLECTION).document()
                val now = Timestamp.now()
                val newChat = hashMapOf(
                    "chatId" to newChatRef.id,
                    "participants" to listOf(currentUserId, otherUserId),
                    "participantNames" to mapOf(otherUserId to otherUserName),
                    "participantAvatars" to if (otherUserAvatar != null) mapOf(otherUserId to otherUserAvatar) else emptyMap<String, String>(),
                    "isGroup" to false,
                    "createdAt" to now,
                    "lastMessageTimestamp" to now
                )
                newChatRef.set(newChat).await()
                Result.success(newChatRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

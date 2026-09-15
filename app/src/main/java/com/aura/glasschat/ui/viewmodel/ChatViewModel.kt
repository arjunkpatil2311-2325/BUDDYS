package com.aura.glasschat.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.Friend
import com.aura.glasschat.data.model.Message
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.ChatRepository
import com.aura.glasschat.data.repository.ChatThemeRepository
import com.aura.glasschat.data.repository.MediaRepository
import com.aura.glasschat.data.repository.PresenceRepository
import com.aura.glasschat.data.repository.SavedMessagesRepository
import com.aura.glasschat.data.repository.UserRepository
import com.aura.glasschat.util.PlaybackState
import com.aura.glasschat.util.VoicePlayer
import com.aura.glasschat.util.VoiceRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class LinkItem(
    val url: String,
    val domain: String,
    val message: Message
)

data class ChatUiState(
    val chatId: String = "",
    val otherUser: User? = null,
    val currentUserId: String = "",
    val messages: List<Message> = emptyList(),
    val searchQuery: String = "",
    val isSearchModeActive: Boolean = false,
    val highlightedMessageId: String? = null,
    val inputText: String = "",
    val replyingToMessage: Message? = null,
    val editingMessage: Message? = null,
    val isOtherUserTyping: Boolean = false,
    val isSending: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedMessageIds: Set<String> = emptySet(),
    val showForwardDialog: Boolean = false,
    val friends: List<Friend> = emptyList(),
    val activeMenuMessage: Message? = null,

    // Pinned Messages
    val pinnedMessageIds: List<String> = emptyList(),
    val showPinnedSheet: Boolean = false,

    // Media, Links & Files
    val showMediaLinksFilesSheet: Boolean = false,

    // In-Chat Presence & Unread Dividers
    val isOtherUserInChat: Boolean = false,
    val firstUnreadMessageId: String? = null,
    val unreadCount: Int = 0,

    // Image Sharing States
    val selectedImageUri: Uri? = null,
    val selectedImageBytes: ByteArray? = null,
    val isUploadingMedia: Boolean = false,
    val showAttachmentMenu: Boolean = false,
    val fullScreenImageUrl: String? = null,

    // Voice Messaging States
    val isRecordingAudio: Boolean = false,
    val isRecordingLocked: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationSec: Int = 0,
    val liveWaveformAmplitudes: List<Float> = emptyList(),
    val recordedVoiceFile: File? = null,
    val recordedVoiceDurationMs: Long = 0L,

    // Audio Playback State for active voice messages
    val playbackState: PlaybackState = PlaybackState(),

    // Custom Chat Wallpaper Background
    val chatBackgroundPath: String? = null,
    val previewBackgroundUri: Uri? = null,
    val showThemeSheet: Boolean = false,

    // Chat Lock & Gate
    val isChatLocked: Boolean = false,
    val isChatUnlockedForSession: Boolean = false,
    val isChatHidden: Boolean = false
) {
    val visibleMessages: List<Message>
        get() = messages.filter { it.isVisibleToUser(currentUserId) }

    val searchResults: List<Message>
        get() {
            if (searchQuery.isBlank()) return emptyList()
            val cleanQuery = searchQuery.trim()
            return messages.filter {
                it.isVisibleToUser(currentUserId) &&
                !it.isUnsent &&
                it.content.contains(cleanQuery, ignoreCase = true)
            }
        }

    val pinnedMessages: List<Message>
        get() = messages.filter {
            pinnedMessageIds.contains(it.id) &&
            it.isVisibleToUser(currentUserId) &&
            !it.isUnsent
        }

    val mediaMessages: List<Message>
        get() = messages.filter {
            it.isImageMessage &&
            !it.mediaUrl.isNullOrBlank() &&
            it.isVisibleToUser(currentUserId) &&
            !it.isUnsent
        }

    val linkItems: List<LinkItem>
        get() {
            val urlRegex = "(https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?)".toRegex(RegexOption.IGNORE_CASE)
            val result = mutableListOf<LinkItem>()
            messages.filter { it.isVisibleToUser(currentUserId) && !it.isUnsent }.forEach { msg ->
                val matches = urlRegex.findAll(msg.content)
                for (m in matches) {
                    val url = m.value
                    val domain = try {
                        java.net.URI(url).host ?: url
                    } catch (_: Exception) {
                        url
                    }
                    result.add(LinkItem(url = url, domain = domain, message = msg))
                }
            }
            return result
        }

    val voiceMessages: List<Message>
        get() = messages.filter {
            it.isVoiceMessage &&
            !it.mediaUrl.isNullOrBlank() &&
            it.isVisibleToUser(currentUserId) &&
            !it.isUnsent
        }
}

class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val chatRepository: ChatRepository = ChatRepository(),
    private val mediaRepository: MediaRepository = MediaRepository(),
    private val presenceRepository: PresenceRepository = PresenceRepository(),
    private val savedMessagesRepository: SavedMessagesRepository = SavedMessagesRepository(),
    private val chatThemeRepository: ChatThemeRepository = ChatThemeRepository(application.applicationContext)
) : AndroidViewModel(application) {

    private val draftManager = com.aura.glasschat.util.DraftManager(application.applicationContext)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var typingJob: Job? = null
    private var recordingTimerJob: Job? = null

    private val voiceRecorder = VoiceRecorder(application.applicationContext)
    private val voicePlayer = VoicePlayer(application.applicationContext)

    init {
        // Observe playback state from VoicePlayer
        viewModelScope.launch {
            voicePlayer.playbackState.collect { pbState ->
                _uiState.update { it.copy(playbackState = pbState) }
            }
        }
    }

    private val markedSeenIds = mutableSetOf<String>()
    private var isScreenActive = true

    fun setScreenActive(active: Boolean) {
        isScreenActive = active
        if (active) {
            markVisibleMessagesSeen()
        }
    }

    private fun markVisibleMessagesSeen() {
        val state = _uiState.value
        val chatId = state.chatId
        val currentUid = state.currentUserId
        if (chatId.isBlank() || currentUid.isBlank() || !isScreenActive) return

        val unreadIncoming = state.messages.filter { 
            it.senderId != currentUid && it.status != "SEEN" && !markedSeenIds.contains(it.id)
        }

        if (unreadIncoming.isNotEmpty()) {
            val idsToMark = unreadIncoming.map { it.id }
            markedSeenIds.addAll(idsToMark)
            viewModelScope.launch {
                chatRepository.markMessagesAsSeen(chatId, idsToMark, currentUid)
            }
        }
    }

    fun unlockChatForSession() {
        _uiState.update { it.copy(isChatUnlockedForSession = true) }
    }

    fun initChat(chatId: String, otherUserId: String) {
        val currentUid = authRepository.currentUserId
        val savedDraft = draftManager.getDraft(chatId)

        _uiState.update {
            it.copy(
                chatId = chatId,
                currentUserId = currentUid,
                inputText = savedDraft,
                isLoading = true
            )
        }

        markedSeenIds.clear()
        isScreenActive = true

        // Set "In this chat" presence
        viewModelScope.launch {
            presenceRepository.setChatPresence(chatId, currentUid, true)
        }

        // 1. Observe other user profile & online state
        viewModelScope.launch {
            userRepository.observeUserProfile(otherUserId).collect { user ->
                _uiState.update { it.copy(otherUser = user) }
            }
        }

        // 2. Observe "In this chat" real-time presence of other user
        viewModelScope.launch {
            presenceRepository.observeOtherUserInChat(chatId, otherUserId).collect { inChat ->
                _uiState.update { it.copy(isOtherUserInChat = inChat) }
            }
        }

        // 3. Observe chat for real-time typing indicators & metadata
        viewModelScope.launch {
            chatRepository.observeChat(chatId).collect { chat ->
                if (chat != null) {
                    val isTyping = chat.isUserTyping(otherUserId)
                    val isLocked = chat.isLocked(currentUid)
                    val isHidden = chat.isHidden(currentUid)
                    _uiState.update { 
                        it.copy(
                            isOtherUserTyping = isTyping,
                            pinnedMessageIds = chat.pinnedMessageIds,
                            isChatLocked = isLocked,
                            isChatHidden = isHidden
                        ) 
                    }
                }
            }
        }

        // 4. Observe real-time messages & calculate unread divider
        viewModelScope.launch {
            chatRepository.observeMessages(chatId).collect { msgs ->
                val unreadMsgs = msgs.filter { it.senderId != currentUid && it.status != "SEEN" }
                val firstUnreadId = unreadMsgs.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        messages = msgs,
                        firstUnreadMessageId = firstUnreadId,
                        unreadCount = unreadMsgs.size,
                        isLoading = false
                    )
                }

                // If currently viewing this screen, mark incoming messages as SEEN
                if (isScreenActive) {
                    markVisibleMessagesSeen()
                } else {
                    // Mark as DELIVERED if not actively in screen
                    val undelivered = msgs.filter { it.senderId != currentUid && it.status == "SENT" }
                    if (undelivered.isNotEmpty()) {
                        chatRepository.markMessagesAsDelivered(chatId, undelivered.map { it.id })
                    }
                }
            }
        }

        // 5. Observe friends for forward dialog
        viewModelScope.launch {
            userRepository.observeFriends(currentUid).collect { friendsList ->
                _uiState.update { it.copy(friends = friendsList) }
            }
        }

        // 6. Observe custom wallpaper background for this chat
        viewModelScope.launch {
            chatThemeRepository.getBackgroundFlow(chatId).collect { bgPath ->
                _uiState.update { it.copy(chatBackgroundPath = bgPath) }
            }
        }
    }

    // ==========================================
    // CHAT BACKGROUND CUSTOMIZATION
    // ==========================================

    fun openThemeSheet() {
        _uiState.update { it.copy(showThemeSheet = true, previewBackgroundUri = null) }
    }

    fun dismissThemeSheet() {
        _uiState.update { it.copy(showThemeSheet = false, previewBackgroundUri = null) }
    }

    fun onBackgroundSelectedForPreview(uri: Uri) {
        _uiState.update { it.copy(previewBackgroundUri = uri) }
    }

    fun cancelBackgroundPreview() {
        _uiState.update { it.copy(previewBackgroundUri = null) }
    }

    fun applyChatBackground() {
        val uri = _uiState.value.previewBackgroundUri ?: return
        val chatId = _uiState.value.chatId
        if (chatId.isBlank()) return

        viewModelScope.launch {
            val result = chatThemeRepository.saveChatBackground(chatId, uri)
            result.fold(
                onSuccess = { path ->
                    _uiState.update { 
                        it.copy(
                            chatBackgroundPath = path,
                            previewBackgroundUri = null,
                            showThemeSheet = false
                        ) 
                    }
                },
                onFailure = { err ->
                    _uiState.update { 
                        it.copy(
                            errorMessage = "Failed to set background: ${err.localizedMessage ?: "Unknown error"}"
                        ) 
                    }
                }
            )
        }
    }

    fun removeChatBackground() {
        val chatId = _uiState.value.chatId
        if (chatId.isBlank()) return

        viewModelScope.launch {
            chatThemeRepository.removeChatBackground(chatId)
            _uiState.update { 
                it.copy(
                    chatBackgroundPath = null,
                    previewBackgroundUri = null,
                    showThemeSheet = false
                ) 
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }

        val chatId = _uiState.value.chatId
        val currentUid = _uiState.value.currentUserId
        if (chatId.isBlank() || currentUid.isBlank()) return

        draftManager.saveDraft(chatId, text)

        // Manage typing status debounce
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            if (text.isNotBlank()) {
                chatRepository.setTypingStatus(chatId, currentUid, true)
                delay(2500)
                chatRepository.setTypingStatus(chatId, currentUid, false)
            } else {
                chatRepository.setTypingStatus(chatId, currentUid, false)
            }
        }
    }

    fun openSearch() {
        _uiState.update { it.copy(isSearchModeActive = true, searchQuery = "") }
    }

    fun closeSearch() {
        _uiState.update { it.copy(isSearchModeActive = false, searchQuery = "") }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun highlightMessage(messageId: String) {
        _uiState.update { it.copy(highlightedMessageId = messageId) }
        viewModelScope.launch {
            delay(1800)
            if (_uiState.value.highlightedMessageId == messageId) {
                _uiState.update { it.copy(highlightedMessageId = null) }
            }
        }
    }

    fun pinMessage(messageId: String) {
        val chatId = _uiState.value.chatId
        if (chatId.isBlank() || messageId.isBlank()) return
        viewModelScope.launch {
            val result = chatRepository.pinMessage(chatId, messageId)
            result.fold(
                onSuccess = {
                    closeMessageMenu()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to pin message: ${err.localizedMessage ?: "Unknown error"}")
                    }
                }
            )
        }
    }

    fun pinMessage(message: Message) = pinMessage(message.id)

    fun togglePinMessage(message: Message) {
        if (_uiState.value.pinnedMessageIds.contains(message.id)) {
            unpinMessage(message.id)
        } else {
            pinMessage(message.id)
        }
    }

    fun unpinMessage(messageId: String) {
        val chatId = _uiState.value.chatId
        if (chatId.isBlank() || messageId.isBlank()) return
        viewModelScope.launch {
            val result = chatRepository.unpinMessage(chatId, messageId)
            result.fold(
                onSuccess = {
                    closeMessageMenu()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to unpin message: ${err.localizedMessage ?: "Unknown error"}")
                    }
                }
            )
        }
    }

    fun openPinnedSheet() {
        _uiState.update { it.copy(showPinnedSheet = true) }
    }

    fun dismissPinnedSheet() {
        _uiState.update { it.copy(showPinnedSheet = false) }
    }

    fun openMediaLinksFilesSheet() {
        _uiState.update { it.copy(showMediaLinksFilesSheet = true) }
    }

    fun dismissMediaLinksFilesSheet() {
        _uiState.update { it.copy(showMediaLinksFilesSheet = false) }
    }

    fun openAttachmentMenu() {
        _uiState.update { it.copy(showAttachmentMenu = true) }
    }

    fun dismissAttachmentMenu() {
        _uiState.update { it.copy(showAttachmentMenu = false) }
    }

    // ==========================================
    // IMAGE HANDLING
    // ==========================================

    fun onImageSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                showAttachmentMenu = false,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = mediaRepository.compressImage(getApplication(), uri)
            result.fold(
                onSuccess = { bytes ->
                    _uiState.update { it.copy(selectedImageBytes = bytes) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            selectedImageUri = null,
                            selectedImageBytes = null,
                            errorMessage = "Failed to process selected image: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun cancelSelectedImage() {
        _uiState.update {
            it.copy(
                selectedImageUri = null,
                selectedImageBytes = null
            )
        }
    }

    fun sendSelectedImage() {
        val state = _uiState.value
        val uri = state.selectedImageUri
        val cachedBytes = state.selectedImageBytes
        if ((uri == null && cachedBytes == null) || state.isUploadingMedia) return

        val chatId = state.chatId
        val currentUid = state.currentUserId
        val currentUser = authRepository.currentUser
        val senderName = currentUser?.displayName ?: "User"
        val caption = state.inputText.trim()
        val replying = state.replyingToMessage
        val tempMessageId = UUID.randomUUID().toString()

        _uiState.update {
            it.copy(
                isUploadingMedia = true,
                selectedImageUri = null,
                selectedImageBytes = null,
                inputText = "",
                replyingToMessage = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val bytes = cachedBytes ?: if (uri != null) {
                val compressResult = mediaRepository.compressImage(getApplication(), uri)
                compressResult.getOrNull()
            } else null

            if (bytes == null) {
                _uiState.update {
                    it.copy(
                        isUploadingMedia = false,
                        errorMessage = "Couldn't process image. Please try again."
                    )
                }
                return@launch
            }

            val uploadResult = mediaRepository.uploadChatImage(
                chatId = chatId,
                messageId = tempMessageId,
                imageBytes = bytes,
                senderId = currentUid
            )
            uploadResult.fold(
                onSuccess = { downloadUrl ->
                    val sendResult = chatRepository.sendImageMessage(
                        chatId = chatId,
                        senderId = currentUid,
                        senderName = senderName,
                        imageUrl = downloadUrl,
                        caption = caption,
                        replyToMessage = replying,
                        messageId = tempMessageId
                    )
                    sendResult.fold(
                        onSuccess = {
                            _uiState.update { it.copy(isUploadingMedia = false) }
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    isUploadingMedia = false,
                                    errorMessage = "Couldn't send image: ${err.localizedMessage ?: "Please try again."}"
                                )
                            }
                        }
                    )
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isUploadingMedia = false,
                            errorMessage = com.aura.glasschat.util.ChatUtils.getFriendlyStorageErrorMessage(err)
                        )
                    }
                }
            )
        }
    }

    fun openFullScreenImage(url: String) {
        _uiState.update { it.copy(fullScreenImageUrl = url) }
    }

    fun closeFullScreenImage() {
        _uiState.update { it.copy(fullScreenImageUrl = null) }
    }

    // ==========================================
    // VOICE RECORDING & PLAYBACK
    // ==========================================

    fun startAudioRecording() {
        voicePlayer.stop()
        val result = voiceRecorder.startRecording()
        result.fold(
            onSuccess = {
                _uiState.update {
                    it.copy(
                        isRecordingAudio = true,
                        isRecordingLocked = false,
                        isRecordingPaused = false,
                        recordingDurationSec = 0,
                        liveWaveformAmplitudes = emptyList(),
                        recordedVoiceFile = null,
                        recordedVoiceDurationMs = 0L,
                        errorMessage = null
                    )
                }
                startRecordingTimer()
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isRecordingAudio = false,
                        isRecordingLocked = false,
                        isRecordingPaused = false,
                        errorMessage = "Microphone unavailable: ${error.localizedMessage}"
                    )
                }
            }
        )
    }

    fun lockAudioRecording() {
        _uiState.update { it.copy(isRecordingLocked = true) }
    }

    fun pauseAudioRecording() {
        if (voiceRecorder.pauseRecording()) {
            _uiState.update { it.copy(isRecordingPaused = true) }
        }
    }

    fun resumeAudioRecording() {
        if (voiceRecorder.resumeRecording()) {
            _uiState.update { it.copy(isRecordingPaused = false) }
        }
    }

    fun stopAudioRecording() {
        stopRecordingTimer()
        val result = voiceRecorder.stopRecording()
        result.fold(
            onSuccess = { recordedAudio ->
                _uiState.update {
                    it.copy(
                        isRecordingAudio = false,
                        isRecordingLocked = false,
                        isRecordingPaused = false,
                        recordedVoiceFile = recordedAudio.file,
                        recordedVoiceDurationMs = recordedAudio.durationMs
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isRecordingAudio = false,
                        isRecordingLocked = false,
                        isRecordingPaused = false,
                        recordedVoiceFile = null,
                        recordedVoiceDurationMs = 0L,
                        errorMessage = if (error.message?.contains("short") == true) null else "Voice recording failed"
                    )
                }
            }
        )
    }

    fun cancelAudioRecording() {
        stopRecordingTimer()
        voiceRecorder.cancelRecording()
        _uiState.update {
            it.copy(
                isRecordingAudio = false,
                isRecordingLocked = false,
                isRecordingPaused = false,
                recordingDurationSec = 0,
                liveWaveformAmplitudes = emptyList(),
                recordedVoiceFile = null,
                recordedVoiceDurationMs = 0L
            )
        }
    }

    fun playVoicePreview() {
        val file = _uiState.value.recordedVoiceFile ?: return
        voicePlayer.playLocalFile("preview_voice", file)
    }

    fun pauseVoicePreview() {
        voicePlayer.pause()
    }

    fun deleteVoicePreview() {
        voicePlayer.stop()
        _uiState.value.recordedVoiceFile?.delete()
        _uiState.update {
            it.copy(
                recordedVoiceFile = null,
                recordedVoiceDurationMs = 0L
            )
        }
    }

    fun sendVoiceMessage() {
        val state = _uiState.value
        val audioFile = state.recordedVoiceFile ?: return
        val durationMs = state.recordedVoiceDurationMs
        if (state.isUploadingMedia) return

        voicePlayer.stop()

        val chatId = state.chatId
        val currentUid = state.currentUserId
        val currentUser = authRepository.currentUser
        val senderName = currentUser?.displayName ?: "User"
        val replying = state.replyingToMessage
        val tempMessageId = UUID.randomUUID().toString()

        _uiState.update {
            it.copy(
                isUploadingMedia = true,
                recordedVoiceFile = null,
                recordedVoiceDurationMs = 0L,
                replyingToMessage = null
            )
        }

        viewModelScope.launch {
            val uploadResult = mediaRepository.uploadChatVoice(
                chatId = chatId,
                messageId = tempMessageId,
                audioFile = audioFile,
                senderId = currentUid
            )
            uploadResult.fold(
                onSuccess = { downloadUrl ->
                    val sendResult = chatRepository.sendVoiceMessage(
                        chatId = chatId,
                        senderId = currentUid,
                        senderName = senderName,
                        voiceUrl = downloadUrl,
                        durationMs = durationMs,
                        replyToMessage = replying,
                        messageId = tempMessageId
                    )
                    audioFile.delete()
                    sendResult.fold(
                        onSuccess = {
                            _uiState.update { it.copy(isUploadingMedia = false) }
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    isUploadingMedia = false,
                                    errorMessage = "Couldn't send voice message: ${err.localizedMessage}"
                                )
                            }
                        }
                    )
                },
                onFailure = { err ->
                    audioFile.delete()
                    _uiState.update {
                        it.copy(
                            isUploadingMedia = false,
                            errorMessage = com.aura.glasschat.util.ChatUtils.getFriendlyStorageErrorMessage(err)
                        )
                    }
                }
            )
        }
    }

    fun playVoiceMessage(message: Message) {
        val url = message.mediaUrl
        if (!url.isNullOrBlank()) {
            voicePlayer.playUrl(message.id, url)
        }
    }

    fun pauseVoiceMessage() {
        voicePlayer.pause()
    }

    fun seekVoiceMessage(positionMs: Long) {
        voicePlayer.seekTo(positionMs)
    }

    fun togglePlaybackSpeed() {
        voicePlayer.toggleSpeed()
    }

    private fun startRecordingTimer() {
        stopRecordingTimer()
        recordingTimerJob = viewModelScope.launch {
            var tickCount = 0
            val amplitudes = mutableListOf<Float>()
            while (isActive) {
                delay(100)
                if (!_uiState.value.isRecordingPaused) {
                    tickCount++
                    val amp = voiceRecorder.getNormalizedAmplitude()
                    amplitudes.add(amp)
                    if (amplitudes.size > 24) {
                        amplitudes.removeAt(0)
                    }
                    val currentSec = (tickCount / 10)
                    _uiState.update {
                        it.copy(
                            recordingDurationSec = currentSec,
                            liveWaveformAmplitudes = amplitudes.toList()
                        )
                    }
                    // Max 10 minutes recording safety limit (600 seconds)
                    if (currentSec >= 600) {
                        stopAudioRecording()
                        break
                    }
                }
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    // ==========================================
    // EXISTING CHAT CONTROLS (REPLY, EDIT, UNsend, REACTION)
    // ==========================================

    fun startReply(message: Message) {
        _uiState.update {
            it.copy(
                replyingToMessage = message,
                editingMessage = null,
                activeMenuMessage = null
            )
        }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingToMessage = null) }
    }

    fun startEdit(message: Message) {
        _uiState.update {
            it.copy(
                editingMessage = message,
                inputText = message.content,
                replyingToMessage = null,
                activeMenuMessage = null
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingMessage = null, inputText = "") }
    }

    fun openMessageMenu(message: Message) {
        _uiState.update { it.copy(activeMenuMessage = message) }
    }

    fun closeMessageMenu() {
        _uiState.update { it.copy(activeMenuMessage = null) }
    }

    fun unsendMessage(message: Message) {
        val state = _uiState.value
        viewModelScope.launch {
            chatRepository.unsendMessage(state.chatId, message.id, state.currentUserId)
            // Clean up storage media if attached
            if (!message.mediaUrl.isNullOrBlank()) {
                mediaRepository.deleteChatMedia(state.chatId, message.id)
            }
            closeMessageMenu()
        }
    }

    fun deleteMessageForMe(message: Message) {
        val state = _uiState.value
        viewModelScope.launch {
            chatRepository.deleteMessageForMe(state.chatId, message.id, state.currentUserId)
            closeMessageMenu()
        }
    }

    fun toggleReaction(message: Message, emoji: String) {
        val state = _uiState.value
        val currentReaction = message.reactions[state.currentUserId]
        viewModelScope.launch {
            chatRepository.toggleReaction(
                chatId = state.chatId,
                messageId = message.id,
                userId = state.currentUserId,
                emoji = emoji,
                currentReaction = currentReaction
            )
            closeMessageMenu()
        }
    }

    fun enterSelectionMode(initialMessageId: String) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedMessageIds = setOf(initialMessageId),
                activeMenuMessage = null
            )
        }
    }

    fun toggleMessageSelection(messageId: String) {
        _uiState.update { state ->
            val updated = state.selectedMessageIds.toMutableSet()
            if (updated.contains(messageId)) {
                updated.remove(messageId)
            } else {
                updated.add(messageId)
            }
            if (updated.isEmpty()) {
                state.copy(isSelectionMode = false, selectedMessageIds = emptySet())
            } else {
                state.copy(selectedMessageIds = updated)
            }
        }
    }

    fun exitSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedMessageIds = emptySet()
            )
        }
    }

    fun deleteSelectedMessagesForMe() {
        val state = _uiState.value
        viewModelScope.launch {
            for (msgId in state.selectedMessageIds) {
                chatRepository.deleteMessageForMe(state.chatId, msgId, state.currentUserId)
            }
            exitSelectionMode()
        }
    }

    fun openForwardDialog() {
        _uiState.update { it.copy(showForwardDialog = true) }
    }

    fun dismissForwardDialog() {
        _uiState.update { it.copy(showForwardDialog = false) }
    }

    fun forwardSelectedMessages(targetChatId: String) {
        val state = _uiState.value
        val messagesToForward = state.messages.filter { state.selectedMessageIds.contains(it.id) }
        val currentUser = authRepository.currentUser
        val senderName = currentUser?.displayName ?: "User"

        viewModelScope.launch {
            chatRepository.forwardMessages(
                targetChatId = targetChatId,
                senderId = state.currentUserId,
                senderName = senderName,
                messages = messagesToForward
            )
            dismissForwardDialog()
            exitSelectionMode()
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isBlank() || state.isSending) return

        val currentUid = state.currentUserId
        val currentUser = authRepository.currentUser
        val senderName = currentUser?.displayName ?: "User"

        // Cancel typing status immediately
        typingJob?.cancel()
        viewModelScope.launch {
            chatRepository.setTypingStatus(state.chatId, currentUid, false)
        }

        // If in Edit Mode, submit edit
        val editing = state.editingMessage
        if (editing != null) {
            _uiState.update { it.copy(inputText = "", editingMessage = null, isSending = true) }
            viewModelScope.launch {
                chatRepository.editMessage(state.chatId, editing.id, currentUid, text)
                _uiState.update { it.copy(isSending = false) }
            }
            return
        }

        val replying = state.replyingToMessage
        _uiState.update { it.copy(inputText = "", replyingToMessage = null, isSending = true) }
        draftManager.clearDraft(state.chatId)

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                chatId = state.chatId,
                senderId = currentUid,
                senderName = senderName,
                content = text,
                replyToMessage = replying
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSending = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.localizedMessage ?: "Failed to send message."
                        )
                    }
                }
            )
        }
    }

    fun saveMessage(message: Message) {
        val state = _uiState.value
        if (state.currentUserId.isBlank()) return
        viewModelScope.launch {
            savedMessagesRepository.saveMessage(state.currentUserId, message)
            closeMessageMenu()
        }
    }

    fun remindMe(message: Message, delayMinutes: Long) {
        val state = _uiState.value
        val context = getApplication<Application>().applicationContext
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60 * 1000
        val senderName = if (message.senderId == state.currentUserId) "You" else (state.otherUser?.displayName ?: "Buddy")
        com.aura.glasschat.util.MessageReminderManager.scheduleReminder(
            context = context,
            chatId = state.chatId,
            message = message,
            senderName = senderName,
            triggerAtMillis = triggerAt
        )
        closeMessageMenu()
    }

    fun toggleLockChat() {
        val state = _uiState.value
        val isLocked = state.isChatLocked
        viewModelScope.launch {
            chatRepository.toggleLockChat(state.chatId, state.currentUserId, isLocked)
        }
    }

    fun toggleHideChat() {
        val state = _uiState.value
        val isHidden = state.isChatHidden
        viewModelScope.launch {
            chatRepository.toggleHideChat(state.chatId, state.currentUserId, isHidden)
        }
    }

    fun retrySendMessage(failedMessage: Message) {
        val state = _uiState.value
        val currentUid = state.currentUserId
        val currentUser = authRepository.currentUser
        val senderName = currentUser?.displayName ?: "User"

        viewModelScope.launch {
            when {
                failedMessage.isImageMessage && !failedMessage.mediaUrl.isNullOrBlank() -> {
                    chatRepository.sendImageMessage(
                        chatId = state.chatId,
                        senderId = currentUid,
                        senderName = senderName,
                        imageUrl = failedMessage.mediaUrl,
                        caption = failedMessage.content
                    )
                }
                failedMessage.isVoiceMessage && !failedMessage.mediaUrl.isNullOrBlank() -> {
                    chatRepository.sendVoiceMessage(
                        chatId = state.chatId,
                        senderId = currentUid,
                        senderName = senderName,
                        voiceUrl = failedMessage.mediaUrl,
                        durationMs = failedMessage.durationMs ?: 0L
                    )
                }
                else -> {
                    chatRepository.sendMessage(
                        chatId = state.chatId,
                        senderId = currentUid,
                        senderName = senderName,
                        content = failedMessage.content
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun enterSearchMode() {
        _uiState.update { it.copy(isSearchModeActive = true, searchQuery = "") }
    }

    fun exitSearchMode() {
        _uiState.update { it.copy(isSearchModeActive = false, searchQuery = "", highlightedMessageId = null) }
    }

    fun jumpToMessage(messageId: String) {
        _uiState.update { it.copy(highlightedMessageId = messageId) }
        viewModelScope.launch {
            delay(2000)
            if (_uiState.value.highlightedMessageId == messageId) {
                _uiState.update { it.copy(highlightedMessageId = null) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecorder.cancelRecording()
        voicePlayer.release()
        val state = _uiState.value
        if (state.chatId.isNotBlank() && state.currentUserId.isNotBlank()) {
            viewModelScope.launch {
                chatRepository.setTypingStatus(state.chatId, state.currentUserId, false)
                presenceRepository.setChatPresence(state.chatId, state.currentUserId, false)
            }
        }
    }
}

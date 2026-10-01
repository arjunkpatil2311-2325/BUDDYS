package com.aura.glasschat.ui.components

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Event payload types for the Android Dynamic Island quick-response system.
 */
sealed class IslandEvent {
    data class NewMessage(
        val chatId: String,
        val senderId: String,
        val senderName: String,
        val messagePreview: String,
        val avatarUrl: String? = null
    ) : IslandEvent()

    data class VoiceMessage(
        val chatId: String,
        val senderId: String,
        val senderName: String,
        val durationText: String = "0:07",
        val avatarUrl: String? = null
    ) : IslandEvent()

    data class IncomingCall(
        val callerId: String,
        val callerName: String,
        val isVideo: Boolean = false,
        val avatarUrl: String? = null
    ) : IslandEvent()

    data class AiThinking(
        val query: String = "Analyzing...",
        val status: String = "Buddys AI is thinking..."
    ) : IslandEvent()

    data class AiResponseReady(
        val previewText: String,
        val prompt: String = ""
    ) : IslandEvent()

    data class UploadProgress(
        val title: String,
        val progress: Float, // 0.0f to 1.0f
        val isCompleted: Boolean = false
    ) : IslandEvent()
}

/**
 * Global Manager for the Android-Native Dynamic Island Quick-Response Overlay.
 */
object DynamicIslandManager {
    private val _currentEvent = MutableStateFlow<IslandEvent?>(null)
    val currentEvent: StateFlow<IslandEvent?> = _currentEvent.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var dismissJob: Job? = null

    fun postMessage(chatId: String, senderId: String, senderName: String, preview: String, avatarUrl: String? = null) {
        show(IslandEvent.NewMessage(chatId, senderId, senderName, preview, avatarUrl), autoDismissMs = 4500L)
    }

    fun postVoiceMessage(chatId: String, senderId: String, senderName: String, duration: String, avatarUrl: String? = null) {
        show(IslandEvent.VoiceMessage(chatId, senderId, senderName, duration, avatarUrl), autoDismissMs = 4500L)
    }

    fun postIncomingCall(callerId: String, callerName: String, isVideo: Boolean, avatarUrl: String? = null) {
        // Calls stay visible until answered/declined
        show(IslandEvent.IncomingCall(callerId, callerName, isVideo, avatarUrl), autoDismissMs = null)
    }

    fun postAiThinking(query: String = "Buddys AI is thinking...") {
        show(IslandEvent.AiThinking(query = query), autoDismissMs = null)
    }

    fun postAiResponseReady(preview: String, prompt: String = "") {
        show(IslandEvent.AiResponseReady(previewText = preview, prompt = prompt), autoDismissMs = 5000L)
    }

    fun postUploadProgress(title: String, progress: Float, isCompleted: Boolean = false) {
        show(IslandEvent.UploadProgress(title, progress, isCompleted), autoDismissMs = if (isCompleted) 2500L else null)
    }

    fun show(event: IslandEvent, autoDismissMs: Long? = 4000L) {
        dismissJob?.cancel()
        _currentEvent.value = event
        if (autoDismissMs != null && autoDismissMs > 0) {
            dismissJob = scope.launch {
                delay(autoDismissMs)
                dismiss()
            }
        }
    }

    fun dismiss() {
        dismissJob?.cancel()
        _currentEvent.value = null
    }
}

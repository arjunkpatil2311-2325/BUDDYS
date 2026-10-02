package com.aura.glasschat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.repository.AiResponseResult
import com.aura.glasschat.data.repository.BuddysAiRepository
import com.aura.glasschat.data.repository.ChatHistoryMessage
import com.aura.glasschat.ui.components.DynamicIslandManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: String = "Now",
    val isThinking: Boolean = false,
    val isError: Boolean = false
)

data class BuddysAiUiState(
    val messages: List<AiMessage> = listOf(
        AiMessage(
            sender = "ai",
            text = "Hey! I'm Buddys AI ✨ Your personal creative assistant. What can I help you explore today?",
            timestamp = "Just now"
        )
    ),
    val isThinking: Boolean = false,
    val suggestions: List<String> = listOf(
        "Help me write a message",
        "Give me fun weekend ideas",
        "Explain quantum computing simply",
        "Draft a catchy bio for Buddys"
    )
)

class BuddysAiViewModel(
    private val repository: BuddysAiRepository = BuddysAiRepository.getInstance()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BuddysAiUiState())
    val uiState: StateFlow<BuddysAiUiState> = _uiState.asStateFlow()

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() || _uiState.value.isThinking) return

        val userMsg = AiMessage(
            sender = "user",
            text = prompt.trim(),
            timestamp = "Just now"
        )

        val thinkingMsg = AiMessage(
            sender = "ai",
            text = "",
            timestamp = "Thinking...",
            isThinking = true
        )

        val currentList = _uiState.value.messages + userMsg
        _uiState.value = _uiState.value.copy(
            messages = currentList + thinkingMsg,
            isThinking = true
        )

        // Post thinking event to Dynamic Island
        DynamicIslandManager.postAiThinking("Thinking: ${prompt.take(25)}...")

        viewModelScope.launch {
            // Build conversation history (only valid text messages, avoiding thinking / errors)
            val historyPayload = currentList
                .filterNot { it.isThinking || it.isError || it.text.isBlank() }
                .map { msg ->
                    ChatHistoryMessage(
                        role = if (msg.sender == "user") "user" else "assistant",
                        content = msg.text
                    )
                }

            val result = repository.getAiResponse(historyPayload)

            when (result) {
                is AiResponseResult.Success -> {
                    val finalAiMsg = AiMessage(
                        id = thinkingMsg.id,
                        sender = "ai",
                        text = result.reply,
                        timestamp = "Just now",
                        isThinking = false,
                        isError = false
                    )

                    val updatedList = _uiState.value.messages
                        .filterNot { it.id == thinkingMsg.id } + finalAiMsg

                    _uiState.value = _uiState.value.copy(
                        messages = updatedList,
                        isThinking = false
                    )

                    // Post response ready event to Dynamic Island
                    DynamicIslandManager.postAiResponseReady(
                        preview = result.reply.take(50) + "...",
                        prompt = prompt
                    )
                }
                is AiResponseResult.Error -> {
                    val errorAiMsg = AiMessage(
                        id = thinkingMsg.id,
                        sender = "ai",
                        text = result.message,
                        timestamp = "Error",
                        isThinking = false,
                        isError = true
                    )

                    val updatedList = _uiState.value.messages
                        .filterNot { it.id == thinkingMsg.id } + errorAiMsg

                    _uiState.value = _uiState.value.copy(
                        messages = updatedList,
                        isThinking = false
                    )
                }
            }
        }
    }

    fun regenerateLastResponse() {
        val lastUserMsg = _uiState.value.messages.lastOrNull { it.sender == "user" }
        if (lastUserMsg != null && !_uiState.value.isThinking) {
            val lastUserIdx = _uiState.value.messages.indexOfLast { it.id == lastUserMsg.id }
            if (lastUserIdx != -1) {
                val trimmedList = _uiState.value.messages.take(lastUserIdx)
                _uiState.value = _uiState.value.copy(messages = trimmedList)
                sendMessage(lastUserMsg.text)
            }
        }
    }
}

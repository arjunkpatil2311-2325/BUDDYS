package com.aura.glasschat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.ui.components.DynamicIslandManager
import kotlinx.coroutines.delay
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
    val isThinking: Boolean = false
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

class BuddysAiViewModel : ViewModel() {
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

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + thinkingMsg,
            isThinking = true
        )

        // Post thinking event to Dynamic Island
        DynamicIslandManager.postAiThinking("Thinking about: ${prompt.take(25)}...")

        viewModelScope.launch {
            // Intelligent response generator simulation with rich responses
            delay(1200)

            val replyText = generateAiResponse(prompt.trim())

            val finalAiMsg = AiMessage(
                id = thinkingMsg.id,
                sender = "ai",
                text = replyText,
                timestamp = "Just now",
                isThinking = false
            )

            val updatedList = _uiState.value.messages
                .filterNot { it.id == thinkingMsg.id } + finalAiMsg

            _uiState.value = _uiState.value.copy(
                messages = updatedList,
                isThinking = false
            )

            // Post response ready event to Dynamic Island
            DynamicIslandManager.postAiResponseReady(
                preview = replyText.take(50) + "...",
                prompt = prompt
            )
        }
    }

    fun regenerateLastResponse() {
        val lastUserMsg = _uiState.value.messages.lastOrNull { it.sender == "user" }
        if (lastUserMsg != null) {
            sendMessage(lastUserMsg.text)
        }
    }

    private fun generateAiResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            "write" in lower || "message" in lower -> {
                "Here is a thoughtful message you can send:\n\n\"Hey! Just wanted to check in and see how you're doing. Hope everything is going well on your side! Let's catch up soon ✨\""
            }
            "idea" in lower || "weekend" in lower -> {
                "Here are 3 fun ideas to explore:\n\n1. 🎨 Creative DIY: Try creating a miniature clay figure or digital doodle\n2. ☕ Local Explorer: Check out a cozy cafe you haven't visited yet\n3. 🚴 Outdoor Sprint: Go on a sunset bike ride or brisk walk with a great playlist!"
            }
            "explain" in lower -> {
                "Here is the simple breakdown:\n\nImagine regular computers as light switches that can only be ON (1) or OFF (0). Quantum computing uses special quantum bits (qubits) that can be in BOTH states at once—allowing it to solve complex puzzles thousands of times faster!"
            }
            "bio" in lower -> {
                "Here are 2 clean bio options for your profile:\n\n✨ Option 1: \"Living in color & good conversations • Tap to buddy anytime ✌️\"\n🚀 Option 2: \"Creating, connecting, and keeping it private on Buddys.\""
            }
            else -> {
                "That's an interesting thought! Here is what I think:\n\nBuddys is all about authentic, private connections and playful creativity. Whether you're brainstorming a new project, writing a voice note, or sharing moments with close friends, keeping it genuine always wins! 💡"
            }
        }
    }
}

package com.aura.glasschat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.PairingCode
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.PairingRepository
import com.aura.glasschat.data.repository.PairingResult
import com.aura.glasschat.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairingUiState(
    val selectedTab: Int = 0, // 0 = Generate Code, 1 = Enter Code
    val activeCode: PairingCode? = null,
    val remainingSeconds: Int = 0,
    val isGenerating: Boolean = false,
    val enteredCode: String = "",
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val pairingSuccessChatId: String? = null,
    val pairedFriend: User? = null
)

class PairingViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val pairingRepository: PairingRepository = PairingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex, errorMessage = null) }
    }

    fun onEnteredCodeChanged(code: String) {
        if (code.length <= 12) {
            _uiState.update { it.copy(enteredCode = code.uppercase(), errorMessage = null) }
        }
    }

    fun generateNewCode() {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }

        viewModelScope.launch {
            val user = userRepository.getUser(currentUid)
            if (user == null) {
                _uiState.update { it.copy(isGenerating = false, errorMessage = "User account not loaded.") }
                return@launch
            }

            val result = pairingRepository.generatePairingCode(user)
            result.fold(
                onSuccess = { code ->
                    _uiState.update {
                        it.copy(
                            activeCode = code,
                            isGenerating = false,
                            remainingSeconds = 15 * 60
                        )
                    }
                    startTimer()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = error.localizedMessage ?: "Failed to generate code."
                        )
                    }
                }
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(remainingSeconds = (it.remainingSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun submitPairingCode() {
        val currentUid = authRepository.currentUserId
        val entered = _uiState.value.enteredCode.trim()

        if (entered.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid pairing code.") }
            return
        }

        _uiState.update { it.copy(isVerifying = true, errorMessage = null) }

        viewModelScope.launch {
            val currentUser = userRepository.getUser(currentUid)
            if (currentUser == null) {
                _uiState.update { it.copy(isVerifying = false, errorMessage = "Current user not found.") }
                return@launch
            }

            when (val result = pairingRepository.pairWithCode(entered, currentUser)) {
                is PairingResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            pairingSuccessChatId = result.chatId,
                            pairedFriend = result.friendUser
                        )
                    }
                }
                is PairingResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(pairingSuccessChatId = null, pairedFriend = null) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

package com.aura.glasschat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.Block
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.FollowRepository
import com.aura.glasschat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrivacySettingsUiState(
    val user: User? = null,
    val isPrivate: Boolean = false,
    val blockedUsers: List<Block> = emptyList(),
    val isLoading: Boolean = true
)

class PrivacySettingsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val followRepository: FollowRepository = FollowRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacySettingsUiState())
    val uiState: StateFlow<PrivacySettingsUiState> = _uiState.asStateFlow()

    init {
        val currentUid = authRepository.currentUserId
        if (currentUid.isNotEmpty()) {
            viewModelScope.launch {
                userRepository.observeUserProfile(currentUid).collect { user ->
                    _uiState.update {
                        it.copy(
                            user = user,
                            isPrivate = user?.isPrivate ?: false,
                            isLoading = false
                        )
                    }
                }
            }

            viewModelScope.launch {
                followRepository.observeBlockedUsers(currentUid).collect { blocks ->
                    _uiState.update { it.copy(blockedUsers = blocks) }
                }
            }
        }
    }

    fun togglePrivateAccount(isPrivate: Boolean) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        _uiState.update { it.copy(isPrivate = isPrivate) }
        viewModelScope.launch {
            userRepository.updateAccountPrivacy(currentUid, isPrivate)
        }
    }

    fun unblockUser(blockedUid: String) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            followRepository.unblockUser(currentUid, blockedUid)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}


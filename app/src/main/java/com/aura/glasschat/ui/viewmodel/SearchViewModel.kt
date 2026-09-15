package com.aura.glasschat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.FollowRepository
import com.aura.glasschat.data.repository.FollowStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUserItem(
    val user: User,
    val followStatus: FollowStatus = FollowStatus.NOT_FOLLOWING,
    val isActionLoading: Boolean = false
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchUserItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: String = ""
)

class SearchViewModel @JvmOverloads constructor(
    private val followRepository: FollowRepository = FollowRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentUser: User? = null

    init {
        val uid = authRepository.currentUserId
        _uiState.update { it.copy(currentUserId = uid) }
        viewModelScope.launch {
            // Load current user profile for follow operations
            currentUser = com.aura.glasschat.data.repository.UserRepository().getUser(uid)
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, errorMessage = null) }
        searchJob?.cancel()

        val trimmed = newQuery.trim().lowercase().removePrefix("@")
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            _uiState.update { it.copy(isLoading = true) }

            val result = followRepository.searchUsers(trimmed, _uiState.value.currentUserId)
            result.fold(
                onSuccess = { users ->
                    val items = users.map { u ->
                        SearchUserItem(user = u, followStatus = FollowStatus.NOT_FOLLOWING)
                    }
                    _uiState.update { it.copy(results = items, isLoading = false) }

                    // Fetch follow status for each result
                    users.forEach { u ->
                        viewModelScope.launch {
                            followRepository.observeFollowStatus(_uiState.value.currentUserId, u.uid).collect { status ->
                                _uiState.update { state ->
                                    val updated = state.results.map { item ->
                                        if (item.user.uid == u.uid) item.copy(followStatus = status) else item
                                    }
                                    state.copy(results = updated)
                                }
                            }
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to search users"
                        )
                    }
                }
            )
        }
    }

    fun toggleFollow(targetUser: User) {
        val me = currentUser ?: return
        val currentStatus = _uiState.value.results.find { it.user.uid == targetUser.uid }?.followStatus ?: return

        // Set action loading
        _uiState.update { state ->
            val updated = state.results.map {
                if (it.user.uid == targetUser.uid) it.copy(isActionLoading = true) else it
            }
            state.copy(results = updated)
        }

        viewModelScope.launch {
            if (currentStatus == FollowStatus.FOLLOWING || currentStatus == FollowStatus.REQUESTED) {
                followRepository.unfollowUser(me.uid, targetUser.uid)
            } else {
                followRepository.followUser(me, targetUser)
            }

            _uiState.update { state ->
                val updated = state.results.map {
                    if (it.user.uid == targetUser.uid) it.copy(isActionLoading = false) else it
                }
                state.copy(results = updated)
            }
        }
    }

    fun lookupQrUser(
        rawPayload: String,
        onUserFound: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        val username = com.aura.glasschat.util.QrCodeGenerator.parseScannedPayload(rawPayload)
        if (username.isNullOrBlank()) {
            onError("Invalid Buddies QR Code")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = followRepository.getUserByUsername(username)
            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = { user ->
                    if (user != null) {
                        onUserFound(user)
                    } else {
                        onError("User @$username not found")
                    }
                },
                onFailure = {
                    onError(it.localizedMessage ?: "Failed to find user")
                }
            )
        }
    }
}

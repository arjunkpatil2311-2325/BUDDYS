package com.aura.glasschat.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.Story
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.model.UserStories
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.StoryRepository
import com.aura.glasschat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoryUiState(
    val activeUserStories: List<UserStories> = emptyList(),
    val myStories: UserStories? = null,
    val selectedUserStories: UserStories? = null,
    val currentStoryIndex: Int = 0,
    val isPaused: Boolean = false,
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: String = ""
) {
    val currentStory: Story?
        get() = selectedUserStories?.stories?.getOrNull(currentStoryIndex)
}

class StoryViewModel @JvmOverloads constructor(
    private val storyRepository: StoryRepository = StoryRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private var currentUser: User? = null

    init {
        val uid = authRepository.currentUserId
        _uiState.update { it.copy(currentUserId = uid) }
        if (uid.isNotEmpty()) {
            viewModelScope.launch {
                currentUser = userRepository.getUser(uid)
            }
            observeStories(uid)
        }
    }

    private fun observeStories(currentUid: String) {
        viewModelScope.launch {
            storyRepository.observeActiveStories(currentUid).collect { list ->
                val myStory = list.find { it.userId == currentUid }
                val others = list.filter { it.userId != currentUid }
                _uiState.update { state ->
                    state.copy(
                        activeUserStories = list,
                        myStories = myStory
                    )
                }
            }
        }
    }

    fun uploadStory(context: Context, imageUri: Uri, caption: String, audience: String = "EVERYONE") {
        val uid = _uiState.value.currentUserId.ifBlank { authRepository.currentUserId }
        if (uid.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please log in to post stories") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, errorMessage = null, uploadSuccess = false) }

            val user = currentUser ?: userRepository.getUser(uid) ?: run {
                val fbUser = authRepository.currentUser
                User(
                    uid = uid,
                    email = fbUser?.email ?: "",
                    displayName = fbUser?.displayName ?: "Buddy",
                    username = fbUser?.displayName?.replace(" ", "")?.lowercase() ?: "buddy",
                    avatarUrl = fbUser?.photoUrl?.toString()
                )
            }
            currentUser = user

            val result = storyRepository.uploadStory(context, imageUri, caption, user, audience)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isUploading = false, uploadSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isUploading = false, errorMessage = error.localizedMessage ?: "Failed to post story") }
                }
            )
        }
    }

    fun addStoryReaction(storyId: String, emoji: String) {
        val uid = _uiState.value.currentUserId
        if (uid.isNotBlank() && storyId.isNotBlank()) {
            viewModelScope.launch {
                storyRepository.addStoryReaction(storyId, uid, emoji)
            }
        }
    }

    fun openStoryViewer(userStories: UserStories, startIndex: Int = 0) {
        _uiState.update {
            it.copy(
                selectedUserStories = userStories,
                currentStoryIndex = startIndex.coerceIn(0, (userStories.stories.size - 1).coerceAtLeast(0)),
                isPaused = false
            )
        }
        markCurrentAsViewed()
    }

    fun nextStory(onStoriesFinished: () -> Unit = {}) {
        val stories = _uiState.value.selectedUserStories?.stories ?: return
        val nextIdx = _uiState.value.currentStoryIndex + 1
        if (nextIdx < stories.size) {
            _uiState.update { it.copy(currentStoryIndex = nextIdx) }
            markCurrentAsViewed()
        } else {
            // Find next user's story if available
            val all = _uiState.value.activeUserStories
            val currentAuthorId = _uiState.value.selectedUserStories?.userId
            val currentAuthorIdx = all.indexOfFirst { it.userId == currentAuthorId }
            if (currentAuthorIdx != -1 && currentAuthorIdx + 1 < all.size) {
                openStoryViewer(all[currentAuthorIdx + 1], 0)
            } else {
                onStoriesFinished()
            }
        }
    }

    fun previousStory() {
        val prevIdx = _uiState.value.currentStoryIndex - 1
        if (prevIdx >= 0) {
            _uiState.update { it.copy(currentStoryIndex = prevIdx) }
        }
    }

    fun setPaused(paused: Boolean) {
        _uiState.update { it.copy(isPaused = paused) }
    }

    fun markCurrentAsViewed() {
        val story = _uiState.value.currentStory ?: return
        val currentUid = _uiState.value.currentUserId
        viewModelScope.launch {
            storyRepository.markStoryAsViewed(story.id, currentUid)
        }
    }

    fun deleteCurrentStory(onDeleted: () -> Unit = {}) {
        val story = _uiState.value.currentStory ?: return
        viewModelScope.launch {
            storyRepository.deleteStory(story.id)
            onDeleted()
        }
    }

    fun clearUploadStatus() {
        _uiState.update { it.copy(uploadSuccess = false, errorMessage = null) }
    }
}

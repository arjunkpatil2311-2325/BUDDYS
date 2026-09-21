package com.aura.glasschat.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.Post
import com.aura.glasschat.data.model.ProfileHighlight
import com.aura.glasschat.data.model.Story
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.HighlightRepository
import com.aura.glasschat.data.repository.PostRepository
import com.aura.glasschat.data.repository.StoryRepository
import com.aura.glasschat.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

data class ProfileUiState(
    val user: User? = null,
    val highlights: List<ProfileHighlight> = emptyList(),
    val posts: List<Post> = emptyList(),
    val archivedStories: List<Story> = emptyList(),
    val isEditing: Boolean = false,
    val editDisplayName: String = "",
    val editStatusMessage: String = "",
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val isSavingHighlight: Boolean = false,
    val isSavingPost: Boolean = false,
    val isLoggedOut: Boolean = false,
    val showPhotoOptions: Boolean = false,
    val showCreateHighlightSheet: Boolean = false,
    val selectedHighlightForEdit: ProfileHighlight? = null,
    val activeHighlightForViewing: ProfileHighlight? = null,
    val selectedPostForDetail: Post? = null,
    val errorMessage: String? = null
) {
    val pinnedPostsCount: Int
        get() = posts.count { it.isPinned }
}

class ProfileViewModel @JvmOverloads constructor(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val highlightRepository: HighlightRepository = HighlightRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val storyRepository: StoryRepository = StoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var profileJob: Job? = null
    private var highlightsJob: Job? = null
    private var postsJob: Job? = null
    private var storiesJob: Job? = null

    init {
        loadProfile()
    }

    fun refresh() {
        profileJob?.cancel()
        highlightsJob?.cancel()
        postsJob?.cancel()
        storiesJob?.cancel()
        _uiState.update { ProfileUiState() }
        loadProfile()
    }

    private fun loadProfile() {
        val currentUid = authRepository.currentUserId
        if (currentUid.isNotEmpty()) {
            profileJob?.cancel()
            profileJob = viewModelScope.launch {
                userRepository.observeUserProfile(currentUid).collect { user ->
                    _uiState.update {
                        it.copy(
                            user = user,
                            editDisplayName = user?.displayName ?: "",
                            editStatusMessage = user?.statusMessage ?: ""
                        )
                    }
                }
            }

            highlightsJob?.cancel()
            highlightsJob = viewModelScope.launch {
                highlightRepository.observeUserHighlights(currentUid).collect { highlights ->
                    _uiState.update { it.copy(highlights = highlights) }
                }
            }

            postsJob?.cancel()
            postsJob = viewModelScope.launch {
                postRepository.observeUserPosts(currentUid).collect { posts ->
                    _uiState.update {
                        val currentDetail = it.selectedPostForDetail
                        val updatedDetail = if (currentDetail != null) {
                            posts.find { p -> p.id == currentDetail.id } ?: currentDetail
                        } else null
                        it.copy(posts = posts, selectedPostForDetail = updatedDetail)
                    }
                }
            }

            storiesJob?.cancel()
            storiesJob = viewModelScope.launch {
                storyRepository.observeStoryArchive(currentUid).collect { stories ->
                    _uiState.update { it.copy(archivedStories = stories) }
                }
            }
        }
    }

    // ==========================================
    // HIGHLIGHTS ACTIONS
    // ==========================================

    fun openCreateHighlight() {
        _uiState.update { it.copy(showCreateHighlightSheet = true) }
    }

    fun closeCreateHighlight() {
        _uiState.update { it.copy(showCreateHighlightSheet = false) }
    }

    fun openEditHighlight(highlight: ProfileHighlight) {
        _uiState.update { it.copy(selectedHighlightForEdit = highlight) }
    }

    fun closeEditHighlight() {
        _uiState.update { it.copy(selectedHighlightForEdit = null) }
    }

    fun openHighlightViewer(highlight: ProfileHighlight) {
        _uiState.update { it.copy(activeHighlightForViewing = highlight) }
    }

    fun closeHighlightViewer() {
        _uiState.update { it.copy(activeHighlightForViewing = null) }
    }

    fun createHighlight(title: String, coverUrl: String, selectedStories: List<Story>) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        _uiState.update { it.copy(isSavingHighlight = true, errorMessage = null) }

        viewModelScope.launch {
            val storyIds = selectedStories.map { it.id }
            val result = highlightRepository.createHighlight(
                userId = currentUid,
                title = title,
                coverUrl = coverUrl,
                storyIds = storyIds,
                stories = selectedStories
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSavingHighlight = false, showCreateHighlightSheet = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingHighlight = false,
                            errorMessage = error.localizedMessage ?: "Failed to create highlight"
                        )
                    }
                }
            )
        }
    }

    fun updateHighlight(highlightId: String, title: String, coverUrl: String, selectedStories: List<Story>) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        _uiState.update { it.copy(isSavingHighlight = true, errorMessage = null) }

        viewModelScope.launch {
            val storyIds = selectedStories.map { it.id }
            val result = highlightRepository.updateHighlight(
                userId = currentUid,
                highlightId = highlightId,
                title = title,
                coverUrl = coverUrl,
                storyIds = storyIds,
                stories = selectedStories
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSavingHighlight = false, selectedHighlightForEdit = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingHighlight = false,
                            errorMessage = error.localizedMessage ?: "Failed to update highlight"
                        )
                    }
                }
            )
        }
    }

    fun deleteHighlight(highlightId: String) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            val result = highlightRepository.deleteHighlight(currentUid, highlightId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(selectedHighlightForEdit = null, activeHighlightForViewing = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage ?: "Failed to delete highlight") }
                }
            )
        }
    }

    // ==========================================
    // PINNED POSTS & POST ACTIONS
    // ==========================================

    fun selectPostForDetail(post: Post?) {
        _uiState.update { it.copy(selectedPostForDetail = post) }
    }

    fun togglePinPost(post: Post) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            val result = if (post.isPinned) {
                postRepository.unpinPost(currentUid, post.id)
            } else {
                postRepository.pinPost(currentUid, post.id)
            }

            result.fold(
                onSuccess = {
                    // Handled automatically via Flow listener
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.localizedMessage ?: "Failed to update pin state")
                    }
                }
            )
        }
    }

    fun deletePost(post: Post) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            val result = postRepository.deletePost(post.id, currentUid)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(selectedPostForDetail = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage ?: "Failed to delete post") }
                }
            )
        }
    }

    fun toggleLikePost(post: Post) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            postRepository.toggleLikePost(post.id, currentUid)
        }
    }

    fun createPost(context: Context, imageUri: Uri, caption: String) {
        val currentUser = _uiState.value.user ?: return
        _uiState.update { it.copy(isSavingPost = true, errorMessage = null) }

        viewModelScope.launch {
            val result = postRepository.createPost(context, imageUri, caption, currentUser)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSavingPost = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingPost = false,
                            errorMessage = error.localizedMessage ?: "Failed to create post"
                        )
                    }
                }
            )
        }
    }

    // ==========================================
    // PROFILE PHOTO & EDITING
    // ==========================================

    fun openPhotoOptions() {
        _uiState.update { it.copy(showPhotoOptions = true) }
    }

    fun dismissPhotoOptions() {
        _uiState.update { it.copy(showPhotoOptions = false) }
    }

    fun onPhotoSelected(context: Context, uri: Uri) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        _uiState.update { it.copy(isUploadingPhoto = true, showPhotoOptions = false, errorMessage = null) }

        viewModelScope.launch {
            val compressedBytes = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream) ?: return@use null
                        
                        val maxDim = 600
                        val scale = min(1f, maxDim.toFloat() / original.width.coerceAtLeast(original.height))
                        val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
                        val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)

                        val scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
                        out.toByteArray()
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (compressedBytes == null) {
                _uiState.update {
                    it.copy(isUploadingPhoto = false, errorMessage = "Failed to process selected image.")
                }
                return@launch
            }

            val result = userRepository.uploadProfilePicture(currentUid, compressedBytes)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isUploadingPhoto = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            errorMessage = error.localizedMessage ?: "Failed to upload photo."
                        )
                    }
                }
            )
        }
    }

    fun removePhoto() {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        _uiState.update { it.copy(isUploadingPhoto = true, showPhotoOptions = false, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.removeProfilePicture(currentUid)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isUploadingPhoto = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            errorMessage = error.localizedMessage ?: "Failed to remove photo."
                        )
                    }
                }
            )
        }
    }

    fun startEditing() {
        val currentUser = _uiState.value.user
        _uiState.update {
            it.copy(
                isEditing = true,
                editDisplayName = currentUser?.displayName ?: "",
                editStatusMessage = currentUser?.statusMessage ?: ""
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false, errorMessage = null) }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(editDisplayName = name) }
    }

    fun onStatusMessageChanged(status: String) {
        _uiState.update { it.copy(editStatusMessage = status) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        if (state.editDisplayName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Display name cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.updateProfile(
                userId = currentUid,
                displayName = state.editDisplayName,
                statusMessage = state.editStatusMessage
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isEditing = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.localizedMessage ?: "Failed to update profile."
                        )
                    }
                }
            )
        }
    }

    fun toggleAccountPrivacy(isPrivate: Boolean) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            userRepository.updateAccountPrivacy(currentUid, isPrivate)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}

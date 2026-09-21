package com.aura.glasschat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.Post
import com.aura.glasschat.data.model.ProfileHighlight
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.FollowRepository
import com.aura.glasschat.data.repository.HighlightRepository
import com.aura.glasschat.data.repository.PostRepository
import com.aura.glasschat.data.repository.RelationshipState
import com.aura.glasschat.data.repository.UserRepository
import com.aura.glasschat.util.ChatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublicProfileUiState(
    val targetUser: User? = null,
    val currentUserId: String = "",
    val relationshipState: RelationshipState = RelationshipState.NOT_FOLLOWING,
    val highlights: List<ProfileHighlight> = emptyList(),
    val posts: List<Post> = emptyList(),
    val selectedPostForDetail: Post? = null,
    val activeHighlightForViewing: ProfileHighlight? = null,
    val isBlocked: Boolean = false,
    val isLoading: Boolean = true,
    val isActionLoading: Boolean = false,
    val reportSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val canViewPrivateContent: Boolean
        get() = targetUser?.isPrivate != true || relationshipState == RelationshipState.FOLLOWING || relationshipState == RelationshipState.MUTUAL || currentUserId == targetUser?.uid
}

class PublicProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val followRepository: FollowRepository = FollowRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val highlightRepository: HighlightRepository = HighlightRepository(),
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    private var currentUser: User? = null
    private var highlightsJob: Job? = null
    private var postsJob: Job? = null
    private var userJob: Job? = null
    private var relationshipJob: Job? = null

    fun loadProfile(targetUserId: String) {
        val currentUid = authRepository.currentUserId
        _uiState.update { it.copy(currentUserId = currentUid, isLoading = true) }

        viewModelScope.launch {
            currentUser = userRepository.getUser(currentUid)
            val blocked = followRepository.isUserBlocked(currentUid, targetUserId)
            _uiState.update { it.copy(isBlocked = blocked) }
        }

        userJob?.cancel()
        userJob = viewModelScope.launch {
            userRepository.observeUserProfile(targetUserId).collect { user ->
                _uiState.update {
                    it.copy(
                        targetUser = user,
                        isLoading = false
                    )
                }
            }
        }

        relationshipJob?.cancel()
        relationshipJob = viewModelScope.launch {
            followRepository.observeRelationshipState(currentUid, targetUserId).collect { status ->
                _uiState.update { it.copy(relationshipState = status) }
            }
        }

        highlightsJob?.cancel()
        highlightsJob = viewModelScope.launch {
            highlightRepository.observeUserHighlights(targetUserId).collect { highlights ->
                _uiState.update { it.copy(highlights = highlights) }
            }
        }

        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            postRepository.observeUserPosts(targetUserId).collect { posts ->
                _uiState.update {
                    val currentDetail = it.selectedPostForDetail
                    val updatedDetail = if (currentDetail != null) {
                        posts.find { p -> p.id == currentDetail.id } ?: currentDetail
                    } else null
                    it.copy(posts = posts, selectedPostForDetail = updatedDetail)
                }
            }
        }
    }

    fun selectPostForDetail(post: Post?) {
        _uiState.update { it.copy(selectedPostForDetail = post) }
    }

    fun openHighlightViewer(highlight: ProfileHighlight) {
        _uiState.update { it.copy(activeHighlightForViewing = highlight) }
    }

    fun closeHighlightViewer() {
        _uiState.update { it.copy(activeHighlightForViewing = null) }
    }

    fun toggleLikePost(post: Post) {
        val currentUid = _uiState.value.currentUserId
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            postRepository.toggleLikePost(post.id, currentUid)
        }
    }

    fun toggleFollow() {
        val me = currentUser ?: return
        val target = _uiState.value.targetUser ?: return
        val currentStatus = _uiState.value.relationshipState

        _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

        viewModelScope.launch {
            if (currentStatus == RelationshipState.FOLLOWING || currentStatus == RelationshipState.MUTUAL || currentStatus == RelationshipState.REQUESTED) {
                followRepository.unfollowUser(me.uid, target.uid)
            } else {
                followRepository.followUser(me, target)
            }
            _uiState.update { it.copy(isActionLoading = false) }
        }
    }

    fun blockUser() {
        val myUid = _uiState.value.currentUserId
        val targetUid = _uiState.value.targetUser?.uid ?: return

        viewModelScope.launch {
            followRepository.blockUser(myUid, targetUid)
            _uiState.update { it.copy(isBlocked = true) }
        }
    }

    fun unblockUser() {
        val myUid = _uiState.value.currentUserId
        val targetUid = _uiState.value.targetUser?.uid ?: return

        viewModelScope.launch {
            followRepository.unblockUser(myUid, targetUid)
            _uiState.update { it.copy(isBlocked = false) }
        }
    }

    fun reportUser(reason: String, details: String) {
        val myUid = _uiState.value.currentUserId
        val target = _uiState.value.targetUser ?: return

        viewModelScope.launch {
            val res = followRepository.reportUser(
                reporterUid = myUid,
                reportedUid = target.uid,
                reportedUsername = target.username,
                reason = reason,
                details = details
            )
            res.fold(
                onSuccess = { _uiState.update { it.copy(reportSuccess = true) } },
                onFailure = { err -> _uiState.update { it.copy(errorMessage = err.localizedMessage) } }
            )
        }
    }

    fun getChatId(): String? {
        val myUid = _uiState.value.currentUserId
        val otherUid = _uiState.value.targetUser?.uid ?: return null
        return if (myUid.isNotBlank() && otherUid.isNotBlank() && myUid != otherUid) {
            ChatUtils.getDeterministicChatId(myUid, otherUid)
        } else null
    }
}

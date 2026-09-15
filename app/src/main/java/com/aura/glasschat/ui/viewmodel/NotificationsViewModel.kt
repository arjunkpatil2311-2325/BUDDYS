package com.aura.glasschat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.AppNotification
import com.aura.glasschat.data.model.FollowRequest
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.FollowRepository
import com.aura.glasschat.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val pendingRequests: List<FollowRequest> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String = ""
)

class NotificationsViewModel(
    private val followRepository: FollowRepository = FollowRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var currentUser: User? = null

    init {
        val currentUid = authRepository.currentUserId
        _uiState.update { it.copy(currentUserId = currentUid) }

        viewModelScope.launch {
            currentUser = userRepository.getUser(currentUid)
        }

        // 1. Observe notifications
        viewModelScope.launch {
            followRepository.observeNotifications(currentUid).collect { notifs ->
                _uiState.update { it.copy(notifications = notifs, isLoading = false) }
            }
        }

        // 2. Observe pending follow requests
        viewModelScope.launch {
            observePendingRequests(currentUid).collect { requests ->
                _uiState.update { it.copy(pendingRequests = requests) }
            }
        }
    }

    private fun observePendingRequests(userId: String): Flow<List<FollowRequest>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("follow_requests")
            .whereEqualTo("targetUid", userId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val reqs = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FollowRequest::class.java)?.copy(requestId = doc.id)
                }
                trySend(reqs)
            }

        awaitClose {
            listener.remove()
        }
    }

    fun acceptRequest(request: FollowRequest) {
        val me = currentUser ?: return
        viewModelScope.launch {
            followRepository.acceptFollowRequest(request, me)
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            followRepository.declineFollowRequest(requestId)
        }
    }

    fun markAsRead(notifId: String) {
        viewModelScope.launch {
            followRepository.markNotificationAsRead(notifId)
        }
    }
}

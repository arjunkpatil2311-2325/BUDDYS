package com.aura.glasschat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.CallSession
import com.aura.glasschat.data.model.Chat
import com.aura.glasschat.data.model.Friend
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.CallRepository
import com.aura.glasschat.data.repository.ChatRepository
import com.aura.glasschat.data.repository.UserRepository
import com.aura.glasschat.util.NetworkConnectivityObserver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentUser: User? = null,
    val chats: List<Chat> = emptyList(),
    val filteredChats: List<Chat> = emptyList(),
    val hiddenChatsCount: Int = 0,
    val friends: List<Friend> = emptyList(),
    val callHistory: List<CallSession> = emptyList(),
    val incomingCall: CallSession? = null,
    val isOnline: Boolean = true,
    val selectedTab: Int = 0, // 0 = Chats, 1 = Calls
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val showNoteDialog: Boolean = false
)

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val chatRepository: ChatRepository = ChatRepository(),
    private val callRepository: CallRepository = CallRepository()
) : AndroidViewModel(application) {

    private val connectivityObserver = NetworkConnectivityObserver(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val dataJobs = mutableListOf<kotlinx.coroutines.Job>()

    init {
        loadData()
        observeNetwork()
    }

    fun refresh() {
        dataJobs.forEach { it.cancel() }
        dataJobs.clear()
        _uiState.update { HomeUiState(isLoading = true, isOnline = it.isOnline) }
        loadData()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            connectivityObserver.observe().collect { isConnected ->
                _uiState.update { it.copy(isOnline = isConnected) }
            }
        }
    }

    private fun loadData() {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        // 1. Observe User Profile
        dataJobs.add(viewModelScope.launch {
            userRepository.observeUserProfile(currentUid).collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user != null && user.uid.isNotBlank()) {
                    com.aura.glasschat.data.repository.AccountManagerRepository.getInstance(getApplication()).saveAccount(user)
                }
            }
        })

        // 2. Observe Friends List
        dataJobs.add(viewModelScope.launch {
            userRepository.observeFriends(currentUid).collect { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
        })

        // 3. Observe Real-time Chats
        dataJobs.add(viewModelScope.launch {
            chatRepository.observeUserChats(currentUid).collect { chats ->
                val hiddenCount = chats.count { it.isHidden(currentUid) }
                _uiState.update { state ->
                    state.copy(
                        chats = chats,
                        filteredChats = filterChats(chats, state.searchQuery),
                        hiddenChatsCount = hiddenCount,
                        isLoading = false
                    )
                }

                // Acknowledge delivery for incoming unread chats
                for (chat in chats) {
                    if (chat.lastMessageSenderId.isNotBlank() && chat.lastMessageSenderId != currentUid && chat.hasUnread(currentUid)) {
                        chatRepository.acknowledgeChatDelivery(chat.chatId, currentUid)
                    }
                }
            }
        })

        // 4. Observe Call History
        dataJobs.add(viewModelScope.launch {
            callRepository.observeCallHistory(currentUid).collect { calls ->
                _uiState.update { it.copy(callHistory = calls) }
            }
        })

        // 5. Observe Incoming Calls
        dataJobs.add(viewModelScope.launch {
            callRepository.observeIncomingCalls(currentUid).collect { incoming ->
                _uiState.update { it.copy(incomingCall = incoming) }
            }
        })
    }

    fun declineIncomingCall(session: CallSession) {
        viewModelScope.launch {
            callRepository.rejectCall(session.callId)
            _uiState.update { it.copy(incomingCall = null) }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun openNoteDialog() {
        _uiState.update { it.copy(showNoteDialog = true) }
    }

    fun closeNoteDialog() {
        _uiState.update { it.copy(showNoteDialog = false) }
    }

    fun saveNote(note: String) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isBlank()) return
        viewModelScope.launch {
            userRepository.updateNote(currentUid, note)
            closeNoteDialog()
        }
    }

    fun deleteNote() {
        val currentUid = authRepository.currentUserId
        if (currentUid.isBlank()) return
        viewModelScope.launch {
            userRepository.deleteNote(currentUid)
            closeNoteDialog()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredChats = filterChats(state.chats, query)
            )
        }
    }

    fun togglePinChat(chat: Chat) {
        val currentUid = authRepository.currentUserId
        viewModelScope.launch {
            chatRepository.togglePinChat(chat.chatId, currentUid, chat.isPinned(currentUid))
        }
    }

    fun toggleMuteChat(chat: Chat) {
        val currentUid = authRepository.currentUserId
        viewModelScope.launch {
            chatRepository.toggleMuteChat(chat.chatId, currentUid, chat.isMuted(currentUid))
        }
    }

    fun toggleHideChat(chat: Chat) {
        val currentUid = authRepository.currentUserId
        val isHidden = chat.isHidden(currentUid)
        viewModelScope.launch {
            chatRepository.toggleHideChat(chat.chatId, currentUid, isHidden)
        }
    }

    fun toggleLockChat(chat: Chat) {
        val currentUid = authRepository.currentUserId
        val isLocked = chat.isLocked(currentUid)
        viewModelScope.launch {
            chatRepository.toggleLockChat(chat.chatId, currentUid, isLocked)
        }
    }

    private fun filterChats(chats: List<Chat>, query: String): List<Chat> {
        val currentUid = authRepository.currentUserId
        // Filter out hidden and locked chats from normal inbox list
        val visibleChats = chats.filter { !it.isHidden(currentUid) && !it.isLocked(currentUid) }

        val filtered = if (query.isBlank()) {
            visibleChats
        } else {
            val lower = query.lowercase().trim()
            visibleChats.filter { chat ->
                val otherInfo = chat.getOtherParticipantInfo(currentUid)
                otherInfo.displayName.lowercase().contains(lower) ||
                otherInfo.username.lowercase().contains(lower) ||
                chat.lastMessage.lowercase().contains(lower)
            }
        }

        // Sort: Pinned chats on top, then by lastMessageTimestamp descending
        return filtered.sortedWith(
            compareByDescending<Chat> { it.isPinned(currentUid) }
                .thenByDescending { it.lastMessageTimestamp?.seconds ?: 0L }
        )
    }
}

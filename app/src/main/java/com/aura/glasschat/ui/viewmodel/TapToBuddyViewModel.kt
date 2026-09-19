package com.aura.glasschat.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.nearby.*
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.PairingRepository
import com.aura.glasschat.data.repository.UserRepository
import com.aura.glasschat.util.MediaStoreUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class TapToBuddyStatus {
    READY,
    SEARCHING,
    DEVICE_FOUND,
    WAITING_CONFIRMATION,
    SUCCESS,
    ERROR
}

data class TapToBuddyUiState(
    val status: TapToBuddyStatus = TapToBuddyStatus.READY,
    val currentUser: User? = null,
    val peerUser: User? = null,
    val peerEndpointId: String? = null,
    val activeSessionId: String? = null,
    val createdChatId: String? = null,
    val errorMessage: String? = null,
    val isLocalConfirmed: Boolean = false,
    val isPeerConfirmed: Boolean = false,

    // Media Sharing State
    val isTransferringMedia: Boolean = false,
    val mediaTransferProgress: Float = 0f,
    val mediaBytesTransferred: Long = 0L,
    val mediaTotalBytes: Long = 0L,
    val incomingMediaBytes: ByteArray? = null,
    val incomingMediaMetadata: NearbyPairingPayload? = null,
    val showMediaPreviewDialog: Boolean = false,
    val mediaSaveSuccessMessage: String? = null
)

class TapToBuddyViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        const val DISCOVERY_TIMEOUT_MS = 15_000L
    }

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val pairingRepository = PairingRepository()
    private val nearbyManager = NearbyPairingManager(application)

    private val _uiState = MutableStateFlow(TapToBuddyUiState())
    val uiState: StateFlow<TapToBuddyUiState> = _uiState.asStateFlow()

    private var timeoutJob: Job? = null

    init {
        loadCurrentUser()
        observeNearbyEvents()
    }

    private fun loadCurrentUser() {
        val currentUid = authRepository.currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid.isNullOrBlank()) return

        viewModelScope.launch {
            val user = userRepository.getUser(currentUid) ?: User(
                uid = currentUid,
                displayName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Me",
                username = "",
                avatarUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
            )
            _uiState.update { it.copy(currentUser = user) }
        }
    }

    private fun observeNearbyEvents() {
        viewModelScope.launch {
            nearbyManager.event.collect { event ->
                when (event) {
                    is NearbyEvent.Idle -> {}
                    is NearbyEvent.AdvertisingStarted,
                    is NearbyEvent.DiscoveryStarted -> {}
                    is NearbyEvent.PeerFound -> handlePeerFound(event.endpointId, event.payload)
                    is NearbyEvent.Connecting -> {
                        // Connection in progress
                    }
                    is NearbyEvent.Connected -> {
                        _uiState.update { it.copy(peerEndpointId = event.endpointId) }
                    }
                    is NearbyEvent.PayloadReceived -> handlePayloadReceived(event.endpointId, event.payload)
                    is NearbyEvent.MediaMetadataReceived -> {
                        _uiState.update {
                            it.copy(
                                incomingMediaMetadata = event.metadata,
                                isTransferringMedia = true,
                                mediaTransferProgress = 0f,
                                mediaBytesTransferred = 0L,
                                mediaTotalBytes = event.metadata.mediaSize
                            )
                        }
                    }
                    is NearbyEvent.MediaReceived -> {
                        _uiState.update {
                            it.copy(
                                incomingMediaBytes = event.bytes,
                                incomingMediaMetadata = event.metadata ?: it.incomingMediaMetadata,
                                isTransferringMedia = false,
                                showMediaPreviewDialog = true
                            )
                        }
                    }
                    is NearbyEvent.TransferProgress -> {
                        val progress = if (event.totalBytes > 0) {
                            (event.bytesTransferred.toFloat() / event.totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        _uiState.update {
                            it.copy(
                                isTransferringMedia = !event.isSuccess && !event.isFailure,
                                mediaTransferProgress = if (event.isSuccess) 1f else progress,
                                mediaBytesTransferred = event.bytesTransferred,
                                mediaTotalBytes = event.totalBytes
                            )
                        }
                    }
                    is NearbyEvent.Disconnected -> handleDisconnected(event.endpointId)
                    is NearbyEvent.Error -> {
                        // Do not abort search if discovery is still searching unless it's a fatal error
                        if (_uiState.value.status != TapToBuddyStatus.SEARCHING) {
                            timeoutJob?.cancel()
                            _uiState.update {
                                it.copy(
                                    status = TapToBuddyStatus.ERROR,
                                    errorMessage = event.message,
                                    isTransferringMedia = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun startNearbySearch() {
        val user = _uiState.value.currentUser ?: return
        val sessionId = UUID.randomUUID().toString()

        // Cancel previous timeout immediately
        timeoutJob?.cancel()

        _uiState.update {
            it.copy(
                status = TapToBuddyStatus.SEARCHING,
                peerUser = null,
                peerEndpointId = null,
                activeSessionId = sessionId,
                errorMessage = null,
                isLocalConfirmed = false,
                isPeerConfirmed = false,
                isTransferringMedia = false,
                showMediaPreviewDialog = false
            )
        }

        val offerPayload = NearbyPairingPayload.createOffer(
            senderUid = user.uid,
            displayName = user.displayName.ifBlank { user.username },
            username = user.username,
            avatarUrl = user.avatarUrl,
            sessionId = sessionId
        )

        nearbyManager.startNearbyPairing(offerPayload)

        // Strict 15-second discovery attempt window
        timeoutJob = viewModelScope.launch {
            delay(DISCOVERY_TIMEOUT_MS)
            val currentState = _uiState.value
            if (currentState.status == TapToBuddyStatus.SEARCHING && currentState.activeSessionId == sessionId) {
                nearbyManager.stopNearbyPairing()
                _uiState.update {
                    it.copy(
                        status = TapToBuddyStatus.ERROR,
                        errorMessage = "Couldn't find anyone nearby. Make sure both phones have Tap to Buddy open and are close together."
                    )
                }
            }
        }
    }

    private fun handlePeerFound(endpointId: String, payload: NearbyPairingPayload) {
        val currentUser = _uiState.value.currentUser ?: return

        // Reject self-pairing
        if (payload.senderUid == currentUser.uid) return

        val currentStatus = _uiState.value.status
        // Ignore stale callbacks if user already cancelled or session ended
        if (currentStatus != TapToBuddyStatus.SEARCHING && currentStatus != TapToBuddyStatus.DEVICE_FOUND) {
            return
        }

        // Cancel timeout coroutine immediately upon device discovery
        timeoutJob?.cancel()

        val peer = User(
            uid = payload.senderUid,
            displayName = payload.displayName,
            username = payload.username,
            avatarUrl = payload.avatarUrl ?: _uiState.value.peerUser?.avatarUrl
        )

        _uiState.update {
            it.copy(
                status = TapToBuddyStatus.DEVICE_FOUND,
                peerUser = peer,
                peerEndpointId = endpointId,
                activeSessionId = payload.sessionId.ifBlank { it.activeSessionId },
                errorMessage = null
            )
        }
    }

    private fun handlePayloadReceived(endpointId: String, payload: NearbyPairingPayload) {
        when (payload.type) {
            NearbyPayloadType.OFFER -> handlePeerFound(endpointId, payload)
            NearbyPayloadType.CONFIRM_REQUEST, NearbyPayloadType.CONFIRM_ACCEPT -> {
                _uiState.update { it.copy(isPeerConfirmed = true) }
                if (_uiState.value.isLocalConfirmed) {
                    finalizeFriendship()
                }
            }
            NearbyPayloadType.CANCEL, NearbyPayloadType.REJECT -> {
                nearbyManager.stopNearbyPairing()
                _uiState.update {
                    it.copy(
                        status = TapToBuddyStatus.ERROR,
                        errorMessage = "Pairing was cancelled by the other device."
                    )
                }
            }
            NearbyPayloadType.MEDIA_METADATA -> {
                // Handled in observeNearbyEvents
            }
        }
    }

    private fun handleDisconnected(endpointId: String) {
        val currentStatus = _uiState.value.status
        if (currentStatus == TapToBuddyStatus.DEVICE_FOUND || currentStatus == TapToBuddyStatus.WAITING_CONFIRMATION) {
            _uiState.update {
                it.copy(
                    status = TapToBuddyStatus.ERROR,
                    errorMessage = "Nearby phone disconnected. Please bring the phones closer and try again."
                )
            }
        }
    }

    fun confirmPairing() {
        val currentUser = _uiState.value.currentUser ?: return
        val peerEndpointId = _uiState.value.peerEndpointId ?: return
        val sessionId = _uiState.value.activeSessionId ?: UUID.randomUUID().toString()

        _uiState.update {
            it.copy(
                isLocalConfirmed = true,
                status = TapToBuddyStatus.WAITING_CONFIRMATION
            )
        }

        // Send confirmation acceptance to peer
        val confirmPayload = NearbyPairingPayload(
            type = NearbyPayloadType.CONFIRM_ACCEPT,
            sessionId = sessionId,
            senderUid = currentUser.uid,
            displayName = currentUser.displayName,
            username = currentUser.username,
            avatarUrl = currentUser.avatarUrl
        )
        nearbyManager.sendPayload(peerEndpointId, confirmPayload)

        if (_uiState.value.isPeerConfirmed) {
            finalizeFriendship()
        }
    }

    private fun finalizeFriendship() {
        val currentUser = _uiState.value.currentUser ?: return
        val peerUser = _uiState.value.peerUser ?: return

        viewModelScope.launch {
            val result = pairingRepository.createMutualFriendship(currentUser, peerUser)
            result.onSuccess { chatId ->
                _uiState.update {
                    it.copy(
                        status = TapToBuddyStatus.SUCCESS,
                        createdChatId = chatId
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        status = TapToBuddyStatus.ERROR,
                        errorMessage = "Could not save connection: " + (error.localizedMessage ?: "Please try again.")
                    )
                }
            }
        }
    }

    /**
     * Sends a photo/media file directly to the connected nearby peer over Nearby Connections.
     */
    fun sendMediaToPeer(context: Context, uri: Uri) {
        val currentUser = _uiState.value.currentUser ?: return
        val peerEndpointId = _uiState.value.peerEndpointId ?: return

        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isTransferringMedia = true,
                        mediaTransferProgress = 0f,
                        mediaBytesTransferred = 0L
                    )
                }

                val imageBytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }

                if (imageBytes == null || imageBytes.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isTransferringMedia = false,
                            errorMessage = "Could not read selected media."
                        )
                    }
                    return@launch
                }

                val filename = "Buddies_Media_" + System.currentTimeMillis() + ".jpg"
                val metadata = NearbyPairingPayload.createMediaMetadata(
                    senderUid = currentUser.uid,
                    displayName = currentUser.displayName,
                    username = currentUser.username,
                    fileName = filename,
                    mimeType = "image/jpeg",
                    mediaSize = imageBytes.size.toLong()
                )

                _uiState.update { it.copy(mediaTotalBytes = imageBytes.size.toLong()) }

                nearbyManager.sendMediaBytes(peerEndpointId, metadata, imageBytes)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTransferringMedia = false,
                        errorMessage = "Media transfer failed: " + e.localizedMessage
                    )
                }
            }
        }
    }

    /**
     * Saves received media bytes to the dedicated 'Buddies' album in MediaStore.
     */
    fun saveReceivedMedia(context: Context) {
        val bytes = _uiState.value.incomingMediaBytes ?: return
        val filename = _uiState.value.incomingMediaMetadata?.fileName ?: ("Buddies_" + System.currentTimeMillis() + ".jpg")

        viewModelScope.launch {
            val savedUri = withContext(Dispatchers.IO) {
                MediaStoreUtils.saveImageToBuddiesAlbum(
                    context = context,
                    imageBytes = bytes,
                    filename = filename
                )
            }

            if (savedUri != null) {
                _uiState.update {
                    it.copy(
                        showMediaPreviewDialog = false,
                        mediaSaveSuccessMessage = "Photo saved to 'Buddies' album in your gallery!",
                        incomingMediaBytes = null,
                        incomingMediaMetadata = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "Failed to save photo to gallery."
                    )
                }
            }
        }
    }

    fun dismissReceivedMedia() {
        _uiState.update {
            it.copy(
                showMediaPreviewDialog = false,
                incomingMediaBytes = null,
                incomingMediaMetadata = null
            )
        }
    }

    fun clearMediaSuccessMessage() {
        _uiState.update { it.copy(mediaSaveSuccessMessage = null) }
    }

    fun cancelPairing() {
        val currentUser = _uiState.value.currentUser
        val peerEndpointId = _uiState.value.peerEndpointId

        if (currentUser != null && peerEndpointId != null) {
            val cancelPayload = NearbyPairingPayload(
                type = NearbyPayloadType.CANCEL,
                sessionId = _uiState.value.activeSessionId ?: "",
                senderUid = currentUser.uid,
                displayName = currentUser.displayName,
                username = currentUser.username
            )
            nearbyManager.sendPayload(peerEndpointId, cancelPayload)
        }

        nearbyManager.stopNearbyPairing()
        timeoutJob?.cancel()

        _uiState.update {
            it.copy(
                status = TapToBuddyStatus.READY,
                peerUser = null,
                peerEndpointId = null,
                errorMessage = null,
                isLocalConfirmed = false,
                isPeerConfirmed = false,
                isTransferringMedia = false,
                showMediaPreviewDialog = false
            )
        }
    }

    fun resetToReady() {
        cancelPairing()
    }

    override fun onCleared() {
        super.onCleared()
        nearbyManager.cleanUp()
        pairingRepository.cleanUp()
        timeoutJob?.cancel()
    }
}

package com.aura.glasschat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.*
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.CallRepository
import com.aura.glasschat.data.repository.ChatRepository
import com.aura.glasschat.data.repository.UserRepository
import com.aura.glasschat.util.ChatUtils
import com.aura.glasschat.webrtc.WebRtcManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.util.UUID

data class CallUiState(
    val callSession: CallSession? = null,
    val callType: CallType = CallType.AUDIO,
    val callStatus: CallStatus = CallStatus.CALLING,
    val isMicMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isSpeakerOn: Boolean = false,
    val isFrontCamera: Boolean = true,
    val callDurationSec: Int = 0,
    val remoteVideoTrack: VideoTrack? = null,
    val errorMessage: String? = null,
    val isEnded: Boolean = false,
    val currentUserId: String = ""
) {
    val durationFormatted: String
        get() {
            val minutes = callDurationSec / 60
            val seconds = callDurationSec % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

class CallViewModel @JvmOverloads constructor(
    application: Application,
    private val callRepository: CallRepository = CallRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CallUiState(currentUserId = authRepository.currentUserId))
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    val eglBaseContext: org.webrtc.EglBase.Context?
        get() = webRtcManager?.eglBase?.eglBaseContext

    private var webRtcManager: WebRtcManager? = null
    private var durationJob: Job? = null
    private var sessionListenerJob: Job? = null
    private var iceCandidatesListenerJob: Job? = null
    private var callTimeoutJob: Job? = null

    init {
        val currentUid = authRepository.currentUserId
        _uiState.update { it.copy(currentUserId = currentUid) }
    }

    fun initWebRtc(isVideo: Boolean, localRenderer: SurfaceViewRenderer? = null) {
        try {
            val app = getApplication<Application>()
            webRtcManager = WebRtcManager(
                context = app.applicationContext,
                onIceCandidateGenerated = { iceCandidate ->
                    val callId = _uiState.value.callSession?.callId ?: return@WebRtcManager
                    val currentUid = _uiState.value.currentUserId
                    viewModelScope.launch {
                        val candidateModel = IceCandidateModel(
                            sdpMid = iceCandidate.sdpMid,
                            sdpMLineIndex = iceCandidate.sdpMLineIndex,
                            sdp = iceCandidate.sdp,
                            senderUid = currentUid
                        )
                        callRepository.sendIceCandidate(callId, candidateModel)
                    }
                },
                onRemoteTrackReceived = { track ->
                    if (track is VideoTrack) {
                        _uiState.update { it.copy(remoteVideoTrack = track) }
                    }
                }
            )
            webRtcManager?.initLocalMedia(isVideo, localRenderer)
            webRtcManager?.createPeerConnection()
        } catch (e: Exception) {
            android.util.Log.e("BUDDYS_CALL", "WebRTC initialization error: ${e.message}", e)
        }
    }

    /**
     * Start an outgoing Call or Answer an incoming Call if one is ringing from this user.
     */
    fun startOrAnswerCall(
        otherUserId: String,
        otherName: String,
        otherAvatarUrl: String?,
        isVideo: Boolean,
        localRenderer: SurfaceViewRenderer? = null
    ) {
        val currentUid = _uiState.value.currentUserId
        viewModelScope.launch {
            val activeIncoming = callRepository.observeIncomingCalls(currentUid).firstOrNull()
            if (activeIncoming != null && activeIncoming.callerUid == otherUserId && (activeIncoming.status == "CALLING" || activeIncoming.status == "RINGING")) {
                answerIncomingCall(activeIncoming, localRenderer)
            } else {
                startOutgoingCall(otherUserId, otherName, otherAvatarUrl, isVideo, localRenderer)
            }
        }
    }

    /**
     * Start an outgoing Call.
     */
    fun startOutgoingCall(
        receiverUid: String,
        receiverName: String,
        receiverAvatarUrl: String?,
        isVideo: Boolean,
        localRenderer: SurfaceViewRenderer? = null
    ) {
        val currentUid = _uiState.value.currentUserId
        val callId = UUID.randomUUID().toString()
        val type = if (isVideo) CallType.VIDEO else CallType.AUDIO

        _uiState.update {
            it.copy(
                callType = type,
                callStatus = CallStatus.CALLING,
                isVideoEnabled = isVideo,
                isSpeakerOn = isVideo
            )
        }

        initWebRtc(isVideo, localRenderer)
        webRtcManager?.setSpeakerphone(isVideo)

        // 45 seconds unanswered call timeout
        callTimeoutJob?.cancel()
        callTimeoutJob = viewModelScope.launch {
            delay(45_000)
            if (_uiState.value.callStatus == CallStatus.CALLING || _uiState.value.callStatus == CallStatus.RINGING) {
                val session = _uiState.value.callSession
                if (session != null) {
                    callRepository.endCall(session.callId, 0)
                }
                _uiState.update { it.copy(errorMessage = "No answer from $receiverName") }
                handleCallEnded()
            }
        }

        viewModelScope.launch {
            val callerUser = userRepository.getUser(currentUid)
            val offerResult = webRtcManager?.createOffer() ?: Result.failure(Exception("WebRTC offer failed"))
            val offerSdp = offerResult.getOrNull() ?: ""

            val session = CallSession(
                callId = callId,
                callerUid = currentUid,
                callerName = callerUser?.displayName ?: "Me",
                callerAvatarUrl = callerUser?.avatarUrl,
                receiverUid = receiverUid,
                receiverName = receiverName,
                receiverAvatarUrl = receiverAvatarUrl,
                type = if (isVideo) "VIDEO" else "AUDIO",
                status = "CALLING",
                sdpOffer = offerSdp,
                createdAt = Timestamp.now()
            )

            _uiState.update { it.copy(callSession = session) }
            callRepository.startCall(session)

            observeCallSession(callId, receiverUid)
        }
    }

    /**
     * Answer an incoming call.
     */
    fun answerIncomingCall(
        incomingSession: CallSession,
        localRenderer: SurfaceViewRenderer? = null
    ) {
        val isVideo = incomingSession.isVideo
        _uiState.update {
            it.copy(
                callSession = incomingSession,
                callType = if (isVideo) CallType.VIDEO else CallType.AUDIO,
                callStatus = CallStatus.ACCEPTED,
                isVideoEnabled = isVideo,
                isSpeakerOn = isVideo
            )
        }

        callTimeoutJob?.cancel()
        initWebRtc(isVideo, localRenderer)
        webRtcManager?.setSpeakerphone(isVideo)

        viewModelScope.launch {
            val offerSdp = incomingSession.sdpOffer ?: ""
            val answerResult = webRtcManager?.createAnswer(offerSdp) ?: Result.failure(Exception("Create answer failed"))
            val answerSdp = answerResult.getOrNull() ?: ""

            callRepository.acceptCall(incomingSession.callId, answerSdp)
            startDurationTimer()
            observeCallSession(incomingSession.callId, incomingSession.callerUid)
        }
    }

    private fun observeCallSession(callId: String, otherUid: String) {
        sessionListenerJob?.cancel()
        sessionListenerJob = viewModelScope.launch {
            callRepository.observeCallSession(callId).collect { session ->
                if (session != null) {
                    if (session.status == "ENDED" || session.status == "REJECTED" || session.status == "MISSED") {
                        handleCallEnded()
                        return@collect
                    }

                    _uiState.update { it.copy(callSession = session) }

                    // If Caller receives SDP Answer from Callee
                    if (session.callerUid == _uiState.value.currentUserId && session.status == "ACCEPTED" && !session.sdpAnswer.isNullOrBlank()) {
                        callTimeoutJob?.cancel()
                        if (_uiState.value.callStatus != CallStatus.ACCEPTED) {
                            _uiState.update { it.copy(callStatus = CallStatus.ACCEPTED) }
                            webRtcManager?.setRemoteAnswer(session.sdpAnswer)
                            startDurationTimer()
                        }
                    }
                }
            }
        }

        // Listen to ICE Candidates from other user
        iceCandidatesListenerJob?.cancel()
        iceCandidatesListenerJob = viewModelScope.launch {
            callRepository.observeIceCandidates(callId, otherUid).collect { candidates ->
                candidates.forEach { model ->
                    val iceCandidate = IceCandidate(model.sdpMid, model.sdpMLineIndex, model.sdp)
                    webRtcManager?.addIceCandidate(iceCandidate)
                }
            }
        }
    }

    private fun startDurationTimer() {
        callTimeoutJob?.cancel()
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(callDurationSec = it.callDurationSec + 1) }
            }
        }
    }

    fun toggleMic() {
        val newMute = !_uiState.value.isMicMuted
        webRtcManager?.toggleAudio(!newMute)
        _uiState.update { it.copy(isMicMuted = newMute) }
    }

    fun toggleVideo() {
        val newVideo = !_uiState.value.isVideoEnabled
        webRtcManager?.toggleVideo(newVideo)
        _uiState.update { it.copy(isVideoEnabled = newVideo) }
    }

    fun switchCamera() {
        webRtcManager?.switchCamera()
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun toggleSpeakerphone() {
        val newSpeaker = !_uiState.value.isSpeakerOn
        webRtcManager?.setSpeakerphone(newSpeaker)
        _uiState.update { it.copy(isSpeakerOn = newSpeaker) }
    }

    fun endCall() {
        val session = _uiState.value.callSession
        val duration = _uiState.value.callDurationSec
        if (session != null) {
            viewModelScope.launch {
                callRepository.endCall(session.callId, duration)
            }
        }
        handleCallEnded()
    }

    fun rejectCall(callId: String) {
        viewModelScope.launch {
            callRepository.rejectCall(callId)
        }
        handleCallEnded()
    }

    private fun handleCallEnded() {
        val session = _uiState.value.callSession
        val currentUid = _uiState.value.currentUserId
        val duration = _uiState.value.callDurationSec
        val wasAccepted = _uiState.value.callStatus == CallStatus.ACCEPTED || duration > 0

        durationJob?.cancel()
        sessionListenerJob?.cancel()
        iceCandidatesListenerJob?.cancel()
        webRtcManager?.close()
        _uiState.update { it.copy(isEnded = true, callStatus = CallStatus.ENDED) }

        // Log call summary to the 1-to-1 conversation if this was the caller
        if (session != null && session.callerUid == currentUid && session.receiverUid.isNotBlank()) {
            val chatId = ChatUtils.getDeterministicChatId(session.callerUid, session.receiverUid)
            val callSummary = if (wasAccepted) {
                "${if (session.isVideo) "📹 Video" else "📞 Voice"} call · ${_uiState.value.durationFormatted}"
            } else {
                "${if (session.isVideo) "📹 Missed video" else "📞 Missed voice"} call"
            }
            viewModelScope.launch {
                chatRepository.sendMessage(
                    chatId = chatId,
                    senderId = currentUid,
                    senderName = session.callerName.ifBlank { "Me" },
                    content = callSummary
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webRtcManager?.close()
    }
}

package com.aura.glasschat.webrtc

import android.content.Context
import android.media.AudioManager
import org.webrtc.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class WebRtcManager(
    private val context: Context,
    private val onIceCandidateGenerated: (IceCandidate) -> Unit = {},
    private val onRemoteTrackReceived: (MediaStreamTrack) -> Unit = {}
) {
    val eglBase: EglBase = EglBase.create()
    private val executor = Executors.newSingleThreadExecutor()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true,
            true
        )
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun initLocalMedia(isVideo: Boolean, localSurfaceRenderer: SurfaceViewRenderer? = null) {
        val factory = peerConnectionFactory ?: return

        // 1. Audio Track
        try {
            val audioConstraints = MediaConstraints()
            localAudioSource = factory.createAudioSource(audioConstraints)
            localAudioTrack = factory.createAudioTrack("local_audio_track", localAudioSource)
        } catch (e: Exception) {
            android.util.Log.e("BUDDYS_WEBRTC", "Failed to initialize local audio: ${e.message}", e)
        }

        // 2. Video Track
        if (isVideo) {
            try {
                videoCapturer = createCameraCapturer()
                if (videoCapturer != null) {
                    val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
                    localVideoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
                    videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
                    videoCapturer?.startCapture(640, 480, 30)

                    localVideoTrack = factory.createVideoTrack("local_video_track", localVideoSource)
                    if (localSurfaceRenderer != null) {
                        localVideoTrack?.addSink(localSurfaceRenderer)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BUDDYS_WEBRTC", "Failed to initialize local camera video: ${e.message}", e)
            }
        }
    }

    fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { onIceCandidateGenerated(it) }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}

                override fun onTrack(transceiver: RtpTransceiver?) {
                    transceiver?.receiver?.track()?.let { track ->
                        onRemoteTrackReceived(track)
                    }
                }
            }
        )

        // Add local tracks to PeerConnection
        localAudioTrack?.let {
            peerConnection?.addTrack(it, listOf("local_stream"))
        }
        localVideoTrack?.let {
            peerConnection?.addTrack(it, listOf("local_stream"))
        }
    }

    suspend fun createOffer(): Result<String> = suspendCancellableCoroutine { continuation ->
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            continuation.resume(Result.success(desc.description))
                        }
                        override fun onCreateFailure(error: String?) {}
                        override fun onSetFailure(error: String?) {
                            continuation.resume(Result.failure(Exception("Set local SDP failed: $error")))
                        }
                    }, desc)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                continuation.resume(Result.failure(Exception("Create SDP offer failed: $error")))
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    suspend fun createAnswer(offerSdp: String): Result<String> = suspendCancellableCoroutine { continuation ->
        val offerDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answerDesc: SessionDescription?) {
                        if (answerDesc != null) {
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    continuation.resume(Result.success(answerDesc.description))
                                }
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetFailure(error: String?) {
                                    continuation.resume(Result.failure(Exception("Set local answer failed: $error")))
                                }
                            }, answerDesc)
                        }
                    }

                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String?) {
                        continuation.resume(Result.failure(Exception("Create answer failed: $error")))
                    }
                    override fun onSetFailure(error: String?) {}
                }, constraints)
            }

            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(error: String?) {
                continuation.resume(Result.failure(Exception("Set remote offer failed: $error")))
            }
        }, offerDesc)
    }

    suspend fun setRemoteAnswer(answerSdp: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val answerDesc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                continuation.resume(Result.success(Unit))
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(error: String?) {
                continuation.resume(Result.failure(Exception("Set remote answer failed: $error")))
            }
        }, answerDesc)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleAudio(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun toggleVideo(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun setSpeakerphone(enabled: Boolean) {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = enabled
        } catch (_: Exception) {}
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Try front camera first
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }

        // Fallback to back camera
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        return null
    }

    fun close() {
        executor.execute {
            try {
                videoCapturer?.stopCapture()
                videoCapturer?.dispose()
            } catch (_: Exception) {}

            try { localAudioTrack?.dispose() } catch (_: Exception) {}
            try { localVideoTrack?.dispose() } catch (_: Exception) {}
            try { localAudioSource?.dispose() } catch (_: Exception) {}
            try { localVideoSource?.dispose() } catch (_: Exception) {}
            try { peerConnection?.close() } catch (_: Exception) {}
            try { peerConnection?.dispose() } catch (_: Exception) {}
            try { peerConnectionFactory?.dispose() } catch (_: Exception) {}
            try { eglBase.release() } catch (_: Exception) {}
        }
    }
}

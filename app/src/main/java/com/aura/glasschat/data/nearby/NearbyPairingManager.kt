package com.aura.glasschat.data.nearby

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

sealed class NearbyEvent {
    object Idle : NearbyEvent()
    object AdvertisingStarted : NearbyEvent()
    object DiscoveryStarted : NearbyEvent()
    data class PeerFound(val endpointId: String, val payload: NearbyPairingPayload) : NearbyEvent()
    data class Connecting(val endpointId: String) : NearbyEvent()
    data class Connected(val endpointId: String) : NearbyEvent()
    data class PayloadReceived(val endpointId: String, val payload: NearbyPairingPayload) : NearbyEvent()
    data class MediaMetadataReceived(val endpointId: String, val metadata: NearbyPairingPayload) : NearbyEvent()
    data class MediaReceived(val endpointId: String, val metadata: NearbyPairingPayload?, val bytes: ByteArray) : NearbyEvent()
    data class TransferProgress(
        val endpointId: String,
        val payloadId: Long,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val isSuccess: Boolean,
        val isFailure: Boolean
    ) : NearbyEvent()
    data class Disconnected(val endpointId: String) : NearbyEvent()
    data class Error(val message: String) : NearbyEvent()
}

class NearbyPairingManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    companion object {
        const val TAG = "TAP_TO_BUDDY"
        const val SERVICE_ID = "com.aura.glasschat.nearby.v1"
        val STRATEGY: Strategy = Strategy.P2P_POINT_TO_POINT
    }

    private val connectionsClient = Nearby.getConnectionsClient(context.applicationContext)

    private val _event = MutableStateFlow<NearbyEvent>(NearbyEvent.Idle)
    val event: StateFlow<NearbyEvent> = _event.asStateFlow()

    private var localPayload: NearbyPairingPayload? = null
    var connectedEndpointId: String? = null
        private set

    private var isAdvertising = false
    private var isDiscovering = false
    private var isConnecting = false
    private var discoveryStartTimeMs: Long = 0L

    private val pendingMediaMetadata = ConcurrentHashMap<String, NearbyPairingPayload>()

    private fun logTimestamp(eventKey: String, details: String = "") {
        val elapsed = if (discoveryStartTimeMs > 0L) {
            SystemClock.elapsedRealtime() - discoveryStartTimeMs
        } else {
            0L
        }
        val detailStr = if (details.isNotBlank()) " $details" else ""
        Log.d(TAG, "[$TAG] $eventKey=${elapsed}ms$detailStr")
    }

    fun startNearbyPairing(payload: NearbyPairingPayload) {
        discoveryStartTimeMs = SystemClock.elapsedRealtime()
        logTimestamp("discoveryStart")
        localPayload = payload
        stopNearbyPairingInternal(resetStartTime = false)

        // Start both simultaneously without delay
        startAdvertising(payload)
        startDiscovery()
    }

    private fun startAdvertising(payload: NearbyPairingPayload) {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        val endpointName = NearbyPairingPayload.encodeCompactEndpointName(
            uid = payload.senderUid,
            username = payload.username,
            displayName = payload.displayName
        )

        connectionsClient.startAdvertising(
            endpointName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            isAdvertising = true
            logTimestamp("advertisingStarted")
            _event.value = NearbyEvent.AdvertisingStarted
        }.addOnFailureListener { e ->
            isAdvertising = false
            logTimestamp("advertisingFailed", e.localizedMessage ?: "Unknown error")
            _event.value = NearbyEvent.Error("Nearby advertising unavailable: " + (e.localizedMessage ?: "Unknown error"))
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            isDiscovering = true
            logTimestamp("discoveryStarted")
            _event.value = NearbyEvent.DiscoveryStarted
        }.addOnFailureListener { e ->
            isDiscovering = false
            logTimestamp("discoveryFailed", e.localizedMessage ?: "Unknown error")
            _event.value = NearbyEvent.Error("Nearby discovery unavailable: " + (e.localizedMessage ?: "Unknown error"))
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            logTimestamp("endpointFound", "endpointId=$endpointId rawName='${info.endpointName}'")

            val currentPayload = localPayload ?: return

            // Immediately parse compact endpoint name if provided for sub-100ms discovery response
            val compactInfo = NearbyPairingPayload.decodeCompactEndpointName(info.endpointName)
            if (compactInfo != null) {
                val (peerUid, peerUsername, peerDisplay) = compactInfo
                if (peerUid != currentPayload.senderUid) {
                    val peerOffer = NearbyPairingPayload.createOffer(
                        senderUid = peerUid,
                        displayName = peerDisplay,
                        username = peerUsername,
                        avatarUrl = null,
                        sessionId = currentPayload.sessionId
                    )
                    logTimestamp("peerFoundInstant", "peer=@$peerUsername uid=$peerUid")
                    _event.value = NearbyEvent.PeerFound(endpointId, peerOffer)
                }
            }

            if (isConnecting || connectedEndpointId != null) return

            // Asymmetric tie-breaking for simultaneous discovery:
            // The device with lexicographically smaller UID initiates the connection request
            // to avoid simultaneous connection collision in P2P_POINT_TO_POINT mode.
            val peerUid = compactInfo?.first
            val shouldInitiate = peerUid == null || currentPayload.senderUid < peerUid

            if (shouldInitiate) {
                isConnecting = true
                val localName = NearbyPairingPayload.encodeCompactEndpointName(
                    uid = currentPayload.senderUid,
                    username = currentPayload.username,
                    displayName = currentPayload.displayName
                )

                logTimestamp("connectionStarted", "initiating connection to $endpointId")
                _event.value = NearbyEvent.Connecting(endpointId)

                connectionsClient.requestConnection(
                    localName,
                    endpointId,
                    connectionLifecycleCallback
                ).addOnFailureListener { e ->
                    isConnecting = false
                    logTimestamp("connectionRequestFailed", e.localizedMessage ?: "")
                    _event.value = NearbyEvent.Error("Could not connect to nearby phone: " + e.localizedMessage)
                }
            } else {
                logTimestamp("awaitingPeerConnection", "peer has lower priority UID, waiting for incoming request")
            }
        }

        override fun onEndpointLost(endpointId: String) {
            logTimestamp("endpointLost", "endpointId=$endpointId")
            if (connectedEndpointId == endpointId) {
                _event.value = NearbyEvent.Disconnected(endpointId)
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            logTimestamp("connectionInitiated", "endpointId=$endpointId peerName='${connectionInfo.endpointName}'")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    connectedEndpointId = endpointId
                }
                .addOnFailureListener { e ->
                    logTimestamp("connectionAcceptFailed", e.localizedMessage ?: "")
                    _event.value = NearbyEvent.Error("Connection rejected: " + e.localizedMessage)
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            isConnecting = false
            logTimestamp("connectionResult", "status=${result.status.statusCode}")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpointId = endpointId
                    _event.value = NearbyEvent.Connected(endpointId)

                    localPayload?.let { payload ->
                        logTimestamp("offerSent", "target=$endpointId")
                        sendPayload(endpointId, payload)
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    connectedEndpointId = null
                    _event.value = NearbyEvent.Error("Nearby connection was rejected by peer.")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    connectedEndpointId = null
                    _event.value = NearbyEvent.Error("Nearby connection lost. Please try again.")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            logTimestamp("disconnected", "endpointId=$endpointId")
            if (connectedEndpointId == endpointId) {
                connectedEndpointId = null
                pendingMediaMetadata.remove(endpointId)
                _event.value = NearbyEvent.Disconnected(endpointId)
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val jsonStr = try {
                    String(bytes, Charsets.UTF_8)
                } catch (_: Exception) {
                    null
                }

                val parsed = jsonStr?.let { NearbyPairingPayload.fromJson(it) }

                if (parsed != null) {
                    when (parsed.type) {
                        NearbyPayloadType.OFFER -> {
                            logTimestamp("offerReceived", "from=@${parsed.username}")
                            _event.value = NearbyEvent.PeerFound(endpointId, parsed)
                        }
                        NearbyPayloadType.MEDIA_METADATA -> {
                            logTimestamp("mediaMetadataReceived", "file=${parsed.fileName} size=${parsed.mediaSize}")
                            pendingMediaMetadata[endpointId] = parsed
                            _event.value = NearbyEvent.MediaMetadataReceived(endpointId, parsed)
                        }
                        NearbyPayloadType.CONFIRM_REQUEST, NearbyPayloadType.CONFIRM_ACCEPT -> {
                            logTimestamp("confirmation", "type=${parsed.type}")
                            _event.value = NearbyEvent.PayloadReceived(endpointId, parsed)
                        }
                        else -> {
                            _event.value = NearbyEvent.PayloadReceived(endpointId, parsed)
                        }
                    }
                } else {
                    val metadata = pendingMediaMetadata.remove(endpointId)
                    logTimestamp("mediaReceived", "bytes=${bytes.size}")
                    _event.value = NearbyEvent.MediaReceived(endpointId, metadata, bytes)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val isSuccess = update.status == PayloadTransferUpdate.Status.SUCCESS
            val isFailure = update.status == PayloadTransferUpdate.Status.FAILURE ||
                    update.status == PayloadTransferUpdate.Status.CANCELED

            _event.value = NearbyEvent.TransferProgress(
                endpointId = endpointId,
                payloadId = update.payloadId,
                bytesTransferred = update.bytesTransferred,
                totalBytes = update.totalBytes,
                isSuccess = isSuccess,
                isFailure = isFailure
            )
        }
    }

    fun sendPayload(endpointId: String? = connectedEndpointId, payload: NearbyPairingPayload) {
        val target = endpointId ?: connectedEndpointId ?: return
        try {
            val bytesPayload = Payload.fromBytes(payload.toByteArray())
            connectionsClient.sendPayload(target, bytesPayload)
        } catch (e: Exception) {
            _event.value = NearbyEvent.Error("Failed to send pairing message: " + e.localizedMessage)
        }
    }

    fun sendMediaBytes(
        endpointId: String? = connectedEndpointId,
        metadata: NearbyPairingPayload,
        mediaBytes: ByteArray
    ) {
        val target = endpointId ?: connectedEndpointId ?: return
        try {
            val headerPayload = Payload.fromBytes(metadata.toByteArray())
            connectionsClient.sendPayload(target, headerPayload)

            val mediaPayload = Payload.fromBytes(mediaBytes)
            connectionsClient.sendPayload(target, mediaPayload)
        } catch (e: Exception) {
            _event.value = NearbyEvent.Error("Failed to transfer media: " + e.localizedMessage)
        }
    }

    private fun stopNearbyPairingInternal(resetStartTime: Boolean = true) {
        try {
            if (isAdvertising) {
                connectionsClient.stopAdvertising()
                isAdvertising = false
            }
            if (isDiscovering) {
                connectionsClient.stopDiscovery()
                isDiscovering = false
            }
            connectionsClient.stopAllEndpoints()
            connectedEndpointId = null
            pendingMediaMetadata.clear()
            isConnecting = false
            if (resetStartTime) {
                discoveryStartTimeMs = 0L
            }
            _event.value = NearbyEvent.Idle
        } catch (_: Exception) {}
    }

    fun stopNearbyPairing() {
        logTimestamp("discoveryStop")
        stopNearbyPairingInternal(resetStartTime = true)
    }

    fun cleanUp() {
        stopNearbyPairing()
        scope.cancel()
    }
}


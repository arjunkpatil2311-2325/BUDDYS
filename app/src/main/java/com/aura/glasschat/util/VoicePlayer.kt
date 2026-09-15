package com.aura.glasschat.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val currentMessageId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val speed: Float = 1.0f
)

class VoicePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var currentSpeed: Float = 1.0f

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun playLocalFile(messageId: String, file: File) {
        if (!file.exists()) return
        playUri(messageId, Uri.fromFile(file))
    }

    fun playUrl(messageId: String, url: String) {
        if (url.isBlank()) return
        playUri(messageId, Uri.parse(url))
    }

    private fun playUri(messageId: String, uri: Uri) {
        val currentState = _playbackState.value

        // If clicking the currently playing message, toggle pause
        if (currentState.currentMessageId == messageId && mediaPlayer != null) {
            if (currentState.isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }

        // Stop current audio and start fresh
        stopAndReset()

        _playbackState.value = PlaybackState(
            currentMessageId = messageId,
            isPlaying = false,
            isBuffering = true,
            currentPositionMs = 0L,
            totalDurationMs = 0L,
            speed = currentSpeed
        )

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    val duration = mp.duration.toLong().coerceAtLeast(0L)
                    applySpeed(mp, currentSpeed)
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                        isBuffering = false,
                        totalDurationMs = duration,
                        currentPositionMs = 0L,
                        speed = currentSpeed
                    )
                    mp.start()
                    startProgressTracker()
                }
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        currentPositionMs = 0L
                    )
                    stopProgressTracker()
                }
                setOnErrorListener { _, _, _ ->
                    stopAndReset()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (_: Exception) {
            stopAndReset()
        }
    }

    fun toggleSpeed() {
        val nextSpeed = when (currentSpeed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        setSpeed(nextSpeed)
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
        mediaPlayer?.let { applySpeed(it, speed) }
        _playbackState.value = _playbackState.value.copy(speed = speed)
    }

    private fun applySpeed(player: MediaPlayer, speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = player.playbackParams ?: PlaybackParams()
                params.speed = speed
                player.playbackParams = params
            }
        } catch (_: Exception) {}
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = it.currentPosition.toLong()
                    )
                }
            }
        } catch (_: Exception) {}
        stopProgressTracker()
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                it.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startProgressTracker()
            }
        } catch (_: Exception) {}
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.let {
                it.seekTo(positionMs.toInt())
                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = positionMs
                )
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        stopAndReset()
    }

    fun release() {
        stopAndReset()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            val pos = player.currentPosition.toLong()
                            val dur = player.duration.toLong().coerceAtLeast(0L)
                            _playbackState.value = _playbackState.value.copy(
                                currentPositionMs = pos,
                                totalDurationMs = if (dur > 0) dur else _playbackState.value.totalDurationMs
                            )
                        }
                    } catch (_: Exception) {}
                }
                delay(50)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopAndReset() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _playbackState.value = PlaybackState(speed = currentSpeed)
    }
}


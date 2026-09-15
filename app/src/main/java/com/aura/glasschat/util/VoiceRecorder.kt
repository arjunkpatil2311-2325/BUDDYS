package com.aura.glasschat.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.io.IOException

class VoiceRecorder(private val context: Context) {

    companion object {
        const val MAX_RECORDING_DURATION_MS = 10 * 60 * 1000L // 10 minutes maximum
        const val MIN_RECORDING_DURATION_MS = 500L
    }

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMillis: Long = 0L
    private var accumulatedDurationMs: Long = 0L
    private var pauseStartTimeMillis: Long = 0L

    private var isRecording: Boolean = false
    private var isPaused: Boolean = false

    fun startRecording(): Result<File> {
        return try {
            stopAndRelease()

            val outputDir = File(context.cacheDir, "voice_notes").apply {
                if (!exists()) mkdirs()
            }
            val file = File(outputDir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            isRecording = true
            isPaused = false
            accumulatedDurationMs = 0L
            startTimeMillis = SystemClock.elapsedRealtime()
            Result.success(file)
        } catch (e: Exception) {
            stopAndRelease()
            currentOutputFile?.delete()
            currentOutputFile = null
            Result.failure(e)
        }
    }

    fun pauseRecording(): Boolean {
        if (!isRecording || isPaused || recorder == null) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorder?.pause()
                accumulatedDurationMs += (SystemClock.elapsedRealtime() - startTimeMillis)
                pauseStartTimeMillis = SystemClock.elapsedRealtime()
                isPaused = true
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun resumeRecording(): Boolean {
        if (!isRecording || !isPaused || recorder == null) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorder?.resume()
                startTimeMillis = SystemClock.elapsedRealtime()
                isPaused = false
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun stopRecording(): Result<RecordedAudio> {
        if (!isRecording || recorder == null) {
            return Result.failure(IllegalStateException("Not recording"))
        }

        return try {
            val finalDurationMs = if (isPaused) {
                accumulatedDurationMs
            } else {
                accumulatedDurationMs + (SystemClock.elapsedRealtime() - startTimeMillis)
            }.coerceAtMost(MAX_RECORDING_DURATION_MS)

            try {
                recorder?.stop()
            } catch (_: Exception) {}

            stopAndRelease()

            val file = currentOutputFile
            if (file != null && file.exists() && file.length() > 0 && finalDurationMs >= MIN_RECORDING_DURATION_MS) {
                Result.success(RecordedAudio(file = file, durationMs = finalDurationMs))
            } else {
                file?.delete()
                currentOutputFile = null
                Result.failure(IOException("Audio recording too short or empty"))
            }
        } catch (e: Exception) {
            stopAndRelease()
            currentOutputFile?.delete()
            currentOutputFile = null
            Result.failure(e)
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                recorder?.stop()
            }
        } catch (_: Exception) {
        } finally {
            stopAndRelease()
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }

    fun getElapsedTimeMs(): Long {
        if (!isRecording) return 0L
        val current = if (isPaused) {
            accumulatedDurationMs
        } else {
            accumulatedDurationMs + (SystemClock.elapsedRealtime() - startTimeMillis)
        }
        return current.coerceAtMost(MAX_RECORDING_DURATION_MS)
    }

    fun getMaxAmplitude(): Int {
        return if (isRecording && !isPaused) {
            try {
                recorder?.maxAmplitude ?: 0
            } catch (_: Exception) {
                0
            }
        } else {
            0
        }
    }

    fun getNormalizedAmplitude(): Float {
        val maxAmp = getMaxAmplitude()
        return (maxAmp.toFloat() / 32767f).coerceIn(0.05f, 1f)
    }

    fun isCurrentlyRecording(): Boolean = isRecording
    fun isCurrentlyPaused(): Boolean = isPaused

    private fun stopAndRelease() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        } finally {
            recorder = null
            isRecording = false
            isPaused = false
            accumulatedDurationMs = 0L
        }
    }
}

data class RecordedAudio(
    val file: File,
    val durationMs: Long
)


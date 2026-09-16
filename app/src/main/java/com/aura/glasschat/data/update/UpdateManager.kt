package com.aura.glasschat.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.aura.glasschat.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    data class Progress(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Success(val apkFile: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        const val UPDATE_MANIFEST_URL = "https://buddys01.vercel.app/update.json"
        private const val PREFS_NAME = "buddys_update_prefs"
        private const val KEY_LAST_CHECK = "last_check_timestamp"
        private const val KEY_CACHED_MANIFEST = "cached_update_manifest"
        private const val CHECK_COOLDOWN_MS = 15 * 60 * 1000L // 15 minutes cooldown between checks

        @Volatile
        private var instance: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            return instance ?: synchronized(this) {
                instance ?: UpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _availableUpdate = MutableStateFlow<UpdateManifest?>(null)
    val availableUpdate = _availableUpdate.asStateFlow()

    private val _promptUpdateEvent = MutableStateFlow<UpdateManifest?>(null)
    val promptUpdateEvent = _promptUpdateEvent.asStateFlow()

    init {
        restoreCachedManifest()
    }

    private fun restoreCachedManifest() {
        val cachedJson = prefs.getString(KEY_CACHED_MANIFEST, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val manifest = UpdateManifest.fromJson(cachedJson)
                val currentVersionCode = BuildConfig.VERSION_CODE
                if (manifest.versionCode > currentVersionCode) {
                    Log.d(TAG, "[UPDATE RESTORE] Restored cached update: v${manifest.latestVersion} (code ${manifest.versionCode}) > current code $currentVersionCode")
                    _availableUpdate.value = manifest
                } else {
                    Log.d(TAG, "[UPDATE RESTORE] Cleared outdated cached update: v${manifest.latestVersion} (code ${manifest.versionCode}) <= current code $currentVersionCode")
                    prefs.edit().remove(KEY_CACHED_MANIFEST).apply()
                    _availableUpdate.value = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "[UPDATE RESTORE] Failed to parse cached manifest: ${e.message}")
                prefs.edit().remove(KEY_CACHED_MANIFEST).apply()
            }
        }
    }

    fun requestUpdatePrompt(manifest: UpdateManifest) {
        Log.d(TAG, "[UPDATE PROMPT REQUESTED] From UI for v${manifest.latestVersion} (code ${manifest.versionCode})")
        _promptUpdateEvent.value = manifest
    }

    fun clearPromptEvent() {
        _promptUpdateEvent.value = null
    }

    /**
     * Checks the online manifest for updates.
     * Returns UpdateManifest if a newer version is available, null otherwise.
     */
    suspend fun checkForUpdates(force: Boolean = false): UpdateManifest? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
        val currentVersionCode = BuildConfig.VERSION_CODE

        Log.d(TAG, "[UPDATE CHECK START] force=$force, localVersion=${BuildConfig.VERSION_NAME} (code $currentVersionCode)")

        if (!force && (now - lastCheck < CHECK_COOLDOWN_MS)) {
            val cached = _availableUpdate.value
            Log.d(TAG, "[UPDATE CHECK] Inside 15m cooldown window (${(now - lastCheck) / 1000}s elapsed). Active cached update: ${cached?.latestVersion ?: "none"}")
            return@withContext cached
        }

        try {
            val request = Request.Builder()
                .url(UPDATE_MANIFEST_URL)
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "Buddies-Android/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            Log.d(TAG, "[UPDATE CHECK] Querying online endpoint: $UPDATE_MANIFEST_URL")
            val response = httpClient.newCall(request).execute()
            Log.d(TAG, "[UPDATE CHECK] HTTP Response code: ${response.code}")

            if (!response.isSuccessful) {
                Log.w(TAG, "[UPDATE CHECK] Server check failed with HTTP ${response.code}")
                return@withContext _availableUpdate.value
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                Log.w(TAG, "[UPDATE CHECK] Empty manifest response body")
                return@withContext _availableUpdate.value
            }

            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

            val manifest = UpdateManifest.fromJson(body)
            Log.d(TAG, "[UPDATE CHECK] Online version: v${manifest.latestVersion} (code ${manifest.versionCode}), Local: v${BuildConfig.VERSION_NAME} (code $currentVersionCode)")

            // Update is available if online versionCode is strictly greater than local versionCode
            if (manifest.versionCode > currentVersionCode) {
                Log.d(TAG, "[UPDATE AVAILABLE] Higher version detected: v${manifest.latestVersion} (code ${manifest.versionCode}) > local $currentVersionCode")
                prefs.edit().putString(KEY_CACHED_MANIFEST, manifest.toJson()).apply()
                _availableUpdate.value = manifest
                return@withContext manifest
            } else {
                Log.d(TAG, "[UP TO DATE] App is current: local code $currentVersionCode >= online code ${manifest.versionCode}")
                prefs.edit().remove(KEY_CACHED_MANIFEST).apply()
                _availableUpdate.value = null
                return@withContext null
            }
        } catch (e: Exception) {
            Log.w(TAG, "[UPDATE CHECK ERROR] Network or parse failure: ${e.message}")
            return@withContext _availableUpdate.value
        }
    }

    /**
     * Actively forces an update check bypassing cooldown and returns (hasUpdate, manifest).
     */
    suspend fun forceCheckForUpdate(): Pair<Boolean, UpdateManifest?> {
        val manifest = checkForUpdates(force = true)
        return Pair(manifest != null, manifest)
    }

    /**
     * High-performance streamed APK download with 64KB buffering and throttled progress updates.
     */
    fun downloadApk(manifest: UpdateManifest): Flow<DownloadState> = flow {
        try {
            Log.d(TAG, "[DOWNLOAD START] File: ${manifest.apkFileName}, Target URL: ${manifest.apkUrl}, Stated size: ${manifest.fileSize}")
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, manifest.apkFileName)

            if (apkFile.exists()) {
                apkFile.delete()
            }

            val request = Request.Builder()
                .url(manifest.apkUrl)
                .header("User-Agent", "Buddies-Android/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "[DOWNLOAD ERROR] HTTP ${response.code} from server")
                emit(DownloadState.Error("Server returned error: ${response.code}"))
                return@flow
            }

            val responseBody = response.body
            if (responseBody == null) {
                Log.e(TAG, "[DOWNLOAD ERROR] Null response body")
                emit(DownloadState.Error("Empty download response body"))
                return@flow
            }

            val contentLength = responseBody.contentLength()
            Log.d(TAG, "[DOWNLOAD STREAM] Content-Length: $contentLength bytes")
            var inputStream: java.io.BufferedInputStream? = null
            var outputStream: java.io.BufferedOutputStream? = null

            try {
                inputStream = java.io.BufferedInputStream(responseBody.byteStream(), 64 * 1024)
                outputStream = java.io.BufferedOutputStream(FileOutputStream(apkFile), 64 * 1024)

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastProgressEmit = 0L
                var lastEmittedPercent = -1

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val now = System.currentTimeMillis()
                    val currentPercent = if (contentLength > 0) {
                        ((totalBytesRead.toDouble() / contentLength.toDouble()) * 100).toInt()
                    } else {
                        -1
                    }

                    // Throttle emissions to every 250ms or on each 1% milestone / finish to prevent UI recomposition lag
                    val shouldEmit = (now - lastProgressEmit > 250) || 
                                     (currentPercent != lastEmittedPercent && currentPercent >= 0) || 
                                     (totalBytesRead == contentLength)

                    if (shouldEmit) {
                        lastProgressEmit = now
                        lastEmittedPercent = currentPercent
                        val progress = if (contentLength > 0) {
                            (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        emit(DownloadState.Progress(progress, totalBytesRead, contentLength))
                    }
                }

                outputStream.flush()
                Log.d(TAG, "[DOWNLOAD COMPLETE] Finished downloading ${apkFile.length()} bytes to ${apkFile.absolutePath}")

                if (apkFile.length() < 10 * 1024 * 1024) {
                    Log.e(TAG, "[DOWNLOAD CORRUPTED] Output file size ${apkFile.length()} bytes is too small")
                    emit(DownloadState.Error("Downloaded file is incomplete or corrupted."))
                } else {
                    emit(DownloadState.Success(apkFile))
                }

            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
            }

        } catch (e: Exception) {
            Log.e(TAG, "[DOWNLOAD EXCEPTION] APK Download failed: ${e.message}", e)
            emit(DownloadState.Error(e.localizedMessage ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Checks if the app has permission to request package installs on Android 8.0+
     */
    fun canInstallApks(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Launches Android's official APK installer.
     */
    fun launchInstaller(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "[INSTALLER ERROR] Cannot launch installer - APK file does not exist at ${apkFile.absolutePath}")
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
            Log.d(TAG, "[INSTALLER LAUNCH] Successfully launched APK installer with URI: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "[INSTALLER EXCEPTION] Failed to launch package installer: ${e.message}", e)
        }
    }

    /**
     * Opens Android System Settings to allow installing unknown apps from BUDDYS.
     */
    fun openUnknownAppSourcesSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open specific package installer settings", e)
            }
        }
    }
}

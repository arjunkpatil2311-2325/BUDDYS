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
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Checks the online manifest for updates.
     * Returns UpdateManifest if a newer version is available, null otherwise.
     */
    suspend fun checkForUpdates(force: Boolean = false): UpdateManifest? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)

        if (!force && (now - lastCheck < CHECK_COOLDOWN_MS)) {
            Log.d(TAG, "Skipping update check - inside cooldown window")
            return@withContext null
        }

        try {
            val request = Request.Builder()
                .url(UPDATE_MANIFEST_URL)
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Update check failed with HTTP ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

            val manifest = UpdateManifest.fromJson(body)
            val currentVersionCode = BuildConfig.VERSION_CODE

            Log.d(TAG, "Online version: ${manifest.latestVersion} (code ${manifest.versionCode}), Local: ${BuildConfig.VERSION_NAME} (code $currentVersionCode)")

            // Update is available if online versionCode is strictly greater than local versionCode
            if (manifest.versionCode > currentVersionCode) {
                return@withContext manifest
            }

            return@withContext null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check for updates: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Streams APK download with progress updates.
     */
    fun downloadApk(manifest: UpdateManifest): Flow<DownloadState> = flow {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, manifest.apkFileName)

            if (apkFile.exists()) {
                apkFile.delete()
            }

            val request = Request.Builder()
                .url(manifest.apkUrl)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadState.Error("Server returned error: ${response.code}"))
                return@flow
            }

            val responseBody = response.body
            if (responseBody == null) {
                emit(DownloadState.Error("Empty download response body"))
                return@flow
            }

            val contentLength = responseBody.contentLength()
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = responseBody.byteStream()
                outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastProgressEmit = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val now = System.currentTimeMillis()
                    // Throttle progress updates to every 100ms
                    if (now - lastProgressEmit > 100 || totalBytesRead == contentLength) {
                        lastProgressEmit = now
                        val progress = if (contentLength > 0) {
                            (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        emit(DownloadState.Progress(progress, totalBytesRead, contentLength))
                    }
                }

                outputStream.flush()
                emit(DownloadState.Success(apkFile))

            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
            }

        } catch (e: Exception) {
            Log.e(TAG, "APK Download failed", e)
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
                Log.e(TAG, "Cannot launch installer - APK file does not exist at ${apkFile.absolutePath}")
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
            Log.d(TAG, "Launched APK installer with URI: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
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

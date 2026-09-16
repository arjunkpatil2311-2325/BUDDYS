package com.aura.glasschat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.BuildConfig
import com.aura.glasschat.data.update.DownloadState
import com.aura.glasschat.data.update.UpdateManager
import com.aura.glasschat.data.update.UpdateManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

import android.util.Log

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpdateAvailable(
        val manifest: UpdateManifest,
        val isMandatory: Boolean
    ) : UpdateUiState
    data class Downloading(
        val manifest: UpdateManifest,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val isMandatory: Boolean
    ) : UpdateUiState
    data class ReadyToInstall(
        val manifest: UpdateManifest,
        val apkFile: File,
        val isMandatory: Boolean
    ) : UpdateUiState
    data class Error(
        val manifest: UpdateManifest?,
        val message: String,
        val isMandatory: Boolean
    ) : UpdateUiState
}

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "UpdateViewModel"
    }

    private val updateManager = UpdateManager.getInstance(application)

    val availableUpdate: StateFlow<UpdateManifest?> = updateManager.availableUpdate

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        // Automatically check for updates on startup
        Log.d(TAG, "[INIT] Checking for updates on startup")
        checkForUpdates(force = false)

        // Observe manual prompt requests from Profile, Notification Tab, Settings
        viewModelScope.launch {
            updateManager.promptUpdateEvent.collect { manifest ->
                if (manifest != null) {
                    Log.d(TAG, "[PROMPT EVENT RECEIVED] Opening update dialog for v${manifest.latestVersion}")
                    promptUpdate(manifest)
                    updateManager.clearPromptEvent()
                }
            }
        }
    }

    /**
     * Manually triggers the update dialog/flow for a manifest.
     */
    fun promptUpdate(manifest: UpdateManifest, isMandatory: Boolean = false) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val mandatory = isMandatory || manifest.isMandatory || (currentVersionCode < manifest.minimumSupportedVersionCode)
        Log.d(TAG, "[PROMPT UPDATE] Setting state to UpdateAvailable for v${manifest.latestVersion} (mandatory=$mandatory)")
        _uiState.value = UpdateUiState.UpdateAvailable(
            manifest = manifest,
            isMandatory = mandatory
        )
    }

    /**
     * Checks online manifest.
     */
    fun checkForUpdates(force: Boolean = false) {
        viewModelScope.launch {
            Log.d(TAG, "[CHECK FOR UPDATES] Initiating check (force=$force)")
            val manifest = updateManager.checkForUpdates(force)
            if (manifest != null) {
                val currentVersionCode = BuildConfig.VERSION_CODE
                val isMandatory = manifest.isMandatory || (currentVersionCode < manifest.minimumSupportedVersionCode)
                Log.d(TAG, "[CHECK RESULT] Update found v${manifest.latestVersion}, presenting dialog")
                _uiState.value = UpdateUiState.UpdateAvailable(
                    manifest = manifest,
                    isMandatory = isMandatory
                )
            } else {
                Log.d(TAG, "[CHECK RESULT] No new update returned by UpdateManager")
            }
        }
    }

    /**
     * Starts downloading the APK from manifest.apkUrl.
     */
    fun startDownload(manifest: UpdateManifest, isMandatory: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "[START DOWNLOAD] Beginning download of ${manifest.apkFileName}")
            _uiState.value = UpdateUiState.Downloading(
                manifest = manifest,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = 0L,
                isMandatory = isMandatory
            )

            updateManager.downloadApk(manifest).collect { state ->
                when (state) {
                    is DownloadState.Progress -> {
                        _uiState.value = UpdateUiState.Downloading(
                            manifest = manifest,
                            progress = state.progress,
                            downloadedBytes = state.downloadedBytes,
                            totalBytes = state.totalBytes,
                            isMandatory = isMandatory
                        )
                    }
                    is DownloadState.Success -> {
                        Log.d(TAG, "[DOWNLOAD SUCCESS] APK ready at ${state.apkFile.absolutePath}")
                        _uiState.value = UpdateUiState.ReadyToInstall(
                            manifest = manifest,
                            apkFile = state.apkFile,
                            isMandatory = isMandatory
                        )
                        // Trigger installer automatically
                        installApk(state.apkFile)
                    }
                    is DownloadState.Error -> {
                        Log.e(TAG, "[DOWNLOAD ERROR] ${state.message}")
                        _uiState.value = UpdateUiState.Error(
                            manifest = manifest,
                            message = state.message,
                            isMandatory = isMandatory
                        )
                    }
                }
            }
        }
    }

    /**
     * Installs the downloaded APK.
     */
    fun installApk(apkFile: File) {
        Log.d(TAG, "[INSTALL APK] Requesting install for ${apkFile.name}")
        if (!updateManager.canInstallApks()) {
            updateManager.openUnknownAppSourcesSetting()
        }
        updateManager.launchInstaller(apkFile)
    }

    /**
     * Dismisses the dialog for non-mandatory updates when user taps 'Later'.
     */
    fun dismiss() {
        val current = _uiState.value
        if (current is UpdateUiState.UpdateAvailable && current.isMandatory) {
            Log.d(TAG, "[DISMISS BLOCKED] Update is mandatory")
            return
        }
        Log.d(TAG, "[DISMISSED WITH LATER] Dialog closed. Centralized available update remains active: ${availableUpdate.value?.latestVersion ?: "none"}")
        _uiState.value = UpdateUiState.Idle
    }
}

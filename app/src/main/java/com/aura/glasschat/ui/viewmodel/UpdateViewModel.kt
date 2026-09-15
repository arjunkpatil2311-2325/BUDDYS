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

    private val updateManager = UpdateManager.getInstance(application)

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        // Automatically check for updates on startup
        checkForUpdates(force = false)
    }

    /**
     * Checks online manifest.
     */
    fun checkForUpdates(force: Boolean = false) {
        viewModelScope.launch {
            val manifest = updateManager.checkForUpdates(force)
            if (manifest != null) {
                val currentVersionCode = BuildConfig.VERSION_CODE
                val isMandatory = manifest.isMandatory || (currentVersionCode < manifest.minimumSupportedVersionCode)
                _uiState.value = UpdateUiState.UpdateAvailable(
                    manifest = manifest,
                    isMandatory = isMandatory
                )
            }
        }
    }

    /**
     * Starts downloading the APK from manifest.apkUrl.
     */
    fun startDownload(manifest: UpdateManifest, isMandatory: Boolean) {
        viewModelScope.launch {
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
                        _uiState.value = UpdateUiState.ReadyToInstall(
                            manifest = manifest,
                            apkFile = state.apkFile,
                            isMandatory = isMandatory
                        )
                        // Trigger installer automatically
                        installApk(state.apkFile)
                    }
                    is DownloadState.Error -> {
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
        if (!updateManager.canInstallApks()) {
            updateManager.openUnknownAppSourcesSetting()
        }
        updateManager.launchInstaller(apkFile)
    }

    /**
     * Dismisses the dialog for non-mandatory updates.
     */
    fun dismiss() {
        val current = _uiState.value
        if (current is UpdateUiState.UpdateAvailable && current.isMandatory) {
            // Cannot dismiss mandatory updates
            return
        }
        _uiState.value = UpdateUiState.Idle
    }
}

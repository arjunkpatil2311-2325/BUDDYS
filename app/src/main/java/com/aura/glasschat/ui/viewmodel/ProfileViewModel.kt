package com.aura.glasschat.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

data class ProfileUiState(
    val user: User? = null,
    val isEditing: Boolean = false,
    val editDisplayName: String = "",
    val editStatusMessage: String = "",
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val isLoggedOut: Boolean = false,
    val showPhotoOptions: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel @JvmOverloads constructor(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var profileJob: kotlinx.coroutines.Job? = null

    init {
        loadProfile()
    }

    fun refresh() {
        loadProfile()
    }

    private fun loadProfile() {
        profileJob?.cancel()
        val currentUid = authRepository.currentUserId
        if (currentUid.isNotEmpty()) {
            profileJob = viewModelScope.launch {
                userRepository.observeUserProfile(currentUid).collect { user ->
                    _uiState.update {
                        it.copy(
                            user = user,
                            editDisplayName = user?.displayName ?: "",
                            editStatusMessage = user?.statusMessage ?: ""
                        )
                    }
                }
            }
        }
    }

    fun openPhotoOptions() {
        _uiState.update { it.copy(showPhotoOptions = true) }
    }

    fun dismissPhotoOptions() {
        _uiState.update { it.copy(showPhotoOptions = false) }
    }

    fun onPhotoSelected(context: Context, uri: Uri) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        _uiState.update { it.copy(isUploadingPhoto = true, showPhotoOptions = false, errorMessage = null) }

        viewModelScope.launch {
            val compressedBytes = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream) ?: return@use null
                        
                        // Resize down if larger than 600px
                        val maxDim = 600
                        val scale = min(1f, maxDim.toFloat() / original.width.coerceAtLeast(original.height))
                        val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
                        val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)

                        val scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
                        out.toByteArray()
                    }
                } catch (_: Exception) {
                    null
                }
            }

            if (compressedBytes == null) {
                _uiState.update {
                    it.copy(isUploadingPhoto = false, errorMessage = "Failed to process selected image.")
                }
                return@launch
            }

            val result = userRepository.uploadProfilePicture(currentUid, compressedBytes)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isUploadingPhoto = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            errorMessage = error.localizedMessage ?: "Failed to upload photo."
                        )
                    }
                }
            )
        }
    }

    fun removePhoto() {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        _uiState.update { it.copy(isUploadingPhoto = true, showPhotoOptions = false, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.removeProfilePicture(currentUid)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isUploadingPhoto = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            errorMessage = error.localizedMessage ?: "Failed to remove photo."
                        )
                    }
                }
            )
        }
    }

    fun startEditing() {
        val currentUser = _uiState.value.user
        _uiState.update {
            it.copy(
                isEditing = true,
                editDisplayName = currentUser?.displayName ?: "",
                editStatusMessage = currentUser?.statusMessage ?: ""
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false, errorMessage = null) }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(editDisplayName = name) }
    }

    fun onStatusMessageChanged(status: String) {
        _uiState.update { it.copy(editStatusMessage = status) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        if (state.editDisplayName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Display name cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val result = userRepository.updateProfile(
                userId = currentUid,
                displayName = state.editDisplayName,
                statusMessage = state.editStatusMessage
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isEditing = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.localizedMessage ?: "Failed to update profile."
                        )
                    }
                }
            )
        }
    }

    fun toggleAccountPrivacy(isPrivate: Boolean) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            userRepository.updateAccountPrivacy(currentUid, isPrivate)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}

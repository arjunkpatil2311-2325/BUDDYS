package com.aura.glasschat.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.FollowRepository
import com.aura.glasschat.data.repository.UserRepository
import com.aura.glasschat.util.UsernameUtils
import com.aura.glasschat.util.UsernameValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

data class EditProfileUiState(
    val user: User? = null,
    val displayName: String = "",
    val username: String = "",
    val originalUsername: String = "",
    val bio: String = "",
    val pronouns: String = "",
    val link: String = "",
    val gender: String = "",
    val isPrivate: Boolean = false,
    val avatarUrl: String? = null,
    val usernameStatus: UsernameCheckStatus = UsernameCheckStatus.IDLE,
    val usernameStatusMessage: String? = null,
    val isUploadingPhoto: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class EditProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val followRepository: FollowRepository = FollowRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    init {
        val currentUid = authRepository.currentUserId
        if (currentUid.isNotEmpty()) {
            viewModelScope.launch {
                val profile = userRepository.getUser(currentUid)
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            user = profile,
                            displayName = profile.displayName,
                            username = profile.username,
                            originalUsername = profile.username,
                            bio = profile.bio.ifBlank { profile.statusMessage },
                            pronouns = profile.pronouns,
                            link = profile.link,
                            gender = profile.gender,
                            isPrivate = profile.isPrivate,
                            avatarUrl = profile.avatarUrl
                        )
                    }
                }
            }
        }
    }

    fun onDisplayNameChanged(value: String) = _uiState.update { it.copy(displayName = value, errorMessage = null) }
    fun onBioChanged(value: String) = _uiState.update { it.copy(bio = value, errorMessage = null) }
    fun onPronounsChanged(value: String) = _uiState.update { it.copy(pronouns = value, errorMessage = null) }
    fun onLinkChanged(value: String) = _uiState.update { it.copy(link = value, errorMessage = null) }
    fun onGenderChanged(value: String) = _uiState.update { it.copy(gender = value, errorMessage = null) }
    fun onPrivacyToggled(isPrivate: Boolean) = _uiState.update { it.copy(isPrivate = isPrivate, errorMessage = null) }

    fun onUsernameChanged(input: String) {
        val normalized = UsernameUtils.normalize(input)
        val orig = _uiState.value.originalUsername

        if (normalized == orig) {
            _uiState.update {
                it.copy(
                    username = normalized,
                    usernameStatus = UsernameCheckStatus.AVAILABLE,
                    usernameStatusMessage = "Current username",
                    errorMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                username = normalized,
                usernameStatus = UsernameCheckStatus.CHECKING,
                usernameStatusMessage = "Checking...",
                errorMessage = null
            )
        }

        usernameCheckJob?.cancel()

        val validation = UsernameUtils.validate(normalized)
        if (validation is UsernameValidationResult.Invalid) {
            _uiState.update {
                it.copy(
                    usernameStatus = UsernameCheckStatus.INVALID,
                    usernameStatusMessage = validation.reason
                )
            }
            return
        }

        usernameCheckJob = viewModelScope.launch {
            delay(300)
            val available = authRepository.checkUsernameAvailable(normalized)
            if (available) {
                _uiState.update {
                    it.copy(
                        usernameStatus = UsernameCheckStatus.AVAILABLE,
                        usernameStatusMessage = "✓ @$normalized is available"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        usernameStatus = UsernameCheckStatus.TAKEN,
                        usernameStatusMessage = "✕ @$normalized is already taken"
                    )
                }
            }
        }
    }

    fun onPhotoSelected(context: Context, uri: Uri) {
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        _uiState.update { it.copy(isUploadingPhoto = true, errorMessage = null) }

        viewModelScope.launch {
            val compressedBytes = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream) ?: return@use null
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

            if (compressedBytes != null) {
                val result = userRepository.uploadProfilePicture(currentUid, compressedBytes)
                result.fold(
                    onSuccess = { url ->
                        _uiState.update { it.copy(isUploadingPhoto = false, avatarUrl = url) }
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(isUploadingPhoto = false, errorMessage = err.localizedMessage) }
                    }
                )
            } else {
                _uiState.update { it.copy(isUploadingPhoto = false) }
            }
        }
    }

    fun saveProfile() {
        val state = _uiState.value
        val currentUid = authRepository.currentUserId
        if (currentUid.isEmpty()) return

        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Display name cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // 1. Update basic and extended profile fields
                userRepository.updateProfile(
                    userId = currentUid,
                    displayName = state.displayName.trim(),
                    statusMessage = state.bio.trim(),
                    bio = state.bio.trim(),
                    pronouns = state.pronouns.trim(),
                    link = state.link.trim(),
                    gender = state.gender.trim(),
                    isPrivate = state.isPrivate
                )

                // 2. Change username if modified
                if (state.username != state.originalUsername && state.username.isNotBlank()) {
                    val changeResult = followRepository.changeUsername(
                        currentUid = currentUid,
                        oldUsername = state.originalUsername,
                        newUsername = state.username
                    )
                    if (changeResult.isFailure) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = changeResult.exceptionOrNull()?.localizedMessage ?: "Failed to update username."
                            )
                        }
                        return@launch
                    }
                }

                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.localizedMessage ?: "Failed to update profile."
                    )
                }
            }
        }
    }
}

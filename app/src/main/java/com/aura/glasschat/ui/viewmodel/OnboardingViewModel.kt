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
import com.aura.glasschat.util.UsernameUtils
import com.aura.glasschat.util.UsernameValidationResult
import com.google.firebase.auth.FirebaseUser
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

enum class OnboardingStep {
    WELCOME,
    EMAIL_AUTH,
    NAME,
    USERNAME,
    PHOTO,
    BIO,
    RULES,
    COMPLETE
}

enum class UsernameCheckStatus {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN,
    INVALID
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val isLoginMode: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val username: String = "",
    val usernameStatus: UsernameCheckStatus = UsernameCheckStatus.IDLE,
    val usernameStatusMessage: String? = null,
    val avatarUri: Uri? = null,
    val avatarUrl: String? = null,
    val isUploadingPhoto: Boolean = false,
    val bio: String = "",
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val authenticatedUser: FirebaseUser? = null
)

class OnboardingViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    init {
        // If user already logged in via Firebase Auth, check profile status
        val currentUser = authRepository.currentUser
        if (currentUser != null) {
            _uiState.update { it.copy(authenticatedUser = currentUser) }
            viewModelScope.launch {
                val profile = userRepository.getUser(currentUser.uid)
                if (profile != null && profile.onboardingCompleted && profile.username.isNotBlank()) {
                    _uiState.update { it.copy(isSuccess = true) }
                } else if (profile != null && profile.username.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            displayName = profile.displayName,
                            username = profile.username,
                            avatarUrl = profile.avatarUrl,
                            bio = profile.bio.ifBlank { profile.statusMessage },
                            step = OnboardingStep.RULES
                        )
                    }
                }
            }
        }
    }

    fun goToStep(step: OnboardingStep) {
        _uiState.update { it.copy(step = step, errorMessage = null) }
    }

    fun toggleLoginMode() {
        _uiState.update {
            it.copy(
                isLoginMode = !it.isLoginMode,
                errorMessage = null
            )
        }
    }

    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun onConfirmPasswordChanged(value: String) = _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    fun onDisplayNameChanged(value: String) = _uiState.update { it.copy(displayName = value, errorMessage = null) }
    fun onBioChanged(value: String) = _uiState.update { it.copy(bio = value, errorMessage = null) }

    fun onUsernameChanged(input: String) {
        val normalized = UsernameUtils.normalize(input)
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
            delay(300) // debounce
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

    fun submitEmailAuth() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password.") }
            return
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }

        if (!state.isLoginMode && state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            if (state.isLoginMode) {
                // Log in existing user
                val result = authRepository.logIn(state.email, state.password)
                result.fold(
                    onSuccess = { fbUser ->
                        val profile = userRepository.getUser(fbUser.uid)
                        if (profile != null && profile.onboardingCompleted && profile.username.isNotBlank()) {
                            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        } else {
                            // Incomplete profile -> resume onboarding
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    authenticatedUser = fbUser,
                                    displayName = profile?.displayName ?: "",
                                    username = profile?.username ?: "",
                                    step = if (profile?.username.isNullOrBlank()) OnboardingStep.NAME else OnboardingStep.PHOTO
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.localizedMessage ?: "Login failed. Please check your credentials."
                            )
                        }
                    }
                )
            } else {
                // New user signup
                val result = authRepository.signUpWithEmail(state.email, state.password)
                result.fold(
                    onSuccess = { fbUser ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                authenticatedUser = fbUser,
                                step = OnboardingStep.NAME
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.localizedMessage ?: "Sign up failed. Please try again."
                            )
                        }
                    }
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Invalid Google token.") }
            return
        }

        _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { (fbUser, isComplete) ->
                    if (isComplete) {
                        _uiState.update { it.copy(isGoogleLoading = false, isSuccess = true) }
                    } else {
                        // Needs onboarding
                        val suggestedName = fbUser.displayName ?: ""
                        val emailCandidate = fbUser.email?.substringBefore("@") ?: ""
                        _uiState.update {
                            it.copy(
                                isGoogleLoading = false,
                                authenticatedUser = fbUser,
                                displayName = suggestedName,
                                avatarUrl = fbUser.photoUrl?.toString(),
                                step = OnboardingStep.NAME
                            )
                        }
                        if (emailCandidate.isNotBlank()) {
                            onUsernameChanged(emailCandidate)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            errorMessage = error.localizedMessage ?: "Google Sign-In failed."
                        )
                    }
                }
            )
        }
    }

    fun onPhotoSelected(context: Context, uri: Uri) {
        val uid = _uiState.value.authenticatedUser?.uid ?: return
        _uiState.update { it.copy(avatarUri = uri, isUploadingPhoto = true, errorMessage = null) }

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
                val uploadResult = userRepository.uploadProfilePicture(uid, compressedBytes)
                uploadResult.fold(
                    onSuccess = { downloadUrl ->
                        _uiState.update { it.copy(isUploadingPhoto = false, avatarUrl = downloadUrl) }
                    },
                    onFailure = {
                        _uiState.update { it.copy(isUploadingPhoto = false) }
                    }
                )
            } else {
                _uiState.update { it.copy(isUploadingPhoto = false) }
            }
        }
    }

    fun finishOnboarding() {
        val state = _uiState.value
        val fbUser = state.authenticatedUser ?: return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.completeOnboarding(
                uid = fbUser.uid,
                email = fbUser.email ?: state.email,
                displayName = state.displayName.ifBlank { state.username },
                username = state.username,
                avatarUrl = state.avatarUrl,
                bio = state.bio
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, step = OnboardingStep.COMPLETE) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to save profile. Please try again."
                        )
                    }
                }
            )
        }
    }
}

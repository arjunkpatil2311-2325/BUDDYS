package com.aura.glasschat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.glasschat.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoginMode: Boolean = true,
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = !_uiState.value.isLoginMode,
            errorMessage = null
        )
    }

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun onUsernameChanged(value: String) {
        _uiState.value = _uiState.value.copy(username = value, errorMessage = null)
    }

    fun onDisplayNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value, errorMessage = null, infoMessage = null)
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address to reset password.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = "Password reset email sent to $email. Please check your inbox."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to send reset email. Please verify your email address."
                    )
                }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Invalid Google ID token.")
            return
        }

        _uiState.value = _uiState.value.copy(isGoogleLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isGoogleLoading = false, isSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isGoogleLoading = false,
                        errorMessage = error.localizedMessage ?: "Google Sign-In failed. Please try again."
                    )
                }
            )
        }
    }

    fun onGoogleSignInError(message: String) {
        _uiState.value = _uiState.value.copy(isGoogleLoading = false, errorMessage = message)
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter both email and password.")
            return
        }

        if (state.password.length < 6) {
            _uiState.value = state.copy(errorMessage = "Password must be at least 6 characters.")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            if (state.isLoginMode) {
                val result = authRepository.logIn(state.email, state.password)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Login failed. Please try again."
                        )
                    }
                )
            } else {
                if (state.username.isBlank()) {
                    _uiState.value = state.copy(
                        isLoading = false,
                        errorMessage = "Please enter a unique username."
                    )
                    return@launch
                }

                val signUpResult = authRepository.signUpWithEmail(state.email, state.password)
                signUpResult.fold(
                    onSuccess = { fbUser ->
                        val onboardingResult = authRepository.completeOnboarding(
                            uid = fbUser.uid,
                            email = state.email,
                            displayName = state.displayName.ifBlank { state.username },
                            username = state.username
                        )
                        onboardingResult.fold(
                            onSuccess = {
                                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                            },
                            onFailure = { err ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = err.localizedMessage ?: "Sign up failed."
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Sign up failed. Please try again."
                        )
                    }
                )
            }
        }
    }
}

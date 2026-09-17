package com.aura.glasschat.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onAuthSuccess()
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (!idToken.isNullOrBlank()) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    viewModel.onGoogleSignInError("Google did not return an ID token.")
                }
            } catch (e: ApiException) {
                val errorMsg = when (e.statusCode) {
                    12500 -> "Google Sign-In configuration error."
                    12501 -> "Google Sign-In cancelled."
                    else -> "Google Sign-In failed (${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
                }
                viewModel.onGoogleSignInError(errorMsg)
            }
        }
    }

    fun launchGoogleSignIn() {
        val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (webClientIdResId != 0) context.getString(webClientIdResId) else ""

        if (webClientId.isBlank()) {
            viewModel.onGoogleSignInError("Google Web Client ID not configured.")
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Premium Brand Emblem
            BuddysSpiderEmblem(
                size = 56.dp,
                tint = BuddysTheme.colors.primaryRed
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Title & Tagline
            Text(
                text = "Buddies",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 28.sp,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Connect. Share. Message. Protect.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BuddysTheme.colors.textSecondary,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Auth Card
            BuddysCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Selector (Login / Sign Up)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BuddysTheme.colors.surfaceSecondary)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isLoginMode) BuddysTheme.colors.surface else Color.Transparent)
                                .clickable { if (!uiState.isLoginMode) viewModel.toggleMode() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Login",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (uiState.isLoginMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (uiState.isLoginMode) BuddysTheme.colors.textPrimary else BuddysTheme.colors.textSecondary
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!uiState.isLoginMode) BuddysTheme.colors.surface else Color.Transparent)
                                .clickable { if (uiState.isLoginMode) viewModel.toggleMode() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Up",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (!uiState.isLoginMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!uiState.isLoginMode) BuddysTheme.colors.textPrimary else BuddysTheme.colors.textSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sign-up Display Name & Username Fields
                    AnimatedVisibility(
                        visible = !uiState.isLoginMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.displayName,
                                onValueChange = { viewModel.onDisplayNameChanged(it) },
                                placeholder = { Text("Your Display Name", color = BuddysTheme.colors.textMuted) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BuddysTheme.colors.primaryRed,
                                    unfocusedBorderColor = BuddysTheme.colors.border,
                                    focusedTextColor = BuddysTheme.colors.textPrimary,
                                    unfocusedTextColor = BuddysTheme.colors.textPrimary,
                                    focusedContainerColor = BuddysTheme.colors.surfaceSecondary,
                                    unfocusedContainerColor = BuddysTheme.colors.surfaceSecondary
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.username,
                                onValueChange = { viewModel.onUsernameChanged(it) },
                                placeholder = { Text("Choose a unique username", color = BuddysTheme.colors.textMuted) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BuddysTheme.colors.primaryRed,
                                    unfocusedBorderColor = BuddysTheme.colors.border,
                                    focusedTextColor = BuddysTheme.colors.textPrimary,
                                    unfocusedTextColor = BuddysTheme.colors.textPrimary,
                                    focusedContainerColor = BuddysTheme.colors.surfaceSecondary,
                                    unfocusedContainerColor = BuddysTheme.colors.surfaceSecondary
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Email Field
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        placeholder = { Text("Email address", color = BuddysTheme.colors.textMuted) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuddysTheme.colors.primaryRed,
                            unfocusedBorderColor = BuddysTheme.colors.border,
                            focusedTextColor = BuddysTheme.colors.textPrimary,
                            unfocusedTextColor = BuddysTheme.colors.textPrimary,
                            focusedContainerColor = BuddysTheme.colors.surfaceSecondary,
                            unfocusedContainerColor = BuddysTheme.colors.surfaceSecondary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        placeholder = { Text("Password (min 6 characters)", color = BuddysTheme.colors.textMuted) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuddysTheme.colors.primaryRed,
                            unfocusedBorderColor = BuddysTheme.colors.border,
                            focusedTextColor = BuddysTheme.colors.textPrimary,
                            unfocusedTextColor = BuddysTheme.colors.textPrimary,
                            focusedContainerColor = BuddysTheme.colors.surfaceSecondary,
                            unfocusedContainerColor = BuddysTheme.colors.surfaceSecondary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Forgot Password in Login Mode
                    if (uiState.isLoginMode) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.sendPasswordReset() }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Forgot password?",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BuddysTheme.colors.primaryRed,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Info Banner
                    if (uiState.infoMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BuddysTheme.colors.primaryRed.copy(alpha = 0.12f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = uiState.infoMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.primaryRed),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Error Banner
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BuddysTheme.colors.error.copy(alpha = 0.12f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.error),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    BuddysButton(
                        text = if (uiState.isLoginMode) "Sign In" else "Create Account",
                        onClick = { viewModel.submit() },
                        isLoading = uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BuddysTheme.colors.border)
                        Text(
                            text = "or",
                            style = MaterialTheme.typography.labelSmall.copy(color = BuddysTheme.colors.textMuted),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BuddysTheme.colors.border)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sign In Button
                    BuddysOutlinedButton(
                        text = if (uiState.isGoogleLoading) "Signing in..." else "Continue with Google",
                        onClick = { launchGoogleSignIn() },
                        enabled = !uiState.isLoading && !uiState.isGoogleLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

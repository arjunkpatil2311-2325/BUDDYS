package com.aura.glasschat.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
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
import com.aura.glasschat.ui.viewmodel.OnboardingStep
import com.aura.glasschat.ui.viewmodel.OnboardingViewModel
import com.aura.glasschat.ui.viewmodel.UsernameCheckStatus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            val authRepo = com.aura.glasschat.data.repository.AuthRepository()
            val uid = authRepo.currentUserId
            if (uid.isNotBlank() && uiState.password.isNotBlank()) {
                val email = uiState.email.trim().ifBlank { authRepo.currentUser?.email ?: "" }
                com.aura.glasschat.data.repository.AccountManagerRepository.getInstance(context)
                    .saveSessionSecret(uid, email, uiState.password)
            }
            onComplete()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhotoSelected(context, uri)
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
                }
            } catch (_: ApiException) {}
        }
    }

    fun launchGoogleSignIn() {
        val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (webClientIdResId != 0) context.getString(webClientIdResId) else ""
        if (webClientId.isNotBlank()) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(context, gso)
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation / Back button for steps > WELCOME
                if (uiState.step != OnboardingStep.WELCOME && uiState.step != OnboardingStep.COMPLETE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = {
                                val prevStep = when (uiState.step) {
                                    OnboardingStep.EMAIL_AUTH -> OnboardingStep.WELCOME
                                    OnboardingStep.NAME -> OnboardingStep.WELCOME
                                    OnboardingStep.USERNAME -> OnboardingStep.NAME
                                    OnboardingStep.PHOTO -> OnboardingStep.USERNAME
                                    OnboardingStep.BIO -> OnboardingStep.PHOTO
                                    OnboardingStep.RULES -> OnboardingStep.BIO
                                    OnboardingStep.APP_LOCK_SETUP -> OnboardingStep.RULES
                                    else -> OnboardingStep.WELCOME
                                }
                                viewModel.goToStep(prevStep)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BuddysTheme.colors.surface)
                                .border(1.dp, BuddysTheme.colors.border, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = BuddysTheme.colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                AnimatedContent(
                    targetState = uiState.step,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "onboarding_steps"
                ) { step ->
                    when (step) {
                        OnboardingStep.WELCOME -> {
                            WelcomeStepContent(
                                onGoogleClick = { launchGoogleSignIn() },
                                onEmailClick = { viewModel.goToStep(OnboardingStep.EMAIL_AUTH) },
                                isGoogleLoading = uiState.isGoogleLoading
                            )
                        }
                        OnboardingStep.EMAIL_AUTH -> {
                            EmailAuthStepContent(
                                uiState = uiState,
                                onEmailChange = { viewModel.onEmailChanged(it) },
                                onPasswordChange = { viewModel.onPasswordChanged(it) },
                                onConfirmPasswordChange = { viewModel.onConfirmPasswordChanged(it) },
                                onToggleMode = { viewModel.toggleLoginMode() },
                                onSubmit = { viewModel.submitEmailAuth() }
                            )
                        }
                        OnboardingStep.NAME -> {
                            NameStepContent(
                                displayName = uiState.displayName,
                                onDisplayNameChange = { viewModel.onDisplayNameChanged(it) },
                                onContinue = { viewModel.goToStep(OnboardingStep.USERNAME) }
                            )
                        }
                        OnboardingStep.USERNAME -> {
                            UsernameStepContent(
                                username = uiState.username,
                                status = uiState.usernameStatus,
                                statusMessage = uiState.usernameStatusMessage,
                                onUsernameChange = { viewModel.onUsernameChanged(it) },
                                onContinue = { viewModel.goToStep(OnboardingStep.PHOTO) }
                            )
                        }
                        OnboardingStep.PHOTO -> {
                            PhotoStepContent(
                                avatarUrl = uiState.avatarUrl,
                                isUploading = uiState.isUploadingPhoto,
                                onPickPhoto = { photoPickerLauncher.launch("image/*") },
                                onSkip = { viewModel.goToStep(OnboardingStep.BIO) },
                                onContinue = { viewModel.goToStep(OnboardingStep.BIO) }
                            )
                        }
                        OnboardingStep.BIO -> {
                            BioStepContent(
                                bio = uiState.bio,
                                onBioChange = { viewModel.onBioChanged(it) },
                                onSkip = { viewModel.goToStep(OnboardingStep.RULES) },
                                onContinue = { viewModel.goToStep(OnboardingStep.RULES) }
                            )
                        }
                        OnboardingStep.RULES -> {
                            RulesStepContent(
                                isLoading = false,
                                errorMessage = uiState.errorMessage,
                                onAgree = { viewModel.goToStep(OnboardingStep.APP_LOCK_SETUP) }
                            )
                        }
                        OnboardingStep.APP_LOCK_SETUP -> {
                            AppLockOnboardingStepContent(
                                isLoading = uiState.isLoading,
                                onPinSet = { pin, enableBio ->
                                    val pinMgr = com.aura.glasschat.security.AppLockManager.getInstance(context).pinManager
                                    pinMgr.setPin(pin)
                                    pinMgr.setBiometricEnabled(enableBio)
                                    viewModel.finishOnboarding()
                                },
                                onSkip = {
                                    viewModel.finishOnboarding()
                                }
                            )
                        }
                        OnboardingStep.COMPLETE -> {
                            CompleteStepContent(
                                username = uiState.username,
                                onFinish = {
                                    val authRepo = com.aura.glasschat.data.repository.AuthRepository()
                                    val uid = authRepo.currentUserId
                                    if (uid.isNotBlank() && uiState.password.isNotBlank()) {
                                        val email = uiState.email.trim().ifBlank { authRepo.currentUser?.email ?: "" }
                                        com.aura.glasschat.data.repository.AccountManagerRepository.getInstance(context)
                                            .saveSessionSecret(uid, email, uiState.password)
                                    }
                                    onComplete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeStepContent(
    onGoogleClick: () -> Unit,
    onEmailClick: () -> Unit,
    isGoogleLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BuddysSpiderEmblem(
            size = 64.dp,
            tint = BuddysTheme.colors.primaryRed
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Buddies",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 32.sp,
                letterSpacing = 2.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Private social messaging & authentic moments",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(44.dp))

        // Google Button
        OutlinedButton(
            onClick = onGoogleClick,
            enabled = !isGoogleLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BuddysTheme.colors.textPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BuddysTheme.colors.primaryRed)
            } else {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Email Button
        BuddysButton(
            text = "Continue with Email",
            onClick = onEmailClick,
            leadingIcon = Icons.Default.Email,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EmailAuthStepContent(
    uiState: com.aura.glasschat.ui.viewmodel.OnboardingUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (uiState.isLoginMode) "Welcome Back" else "Create Account",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 24.sp
            )
        )
        Text(
            text = if (uiState.isLoginMode) "Log in to access your Buddies space" else "Enter your credentials to get started",
            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(24.dp))

        BuddysCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    label = { Text("Email address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password (min 6 characters)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Password",
                                tint = BuddysTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!uiState.isLoginMode) {
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("Confirm password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BuddysTheme.colors.error,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                BuddysButton(
                    text = if (uiState.isLoginMode) "Log In" else "Continue",
                    isLoading = uiState.isLoading,
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.clickable(onClick = onToggleMode),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.isLoginMode) "Don't have an account? " else "Already have an account? ",
                style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
            )
            Text(
                text = if (uiState.isLoginMode) "Sign Up" else "Log In",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BuddysTheme.colors.primaryRed,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun NameStepContent(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BuddysSpiderEmblem(size = 48.dp, tint = BuddysTheme.colors.primaryRed)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "What's your name?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 22.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter the name your buddies know you by",
            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("Display Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        BuddysButton(
            text = "Next",
            enabled = displayName.isNotBlank(),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun UsernameStepContent(
    username: String,
    status: UsernameCheckStatus,
    statusMessage: String?,
    onUsernameChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BuddysSpiderEmblem(size = 48.dp, tint = BuddysTheme.colors.primaryRed)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Choose your username",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 22.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pick your unique @username on Buddies",
            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (statusMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    UsernameCheckStatus.AVAILABLE -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BuddysTheme.colors.success, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(statusMessage, color = BuddysTheme.colors.success, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    UsernameCheckStatus.TAKEN, UsernameCheckStatus.INVALID -> {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = BuddysTheme.colors.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(statusMessage, color = BuddysTheme.colors.error, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    UsernameCheckStatus.CHECKING -> {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BuddysTheme.colors.primaryRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(statusMessage, color = BuddysTheme.colors.textSecondary, fontSize = 13.sp)
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BuddysButton(
            text = "Next",
            enabled = status == UsernameCheckStatus.AVAILABLE,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PhotoStepContent(
    avatarUrl: String?,
    isUploading: Boolean,
    onPickPhoto: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BuddysSpiderEmblem(size = 48.dp, tint = BuddysTheme.colors.primaryRed)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Add a profile photo",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 22.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Help your buddies recognize your profile",
            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(28.dp))

        AvatarView(
            imageUrl = avatarUrl,
            displayName = "Me",
            size = 96.dp,
            isOnline = false,
            isEditable = true,
            onEditClick = onPickPhoto
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isUploading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = BuddysTheme.colors.primaryRed)
            Spacer(modifier = Modifier.height(16.dp))
        }

        BuddysOutlinedButton(
            text = if (avatarUrl != null) "Change Photo" else "Choose from Gallery",
            onClick = onPickPhoto,
            leadingIcon = Icons.Default.PhotoCamera,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (avatarUrl != null) {
            BuddysButton(
                text = "Next",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now", color = BuddysTheme.colors.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun BioStepContent(
    bio: String,
    onBioChange: (String) -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BuddysSpiderEmblem(size = 48.dp, tint = BuddysTheme.colors.primaryRed)

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Add your bio",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 22.sp
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "A short introduction about yourself",
            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            label = { Text("Bio / Status") },
            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = BuddysTheme.colors.textSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        BuddysButton(
            text = if (bio.isNotBlank()) "Next" else "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip", color = BuddysTheme.colors.textSecondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun RulesStepContent(
    isLoading: Boolean,
    errorMessage: String?,
    onAgree: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "COMMUNITY GUIDELINES",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 19.sp,
                letterSpacing = 1.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "To ensure Buddies is a safe and respectful social space:",
            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        BuddysCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rules = listOf(
                    "1. Be respectful to fellow members.",
                    "2. Don't harass, bully, or threaten people.",
                    "3. Don't impersonate others.",
                    "4. Don't spam or post unsolicited links.",
                    "5. Don't share someone's private information.",
                    "6. Don't use Buddies for unauthorized promotions.",
                    "7. Respect blocks and account privacy.",
                    "8. Report harmful behavior responsibly."
                )

                rules.forEach { rule ->
                    Text(
                        text = rule,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BuddysTheme.colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMessage, color = BuddysTheme.colors.error, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        BuddysButton(
            text = "I Agree & Finish Setup",
            isLoading = isLoading,
            onClick = onAgree,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CompleteStepContent(
    username: String,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BuddysSpiderEmblem(size = 72.dp, tint = BuddysTheme.colors.primaryRed)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to Buddies",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 24.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "@$username is ready to connect",
            style = MaterialTheme.typography.bodyMedium.copy(color = BuddysTheme.colors.textSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        BuddysButton(
            text = "Get Started",
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AppLockOnboardingStepContent(
    isLoading: Boolean,
    onPinSet: (pin: String, enableBio: Boolean) -> Unit,
    onSkip: () -> Unit
) {
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var enableBiometrics by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val activePin = if (isConfirming) confirmPin else firstPin

    fun handleDigit(digit: String) {
        if (activePin.length >= 4) return
        val updated = activePin + digit
        errorMessage = null

        if (!isConfirming) {
            firstPin = updated
            if (updated.length == 4) {
                isConfirming = true
            }
        } else {
            confirmPin = updated
            if (updated.length == 4) {
                if (confirmPin == firstPin) {
                    onPinSet(confirmPin, enableBiometrics)
                } else {
                    errorMessage = "PINs do not match. Try again."
                    confirmPin = ""
                }
            }
        }
    }

    fun handleBackspace() {
        if (isConfirming) {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                isConfirming = false
                firstPin = firstPin.dropLast(1)
            }
        } else {
            if (firstPin.isNotEmpty()) {
                firstPin = firstPin.dropLast(1)
            }
        }
        errorMessage = null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BuddiesLogo(size = 52.dp, tint = BuddysTheme.colors.primaryAccent)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isConfirming) "Confirm Privacy PIN" else "Set Privacy PIN",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 23.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isConfirming) "Re-enter your 4-digit PIN to confirm" else "Protect your Buddies chats and moments with a 4-digit lock code",
            style = MaterialTheme.typography.bodySmall.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 13.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4 PIN Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val isFilled = i < activePin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) BuddysTheme.colors.primaryAccent
                            else BuddysTheme.colors.surfaceSecondary
                        )
                        .border(
                            1.5.dp,
                            if (isFilled) BuddysTheme.colors.primaryAccent
                            else BuddysTheme.colors.border,
                            CircleShape
                        )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BuddysTheme.colors.error,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Keypad
        val keypadRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "BACKSPACE")
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (row in keypadRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (key in row) {
                        when (key) {
                            "" -> Spacer(modifier = Modifier.size(60.dp))
                            "BACKSPACE" -> {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .clickable { handleBackspace() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = BuddysTheme.colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(BuddysTheme.colors.surface)
                                        .border(1.dp, BuddysTheme.colors.border, CircleShape)
                                        .clickable { handleDigit(key) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = BuddysTheme.colors.textPrimary,
                                            fontSize = 22.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Biometric toggle
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { enableBiometrics = !enableBiometrics }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = enableBiometrics,
                onCheckedChange = { enableBiometrics = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = BuddysTheme.colors.primaryAccent,
                    uncheckedColor = BuddysTheme.colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Enable Fingerprint / Biometrics",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BuddysTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Skip for now",
                color = BuddysTheme.colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )
        }
    }
}

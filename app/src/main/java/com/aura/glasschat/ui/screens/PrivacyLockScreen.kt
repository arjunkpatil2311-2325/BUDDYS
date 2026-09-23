package com.aura.glasschat.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.security.AppLockManager
import com.aura.glasschat.security.BiometricAuthManager
import com.aura.glasschat.security.PinVerificationResult
import com.aura.glasschat.ui.components.BuddiesLogo
import com.aura.glasschat.ui.components.BuddysButton
import com.aura.glasschat.ui.theme.BuddysTheme
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PrivacyLockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appLockManager = remember { AppLockManager.getInstance(context) }
    val pinManager = appLockManager.pinManager

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isThrottled by remember { mutableStateOf(pinManager.isThrottled()) }
    var cooldownSeconds by remember { mutableStateOf(pinManager.getCooldownRemainingSeconds()) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    // Shake animation on error
    val shakeOffset = remember { Animatable(0f) }

    fun triggerBiometric() {
        val activity = context as? Activity ?: return
        BiometricAuthManager.promptBiometricAuthentication(
            activity = activity,
            onSuccess = {
                errorMessage = null
                appLockManager.unlockApp()
                onUnlocked()
            },
            onError = { err: String ->
                errorMessage = err
            }
        )
    }

    // Auto-prompt biometric on launch if enabled and not throttled
    LaunchedEffect(Unit) {
        if (pinManager.isBiometricEnabled() && !isThrottled) {
            triggerBiometric()
        }
    }

    // Countdown timer for throttling
    LaunchedEffect(isThrottled) {
        if (isThrottled) {
            while (cooldownSeconds > 0) {
                delay(1000L)
                cooldownSeconds = pinManager.getCooldownRemainingSeconds()
            }
            isThrottled = false
            errorMessage = null
        }
    }

    // Auto-verify when 4 digits are entered
    fun handleDigitPress(digit: String) {
        if (isThrottled || enteredPin.length >= 6) return
        val newPin = enteredPin + digit
        enteredPin = newPin

        if (newPin.length >= 4) {
            val result = pinManager.verifyPin(newPin)
            when (result) {
                is PinVerificationResult.Success -> {
                    errorMessage = null
                    appLockManager.unlockApp()
                    onUnlocked()
                }
                is PinVerificationResult.Incorrect -> {
                    if (newPin.length == 4) {
                        scope.launch {
                            shakeOffset.animateTo(12f, animationSpec = tween(50))
                            shakeOffset.animateTo(-12f, animationSpec = tween(50))
                            shakeOffset.animateTo(8f, animationSpec = tween(50))
                            shakeOffset.animateTo(-8f, animationSpec = tween(50))
                            shakeOffset.animateTo(0f, animationSpec = tween(50))
                        }
                        errorMessage = "Incorrect PIN. ${result.attemptsRemaining} attempts left."
                        enteredPin = ""
                    }
                }
                is PinVerificationResult.Throttled -> {
                    isThrottled = true
                    cooldownSeconds = result.remainingSeconds
                    errorMessage = "Too many attempts. Try again in ${result.remainingSeconds}s."
                    enteredPin = ""
                }
                is PinVerificationResult.NoPinSet -> {
                    appLockManager.unlockApp()
                    onUnlocked()
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BuddysTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Brand Header & Lock Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BuddiesLogo(
                    size = 56.dp,
                    tint = BuddysTheme.colors.primaryAccent
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Buddies",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter Privacy PIN",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your private conversations and space are locked",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textSecondary,
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // PIN Dots Display (4 dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) BuddysTheme.colors.primaryRed
                                    else BuddysTheme.colors.surfaceSecondary
                                )
                                .border(
                                    1.5.dp,
                                    if (isFilled) BuddysTheme.colors.primaryRed
                                    else BuddysTheme.colors.border,
                                    CircleShape
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BuddysTheme.colors.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIOMETRIC", "0", "BACKSPACE")
                )

                for (row in keypadRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (key in row) {
                            when (key) {
                                "BIOMETRIC" -> {
                                    val isBioEnabled = pinManager.isBiometricEnabled()
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = isBioEnabled && !isThrottled) {
                                                triggerBiometric()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isBioEnabled) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric Unlock",
                                                tint = if (isThrottled) BuddysTheme.colors.textMuted else BuddysTheme.colors.primaryRed,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                }
                                "BACKSPACE" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable { handleBackspace() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = BuddysTheme.colors.textPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                else -> {
                                    KeypadButton(
                                        digit = key,
                                        onClick = { handleDigitPress(key) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Forgot PIN Link
                TextButton(
                    onClick = { showForgotPinDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Forgot PIN?",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = BuddysTheme.colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                    )
                }
            }
        }
    }

    // Forgot PIN Secure Account Recovery Dialog
    if (showForgotPinDialog) {
        ForgotPinRecoveryDialog(
            onDismiss = { showForgotPinDialog = false },
            onRecoverySuccess = { newPin ->
                pinManager.resetPin(newPin)
                appLockManager.unlockApp()
                showForgotPinDialog = false
                Toast.makeText(context, "PIN successfully reset", Toast.LENGTH_SHORT).show()
                onUnlocked()
            }
        )
    }
}

@Composable
private fun KeypadButton(
    digit: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(BuddysTheme.colors.surface)
            .border(1.5.dp, BuddysTheme.colors.border, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = BuddysTheme.colors.primaryRed.copy(alpha = 0.2f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 24.sp
            )
        )
    }
}

@Composable
private fun ForgotPinRecoveryDialog(
    onDismiss: () -> Unit,
    onRecoverySuccess: (newPin: String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var passwordInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1: Verify Password, 2: Set New PIN
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BuddysTheme.colors.surface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = if (step == 1) "Verify Account Identity" else "Create New Privacy PIN",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (step == 1) {
                    Text(
                        text = "To reset your Privacy PIN, please verify your Buddies account (${currentUser?.email ?: "Account"}).",
                        style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; errorText = null },
                        label = { Text("Account Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text(
                        text = "Enter a new 4-digit Privacy PIN for this device.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text("New PIN (4 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPinInput = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(color = BuddysTheme.colors.error)
                    )
                }
            }
        },
        confirmButton = {
            BuddysButton(
                text = if (step == 1) "Verify" else "Reset PIN",
                isLoading = isLoading,
                onClick = {
                    if (step == 1) {
                        if (currentUser?.email != null && passwordInput.isNotEmpty()) {
                            isLoading = true
                            val credential = EmailAuthProvider.getCredential(currentUser.email!!, passwordInput)
                            currentUser.reauthenticate(credential)
                                .addOnSuccessListener {
                                    isLoading = false
                                    step = 2
                                    errorText = null
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    errorText = "Invalid password. Re-authentication failed."
                                }
                        } else {
                            errorText = "Please enter your password."
                        }
                    } else {
                        if (newPinInput.length == 4 && newPinInput == confirmPinInput) {
                            onRecoverySuccess(newPinInput)
                        } else {
                            errorText = "PINs must match and be 4 digits."
                        }
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BuddysTheme.colors.textSecondary)
            }
        }
    )
}

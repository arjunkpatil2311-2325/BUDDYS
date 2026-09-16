package com.aura.glasschat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.security.AppLockManager
import com.aura.glasschat.ui.components.BuddysSpiderEmblem
import com.aura.glasschat.ui.components.BuddysTopBar
import com.aura.glasschat.ui.theme.BuddysTheme

@Composable
fun PrivacyPinSetupScreen(
    onComplete: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLockManager = remember { AppLockManager.getInstance(context) }
    val pinManager = appLockManager.pinManager

    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
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
                    val success = pinManager.setPin(confirmPin)
                    if (success) {
                        Toast.makeText(context, "Privacy PIN set successfully", Toast.LENGTH_SHORT).show()
                        onComplete()
                    } else {
                        errorMessage = "Failed to save PIN. Try again."
                        firstPin = ""
                        confirmPin = ""
                        isConfirming = false
                    }
                } else {
                    errorMessage = "PINs do not match. Please try again."
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BuddysTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BuddysTopBar(
                title = "Protect your Buddies",
                onBack = onBack
            )

            // Setup Header & PIN Dots
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                BuddysSpiderEmblem(size = 44.dp, tint = BuddysTheme.colors.primaryRed)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isConfirming) "Confirm Privacy PIN" else "Create Privacy PIN",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 21.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isConfirming) "Re-enter your 4-digit PIN to confirm" else "Choose a 4-digit PIN to protect your chats and moments",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textSecondary,
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                // PIN Dots
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
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "BACKSPACE")
                )

                for (row in keypadRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (key in row) {
                            when (key) {
                                "" -> {
                                    Spacer(modifier = Modifier.size(68.dp))
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
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(BuddysTheme.colors.surface)
                                            .border(1.dp, BuddysTheme.colors.border, CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true, color = BuddysTheme.colors.primaryRed.copy(alpha = 0.2f)),
                                                onClick = { handleDigit(key) }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = BuddysTheme.colors.textPrimary,
                                                fontSize = 24.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.aura.glasschat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.PairingViewModel
import com.aura.glasschat.ui.viewmodel.TapToBuddyViewModel

@Composable
fun AddFriendScreen(
    onBack: () -> Unit,
    onPairingSuccess: (chatId: String, otherUserId: String) -> Unit,
    pairingViewModel: PairingViewModel = viewModel(),
    tapToBuddyViewModel: TapToBuddyViewModel = viewModel()
) {
    // 0 = Tap to Buddy (PRIMARY), 1 = Pairing Code (FALLBACK)
    var activeMode by remember { mutableIntStateOf(0) }

    if (activeMode == 0) {
        TapToBuddyScreen(
            onBack = onBack,
            onNavigateToPairingCode = { activeMode = 1 },
            onNavigateToChat = onPairingSuccess,
            viewModel = tapToBuddyViewModel
        )
    } else {
        PairingCodeScreen(
            onBack = { activeMode = 0 },
            onPairingSuccess = onPairingSuccess,
            viewModel = pairingViewModel
        )
    }
}

@Composable
fun PairingCodeScreen(
    onBack: () -> Unit,
    onPairingSuccess: (chatId: String, otherUserId: String) -> Unit,
    viewModel: PairingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (uiState.activeCode == null) {
            viewModel.generateNewCode()
        }
    }

    LaunchedEffect(uiState.pairingSuccessChatId) {
        val chatId = uiState.pairingSuccessChatId
        val friend = uiState.pairedFriend
        if (chatId != null && friend != null) {
            Toast.makeText(context, "Connected with ${friend.displayName.ifBlank { friend.username }}!", Toast.LENGTH_SHORT).show()
            onPairingSuccess(chatId, friend.uid)
            viewModel.resetSuccess()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ThreeDTopBar(
                title = "Pairing Code",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tab Switcher (My Code / Enter Code)
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
                            .background(if (uiState.selectedTab == 0) BuddysTheme.colors.surface else Color.Transparent)
                            .clickable { viewModel.selectTab(0) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "My Code",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.selectedTab == 0) BuddysTheme.colors.textPrimary else BuddysTheme.colors.textSecondary
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (uiState.selectedTab == 1) BuddysTheme.colors.surface else Color.Transparent)
                            .clickable { viewModel.selectTab(1) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enter Code",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.selectedTab == 1) BuddysTheme.colors.textPrimary else BuddysTheme.colors.textSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.selectedTab == 0) {
                    // TAB 0: GENERATE CODE
                    GenerateCodeSection(
                        code = uiState.activeCode?.code ?: "GENERATING...",
                        remainingSeconds = uiState.remainingSeconds,
                        isGenerating = uiState.isGenerating,
                        onCopyClick = {
                            val codeToCopy = uiState.activeCode?.code ?: return@GenerateCodeSection
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Pairing Code", codeToCopy))
                            Toast.makeText(context, "Pairing code copied!", Toast.LENGTH_SHORT).show()
                        },
                        onRegenerateClick = { viewModel.generateNewCode() }
                    )
                } else {
                    // TAB 1: ENTER CODE
                    EnterCodeSection(
                        code = uiState.enteredCode,
                        onCodeChange = { viewModel.onEnteredCodeChanged(it) },
                        isVerifying = uiState.isVerifying,
                        errorMessage = uiState.errorMessage,
                        onSubmit = { viewModel.submitPairingCode() }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Switch back to Tap to Buddy pill button
                ThreeDOutlinedButton(
                    text = "Switch to Tap to Buddy",
                    onClick = onBack,
                    leadingIcon = Icons.Default.Sensors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun GenerateCodeSection(
    code: String,
    remainingSeconds: Int,
    isGenerating: Boolean,
    onCopyClick: () -> Unit,
    onRegenerateClick: () -> Unit
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Share this code with your friend to connect. It expires automatically after 15 minutes.",
            style = MaterialTheme.typography.bodyMedium.copy(color = BuddysTheme.colors.textSecondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        ThreeDCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 3.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "YOUR PAIRING CODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BuddysTheme.colors.primaryRed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Code Display Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = BuddysTheme.colors.primaryRed, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    } else {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = BuddysTheme.colors.textPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expiry timer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (remainingSeconds > 60) BuddysTheme.colors.success else BuddysTheme.colors.error)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Expires in $formattedTime",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (remainingSeconds > 60) BuddysTheme.colors.textSecondary else BuddysTheme.colors.error,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThreeDButton(
                        text = "Copy Code",
                        onClick = onCopyClick,
                        leadingIcon = Icons.Default.ContentCopy,
                        modifier = Modifier.weight(1f)
                    )

                    ThreeDIconButton(
                        onClick = onRegenerateClick,
                        icon = Icons.Default.Refresh,
                        contentDescription = "New Code",
                        size = 48.dp,
                        iconSize = 22.dp
                    )
                }
            }
        }
    }
}

@Composable
fun EnterCodeSection(
    code: String,
    onCodeChange: (String) -> Unit,
    isVerifying: Boolean,
    errorMessage: String?,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Enter the code provided by your friend to pair securely.",
            style = MaterialTheme.typography.bodyMedium.copy(color = BuddysTheme.colors.textSecondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        ThreeDCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 3.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "ENTER PAIRING CODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BuddysTheme.colors.primaryRed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    placeholder = { Text("e.g. BUDDIES-9K8X2P", color = BuddysTheme.colors.textMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = BuddysTheme.colors.textSecondary)
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BuddysTheme.colors.primaryRed,
                        unfocusedBorderColor = BuddysTheme.colors.border,
                        focusedTextColor = BuddysTheme.colors.textPrimary,
                        unfocusedTextColor = BuddysTheme.colors.textPrimary,
                        focusedContainerColor = BuddysTheme.colors.surfaceSecondary,
                        unfocusedContainerColor = BuddysTheme.colors.surfaceSecondary
                    ),
                    singleLine = true
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.primaryRed),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                ThreeDButton(
                    text = "Verify & Connect",
                    onClick = onSubmit,
                    isLoading = isVerifying,
                    enabled = code.isNotBlank(),
                    leadingIcon = Icons.AutoMirrored.Filled.Send,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

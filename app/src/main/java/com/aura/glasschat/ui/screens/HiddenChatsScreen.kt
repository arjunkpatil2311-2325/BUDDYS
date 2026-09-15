package com.aura.glasschat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.data.model.Chat
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.ChatRepository
import com.aura.glasschat.security.AppLockManager
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.BuddysTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenChatsScreen(
    onBack: () -> Unit,
    onOpenChat: (chatId: String, otherUserId: String) -> Unit,
    chatRepository: ChatRepository = remember { ChatRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val context = LocalContext.current
    val currentUserId = authRepository.currentUserId
    val allChats by chatRepository.observeUserChats(currentUserId).collectAsState(initial = emptyList())
    val hiddenChats = remember(allChats, currentUserId) {
        allChats.filter { it.isHidden(currentUserId) }
    }

    val coroutineScope = rememberCoroutineScope()
    val colors = BuddysTheme.colors
    val appLockManager = remember { AppLockManager.getInstance(context) }
    val pinManager = appLockManager.pinManager
    val isPinSet by pinManager.isPinSet.collectAsState()

    var isUnlocked by remember { mutableStateOf(!isPinSet) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            BuddysTopBar(
                title = "Hidden Chats",
                onBack = onBack
            )

            if (!isUnlocked && isPinSet) {
                // PIN Entry view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = colors.primaryRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Enter Privacy PIN",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter your PIN to access hidden conversations.",
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                        pinInput = it
                                        pinError = null
                                        if (pinManager.verifyPin(it) is com.aura.glasschat.security.PinVerificationResult.Success) {
                                            isUnlocked = true
                                        }
                                    }
                                },
                                singleLine = true,
                                isError = pinError != null,
                                label = { Text("PIN", color = colors.textSecondary) },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primaryRed,
                                    unfocusedBorderColor = colors.border,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedContainerColor = colors.surfaceSecondary,
                                    unfocusedContainerColor = colors.surfaceSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (pinError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pinError ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.error)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            BuddysButton(
                                text = "Unlock",
                                onClick = {
                                    val result = pinManager.verifyPin(pinInput)
                                    if (result is com.aura.glasschat.security.PinVerificationResult.Success) {
                                        isUnlocked = true
                                    } else if (result is com.aura.glasschat.security.PinVerificationResult.Throttled) {
                                        pinError = "Too many attempts. Wait ${result.remainingSeconds}s"
                                    } else {
                                        pinError = "Incorrect PIN"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                if (hiddenChats.isEmpty()) {
                    BuddysEmptyState(
                        title = "No Hidden Chats",
                        subtitle = "To hide a conversation from your main inbox, long-press a chat and choose \"Hide Chat\".",
                        icon = Icons.Default.VisibilityOff,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hiddenChats, key = { it.chatId }) { chat ->
                            val otherUserId = chat.getOtherParticipantUid(currentUserId)
                            val otherInfo = chat.getOtherParticipantInfo(currentUserId)

                            BuddysCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChat(chat.chatId, otherUserId) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarView(
                                        imageUrl = otherInfo.avatarUrl,
                                        displayName = otherInfo.displayName,
                                        size = 46.dp
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = otherInfo.displayName,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = chat.lastMessage.ifBlank { "Encrypted conversation" },
                                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                                            maxLines = 1
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                chatRepository.toggleHideChat(
                                                    chatId = chat.chatId,
                                                    userId = currentUserId,
                                                    isCurrentlyHidden = true
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Unhide chat",
                                            tint = colors.textMuted
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

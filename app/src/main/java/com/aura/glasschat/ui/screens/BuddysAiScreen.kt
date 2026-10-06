package com.aura.glasschat.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.ui.components.CartoonAvatar
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.ui.viewmodel.AiMessage
import com.aura.glasschat.ui.viewmodel.BuddysAiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddysAiScreen(
    onBack: () -> Unit,
    viewModel: BuddysAiViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0.dp && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BuddysTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Modern Edge-to-Edge Top Bar with Hairline Divider
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BuddysTheme.colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BuddysTheme.colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    CartoonAvatar(
                        size = 42.dp,
                        isAi = true,
                        name = "Buddys AI",
                        isOnline = true,
                        showOnlineBadge = true,
                        cornerRadius = 14.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Buddys AI",
                                color = BuddysTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BuddysTheme.colors.primaryRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "GEMINI",
                                    color = BuddysTheme.colors.primaryRed,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Online • Instant Answers",
                                color = BuddysTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Message Stream Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(BuddysTheme.colors.background)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages) { message ->
                        AiMessageBubble(
                            message = message,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(message.text))
                                Toast.makeText(context, "Copied response 📋", Toast.LENGTH_SHORT).show()
                            },
                            onRegenerate = {
                                viewModel.regenerateLastResponse()
                            }
                        )
                    }
                }
            }

            // Modern Bottom Composer Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BuddysTheme.colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Quick Suggestion Chips Carousel
                    if (uiState.suggestions.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(uiState.suggestions) { suggestion ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.sendMessage(suggestion)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = BuddysTheme.colors.surfaceSecondary,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
                                ) {
                                    Text(
                                        text = suggestion,
                                        color = BuddysTheme.colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sleek Rounded Input Capsule
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            color = BuddysTheme.colors.surfaceSecondary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            text = "Ask Buddys AI anything...",
                                            color = BuddysTheme.colors.textMuted,
                                            fontSize = 14.sp
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = BuddysTheme.colors.textPrimary,
                                        unfocusedTextColor = BuddysTheme.colors.textPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (inputText.isNotBlank()) {
                                            viewModel.sendMessage(inputText)
                                            inputText = ""
                                        }
                                    }),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Modern Send Button
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BuddysTheme.colors.primaryRed)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = BuddysTheme.colors.textOnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiMessage,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            CartoonAvatar(
                size = 32.dp,
                isAi = true,
                name = "Buddys AI",
                cornerRadius = 10.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 285.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (message.isThinking) {
                // Thinking Bubble with Animated Spinner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BuddysTheme.colors.surfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = BuddysTheme.colors.primaryRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thinking...",
                            color = BuddysTheme.colors.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ),
                    color = when {
                        isUser -> BuddysTheme.colors.bubbleOutgoing
                        message.isError -> Color(0xFFFEF2F2)
                        else -> BuddysTheme.colors.bubbleIncoming
                    },
                    border = when {
                        isUser -> null
                        message.isError -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                        else -> androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
                    }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = message.text,
                            color = when {
                                isUser -> BuddysTheme.colors.textOnPrimary
                                message.isError -> Color(0xFFDC2626)
                                else -> BuddysTheme.colors.textPrimary
                            },
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message.timestamp,
                                color = when {
                                    isUser -> BuddysTheme.colors.textOnPrimary.copy(alpha = 0.75f)
                                    message.isError -> Color(0xFFEF4444)
                                    else -> BuddysTheme.colors.textMuted
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                            if (isUser) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "✓✓",
                                    color = BuddysTheme.colors.textOnPrimary.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // AI Action Row (Copy, Regenerate / Retry)
                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!message.isError) {
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = BuddysTheme.colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (message.isError) "Retry" else "Regenerate",
                                tint = if (message.isError) Color(0xFFEF4444) else BuddysTheme.colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (message.isError) {
                            Text(
                                text = "Tap to retry",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onRegenerate() }
                            )
                        }
                    }
                }
            }
        }
    }
}

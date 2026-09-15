package com.aura.glasschat.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.aura.glasschat.data.model.UserStories
import com.aura.glasschat.data.repository.ChatRepository
import com.aura.glasschat.ui.components.AvatarView
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.StoryViewModel
import com.aura.glasschat.util.ChatUtils
import kotlinx.coroutines.launch

@Composable
fun StoryViewerScreen(
    userStories: UserStories,
    onClose: () -> Unit,
    onOpenChatWithReply: (chatId: String, otherUid: String) -> Unit = { _, _ -> },
    viewModel: StoryViewModel = viewModel(),
    chatRepository: ChatRepository = remember { ChatRepository() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userStories) {
        viewModel.openStoryViewer(userStories, 0)
    }

    val currentStory = uiState.currentStory ?: userStories.stories.firstOrNull()
    val stories = uiState.selectedUserStories?.stories ?: userStories.stories
    val currentIndex = uiState.currentStoryIndex
    val isSelf = currentStory?.userId == uiState.currentUserId

    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }

    // Segment progress timer (5000ms)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentIndex, uiState.isPaused) {
        if (!uiState.isPaused) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            viewModel.nextStory(onStoriesFinished = onClose)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (currentStory != null) {
            // Story Image
            Image(
                painter = rememberAsyncImagePainter(currentStory.mediaUrl),
                contentDescription = "Story Image",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentIndex) {
                        detectTapGestures(
                            onPress = {
                                viewModel.setPaused(true)
                                tryAwaitRelease()
                                viewModel.setPaused(false)
                            },
                            onTap = { offset ->
                                val screenWidth = size.width
                                if (offset.x < screenWidth * 0.35f) {
                                    viewModel.previousStory()
                                } else {
                                    viewModel.nextStory(onStoriesFinished = onClose)
                                }
                            }
                        )
                    },
                contentScale = ContentScale.Crop
            )

            // Top gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
            )

            // Bottom gradient overlay for caption and reply
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Top Progress Bars & Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Segmented Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        val segmentProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                // Author Header Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarView(
                            imageUrl = currentStory.userAvatarUrl,
                            displayName = currentStory.userDisplayName.ifBlank { currentStory.username },
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentStory.userDisplayName.ifBlank { currentStory.username },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                if (currentStory.isCloseFriendsOnly) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF2E7D32),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "⭐ Close Friends",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = currentStory.createdAt?.let { ChatUtils.formatTimestamp(it) } ?: "Just now",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelf) {
                            IconButton(
                                onClick = {
                                    viewModel.deleteCurrentStory {
                                        Toast.makeText(context, "Story deleted", Toast.LENGTH_SHORT).show()
                                        onClose()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Story", tint = Color.White)
                            }
                        }

                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            // Bottom Caption and Reply Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding()
            ) {
                if (currentStory.caption.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = currentStory.caption,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // If not viewing self, show quick reactions & direct reply bar
                if (!isSelf) {
                    // Quick Emoji Reaction Bar
                    val quickEmojis = listOf("🔥", "❤️", "😂", "😍", "👏", "🙌")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickEmojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 26.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.addStoryReaction(currentStory.id, emoji)
                                        coroutineScope.launch {
                                            val currentUid = uiState.currentUserId
                                            val targetUid = currentStory.userId
                                            val chatId = ChatUtils.getDeterministicChatId(currentUid, targetUid)
                                            chatRepository.sendStoryReplyMessage(
                                                chatId = chatId,
                                                senderId = currentUid,
                                                senderName = "You",
                                                replyText = emoji,
                                                storyId = currentStory.id,
                                                storyImageUrl = currentStory.mediaUrl
                                            )
                                            Toast.makeText(context, "Sent $emoji reaction! ✨", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            decorationBox = { innerTextField ->
                                if (replyText.isEmpty()) {
                                    Text(
                                        "Send message...",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f))
                                    )
                                }
                                innerTextField()
                            },
                            singleLine = true
                        )

                        if (replyText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val textToSend = replyText.trim()
                                    if (textToSend.isNotBlank() && !isSendingReply) {
                                        isSendingReply = true
                                        coroutineScope.launch {
                                            val currentUid = uiState.currentUserId
                                            val targetUid = currentStory.userId
                                            val chatId = ChatUtils.getDeterministicChatId(currentUid, targetUid)
                                            chatRepository.sendStoryReplyMessage(
                                                chatId = chatId,
                                                senderId = currentUid,
                                                senderName = "You",
                                                replyText = textToSend,
                                                storyId = currentStory.id,
                                                storyImageUrl = currentStory.mediaUrl
                                            )
                                            isSendingReply = false
                                            replyText = ""
                                            Toast.makeText(context, "Reply sent to chat! ✨", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Reply",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

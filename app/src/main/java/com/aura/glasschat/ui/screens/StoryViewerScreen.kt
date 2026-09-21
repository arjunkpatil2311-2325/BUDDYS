package com.aura.glasschat.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import coil.compose.SubcomposeAsyncImage
import com.aura.glasschat.data.model.StoryStickerItem
import com.aura.glasschat.data.model.StoryTextOverlay
import com.aura.glasschat.data.model.StoryViewerEntry
import com.aura.glasschat.data.model.UserStories
import com.aura.glasschat.data.repository.ChatRepository
import com.aura.glasschat.data.repository.SupabaseMediaStorageRepository
import com.aura.glasschat.ui.components.AvatarView
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.StoryViewModel
import com.aura.glasschat.util.ChatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val mediaStorageRepo = remember { SupabaseMediaStorageRepository.getInstance() }

    LaunchedEffect(userStories) {
        viewModel.openStoryViewer(userStories, 0)
    }

    val currentStory = uiState.currentStory ?: userStories.stories.firstOrNull()
    val stories = uiState.selectedUserStories?.stories ?: userStories.stories
    val currentIndex = uiState.currentStoryIndex
    val isSelf = currentStory?.userId == uiState.currentUserId

    var resolvedMediaUrl by remember(currentStory?.id, currentStory?.mediaUrl) { mutableStateOf(currentStory?.mediaUrl ?: "") }
    var isMediaLoading by remember(currentStory?.id) { mutableStateOf(true) }
    var isMediaError by remember(currentStory?.id) { mutableStateOf(false) }

    LaunchedEffect(currentStory?.id, currentStory?.mediaUrl) {
        val raw = currentStory?.mediaUrl
        if (!raw.isNullOrBlank()) {
            isMediaLoading = true
            isMediaError = false
            try {
                val freshUrl = mediaStorageRepo.resolveMediaUrl(raw, 3600)
                resolvedMediaUrl = freshUrl
            } catch (_: Exception) {
                resolvedMediaUrl = raw
            }
        }
    }

    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }
    var showViewerInsightsSheet by remember { mutableStateOf(false) }
    var isPressingDown by remember { mutableStateOf(false) }

    // Segment progress timer (5000ms)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentIndex, uiState.isPaused, showViewerInsightsSheet, isMediaLoading, isMediaError) {
        val paused = uiState.isPaused || showViewerInsightsSheet || isMediaLoading || isMediaError
        if (!paused) {
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
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (dragAmount.y > 45 && !showViewerInsightsSheet) {
                        change.consume()
                        onClose()
                    } else if (dragAmount.y < -45 && isSelf && !showViewerInsightsSheet) {
                        change.consume()
                        showViewerInsightsSheet = true
                    }
                }
            }
    ) {
        if (currentStory != null) {
            // Story Image with Press & Tap Gestures + Resolution handling
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentIndex) {
                        detectTapGestures(
                            onPress = {
                                isPressingDown = true
                                viewModel.setPaused(true)
                                tryAwaitRelease()
                                isPressingDown = false
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
                    }
            ) {
                SubcomposeAsyncImage(
                    model = resolvedMediaUrl.ifBlank { currentStory.mediaUrl },
                    contentDescription = "Story Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = {
                        isMediaLoading = false
                        isMediaError = false
                    },
                    onError = {
                        isMediaLoading = false
                        isMediaError = true
                    },
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = BuddysTheme.colors.primaryRed,
                                strokeWidth = 3.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF121218)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Story media unavailable",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "The story media link may have expired or is private.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isMediaLoading = true
                                            isMediaError = false
                                            val fresh = mediaStorageRepo.resolveMediaUrl(currentStory.mediaUrl, 3600)
                                            resolvedMediaUrl = fresh
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BuddysTheme.colors.primaryRed),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Loading", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                )
            }

            // Top gradient overlay for text readability
            AnimatedVisibility(
                visible = !isPressingDown,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                            )
                        )
                )
            }

            // Bottom gradient overlay for caption and reply
            AnimatedVisibility(
                visible = !isPressingDown,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
            }

            // Top Progress Bars & Header
            AnimatedVisibility(
                visible = !isPressingDown,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                            color = Color(0xFF00E676),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "⭐ Close Friends",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
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
            }

            // Bottom Caption and Reply Section
            AnimatedVisibility(
                visible = !isPressingDown,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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

                    // If viewing own story: show Insights button (view count + swipe up prompt)
                    if (isSelf) {
                        val viewCount = currentStory.viewedBy.size
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable { showViewerInsightsSheet = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = "Viewers", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "$viewCount Viewers (Tap for insights)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        // If not viewing self, show quick reactions & direct reply bar
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

        // Viewer Insights Bottom Sheet (For story owner)
        if (showViewerInsightsSheet && currentStory != null) {
            ModalBottomSheet(
                onDismissRequest = { showViewerInsightsSheet = false },
                containerColor = Color(0xFF16161E),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                StoryViewerInsightsSheetContent(
                    viewers = currentStory.viewerDetails.values.toList().sortedByDescending { it.viewedAt.seconds },
                    viewCount = currentStory.viewedBy.size,
                    reactions = currentStory.reactions,
                    onDeleteStory = {
                        showViewerInsightsSheet = false
                        viewModel.deleteCurrentStory {
                            Toast.makeText(context, "Story deleted", Toast.LENGTH_SHORT).show()
                            onClose()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StoryViewerInsightsSheetContent(
    viewers: List<StoryViewerEntry>,
    viewCount: Int,
    reactions: Map<String, String>,
    onDeleteStory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RemoveRedEye, contentDescription = "Views", tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$viewCount Views",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }

            IconButton(onClick = onDeleteStory) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Story", tint = Color(0xFFFF5252))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No viewers yet.\nShare your story with close friends!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(viewers) { entry ->
                    val userReaction = entry.reaction ?: reactions[entry.uid]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E1E28))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarView(
                                imageUrl = entry.avatarUrl,
                                displayName = entry.displayName.ifBlank { entry.username },
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = entry.displayName.ifBlank { entry.username },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "@${entry.username}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (userReaction != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = userReaction,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

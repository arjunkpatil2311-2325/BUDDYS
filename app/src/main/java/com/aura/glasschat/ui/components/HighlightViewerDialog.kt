package com.aura.glasschat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.aura.glasschat.data.model.ProfileHighlight
import com.aura.glasschat.data.model.Story
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.util.ChatUtils

@Composable
fun HighlightViewerDialog(
    highlight: ProfileHighlight,
    isOwner: Boolean,
    onClose: () -> Unit,
    onEditHighlight: (ProfileHighlight) -> Unit = {},
    onDeleteHighlight: (ProfileHighlight) -> Unit = {}
) {
    val stories = highlight.stories.ifEmpty {
        listOf(
            Story(
                id = highlight.id,
                userId = highlight.userId,
                mediaUrl = highlight.coverUrl,
                caption = highlight.title
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var isPressingDown by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val currentStory = stories.getOrNull(currentIndex) ?: stories.first()
    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            if (currentIndex < stories.size - 1) {
                currentIndex += 1
            } else {
                onClose()
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        if (dragAmount.y > 45) {
                            change.consume()
                            onClose()
                        }
                    }
                }
        ) {
            // Story Image
            Image(
                painter = rememberAsyncImagePainter(currentStory.mediaUrl),
                contentDescription = "Highlight Media",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentIndex) {
                        detectTapGestures(
                            onPress = {
                                isPressingDown = true
                                isPaused = true
                                tryAwaitRelease()
                                isPressingDown = false
                                isPaused = false
                            },
                            onTap = { offset ->
                                val screenWidth = size.width
                                if (offset.x < screenWidth * 0.35f) {
                                    if (currentIndex > 0) currentIndex -= 1
                                } else {
                                    if (currentIndex < stories.size - 1) {
                                        currentIndex += 1
                                    } else {
                                        onClose()
                                    }
                                }
                            }
                        )
                    },
                contentScale = ContentScale.Crop
            )

            // Top gradient
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

            // Top Header & Progress
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

                    // Highlight Title & Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarView(
                                imageUrl = highlight.coverUrl,
                                displayName = highlight.title,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = highlight.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Story Highlight",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isOwner) {
                                Box {
                                    IconButton(onClick = {
                                        isPaused = true
                                        showMenu = true
                                    }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = {
                                            showMenu = false
                                            isPaused = false
                                        },
                                        modifier = Modifier.background(BuddysTheme.colors.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Highlight", color = BuddysTheme.colors.textPrimary) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = BuddysTheme.colors.primaryRed) },
                                            onClick = {
                                                showMenu = false
                                                onClose()
                                                onEditHighlight(highlight)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Highlight", color = BuddysTheme.colors.error) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BuddysTheme.colors.error) },
                                            onClick = {
                                                showMenu = false
                                                onClose()
                                                onDeleteHighlight(highlight)
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Bottom Caption
            if (currentStory.caption.isNotBlank()) {
                AnimatedVisibility(
                    visible = !isPressingDown,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
            }
        }
    }
}

package com.aura.glasschat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aura.glasschat.data.model.Story
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.StoryRepository
import com.aura.glasschat.ui.theme.BuddysTheme
import java.text.SimpleDateFormat
import java.util.*

import com.aura.glasschat.ui.components.*

@Composable
fun StoryArchiveScreen(
    onBack: () -> Unit,
    storyRepository: StoryRepository = remember { StoryRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val currentUserId = authRepository.currentUserId
    val archivedStories by storyRepository.observeStoryArchive(currentUserId).collectAsState(initial = emptyList())
    val colors = BuddysTheme.colors

    var selectedStoryForPreview by remember { mutableStateOf<Story?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            BuddysTopBar(
                title = "Story Archive",
                onBack = onBack
            )

            if (archivedStories.isEmpty()) {
                BuddysEmptyState(
                    title = "No Archived Stories",
                    subtitle = "When your 24-hour stories expire, they stay saved here privately for only you to view.",
                    icon = Icons.Default.History,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(archivedStories, key = { it.id }) { story ->
                        StoryArchiveGridItem(
                            story = story,
                            onClick = { selectedStoryForPreview = story }
                        )
                    }
                }
            }
        }

        selectedStoryForPreview?.let { story ->
            StoryArchivePreviewDialog(
                story = story,
                onDismiss = { selectedStoryForPreview = null }
            )
        }
    }
}

@Composable
private fun StoryArchiveGridItem(
    story: Story,
    onClick: () -> Unit
) {
    val dateStr = remember(story.createdAt) {
        val date = story.createdAt?.toDate() ?: Date()
        SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.65f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = story.mediaUrl,
            contentDescription = "Archived Story",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Date badge on top left
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .padding(4.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = dateStr,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // Close friends badge if audience == CLOSE_FRIENDS
        if (story.isCloseFriendsOnly) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = "Close Friends",
                tint = Color(0xFF22A06B),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            )
        }
    }
}

@Composable
private fun StoryArchivePreviewDialog(
    story: Story,
    onDismiss: () -> Unit
) {
    val dateStr = remember(story.createdAt) {
        val date = story.createdAt?.toDate() ?: Date()
        SimpleDateFormat("MMMM d, yyyy · h:mm a", Locale.getDefault()).format(date)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Story from $dateStr",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (story.isCloseFriendsOnly) {
                    Text(
                        text = "⭐ Shared with Close Friends only",
                        fontSize = 12.sp,
                        color = Color(0xFF22A06B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = story.mediaUrl,
                    contentDescription = "Story preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                if (story.caption.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = story.caption,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Viewed by ${story.viewedBy.size} friends",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

package com.aura.glasschat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import coil.compose.rememberAsyncImagePainter
import com.aura.glasschat.data.model.ProfileHighlight
import com.aura.glasschat.data.model.Story
import com.aura.glasschat.ui.theme.BuddysTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHighlightSheet(
    highlight: ProfileHighlight,
    availableStories: List<Story>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUpdateHighlight: (title: String, coverUrl: String, selectedStories: List<Story>) -> Unit,
    onDeleteHighlight: () -> Unit
) {
    var title by remember { mutableStateOf(highlight.title) }
    var coverUrl by remember { mutableStateOf(highlight.coverUrl) }
    val selectedStoryIds = remember { mutableStateListOf<String>().apply { addAll(highlight.storyIds) } }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val allStories = remember(availableStories, highlight.stories) {
        val map = (highlight.stories + availableStories).associateBy { it.id }
        map.values.toList()
    }

    val selectedStories = remember(selectedStoryIds.toList(), allStories) {
        allStories.filter { selectedStoryIds.contains(it.id) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BuddysTheme.colors.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = BuddysTheme.colors.textSecondary
                    )
                }

                Text(
                    text = "Edit Highlight",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary
                    )
                )

                Button(
                    onClick = {
                        val finalCover = coverUrl.ifBlank { selectedStories.firstOrNull()?.mediaUrl ?: highlight.coverUrl }
                        val finalTitle = title.trim().ifBlank { "Highlight" }
                        onUpdateHighlight(finalTitle, finalCover, selectedStories)
                    },
                    enabled = !isSaving && selectedStories.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BuddysTheme.colors.primaryRed,
                        contentColor = BuddysTheme.colors.textOnPrimary
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = BuddysTheme.colors.textOnPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = BuddysTheme.colors.border)
            Spacer(modifier = Modifier.height(14.dp))

            // Cover & Title Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.5.dp, BuddysTheme.colors.primaryRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val coverToShow = coverUrl.ifBlank { selectedStories.firstOrNull()?.mediaUrl ?: highlight.coverUrl }
                    if (coverToShow.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(coverToShow),
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 30) title = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BuddysTheme.colors.primaryRed,
                        unfocusedBorderColor = BuddysTheme.colors.border,
                        focusedTextColor = BuddysTheme.colors.textPrimary,
                        unfocusedTextColor = BuddysTheme.colors.textPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pick Cover Frame Horizontal List
            Text(
                text = "Tap to choose cover frame:",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BuddysTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedStories) { story ->
                    val isCover = (coverUrl.ifBlank { selectedStories.firstOrNull()?.mediaUrl }) == story.mediaUrl
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isCover) 2.5.dp else 1.dp,
                                color = if (isCover) BuddysTheme.colors.primaryRed else BuddysTheme.colors.border,
                                shape = CircleShape
                            )
                            .clickable { coverUrl = story.mediaUrl }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(story.mediaUrl),
                            contentDescription = "Frame",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BuddysTheme.colors.border)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Stories in Highlight (${selectedStories.size})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // All stories grid to toggle selection
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(allStories, key = { it.id }) { story ->
                    val isSelected = selectedStoryIds.contains(story.id)
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (isSelected) {
                                    if (selectedStoryIds.size > 1) {
                                        selectedStoryIds.remove(story.id)
                                    }
                                } else {
                                    selectedStoryIds.add(story.id)
                                }
                            }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(story.mediaUrl),
                            contentDescription = "Story",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) BuddysTheme.colors.primaryRed else Color.Black.copy(alpha = 0.5f))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Delete Highlight Button
            OutlinedButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BuddysTheme.colors.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Highlight", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Highlight?", fontWeight = FontWeight.Bold, color = BuddysTheme.colors.textPrimary) },
            text = { Text("Are you sure you want to delete \"${highlight.title}\"? The stories will remain in your private archive.", color = BuddysTheme.colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteHighlight()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BuddysTheme.colors.error, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = BuddysTheme.colors.textSecondary)
                }
            },
            containerColor = BuddysTheme.colors.surface
        )
    }
}

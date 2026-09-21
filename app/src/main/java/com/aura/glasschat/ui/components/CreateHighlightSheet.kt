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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.aura.glasschat.data.model.Story
import com.aura.glasschat.ui.theme.BuddysTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHighlightSheet(
    availableStories: List<Story>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveHighlight: (title: String, coverUrl: String, selectedStories: List<Story>) -> Unit
) {
    var step by remember { mutableStateOf(1) } // Step 1: Select stories, Step 2: Name & Cover
    val selectedStoryIds = remember { mutableStateListOf<String>() }
    var highlightTitle by remember { mutableStateOf("") }
    var selectedCoverUrl by remember { mutableStateOf("") }

    val selectedStories = remember(selectedStoryIds.toList(), availableStories) {
        availableStories.filter { selectedStoryIds.contains(it.id) }
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
                if (step == 2) {
                    IconButton(onClick = { step = 1 }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BuddysTheme.colors.textPrimary
                        )
                    }
                } else {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = BuddysTheme.colors.textSecondary
                        )
                    }
                }

                Text(
                    text = if (step == 1) "Select Stories" else "New Highlight",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary
                    )
                )

                if (step == 1) {
                    TextButton(
                        onClick = {
                            if (selectedStoryIds.isNotEmpty()) {
                                if (selectedCoverUrl.isBlank()) {
                                    selectedCoverUrl = selectedStories.firstOrNull()?.mediaUrl ?: ""
                                }
                                step = 2
                            }
                        },
                        enabled = selectedStoryIds.isNotEmpty()
                    ) {
                        Text(
                            text = "Next",
                            color = if (selectedStoryIds.isNotEmpty()) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            val finalCover = selectedCoverUrl.ifBlank { selectedStories.firstOrNull()?.mediaUrl ?: "" }
                            val finalTitle = highlightTitle.trim().ifBlank { "Highlight" }
                            onSaveHighlight(finalTitle, finalCover, selectedStories)
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
            }

            HorizontalDivider(color = BuddysTheme.colors.border)
            Spacer(modifier = Modifier.height(12.dp))

            // STEP 1: Story Selection Grid
            if (step == 1) {
                if (availableStories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No stories found in your archive.\nPost a story first to add it to highlights.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BuddysTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(availableStories, key = { it.id }) { story ->
                            val isSelected = selectedStoryIds.contains(story.id)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isSelected) {
                                            selectedStoryIds.remove(story.id)
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

                                // Checkbox overlay
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
                }
            } else {
                // STEP 2: Title and Cover Choice
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected Cover Thumbnail Preview
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(BuddysTheme.colors.surfaceSecondary)
                            .border(2.dp, BuddysTheme.colors.primaryRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val coverToShow = selectedCoverUrl.ifBlank { selectedStories.firstOrNull()?.mediaUrl }
                        if (coverToShow != null) {
                            Image(
                                painter = rememberAsyncImagePainter(coverToShow),
                                contentDescription = "Cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Edit Cover",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = BuddysTheme.colors.primaryRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    // Cover selector row
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        items(selectedStories) { story ->
                            val isCover = (selectedCoverUrl.ifBlank { selectedStories.firstOrNull()?.mediaUrl }) == story.mediaUrl
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isCover) 2.5.dp else 1.dp,
                                        color = if (isCover) BuddysTheme.colors.primaryRed else BuddysTheme.colors.border,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCoverUrl = story.mediaUrl }
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Highlight Title Input
                    OutlinedTextField(
                        value = highlightTitle,
                        onValueChange = { if (it.length <= 30) highlightTitle = it },
                        label = { Text("Highlight Name") },
                        placeholder = { Text("e.g. Summer 2026, Friends") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuddysTheme.colors.primaryRed,
                            unfocusedBorderColor = BuddysTheme.colors.border,
                            focusedTextColor = BuddysTheme.colors.textPrimary,
                            unfocusedTextColor = BuddysTheme.colors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${selectedStories.size} stories selected",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BuddysTheme.colors.textSecondary
                        )
                    )
                }
            }
        }
    }
}

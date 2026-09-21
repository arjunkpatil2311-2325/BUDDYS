package com.aura.glasschat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.aura.glasschat.data.model.Post
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.util.ChatUtils

@Composable
fun PostDetailDialog(
    post: Post,
    isOwner: Boolean,
    currentUserId: String,
    pinnedPostsCount: Int,
    onDismiss: () -> Unit,
    onTogglePin: (Post) -> Unit,
    onDeletePost: (Post) -> Unit,
    onToggleLike: (Post) -> Unit,
    onSharePost: (Post) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = false) {}, // prevent click-through
                colors = CardDefaults.cardColors(containerColor = BuddysTheme.colors.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarView(
                                imageUrl = post.userAvatarUrl,
                                displayName = post.userDisplayName.ifBlank { post.username },
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = post.userDisplayName.ifBlank { post.username },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BuddysTheme.colors.textPrimary
                                        )
                                    )
                                    if (post.isPinned) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = BuddysTheme.colors.primaryRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "@${post.username}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BuddysTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // More Menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = BuddysTheme.colors.textPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(BuddysTheme.colors.surface)
                            ) {
                                if (isOwner) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (post.isPinned) "Unpin from profile" else "Pin to your profile",
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = null,
                                                tint = BuddysTheme.colors.primaryRed
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onTogglePin(post)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Delete post",
                                                color = BuddysTheme.colors.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = BuddysTheme.colors.error
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showDeleteConfirm = true
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Share post", color = BuddysTheme.colors.textPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = BuddysTheme.colors.textPrimary
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onSharePost(post)
                                    }
                                )
                            }
                        }
                    }

                    // Post Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color.Black)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(post.mediaUrl),
                            contentDescription = "Post Media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Bottom Action Bar & Caption
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val isLiked = post.isLikedBy(currentUserId)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = { onToggleLike(post) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (isLiked) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = "${post.likeCount} likes",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BuddysTheme.colors.textPrimary
                                    )
                                )
                            }

                            Text(
                                text = post.createdAt?.let { ChatUtils.formatTimestamp(it) } ?: "Just now",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BuddysTheme.colors.textSecondary
                                )
                            )
                        }

                        if (post.caption.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row {
                                Text(
                                    text = "${post.username} ",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BuddysTheme.colors.textPrimary
                                    )
                                )
                                Text(
                                    text = post.caption,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = BuddysTheme.colors.textPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete post?", fontWeight = FontWeight.Bold, color = BuddysTheme.colors.textPrimary) },
            text = { Text("Are you sure you want to permanently delete this post?", color = BuddysTheme.colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeletePost(post)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BuddysTheme.colors.error, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = BuddysTheme.colors.textSecondary)
                }
            },
            containerColor = BuddysTheme.colors.surface
        )
    }
}

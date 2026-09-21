package com.aura.glasschat.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.aura.glasschat.data.model.Post
import com.aura.glasschat.data.model.ProfileHighlight
import com.aura.glasschat.data.repository.RelationshipState
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.ui.viewmodel.PublicProfileViewModel
import com.aura.glasschat.util.ChatUtils

@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenFollowers: (userId: String) -> Unit,
    onOpenFollowing: (userId: String) -> Unit,
    onOpenChat: (chatId: String, otherUserId: String) -> Unit,
    viewModel: PublicProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("Spam") }
    var reportDetails by remember { mutableStateOf("") }
    var selectedContentTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val user = uiState.targetUser

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Top Bar
            BuddysTopBar(
                title = if (user != null) "@${user.username}" else "Profile",
                onBack = onBack,
                actions = {
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
                            if (uiState.isBlocked) {
                                DropdownMenuItem(
                                    text = { Text("Unblock User", color = BuddysTheme.colors.textPrimary) },
                                    onClick = {
                                        viewModel.unblockUser()
                                        showMenu = false
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Block @${user?.username ?: "User"}", color = BuddysTheme.colors.primaryRed) },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = BuddysTheme.colors.primaryRed) },
                                    onClick = {
                                        showBlockDialog = true
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report @${user?.username ?: "User"}", color = BuddysTheme.colors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = BuddysTheme.colors.textPrimary) },
                                    onClick = {
                                        showReportDialog = true
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            if (uiState.isLoading || user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BuddysTheme.colors.primaryRed, strokeWidth = 3.dp)
                }
            } else if (uiState.isBlocked) {
                // Blocked state view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You blocked @${user.username}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BuddysTheme.colors.textPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "They won't be able to interact with you through Buddies.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    BuddysOutlinedButton(
                        text = "Unblock",
                        onClick = { viewModel.unblockUser() }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Card Container
                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Large Avatar View
                            AvatarView(
                                imageUrl = user.avatarUrl,
                                displayName = user.displayName.ifBlank { user.username },
                                size = 96.dp,
                                isOnline = user.isOnline
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Display Name
                            Text(
                                text = user.displayName.ifBlank { user.username },
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = BuddysTheme.colors.textPrimary,
                                    fontSize = 22.sp
                                )
                            )

                            // Username Tag
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            // Mutual "Buddys" Badge if applicable
                            if (uiState.relationshipState == RelationshipState.MUTUAL) {
                                Spacer(modifier = Modifier.height(8.dp))
                                BuddysBadge(text = "Mutual Buddys")
                            }

                            // Online / Presence Status
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (user.isOnline) "Active now" else ChatUtils.formatSeenStatus(user.lastSeen),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (user.isOnline) BuddysTheme.colors.success else BuddysTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Clickable Followers & Following Counts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Followers Column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenFollowers(user.uid) }
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${user.followerCount}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = BuddysTheme.colors.textPrimary
                                        )
                                    )
                                    Text(
                                        text = "Followers",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BuddysTheme.colors.textSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .width(1.dp)
                                        .background(BuddysTheme.colors.border)
                                        .align(Alignment.CenterVertically)
                                )

                                // Following Column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenFollowing(user.uid) }
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${user.followingCount}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = BuddysTheme.colors.textPrimary
                                        )
                                    )
                                    Text(
                                        text = "Following",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BuddysTheme.colors.textSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bio Box
                            val bioText = if (user.bio.isNotBlank()) user.bio else if (user.statusMessage.isNotBlank()) user.statusMessage else "Hey there! I am using Buddies"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BuddysTheme.colors.surfaceSecondary)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bioText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = BuddysTheme.colors.textPrimary,
                                        fontSize = 14.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action Buttons: Follow / Message
                            if (uiState.currentUserId != user.uid) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val buttonText = when (uiState.relationshipState) {
                                        RelationshipState.MUTUAL -> "Following"
                                        RelationshipState.FOLLOWING -> "Following"
                                        RelationshipState.REQUESTED -> "Requested"
                                        RelationshipState.FOLLOWED_BY -> "Follow Back"
                                        else -> "+ Follow"
                                    }

                                    Button(
                                        onClick = { viewModel.toggleFollow() },
                                        enabled = !uiState.isActionLoading,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when (uiState.relationshipState) {
                                                RelationshipState.MUTUAL, RelationshipState.FOLLOWING -> BuddysTheme.colors.surfaceSecondary
                                                RelationshipState.REQUESTED -> BuddysTheme.colors.surfaceSecondary
                                                else -> BuddysTheme.colors.primaryRed
                                            },
                                            contentColor = when (uiState.relationshipState) {
                                                RelationshipState.MUTUAL, RelationshipState.FOLLOWING -> BuddysTheme.colors.textPrimary
                                                RelationshipState.REQUESTED -> BuddysTheme.colors.textSecondary
                                                else -> BuddysTheme.colors.textOnPrimary
                                            }
                                        ),
                                        border = if (uiState.relationshipState == RelationshipState.FOLLOWING || uiState.relationshipState == RelationshipState.MUTUAL) androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border) else null
                                    ) {
                                        if (uiState.isActionLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BuddysTheme.colors.primaryRed)
                                        } else {
                                            Text(
                                                text = buttonText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    BuddysButton(
                                        text = "Message",
                                        onClick = {
                                            val chatId = viewModel.getChatId()
                                            if (chatId != null) {
                                                onOpenChat(chatId, user.uid)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Public vs Private Account Check
                    if (!uiState.canViewPrivateContent) {
                        BuddysCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "This account is private. Follow to view full activity.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
                                )
                            }
                        }
                    } else {
                        // Highlights Row (Visitors)
                        if (uiState.highlights.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(uiState.highlights, key = { it.id }) { highlight ->
                                    PublicHighlightItem(
                                        highlight = highlight,
                                        onClick = { viewModel.openHighlightViewer(highlight) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Content Grid (3-Column Posts with Pinned Posts first)
                        if (uiState.posts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BuddysTheme.colors.surface)
                                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No posts yet",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = BuddysTheme.colors.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        } else {
                            val chunkedPosts = remember(uiState.posts) { uiState.posts.chunked(3) }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                chunkedPosts.forEach { rowPosts ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowPosts.forEach { post ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(BuddysTheme.colors.surfaceSecondary)
                                                    .clickable { viewModel.selectPostForDetail(post) }
                                            ) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(post.mediaUrl),
                                                    contentDescription = "Post Thumbnail",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )

                                                if (post.isPinned) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Black.copy(alpha = 0.65f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PushPin,
                                                            contentDescription = "Pinned",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        repeat(3 - rowPosts.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }

        // Highlight Viewer Dialog
        if (uiState.activeHighlightForViewing != null) {
            HighlightViewerDialog(
                highlight = uiState.activeHighlightForViewing!!,
                isOwner = false,
                onClose = { viewModel.closeHighlightViewer() }
            )
        }

        // Post Detail Dialog (for visitor)
        if (uiState.selectedPostForDetail != null) {
            PostDetailDialog(
                post = uiState.selectedPostForDetail!!,
                isOwner = false,
                currentUserId = uiState.currentUserId,
                pinnedPostsCount = 0,
                onDismiss = { viewModel.selectPostForDetail(null) },
                onTogglePin = {},
                onDeletePost = {},
                onToggleLike = { post -> viewModel.toggleLikePost(post) },
                onSharePost = { post ->
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out @${post.username}'s moment on Buddies: ${post.caption}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share moment via"))
                }
            )
        }
    }

    // Block Confirmation Dialog
    if (showBlockDialog && user != null) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block @${user.username}?", fontWeight = FontWeight.Bold, color = BuddysTheme.colors.textPrimary) },
            text = { Text("They won't be able to find your profile, follow you, or message you on Buddies.", color = BuddysTheme.colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.blockUser()
                        showBlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BuddysTheme.colors.primaryRed, contentColor = BuddysTheme.colors.textOnPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Block", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel", color = BuddysTheme.colors.textSecondary)
                }
            },
            containerColor = BuddysTheme.colors.surface
        )
    }

    // Report Dialog
    if (showReportDialog && user != null) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report @${user.username}", fontWeight = FontWeight.Bold, color = BuddysTheme.colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Why are you reporting this user?", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = BuddysTheme.colors.textPrimary))
                    val reasons = listOf("Spam", "Harassment", "Impersonation", "Inappropriate content", "Other")
                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedReportReason = reason }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReportReason == reason,
                                onClick = { selectedReportReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = BuddysTheme.colors.primaryRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(reason, style = MaterialTheme.typography.bodyMedium.copy(color = BuddysTheme.colors.textPrimary))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reportUser(selectedReportReason, reportDetails)
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BuddysTheme.colors.primaryRed,
                        contentColor = BuddysTheme.colors.textOnPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = BuddysTheme.colors.textSecondary)
                }
            },
            containerColor = BuddysTheme.colors.surface
        )
    }
}

@Composable
private fun PublicHighlightItem(
    highlight: ProfileHighlight,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(1.5.dp, BuddysTheme.colors.border, CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            if (highlight.coverUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(highlight.coverUrl),
                    contentDescription = highlight.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = BuddysTheme.colors.primaryRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = highlight.title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = BuddysTheme.colors.textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

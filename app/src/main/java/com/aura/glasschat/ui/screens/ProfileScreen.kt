package com.aura.glasschat.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.aura.glasschat.data.model.User
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onBack: (() -> Unit)? = null,
    onOpenEditProfile: () -> Unit = {},
    onOpenPrivacySettings: () -> Unit = {},
    onOpenFollowers: (userId: String) -> Unit = {},
    onOpenFollowing: (userId: String) -> Unit = {},
    onOpenCreateStory: () -> Unit = {},
    onOpenAccountSwitcher: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhotoSelected(context, uri)
        }
    }

    val postImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createPost(context, uri, "Shared from Buddies ✨")
            Toast.makeText(context, "Posting moment...", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLoggedOut()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            BuddysFullProfileView(
                user = user,
                highlights = uiState.highlights,
                posts = uiState.posts,
                onBack = onBack,
                onOpenEditProfile = onOpenEditProfile,
                onOpenPrivacySettings = onOpenPrivacySettings,
                onOpenFollowers = { user?.let { onOpenFollowers(it.uid) } },
                onOpenFollowing = { user?.let { onOpenFollowing(it.uid) } },
                onOpenCreateStory = onOpenCreateStory,
                onOpenAccountSwitcher = onOpenAccountSwitcher,
                onPhotoOptionsClick = { viewModel.openPhotoOptions() },
                onOpenCreateHighlight = { viewModel.openCreateHighlight() },
                onHighlightClick = { highlight -> viewModel.openHighlightViewer(highlight) },
                onHighlightLongClick = { highlight -> viewModel.openEditHighlight(highlight) },
                onPostClick = { post -> viewModel.selectPostForDetail(post) },
                onCreatePostClick = { postImageLauncher.launch("image/*") },
                onSignOutClick = { viewModel.signOut() }
            )
        }

        // Change Profile Picture Dialog
        if (uiState.showPhotoOptions) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPhotoOptions() },
                containerColor = BuddysTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "Profile Picture",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textPrimary
                        )
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.dismissPhotoOptions()
                                    galleryLauncher.launch("image/*")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Choose from gallery",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        if (!user?.avatarUrl.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.removePhoto() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = BuddysTheme.colors.error, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Remove current photo",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = BuddysTheme.colors.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissPhotoOptions() }) {
                        Text("Cancel", color = BuddysTheme.colors.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        // Create Highlight Sheet
        if (uiState.showCreateHighlightSheet) {
            CreateHighlightSheet(
                availableStories = uiState.archivedStories,
                isSaving = uiState.isSavingHighlight,
                onDismiss = { viewModel.closeCreateHighlight() },
                onSaveHighlight = { title, coverUrl, selectedStories ->
                    viewModel.createHighlight(title, coverUrl, selectedStories)
                }
            )
        }

        // Edit Highlight Sheet
        if (uiState.selectedHighlightForEdit != null) {
            EditHighlightSheet(
                highlight = uiState.selectedHighlightForEdit!!,
                availableStories = uiState.archivedStories,
                isSaving = uiState.isSavingHighlight,
                onDismiss = { viewModel.closeEditHighlight() },
                onUpdateHighlight = { title, coverUrl, selectedStories ->
                    viewModel.updateHighlight(uiState.selectedHighlightForEdit!!.id, title, coverUrl, selectedStories)
                },
                onDeleteHighlight = {
                    viewModel.deleteHighlight(uiState.selectedHighlightForEdit!!.id)
                }
            )
        }

        // Highlight Viewer Dialog
        if (uiState.activeHighlightForViewing != null) {
            HighlightViewerDialog(
                highlight = uiState.activeHighlightForViewing!!,
                isOwner = true,
                onClose = { viewModel.closeHighlightViewer() },
                onEditHighlight = { highlight -> viewModel.openEditHighlight(highlight) },
                onDeleteHighlight = { highlight -> viewModel.deleteHighlight(highlight.id) }
            )
        }

        // Post Detail Dialog (with Pin/Unpin, Delete, Like)
        if (uiState.selectedPostForDetail != null) {
            PostDetailDialog(
                post = uiState.selectedPostForDetail!!,
                isOwner = true,
                currentUserId = user?.uid ?: "",
                pinnedPostsCount = uiState.pinnedPostsCount,
                onDismiss = { viewModel.selectPostForDetail(null) },
                onTogglePin = { post -> viewModel.togglePinPost(post) },
                onDeletePost = { post -> viewModel.deletePost(post) },
                onToggleLike = { post -> viewModel.toggleLikePost(post) },
                onSharePost = { post ->
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out @${post.username}'s moment on Buddies: ${post.caption}")
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share moment via"))
                }
            )
        }
    }
}

@Composable
fun BuddysFullProfileView(
    user: User?,
    highlights: List<ProfileHighlight> = emptyList(),
    posts: List<Post> = emptyList(),
    onBack: (() -> Unit)? = null,
    onOpenEditProfile: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenCreateStory: () -> Unit = {},
    onOpenAccountSwitcher: (() -> Unit)? = null,
    onPhotoOptionsClick: () -> Unit,
    onOpenCreateHighlight: () -> Unit = {},
    onHighlightClick: (ProfileHighlight) -> Unit = {},
    onHighlightLongClick: (ProfileHighlight) -> Unit = {},
    onPostClick: (Post) -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onSignOutClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedContentTab by remember { mutableIntStateOf(0) }
    var showShareSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BuddysTheme.colors.surfaceHeader,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BuddysTheme.colors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BuddysTheme.colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onOpenCreateStory,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create",
                        tint = BuddysTheme.colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center: Username with lock icon & subtle dropdown badge (Multi-account switcher trigger)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (onOpenAccountSwitcher != null) {
                            Modifier.clickable { onOpenAccountSwitcher() }
                        } else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (user?.isPrivate == true) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private",
                        tint = BuddysTheme.colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = user?.username?.ifBlank { user.displayName.ifBlank { "User" } } ?: "Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BuddysTheme.colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

                IconButton(
                    onClick = onOpenPrivacySettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Settings",
                        tint = BuddysTheme.colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Scrollable Profile Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            val updateManager = remember { com.aura.glasschat.data.update.UpdateManager.getInstance(context) }
            val availableUpdate by updateManager.availableUpdate.collectAsState()

            if (availableUpdate != null) {
                UpdateAvailableBanner(
                    manifest = availableUpdate!!,
                    onUpdateClick = { updateManager.requestUpdatePrompt(it) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Clean Social Profile Header (No nested card border)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                // Avatar + 3 Stats Columns Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Avatar with Story Ring + Edit Badge
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, BuddysTheme.colors.border, CircleShape)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AvatarView(
                                imageUrl = user?.avatarUrl,
                                displayName = user?.displayName ?: "Me",
                                size = 76.dp,
                                isOnline = true,
                                showHalo = false
                            )
                        }

                        // Quick Edit Camera Badge
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(BuddysTheme.colors.primaryRed)
                                .border(2.dp, BuddysTheme.colors.surface, CircleShape)
                                .clickable { onPhotoOptionsClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Change photo",
                                tint = BuddysTheme.colors.textOnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 3 Stats Columns: Posts, Followers, Following
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        ProfileStatColumn(
                            count = posts.size.coerceAtLeast(user?.postsCount ?: 0),
                            label = "Posts",
                            onClick = {}
                        )
                        ProfileStatColumn(
                            count = user?.followerCount ?: 0,
                            label = "Followers",
                            onClick = onOpenFollowers
                        )
                        ProfileStatColumn(
                            count = user?.followingCount ?: 0,
                            label = "Following",
                            onClick = onOpenFollowing
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // User Display Name and Bio
                Text(
                    text = user?.displayName ?: "Buddy",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Structured Bio
                val rawBio = if (!user?.bio.isNullOrBlank()) user!!.bio else if (!user?.statusMessage.isNullOrBlank()) user!!.statusMessage else "Connecting with buddies on Buddies."
                Text(
                    text = rawBio,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    )
                )

                // Link / Handle chip
                if (!user?.link.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { }
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = user?.link ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BuddysTheme.colors.primaryRed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row: [Edit profile] [Share profile]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BuddysOutlinedButton(
                        text = "Edit profile",
                        onClick = onOpenEditProfile,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    )

                    BuddysOutlinedButton(
                        text = "Share profile",
                        onClick = { showShareSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Profile Highlights Reel (Horizontal Scrolling Row)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "[+] New" Highlight Button (Owner only)
                item {
                    ProfileHighlightItem(
                        title = "New",
                        icon = Icons.Default.Add,
                        isAdd = true,
                        onClick = onOpenCreateHighlight
                    )
                }

                // Dynamic Highlights List
                items(highlights, key = { it.id }) { highlight ->
                    ProfileHighlightCoverItem(
                        highlight = highlight,
                        onClick = { onHighlightClick(highlight) },
                        onLongClick = { onHighlightLongClick(highlight) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Content Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BuddysTheme.colors.surface)
                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val tabs = listOf(
                    Icons.Default.GridOn to "Moments",
                    Icons.Default.PlayCircleOutline to "Reels",
                    Icons.Default.Repeat to "Shared",
                    Icons.Default.BookmarkBorder to "Saved"
                )

                tabs.forEachIndexed { index, (icon, label) ->
                    val isSelected = selectedContentTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BuddysTheme.colors.surfaceSecondary else Color.Transparent)
                            .clickable { selectedContentTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content Grid (3-Column Posts with Pinned Posts First)
            if (selectedContentTab == 0) {
                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BuddysTheme.colors.surface)
                            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BuddysEmptyState(
                            title = "No Moments yet",
                            subtitle = "Capture and share moments with your buddies.",
                            icon = Icons.Default.CameraAlt,
                            actionText = "Post a Moment",
                            onActionClick = onCreatePostClick
                        )
                    }
                } else {
                    // 3-Column Posts Grid
                    val chunkedPosts = remember(posts) { posts.chunked(3) }
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
                                            .clickable { onPostClick(post) }
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(post.mediaUrl),
                                            contentDescription = "Post Thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Subtle Pinned Post Indicator Badge (Top Right)
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

                                // Fill empty spaces if row has fewer than 3 items
                                repeat(3 - rowPosts.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BuddysTheme.colors.surface)
                        .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BuddysEmptyState(
                        title = when (selectedContentTab) {
                            1 -> "No Reels yet"
                            2 -> "No shared moments yet"
                            else -> "No saved moments yet"
                        },
                        subtitle = "Your moments and stories with buddies will appear here.",
                        icon = Icons.Default.CameraAlt,
                        actionText = "Post a Moment",
                        onActionClick = onCreatePostClick
                    )
                }
            }

            // Optional Sign Out button if standalone
            if (onSignOutClick != null) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onSignOutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BuddysTheme.colors.error
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        if (showShareSheet) {
            ShareProfileSheet(
                user = user,
                onDismiss = { showShareSheet = false }
            )
        }
    }
}

@Composable
private fun ProfileStatColumn(
    count: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 18.sp
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun ProfileHighlightItem(
    title: String,
    icon: ImageVector? = null,
    isAdd: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary)
                .border(1.2.dp, BuddysTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isAdd) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
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

@Composable
private fun ProfileHighlightCoverItem(
    highlight: ProfileHighlight,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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

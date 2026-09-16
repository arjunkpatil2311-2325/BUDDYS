package com.aura.glasschat.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import coil.compose.AsyncImage
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.update.UpdateManifest
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenEditProfile: () -> Unit = {},
    onOpenPrivacySettings: () -> Unit = {},
    onOpenFollowers: (userId: String) -> Unit = {},
    onOpenFollowing: (userId: String) -> Unit = {},
    onOpenCreateStory: () -> Unit = {},
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
                onBack = onBack,
                onOpenEditProfile = onOpenEditProfile,
                onOpenPrivacySettings = onOpenPrivacySettings,
                onOpenFollowers = { user?.let { onOpenFollowers(it.uid) } },
                onOpenFollowing = { user?.let { onOpenFollowing(it.uid) } },
                onOpenCreateStory = onOpenCreateStory,
                onPhotoOptionsClick = { viewModel.openPhotoOptions() },
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
    }
}

@Composable
fun BuddysFullProfileView(
    user: User?,
    onBack: (() -> Unit)? = null,
    onOpenEditProfile: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenCreateStory: () -> Unit = {},
    onPhotoOptionsClick: () -> Unit,
    onSignOutClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedContentTab by remember { mutableStateOf(0) }
    var showShareSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BuddysTheme.colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onOpenCreateStory,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create",
                        tint = BuddysTheme.colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center: Username with lock icon & subtle dropdown badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
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
                    text = user?.username?.ifBlank { "buddys_user" } ?: "buddys_user",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 17.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BuddysTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Right: Settings / Menu
            IconButton(
                onClick = onOpenPrivacySettings,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.surfaceSecondary)
                    .border(1.dp, BuddysTheme.colors.border, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Settings",
                    tint = BuddysTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
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

            // Profile Header Card
            BuddysCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
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
                                    .size(86.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, BuddysTheme.colors.primaryRed, CircleShape)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AvatarView(
                                    imageUrl = user?.avatarUrl,
                                    displayName = user?.displayName ?: "Me",
                                    size = 80.dp,
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

                        // 3 Stats Columns: Moments, Followers, Following
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            ProfileStatColumn(
                                count = user?.postsCount ?: 0,
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

                    // User Names and Bio
                    Text(
                        text = user?.displayName ?: "Buddy",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textPrimary,
                            fontSize = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Multi-line Structured Bio
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BuddysTheme.colors.surfaceSecondary)
                                .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = user?.link ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BuddysTheme.colors.primaryRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                                .height(38.dp)
                        )

                        BuddysOutlinedButton(
                            text = "Share profile",
                            onClick = { showShareSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        )

                        IconButton(
                            onClick = onOpenPrivacySettings,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BuddysTheme.colors.surfaceSecondary)
                                .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add buddies",
                                tint = BuddysTheme.colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Story Highlights Reel matching Reference 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // "+ New" Highlight
                ProfileHighlightItem(
                    title = "New",
                    icon = Icons.Default.Add,
                    isAdd = true,
                    onClick = onOpenCreateStory
                )
                ProfileHighlightItem(
                    title = "Moments",
                    icon = Icons.Default.CameraAlt,
                    onClick = {}
                )
                ProfileHighlightItem(
                    title = "Buddies",
                    icon = Icons.Default.Group,
                    onClick = {}
                )
                ProfileHighlightItem(
                    title = "Saved",
                    icon = Icons.Default.Bookmark,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

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

            // Content Grid / Modern Social Empty State
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
                        3 -> "No saved moments yet"
                        else -> "No Moments yet"
                    },
                    subtitle = "Your moments and stories with buddies will appear here.",
                    icon = Icons.Default.CameraAlt,
                    actionText = "Post a Moment",
                    onActionClick = onOpenCreateStory
                )
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
    emoji: String? = null,
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
                .size(60.dp)
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary)
                .border(1.dp, BuddysTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BuddysTheme.colors.primaryRed,
                    modifier = Modifier.size(24.dp)
                )
            } else if (emoji != null) {
                Text(text = emoji, fontSize = 24.sp)
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


package com.aura.glasschat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    var showMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("Spam") }
    var reportDetails by remember { mutableStateOf("") }

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
                                    // Follow / Following / Requested / Follow Back Button
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

                                    // Message Button
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

                    // Private Account Notice (if applicable)
                    if (user.isPrivate && uiState.relationshipState != RelationshipState.FOLLOWING && uiState.relationshipState != RelationshipState.MUTUAL && uiState.currentUserId != user.uid) {
                        Spacer(modifier = Modifier.height(16.dp))
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
                    }
                }
            }
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

package com.aura.glasschat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aura.glasschat.data.model.Friend
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.UserRepository
import com.aura.glasschat.ui.theme.BuddysTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloseFriendsScreen(
    onBack: () -> Unit,
    userRepository: UserRepository = remember { UserRepository() },
    authRepository: AuthRepository = remember { AuthRepository() }
) {
    val currentUserId = authRepository.currentUserId
    val userProfile by userRepository.observeUserProfile(currentUserId).collectAsState(initial = null)
    val friendsList by userRepository.observeFriends(currentUserId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val colors = BuddysTheme.colors

    val closeFriendsSet = remember(userProfile?.closeFriends) {
        userProfile?.closeFriends?.toSet() ?: emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFF22A06B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Close Friends",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.textPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface
                )
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header explanation
            Surface(
                color = colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "We won't send notifications when you edit your Close Friends list.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${closeFriendsSet.size} selected",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22A06B)
                    )
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            if (friendsList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No friends found. Connect with buddies first!",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(friendsList, key = { it.friendUid }) { friend ->
                        val isClose = closeFriendsSet.contains(friend.friendUid)
                        FriendCloseFriendItem(
                            friend = friend,
                            isCloseFriend = isClose,
                            onToggle = {
                                coroutineScope.launch {
                                    userRepository.toggleCloseFriend(
                                        userId = currentUserId,
                                        friendId = friend.friendUid,
                                        isCurrentlyClose = isClose
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendCloseFriendItem(
    friend: Friend,
    isCloseFriend: Boolean,
    onToggle: () -> Unit
) {
    val colors = BuddysTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = friend.avatarUrl,
            contentDescription = friend.displayName,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surfaceSecondary)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.displayName.ifBlank { friend.username },
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.textPrimary
            )
            if (friend.username.isNotBlank()) {
                Text(
                    text = "@${friend.username}",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        IconButton(onClick = onToggle) {
            if (isCloseFriend) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF22A06B),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Not selected",
                    tint = colors.textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

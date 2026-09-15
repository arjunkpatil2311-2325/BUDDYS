package com.aura.glasschat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.glasschat.data.model.Follow
import com.aura.glasschat.data.repository.FollowRepository
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.BuddysTheme

@Composable
fun FollowersScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    followRepository: FollowRepository = remember { FollowRepository() }
) {
    val followers by followRepository.observeFollowers(userId).collectAsState(initial = emptyList())

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            BuddysTopBar(
                title = "Followers",
                onBack = onBack
            )

            if (followers.isEmpty()) {
                BuddysEmptyState(
                    title = "No followers yet",
                    subtitle = "When people follow this profile, they will appear here.",
                    icon = Icons.Default.Group,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(followers, key = { it.uid }) { follow ->
                        FollowUserRow(
                            follow = follow,
                            onClick = { onOpenProfile(follow.uid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FollowUserRow(
    follow: Follow,
    onClick: () -> Unit
) {
    BuddysCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                imageUrl = follow.avatarUrl,
                displayName = follow.displayName.ifBlank { follow.username },
                size = 46.dp,
                isOnline = false
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = follow.displayName.ifBlank { follow.username },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${follow.username}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

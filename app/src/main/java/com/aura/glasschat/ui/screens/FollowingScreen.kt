package com.aura.glasschat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.glasschat.data.repository.FollowRepository
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.BuddysTheme

@Composable
fun FollowingScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    followRepository: FollowRepository = remember { FollowRepository() }
) {
    val following by followRepository.observeFollowing(userId).collectAsState(initial = emptyList())

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
                title = "Following",
                onBack = onBack
            )

            if (following.isEmpty()) {
                BuddysEmptyState(
                    title = "Not following anyone yet",
                    subtitle = "People followed by this account will show up here.",
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
                    items(following, key = { it.uid }) { follow ->
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

package com.aura.glasschat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.data.model.AppNotification
import com.aura.glasschat.data.model.FollowRequest
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.NotificationsViewModel
import com.aura.glasschat.util.ChatUtils

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                title = "Activity",
                onBack = onBack
            )

            if (uiState.notifications.isEmpty() && uiState.pendingRequests.isEmpty()) {
                BuddysEmptyState(
                    title = "All caught up",
                    subtitle = "Follow requests and new notifications will appear here.",
                    icon = Icons.Default.Notifications,
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
                    // Pending Follow Requests Section
                    if (uiState.pendingRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = "FOLLOW REQUESTS (${uiState.pendingRequests.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BuddysTheme.colors.primaryRed,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(uiState.pendingRequests, key = { it.requestId }) { request ->
                            FollowRequestCard(
                                request = request,
                                onOpenProfile = { onOpenProfile(request.requesterUid) },
                                onAccept = { viewModel.acceptRequest(request) },
                                onDecline = { viewModel.declineRequest(request.requestId) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "RECENT ACTIVITY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BuddysTheme.colors.textSecondary,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                    }

                    // Activity Notifications List
                    items(uiState.notifications, key = { it.id }) { notif ->
                        NotificationCard(
                            notification = notif,
                            onClick = {
                                viewModel.markAsRead(notif.id)
                                onOpenProfile(notif.actorUid)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FollowRequestCard(
    request: FollowRequest,
    onOpenProfile: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    BuddysCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clickable(onClick = onOpenProfile)) {
                AvatarView(
                    imageUrl = request.requesterAvatarUrl,
                    displayName = request.requesterDisplayName.ifBlank { request.requesterUsername },
                    size = 46.dp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenProfile)
            ) {
                Text(
                    text = request.requesterDisplayName.ifBlank { request.requesterUsername },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "wants to follow you",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textSecondary,
                        fontSize = 12.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.surfaceSecondary)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Decline", tint = BuddysTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.primaryRed)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = BuddysTheme.colors.textOnPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val isUnread = !notification.isRead

    BuddysCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = if (isUnread) BuddysTheme.colors.softRed.copy(alpha = 0.5f) else BuddysTheme.colors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                imageUrl = notification.actorAvatarUrl,
                displayName = notification.actorDisplayName.ifBlank { notification.actorUsername },
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val notifMessage = when (notification.type) {
                    "FOLLOW_REQUEST" -> "requested to follow you"
                    "FOLLOW_ACCEPT" -> "accepted your follow request"
                    else -> "started following you"
                }
                Text(
                    text = "${notification.actorDisplayName.ifBlank { notification.actorUsername }} $notifMessage",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = ChatUtils.formatTimestamp(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BuddysTheme.colors.textMuted,
                        fontSize = 11.sp
                    )
                )
            }

            if (isUnread) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.primaryRed)
                )
            }
        }
    }
}

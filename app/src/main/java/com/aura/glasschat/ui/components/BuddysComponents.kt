package com.aura.glasschat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.aura.glasschat.ui.theme.*

// ====================================================================
// BUDDYS PREMIUM COMPONENT SYSTEM
// ====================================================================

/**
 * Standard 3D Surface Card (14dp rounded corners, 1dp border, subtle surface elevation).
 */
@Composable
fun BuddysCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BuddysTheme.colors.surface,
    borderColor: Color = BuddysTheme.colors.border,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    elevation: Dp = 1.5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    ThreeDSurface(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        elevation = elevation,
        content = content
    )
}

/**
 * Primary Brand Action Button (BUDDYS Red 3D button).
 */
@Composable
fun BuddysButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    containerColor: Color = BuddysTheme.colors.primaryRed,
    contentColor: Color = BuddysTheme.colors.textOnPrimary
) {
    ThreeDButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        leadingIcon = leadingIcon,
        containerColor = containerColor,
        contentColor = contentColor
    )
}

/**
 * Secondary Outlined 3D Button.
 */
@Composable
fun BuddysOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    borderColor: Color = BuddysTheme.colors.border,
    textColor: Color = BuddysTheme.colors.textPrimary
) {
    ThreeDOutlinedButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        borderColor = borderColor,
        textColor = textColor
    )
}

/**
 * Standard Header Top Bar with 3D depth rim.
 */
@Composable
fun BuddysTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    ThreeDTopBar(
        title = title,
        onBack = onBack,
        modifier = modifier,
        actions = actions
    )
}

/**
 * Clean Section Header with Optional Action Link.
 */
@Composable
fun BuddysSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = BuddysTheme.colors.textSecondary,
                    fontSize = 12.5.sp
                )
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = BuddysTheme.colors.primaryRed,
                    fontSize = 13.sp
                ),
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

/**
 * Clean Modern Search Input Field.
 */
@Composable
fun BuddysSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BuddysTheme.colors.surfaceSecondary)
            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
            .then(if (onSearchClick != null) Modifier.clickable { onSearchClick() } else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = BuddysTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        if (onSearchClick != null) {
            Text(
                text = if (query.isBlank()) placeholder else query,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (query.isBlank()) BuddysTheme.colors.textMuted else BuddysTheme.colors.textPrimary,
                    fontSize = 14.sp
                ),
                modifier = Modifier.weight(1f)
            )
        } else {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 14.sp
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BuddysTheme.colors.textMuted,
                                fontSize = 14.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
        if (query.isNotEmpty() && onSearchClick == null) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = BuddysTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Typography-led, Non-Cartoon Empty State.
 */
@Composable
fun BuddysEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    stickerType: StickerType = StickerType.BRAND_EMBLEM,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.surfaceSecondary)
                    .border(1.dp, BuddysTheme.colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BuddysTheme.colors.primaryRed,
                    modifier = Modifier.size(26.dp)
                )
            }
        } else {
            BuddysSticker(type = stickerType, size = 48.dp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 17.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BuddysTheme.colors.textSecondary,
                fontSize = 13.5.sp,
                lineHeight = 19.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            BuddysButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.wrapContentWidth()
            )
        }
    }
}

/**
 * Story Ring for active stories (Instagram + Snapchat hybrid with dual-gradient ring).
 */
@Composable
fun BuddysStoryRing(
    imageUrl: String?,
    displayName: String,
    size: Dp = 68.dp,
    hasUnreadStory: Boolean = false,
    isSelf: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (hasUnreadStory) {
                    Modifier.border(
                        BorderStroke(2.5.dp, StoryRingGradient),
                        CircleShape
                    )
                } else {
                    Modifier.border(
                        BorderStroke(1.dp, BuddysTheme.colors.border),
                        CircleShape
                    )
                }
            )
            .padding(if (hasUnreadStory) 3.5.dp else 1.5.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.BottomEnd
    ) {
        AvatarView(
            imageUrl = imageUrl,
            displayName = displayName,
            size = size - (if (hasUnreadStory) 7.dp else 3.dp)
        )

        if (isSelf) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.primaryRed)
                    .border(2.dp, BuddysTheme.colors.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add story",
                    tint = BuddysTheme.colors.textOnPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Clean Badge pill component.
 */
@Composable
fun BuddysBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BuddysTheme.colors.primaryRed,
    textColor: Color = BuddysTheme.colors.textOnPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        )
    }
}

/**
 * Snapchat-style chat status indicator icon.
 */
enum class ChatDeliveryType {
    TEXT_SENT,
    TEXT_DELIVERED,
    TEXT_OPENED,
    MEDIA_SENT,
    MEDIA_DELIVERED,
    MEDIA_OPENED,
    VOICE_SENT,
    VOICE_DELIVERED,
    VOICE_OPENED,
    SNAP_RECEIVED,
    SNAP_OPENED,
    CHAT_RECEIVED,
    CHAT_OPENED
}

@Composable
fun BuddysChatStatusIcon(
    type: ChatDeliveryType,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    val color = when (type) {
        ChatDeliveryType.TEXT_SENT, ChatDeliveryType.TEXT_DELIVERED, ChatDeliveryType.TEXT_OPENED,
        ChatDeliveryType.CHAT_RECEIVED, ChatDeliveryType.CHAT_OPENED -> Color(0xFF0084FF)
        ChatDeliveryType.MEDIA_SENT, ChatDeliveryType.MEDIA_DELIVERED, ChatDeliveryType.MEDIA_OPENED,
        ChatDeliveryType.SNAP_RECEIVED, ChatDeliveryType.SNAP_OPENED -> Color(0xFFFF2A55)
        ChatDeliveryType.VOICE_SENT, ChatDeliveryType.VOICE_DELIVERED, ChatDeliveryType.VOICE_OPENED -> Color(0xFFA855F7)
    }

    val isArrow = when (type) {
        ChatDeliveryType.TEXT_SENT, ChatDeliveryType.TEXT_DELIVERED, ChatDeliveryType.TEXT_OPENED,
        ChatDeliveryType.MEDIA_SENT, ChatDeliveryType.MEDIA_DELIVERED, ChatDeliveryType.MEDIA_OPENED,
        ChatDeliveryType.VOICE_SENT, ChatDeliveryType.VOICE_DELIVERED, ChatDeliveryType.VOICE_OPENED -> true
        else -> false
    }

    val isFilled = when (type) {
        ChatDeliveryType.TEXT_SENT, ChatDeliveryType.TEXT_DELIVERED,
        ChatDeliveryType.MEDIA_SENT, ChatDeliveryType.MEDIA_DELIVERED,
        ChatDeliveryType.VOICE_SENT, ChatDeliveryType.VOICE_DELIVERED,
        ChatDeliveryType.SNAP_RECEIVED, ChatDeliveryType.CHAT_RECEIVED -> true
        else -> false
    }

    if (isArrow) {
        androidx.compose.foundation.Canvas(
            modifier = modifier.size(size)
        ) {
            val width = this.size.width
            val height = this.size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(width * 0.15f, height * 0.1f)
                lineTo(width * 0.90f, height * 0.5f)
                lineTo(width * 0.15f, height * 0.9f)
                close()
            }
            if (isFilled) {
                drawPath(path = path, color = color)
            } else {
                drawPath(
                    path = path,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isFilled) color else Color.Transparent)
                .border(
                    1.5.dp,
                    color,
                    RoundedCornerShape(3.dp)
                )
        )
    }
}

/**
 * Modern Segmented Tab Pill for feeds and search filters with 3D tactile depth.
 */
@Composable
fun BuddysTabPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    leadingIcon: ImageVector? = null,
    trailingEmoji: String? = null
) {
    ThreeDTabPill(
        text = text,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        count = count,
        leadingIcon = leadingIcon,
        trailingEmoji = trailingEmoji
    )
}

/**
 * Modern 4-Tab Navigation Bar with 3D lifted states.
 */
@Composable
fun BuddysBottomNavigationBar(
    selectedTab: com.aura.glasschat.ui.screens.HomeBottomTab,
    onTabSelected: (com.aura.glasschat.ui.screens.HomeBottomTab) -> Unit,
    userAvatarUrl: String?,
    userDisplayName: String,
    unreadChatsCount: Int = 0,
    hasUnreadUpdates: Boolean = false,
    missedCallsCount: Int = 0,
    onProfileDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ThreeDBottomBar(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        userAvatarUrl = userAvatarUrl,
        userDisplayName = userDisplayName,
        unreadChatsCount = unreadChatsCount,
        hasUnreadUpdates = hasUnreadUpdates,
        missedCallsCount = missedCallsCount,
        onProfileDoubleTap = onProfileDoubleTap,
        modifier = modifier
    )
}

/**
 * Standard Follow Button with 3 states (NOT_FOLLOWING, FOLLOWING, REQUESTED).
 */
@Composable
fun BuddysFollowButton(
    status: com.aura.glasschat.data.repository.FollowStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (status) {
        com.aura.glasschat.data.repository.FollowStatus.FOLLOWING -> {
            BuddysOutlinedButton(
                text = "Following",
                onClick = onClick,
                modifier = modifier.height(36.dp)
            )
        }
        com.aura.glasschat.data.repository.FollowStatus.REQUESTED -> {
            BuddysOutlinedButton(
                text = "Requested",
                onClick = onClick,
                modifier = modifier.height(36.dp),
                textColor = BuddysTheme.colors.textMuted
            )
        }
        else -> {
            BuddysButton(
                text = "Follow",
                onClick = onClick,
                modifier = modifier.height(36.dp)
            )
        }
    }
}

/**
 * Social Feed Moment Card (Instagram/Snapchat hybrid style).
 * Supports author avatar with story ring, double tap like, media preview, interactive reactions, comments, bookmarking.
 */
@Composable
fun SocialFeedMomentCard(
    authorName: String,
    authorHandle: String,
    authorAvatarUrl: String? = null,
    timeAgo: String,
    caption: String,
    mediaUrl: String? = null,
    likeCount: Int = 0,
    commentCount: Int = 0,
    isLiked: Boolean = false,
    isBookmarked: Boolean = false,
    onLikeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onAuthorClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BuddysCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        backgroundColor = BuddysTheme.colors.surface,
        borderColor = BuddysTheme.colors.border,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 1. Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onAuthorClick != null) { onAuthorClick?.invoke() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Story Ring gradient
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(StoryRingGradient)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(BuddysTheme.colors.surface)
                            .padding(1.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!authorAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = authorAvatarUrl,
                                contentDescription = authorName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(BuddysTheme.colors.surfaceSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = authorName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BuddysTheme.colors.primaryRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BuddysTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (authorHandle.startsWith("@")) authorHandle else "@$authorHandle • $timeAgo",
                        fontSize = 12.sp,
                        color = BuddysTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = BuddysTheme.colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Caption
            if (caption.isNotBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = BuddysTheme.colors.textPrimary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 3. Media Preview (if present)
            if (!mediaUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { onLikeClick() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Moment media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 4. Action Bar (Like, Comment, Direct Share, Bookmark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Like button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onLikeClick)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        if (likeCount > 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = likeCount.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLiked) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textSecondary
                            )
                        }
                    }

                    // Comment button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCommentClick)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = BuddysTheme.colors.textSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                        if (commentCount > 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = commentCount.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = BuddysTheme.colors.textSecondary
                            )
                        }
                    }

                    // Share button
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Share",
                            tint = BuddysTheme.colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Bookmark button
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) BuddysTheme.colors.warning else BuddysTheme.colors.textSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}



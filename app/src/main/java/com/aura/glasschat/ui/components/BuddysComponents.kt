package com.aura.glasschat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aura.glasschat.ui.theme.*

// ====================================================================
// BUDDIES RETRO CARTOON COMPONENT SYSTEM — PAPER & INK
// ====================================================================

/**
 * Illustrated Paper Surface Card with bold 1.5dp ink outline and flat paper fill.
 */
@Composable
fun BuddysCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BuddysTheme.colors.surface,
    borderColor: Color = BuddysTheme.colors.border,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 1.5.dp,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
    ) {
        content()
    }
}

/**
 * Primary Illustrated Action Button (Orange fill + 1.5dp dark ink outline).
 */
@Composable
fun BuddysButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    containerColor: Color = BuddysTheme.colors.primaryAccent,
    contentColor: Color = BuddysTheme.colors.textOnPrimary,
    borderColor: Color = BuddysTheme.colors.border,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    ThreeDButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        leadingIcon = leadingIcon,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = shape
    )
}

/**
 * Secondary Illustrated Outlined Button (Cream/White paper + 1.5dp dark ink outline).
 */
@Composable
fun BuddysOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    borderColor: Color = BuddysTheme.colors.border,
    textColor: Color = BuddysTheme.colors.textPrimary,
    backgroundColor: Color = BuddysTheme.colors.surface,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    ThreeDOutlinedButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        borderColor = borderColor,
        textColor = textColor,
        backgroundColor = backgroundColor
    )
}

/**
 * Illustrated Paper Header Top Bar with bold bottom ink divider.
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
 * Illustrated Section Header with optional Yellow Accent Tag.
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BuddysTheme.colors.yellowHeader)
                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 12.sp
                    )
                )
            }
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textPrimary,
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
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.primaryAccent,
                    fontSize = 13.sp
                ),
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

/**
 * Illustrated Outlined Search Box (Yellow / Cream fill + 1.5dp ink outline).
 */
@Composable
fun BuddysSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search people, posts, topics...",
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BuddysTheme.colors.surfaceSecondary)
            .border(1.5.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
            .then(if (onSearchClick != null) Modifier.clickable { onSearchClick() } else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = BuddysTheme.colors.textPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        if (onSearchClick != null) {
            Text(
                text = if (query.isBlank()) placeholder else query,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (query.isBlank()) BuddysTheme.colors.textMuted else BuddysTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
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
                    tint = BuddysTheme.colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Illustrated Empty State for Notebook Pages.
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
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.yellowHeader)
                    .border(1.5.dp, BuddysTheme.colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BuddysTheme.colors.textPrimary,
                    modifier = Modifier.size(28.dp)
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
                fontSize = 18.sp
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
 * Illustrated Hand-Drawn Story Ring with 2dp ink border & yellow/orange accent.
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
            .background(if (hasUnreadStory) BuddysTheme.colors.yellowHeader else BuddysTheme.colors.surfaceSecondary)
            .border(
                BorderStroke(2.dp, BuddysTheme.colors.border),
                CircleShape
            )
            .padding(3.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.BottomEnd
    ) {
        AvatarView(
            imageUrl = imageUrl,
            displayName = displayName,
            size = size - 8.dp
        )

        if (isSelf) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.primaryAccent)
                    .border(1.5.dp, BuddysTheme.colors.border, CircleShape),
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
 * Illustrated Paper Badge Pill.
 */
@Composable
fun BuddysBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BuddysTheme.colors.primaryAccent,
    textColor: Color = BuddysTheme.colors.textOnPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(8.dp))
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
 * Snapchat/Buddies Delivery Status Indicator with illustrated ink strokes.
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
        ChatDeliveryType.CHAT_RECEIVED, ChatDeliveryType.CHAT_OPENED -> BuddysTheme.colors.primaryAccent
        ChatDeliveryType.MEDIA_SENT, ChatDeliveryType.MEDIA_DELIVERED, ChatDeliveryType.MEDIA_OPENED,
        ChatDeliveryType.SNAP_RECEIVED, ChatDeliveryType.SNAP_OPENED -> Color(0xFFF46A21)
        ChatDeliveryType.VOICE_SENT, ChatDeliveryType.VOICE_DELIVERED, ChatDeliveryType.VOICE_OPENED -> Color(0xFFFF8A3D)
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
                drawPath(
                    path = path,
                    color = Color(0xFF171717),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
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
                    1.2.dp,
                    BuddysTheme.colors.border,
                    RoundedCornerShape(3.dp)
                )
        )
    }
}

/**
 * Illustrated Segmented Tab Pill with Paper & Ink border.
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
 * Illustrated 5-Tab Navigation Bar with Paper Background, Ink Top Border, and Yellow Highlights.
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
 * Illustrated Follow Button with solid ink outline.
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
 * Illustrated Social Feed Post Item with 1.5dp dark ink outline, flat paper card composition,
 * author avatar, media with border, and hand-drawn reaction row.
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BuddysTheme.colors.surface)
            .border(1.5.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // 1. Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clickable(enabled = onAuthorClick != null) { onAuthorClick?.invoke() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with illustrated 1.5dp ink outline
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.yellowHeader)
                        .border(1.5.dp, BuddysTheme.colors.border, CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarView(
                        imageUrl = authorAvatarUrl,
                        displayName = authorName,
                        size = 36.dp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
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
                    tint = BuddysTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Media Preview with solid 1.5dp ink border
            if (!mediaUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(280.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.5.dp, BuddysTheme.colors.border, RoundedCornerShape(10.dp))
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

            // 3. Illustrated Action Bar (Like, Comment, Direct Share, Bookmark)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Like button
                    Row(
                        modifier = Modifier.clickable(onClick = onLikeClick),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) BuddysTheme.colors.primaryAccent else BuddysTheme.colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        if (likeCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = likeCount.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiked) BuddysTheme.colors.primaryAccent else BuddysTheme.colors.textPrimary
                            )
                        }
                    }

                    // Comment button
                    Row(
                        modifier = Modifier.clickable(onClick = onCommentClick),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = BuddysTheme.colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        if (commentCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = commentCount.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.textPrimary
                            )
                        }
                    }

                    // Share button
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Share",
                        tint = BuddysTheme.colors.textPrimary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(onClick = onShareClick)
                    )
                }

                // Bookmark button
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) BuddysTheme.colors.primaryAccent else BuddysTheme.colors.textPrimary,
                    modifier = Modifier
                        .size(23.dp)
                        .clickable(onClick = onBookmarkClick)
                )
            }

            // 4. Caption & Details
            if (caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = authorName + " ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BuddysTheme.colors.textPrimary
                    )
                    Text(
                        text = caption,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = BuddysTheme.colors.textPrimary
                    )
                }
            }

            if (commentCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "View all $commentCount comments",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = BuddysTheme.colors.textSecondary,
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .clickable { onCommentClick() }
                )
            }
        }
    }
}

/**
 * Illustrated Paper Settings Row with thin ink divider.
 */
@Composable
fun BuddysSettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = BuddysTheme.colors.textPrimary,
    badge: String? = null,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BuddysTheme.colors.surfaceSecondary)
                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 14.5.sp
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }

        if (!badge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BuddysTheme.colors.primaryAccent)
                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BuddysTheme.colors.textMuted,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = BuddysTheme.colors.textPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Illustrated Paper Settings Section Card with 1.5dp ink outline.
 */
@Composable
fun BuddysSettingSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = BuddysTheme.colors.textSecondary,
                    letterSpacing = 0.8.sp,
                    fontSize = 11.5.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BuddysTheme.colors.surface)
                .border(1.5.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Illustrated Switch Row.
 */
@Composable
fun BuddysSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BuddysTheme.colors.surfaceSecondary)
                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BuddysTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 14.5.sp
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BuddysTheme.colors.primaryAccent,
                uncheckedThumbColor = BuddysTheme.colors.textSecondary,
                uncheckedTrackColor = BuddysTheme.colors.surfaceSecondary
            )
        )
    }
}

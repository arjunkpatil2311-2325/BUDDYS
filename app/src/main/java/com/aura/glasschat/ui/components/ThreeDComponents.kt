package com.aura.glasschat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aura.glasschat.ui.screens.HomeBottomTab
import com.aura.glasschat.ui.theme.*

// ====================================================================
// BUDDYS 3D DESIGN SYSTEM — CORE TACTILE MODIFIERS & SURFACES
// ====================================================================

/**
 * Interactive 3D Press Scale with Spring Physics.
 * Scales down smoothly on press and bounces back instantly upon release.
 */
fun Modifier.threeDPress(
    enabled: Boolean = true,
    targetScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "threeDScale"
    )

    this
        .scale(scale)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * 3D Surface with soft depth, clean border rim, and subtle elevation.
 */
@Composable
fun ThreeDSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = BuddysTheme.colors.surface,
    borderColor: Color = BuddysTheme.colors.border,
    elevation: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (BuddysTheme.colors.isDark) Color.Black.copy(alpha = 0.5f) else Color(0x10000000),
                spotColor = if (BuddysTheme.colors.isDark) Color.Black.copy(alpha = 0.7f) else Color(0x18000000)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

/**
 * 3D Card with layered depth and optional click handling.
 */
@Composable
fun ThreeDCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    backgroundColor: Color = BuddysTheme.colors.surface,
    borderColor: Color = BuddysTheme.colors.border,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val baseModifier = if (onClick != null) {
        modifier.threeDPress(onClick = onClick)
    } else {
        modifier
    }

    ThreeDSurface(
        modifier = baseModifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        elevation = elevation,
        content = content
    )
}

// ====================================================================
// 3D BUTTONS & CONTROLS
// ====================================================================

/**
 * Primary 3D Action Button (BUDDYS Red gradient with realistic depth & spring).
 */
@Composable
fun ThreeDButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    containerColor: Color = BuddysTheme.colors.primaryRed,
    contentColor: Color = BuddysTheme.colors.textOnPrimary,
    elevation: Dp = 3.dp,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )

    val currentElevation = if (isPressed) 1.dp else elevation

    Box(
        modifier = modifier
            .height(50.dp)
            .scale(scale)
            .shadow(
                elevation = if (enabled) currentElevation else 0.dp,
                shape = shape,
                ambientColor = containerColor.copy(alpha = 0.35f),
                spotColor = containerColor.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(
                        colors = listOf(
                            containerColor,
                            containerColor.copy(alpha = 0.92f)
                        )
                    )
                } else {
                    SolidColor(containerColor.copy(alpha = 0.35f))
                }
            )
            .border(
                1.dp,
                if (enabled) containerColor.copy(alpha = 0.8f) else Color.Transparent,
                shape
            )
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.3.sp,
                        color = if (enabled) contentColor else contentColor.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

/**
 * Secondary Outlined 3D Button.
 */
@Composable
fun ThreeDOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    borderColor: Color = BuddysTheme.colors.border,
    textColor: Color = BuddysTheme.colors.textPrimary,
    backgroundColor: Color = BuddysTheme.colors.surface
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "outlineBtnScale"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 0.5.dp else 1.5.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = if (BuddysTheme.colors.isDark) Color.Black.copy(alpha = 0.4f) else Color(0x0C000000)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = textColor
                )
            )
        }
    }
}

/**
 * 3D Icon Button with circular depth surface.
 */
@Composable
fun ThreeDIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    containerColor: Color = BuddysTheme.colors.surface,
    tint: Color = BuddysTheme.colors.textPrimary,
    borderColor: Color = BuddysTheme.colors.border,
    elevation: Dp = 1.5.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "iconBtnScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 0.5.dp else elevation,
                shape = CircleShape,
                ambientColor = if (BuddysTheme.colors.isDark) Color.Black.copy(alpha = 0.5f) else Color(0x10000000)
            )
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * 3D Floating Action Button (FAB) with soft glow depth.
 */
@Composable
fun ThreeDFloatingButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = BuddysTheme.colors.primaryRed,
    contentColor: Color = Color.White,
    size: Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "fabScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 3.dp else 6.dp,
                shape = CircleShape,
                ambientColor = containerColor.copy(alpha = 0.4f),
                spotColor = containerColor.copy(alpha = 0.5f)
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        containerColor,
                        containerColor.copy(alpha = 0.9f)
                    )
                )
            )
            .border(1.5.dp, containerColor.copy(alpha = 0.9f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

// ====================================================================
// 3D TOP BAR & BOTTOM NAVIGATION
// ====================================================================

/**
 * 3D Top App Bar with subtle depth rim.
 */
@Composable
fun ThreeDTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                ambientColor = if (BuddysTheme.colors.isDark) Color.Black.copy(alpha = 0.4f) else Color(0x0C000000)
            ),
        color = BuddysTheme.colors.surface,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        border = BorderStroke(1.dp, BuddysTheme.colors.border.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                ThreeDIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    size = 38.dp,
                    iconSize = 18.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 19.sp
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions()
                }
            }
        }
    }
}

/**
 * Premium 3D Bottom Navigation Bar with lifted destination pills and smooth springs (5-Tab Instagram + Snapchat navigation).
 */
@Composable
fun ThreeDBottomBar(
    selectedTab: HomeBottomTab,
    onTabSelected: (HomeBottomTab) -> Unit,
    userAvatarUrl: String?,
    userDisplayName: String,
    unreadChatsCount: Int = 0,
    hasUnreadUpdates: Boolean = false,
    missedCallsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                ambientColor = if (BuddysTheme.colors.isDark) Color.Black.copy(alpha = 0.6f) else Color(0x14000000)
            ),
        color = BuddysTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, BuddysTheme.colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TAB 1: HOME (FEED & STORIES)
            val isHome = selectedTab == HomeBottomTab.HOME
            ThreeDNavTabItem(
                selected = isHome,
                label = "Home",
                icon = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                showDotBadge = hasUnreadUpdates,
                onClick = { onTabSelected(HomeBottomTab.HOME) }
            )

            // TAB 2: SEARCH / EXPLORE
            val isSearch = selectedTab == HomeBottomTab.SEARCH
            ThreeDNavTabItem(
                selected = isSearch,
                label = "Search",
                icon = if (isSearch) Icons.Filled.Search else Icons.Outlined.Search,
                onClick = { onTabSelected(HomeBottomTab.SEARCH) }
            )

            // TAB 3: CREATE (STORY & MOMENT)
            val isCreate = selectedTab == HomeBottomTab.CREATE
            ThreeDNavTabItem(
                selected = isCreate,
                label = "Create",
                icon = if (isCreate) Icons.Filled.AddCircle else Icons.Outlined.AddCircle,
                onClick = { onTabSelected(HomeBottomTab.CREATE) }
            )

            // TAB 4: INBOX (DIRECT MESSAGES)
            val isInbox = selectedTab == HomeBottomTab.INBOX
            ThreeDNavTabItem(
                selected = isInbox,
                label = "Inbox",
                icon = if (isInbox) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                badgeCount = unreadChatsCount,
                onClick = { onTabSelected(HomeBottomTab.INBOX) }
            )

            // TAB 5: PROFILE
            val isProfile = selectedTab == HomeBottomTab.PROFILE
            ThreeDNavTabItem(
                selected = isProfile,
                label = "Profile",
                icon = if (isProfile) Icons.Filled.Person else Icons.Outlined.Person,
                avatarUrl = userAvatarUrl,
                avatarName = userDisplayName,
                onClick = { onTabSelected(HomeBottomTab.PROFILE) }
            )
        }
    }
}

@Composable
private fun ThreeDNavTabItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    avatarUrl: String? = null,
    avatarName: String? = null,
    badgeCount: Int = 0,
    showDotBadge: Boolean = false,
    onClick: () -> Unit
) {
    val activeColor = BuddysTheme.colors.primaryRed
    val inactiveColor = BuddysTheme.colors.textMuted

    val liftOffset by animateDpAsState(
        targetValue = if (selected) (-3).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "tabLift"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .offset(y = liftOffset)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null || (avatarName != null && selected)) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            if (selected) activeColor else BuddysTheme.colors.border,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarView(
                        imageUrl = avatarUrl,
                        displayName = avatarName ?: "You",
                        size = 24.dp
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) activeColor else inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .padding(horizontal = 4.5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }
            } else if (showDotBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeColor else inactiveColor,
                fontSize = 11.sp,
                letterSpacing = (-0.1).sp
            )
        )
    }
}

// ====================================================================
// 3D TABS & SEGMENTED PILLS
// ====================================================================

/**
 * 3D Segmented Tab Pill with tactile depth.
 */
@Composable
fun ThreeDTabPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    leadingIcon: ImageVector? = null,
    trailingEmoji: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "pillScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 2.dp else 0.5.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isSelected) BuddysTheme.colors.primaryRed.copy(alpha = 0.25f) else Color.Transparent
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    Brush.verticalGradient(
                        colors = listOf(
                            BuddysTheme.colors.textPrimary,
                            BuddysTheme.colors.textPrimary.copy(alpha = 0.9f)
                        )
                    )
                } else {
                    SolidColor(BuddysTheme.colors.surfaceSecondary)
                }
            )
            .border(
                1.dp,
                if (isSelected) BuddysTheme.colors.textPrimary else BuddysTheme.colors.border,
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isSelected) BuddysTheme.colors.surface else BuddysTheme.colors.textPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) BuddysTheme.colors.surface else BuddysTheme.colors.textPrimary,
                    fontSize = 13.sp
                )
            )
            if (trailingEmoji != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = trailingEmoji, fontSize = 12.sp)
            }
            if (count != null && count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) BuddysTheme.colors.primaryRed else BuddysTheme.colors.primaryRed.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) BuddysTheme.colors.textOnPrimary else BuddysTheme.colors.primaryRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

// ====================================================================
// 3D CHAT BUBBLES
// ====================================================================

/**
 * 3D Chat Message Bubble with realistic depth bevel and distinct incoming/outgoing styling.
 */
@Composable
fun ThreeDChatBubble(
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isOutgoing) 16.dp else 4.dp,
        bottomEnd = if (isOutgoing) 4.dp else 16.dp
    ),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (isOutgoing) {
        BuddysTheme.colors.bubbleOutgoing
    } else {
        BuddysTheme.colors.bubbleIncoming
    }

    val borderColor = if (isOutgoing) {
        BuddysTheme.colors.primaryRed.copy(alpha = 0.85f)
    } else {
        BuddysTheme.colors.border
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 1.5.dp,
                shape = shape,
                ambientColor = if (isOutgoing) BuddysTheme.colors.primaryRed.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.1f)
            )
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

// ====================================================================
// 3D AVATAR & STORY RINGS
// ====================================================================

/**
 * 3D Avatar View with depth border and presence indicators.
 */
@Composable
fun ThreeDAvatar(
    imageUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null
) {
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: "?"
    val baseModifier = if (onClick != null) modifier.threeDPress(onClick = onClick) else modifier

    Box(
        modifier = baseModifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = elevation,
                    shape = CircleShape,
                    ambientColor = if (BuddysTheme.colors.isDark) Color.Black.copy(alpha = 0.5f) else Color(0x15000000)
                )
                .clip(CircleShape)
                .border(1.5.dp, BuddysTheme.colors.border, CircleShape)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BuddysTheme.colors.surfaceSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.primaryRed,
                            fontSize = (size.value * 0.42f).sp
                        )
                    )
                }
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.28f).coerceAtLeast(11f).dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.success)
                    .border(2.dp, BuddysTheme.colors.surface, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

// ====================================================================
// 3D INPUT TEXT FIELD
// ====================================================================

/**
 * 3D Elevated Input Text Field.
 */
@Composable
fun ThreeDTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (isFocused) 2.5.dp else 1.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = if (isFocused) BuddysTheme.colors.primaryRed.copy(alpha = 0.2f) else Color.Transparent
            )
            .clip(RoundedCornerShape(14.dp))
            .background(BuddysTheme.colors.surface)
            .border(
                1.5.dp,
                if (isError) BuddysTheme.colors.error
                else if (isFocused) BuddysTheme.colors.primaryRed
                else BuddysTheme.colors.border,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isFocused) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = BuddysTheme.colors.textMuted,
                            fontSize = 14.5.sp
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 14.5.sp
                    ),
                    singleLine = singleLine,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(BuddysTheme.colors.primaryRed)
                )
            }

            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}

// ====================================================================
// 3D LIST ROW & SETTINGS ITEM
// ====================================================================

/**
 * 3D Elevated List Row Item with micro-depth and bouncy press.
 */
@Composable
fun ThreeDListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
    trailingContent: (@Composable () -> Unit)? = null,
    iconTint: Color = BuddysTheme.colors.primaryRed,
    textColor: Color = BuddysTheme.colors.textPrimary,
    badgeText: String? = null
) {
    ThreeDCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            fontSize = 15.sp
                        )
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BuddysTheme.colors.primaryRed)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BuddysTheme.colors.textSecondary,
                            fontSize = 12.5.sp
                        )
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

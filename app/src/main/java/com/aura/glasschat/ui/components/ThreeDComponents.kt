package com.aura.glasschat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
// BUDDIES RETRO CARTOON COMPONENT IMPLEMENTATIONS
// ====================================================================

/**
 * Subtle Spring Press Scale.
 */
fun Modifier.threeDPress(
    enabled: Boolean = true,
    targetScale: Float = 0.97f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
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
 * Modern Clean Surface with hairline border.
 */
@Composable
fun ThreeDSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = BuddysTheme.colors.surface,
    borderColor: Color = BuddysTheme.colors.border,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor.copy(alpha = if (BuddysTheme.colors.isDark) 0.5f else 0.8f), shape)
    ) {
        content()
    }
}

/**
 * Modern Card with spring press feedback and hairline border.
 */
@Composable
fun ThreeDCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    backgroundColor: Color = BuddysTheme.colors.surface,
    borderColor: Color = BuddysTheme.colors.border,
    elevation: Dp = 0.dp,
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
// BUTTONS & CONTROLS
// ====================================================================

/**
 * Primary Action Button (Signature Crimson fill with smooth corner curves).
 */
@Composable
fun ThreeDButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    containerColor: Color = BuddysTheme.colors.primaryAccent,
    contentColor: Color = BuddysTheme.colors.textOnPrimary,
    elevation: Dp = 0.dp,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .height(50.dp)
            .scale(scale)
            .clip(shape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.15f else 0.05f), shape)
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
                        color = if (enabled) contentColor else contentColor.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

/**
 * Secondary Outlined Button.
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
    backgroundColor: Color = BuddysTheme.colors.surface,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
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
            .height(48.dp)
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
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
 * Modern Circular Icon Button.
 */
@Composable
fun ThreeDIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    containerColor: Color = BuddysTheme.colors.surfaceSecondary,
    tint: Color = BuddysTheme.colors.textPrimary,
    borderColor: Color = BuddysTheme.colors.border,
    elevation: Dp = 0.dp
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
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor.copy(alpha = 0.6f), CircleShape)
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
 * Modern Floating Action Button (FAB).
 */
@Composable
fun ThreeDFloatingButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = BuddysTheme.colors.primaryAccent,
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
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
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
// TOP BAR & BOTTOM NAVIGATION
// ====================================================================

/**
 * Modern Top Bar with clean hairline divider.
 */
@Composable
fun ThreeDTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BuddysTheme.colors.background
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BuddysTheme.colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (actions != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        actions()
                    }
                }
            }
            Divider(
                color = BuddysTheme.colors.divider.copy(alpha = 0.6f),
                thickness = 0.8.dp
            )
        }
    }
}

/**
 * Modern Floating Glass Navigation Bar with signature center compose button.
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
    onProfileDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Glass Dock Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, BuddysTheme.colors.border.copy(alpha = if (BuddysTheme.colors.isDark) 0.5f else 0.8f), RoundedCornerShape(32.dp)),
            color = BuddysTheme.colors.bottomDock.copy(alpha = if (BuddysTheme.colors.isDark) 0.95f else 0.98f),
            shape = RoundedCornerShape(32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. CHATS TAB
                val isChats = selectedTab == HomeBottomTab.INBOX || selectedTab == HomeBottomTab.HOME
                CartoonDockItem(
                    selected = isChats,
                    label = "Chats",
                    icon = Icons.Filled.ChatBubble,
                    badgeCount = unreadChatsCount,
                    onClick = { onTabSelected(HomeBottomTab.INBOX) }
                )

                // 2. CALLS TAB
                val isCalls = false
                CartoonDockItem(
                    selected = isCalls,
                    label = "Calls",
                    icon = Icons.Filled.Call,
                    badgeCount = missedCallsCount,
                    onClick = { onTabSelected(HomeBottomTab.HOME) }
                )

                // 3. CENTER GAP (Reserved for the elevated center button)
                Spacer(modifier = Modifier.width(52.dp))

                // 4. DISCOVER TAB
                val isDiscover = selectedTab == HomeBottomTab.SEARCH
                CartoonDockItem(
                    selected = isDiscover,
                    label = "Discover",
                    icon = Icons.Filled.Explore,
                    onClick = { onTabSelected(HomeBottomTab.SEARCH) }
                )

                // 5. PROFILE TAB
                val isProfile = selectedTab == HomeBottomTab.PROFILE
                CartoonDockItem(
                    selected = isProfile,
                    label = "Profile",
                    icon = Icons.Filled.Person,
                    showDotBadge = hasUnreadUpdates,
                    onDoubleClick = onProfileDoubleTap,
                    onClick = { onTabSelected(HomeBottomTab.PROFILE) }
                )
            }
        }

        // Elevated Center Glowing Button
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val centerScale by animateFloatAsState(
            targetValue = if (isPressed) 0.90f else 1.0f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
            label = "CenterBtnScale"
        )

        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(54.dp)
                .scale(centerScale)
                .clip(CircleShape)
                .background(BuddysRedGradient)
                .border(2.5.dp, BuddysTheme.colors.background, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onTabSelected(HomeBottomTab.CREATE) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Moment",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.CartoonDockItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    badgeCount: Int = 0,
    showDotBadge: Boolean = false,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val activeColor = BuddysTheme.colors.primaryAccent
    val inactiveColor = BuddysTheme.colors.textMuted

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) activeColor else inactiveColor,
                    modifier = Modifier.size(22.dp)
                )

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(BuddysTheme.colors.primaryAccent)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                        )
                    }
                } else if (showDotBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-2).dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BuddysTheme.colors.primaryAccent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                color = if (selected) activeColor else inactiveColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}

// ====================================================================
// TABS & SEGMENTED PILLS
// ====================================================================

/**
 * Modern Segmented Tab Pill.
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
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "pillScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isSelected) BuddysTheme.colors.primaryAccent
                else BuddysTheme.colors.surfaceSecondary
            )
            .border(
                1.dp,
                if (isSelected) Color.Transparent else BuddysTheme.colors.border.copy(alpha = 0.6f),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    tint = if (isSelected) Color.White else BuddysTheme.colors.textPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else BuddysTheme.colors.textPrimary,
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
                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else BuddysTheme.colors.primaryAccent)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
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
// CHAT BUBBLES
// ====================================================================

/**
 * Modern Chat Bubble with smooth corners and hairline border.
 */
@Composable
fun ThreeDChatBubble(
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isOutgoing) 18.dp else 4.dp,
        bottomEnd = if (isOutgoing) 4.dp else 18.dp
    ),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (isOutgoing) {
        BuddysTheme.colors.bubbleOutgoing
    } else {
        BuddysTheme.colors.bubbleIncoming
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, BuddysTheme.colors.border.copy(alpha = if (isOutgoing) 0.1f else 0.4f), shape)
    ) {
        content()
    }
}

// ====================================================================
// AVATAR
// ====================================================================

/**
 * Modern Avatar View with clean initial monogram.
 */
@Composable
fun ThreeDAvatar(
    imageUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    elevation: Dp = 0.dp,
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
                .clip(CircleShape)
                .background(BuddysTheme.colors.surfaceSecondary)
                .border(1.dp, BuddysTheme.colors.border.copy(alpha = 0.6f), CircleShape)
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
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF253B61),
                                    Color(0xFF1B2840)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
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
// INPUT TEXT FIELD
// ====================================================================

/**
 * Modern Input Text Field.
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
            .clip(RoundedCornerShape(14.dp))
            .background(BuddysTheme.colors.surfaceSecondary)
            .border(
                1.dp,
                if (isError) BuddysTheme.colors.error
                else if (isFocused) BuddysTheme.colors.primaryAccent
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
                    tint = if (isFocused) BuddysTheme.colors.primaryAccent else BuddysTheme.colors.textSecondary,
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
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = singleLine,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(BuddysTheme.colors.primaryAccent)
                )
            }

            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}

// ====================================================================
// LIST ROW & SETTINGS ITEM
// ====================================================================

/**
 * Modern List Row Item.
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
    iconTint: Color = BuddysTheme.colors.primaryAccent,
    textColor: Color = BuddysTheme.colors.textPrimary,
    badgeText: String? = null
) {
    ThreeDCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp)
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
                        .border(1.dp, BuddysTheme.colors.border.copy(alpha = 0.5f), CircleShape),
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(BuddysTheme.colors.primaryAccent)
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

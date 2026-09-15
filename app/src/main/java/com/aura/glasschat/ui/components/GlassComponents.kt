package com.aura.glasschat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aura.glasschat.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Clean card surface with subtle 1dp border and modern rounded corners.
 */
fun Modifier.cuteSurface(
    shape: Shape = RoundedCornerShape(14.dp),
    backgroundColor: Color = Color(0xFFFFFFFF),
    borderColor: Color = Color(0xFFE7E7E7),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor, shape)
    .border(borderWidth, borderColor, shape)

// Backward-compatible alias for existing callers
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(14.dp),
    backgroundColor: Color = Color(0xFFFFFFFF),
    borderColor: Color = Color(0xFFE7E7E7),
    borderWidth: Dp = 1.dp
): Modifier = this.cuteSurface(shape, backgroundColor, borderColor, borderWidth)

/**
 * Interactive button bounce modifier for micro-feedback.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "bouncyScale"
    )

    this
        .scale(scale)
        .clip(shape)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

// Backward-compatible alias
fun Modifier.glassClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit
): Modifier = this.bouncyClickable(enabled, shape, onClick)

/**
 * Clean background surface with subtle spider/web corner geometry.
 */
@Composable
fun AmbientNostalgicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BuddysTheme.colors.background)
    ) {
        BuddysWebDecoration(size = 80.dp, modifier = Modifier.align(Alignment.TopStart))
        BuddysWebTopRight(size = 80.dp, modifier = Modifier.align(Alignment.TopEnd))
        content()
    }
}

// Backward-compatible alias
@Composable
fun AmbientGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) = AmbientNostalgicBackground(modifier, content)

/**
 * Clean BUDDYS Brand Emblem vector.
 */
@Composable
fun DoodleMascot(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    BuddysSpiderEmblem(
        modifier = modifier,
        size = size,
        tint = BuddysTheme.colors.primaryRed
    )
}

/**
 * Subtle geometric diamond node.
 */
@Composable
fun DoodleStar(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = BuddysTheme.colors.primaryRed
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.width / 2f

        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy)
            close()
        }
        drawPath(path, color)
    }
}

/**
 * Geometric 4-point node
 */
@Composable
fun ComicSparkle(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    color: Color = BuddysTheme.colors.primaryRed
) {
    DoodleStar(modifier = modifier, size = size, color = color)
}

/**
 * Avatar View with optional online indicator and story ring.
 */
@Composable
fun AvatarView(
    imageUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    showHalo: Boolean = false,
    hasStory: Boolean = false,
    isEditable: Boolean = false,
    onEditClick: (() -> Unit)? = null
) {
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: "?"

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, BuddysTheme.colors.border, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.surfaceSecondary)
                    .border(1.dp, BuddysTheme.colors.border, CircleShape),
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

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.28f).coerceAtLeast(10f).dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.success)
                    .border(1.5.dp, BuddysTheme.colors.surface, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }

        if (isEditable && onEditClick != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.primaryRed)
                    .border(2.dp, BuddysTheme.colors.surface, CircleShape)
                    .clickable(onClick = onEditClick)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Edit photo",
                    tint = BuddysTheme.colors.textOnPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

// Backward-compatible alias
@Composable
fun GlassAvatar(
    imageUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    showHalo: Boolean = false,
    hasStory: Boolean = false,
    isEditable: Boolean = false,
    onEditClick: (() -> Unit)? = null
) = AvatarView(
    imageUrl = imageUrl,
    displayName = displayName,
    modifier = modifier,
    size = size,
    isOnline = isOnline,
    showHalo = showHalo,
    hasStory = hasStory,
    isEditable = isEditable,
    onEditClick = onEditClick
)

/**
 * Clean 3D elevated text input field.
 */
@Composable
fun CuteTextField(
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
    ThreeDTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        isError = isError
    )
}

// Backward-compatible alias
@Composable
fun GlassTextField(
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
) = CuteTextField(
    value = value,
    onValueChange = onValueChange,
    placeholder = placeholder,
    modifier = modifier,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    visualTransformation = visualTransformation,
    isError = isError
)

/**
 * Primary Brand 3D Button.
 */
@Composable
fun CuteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    backgroundColor: Color = BuddysTheme.colors.primaryRed,
    textColor: Color = BuddysTheme.colors.textOnPrimary,
    icon: ImageVector? = null
) {
    ThreeDButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        containerColor = backgroundColor,
        contentColor = textColor,
        leadingIcon = icon
    )
}

// Backward-compatible alias
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) = CuteButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    isLoading = isLoading,
    icon = icon
)

/**
 * Wave dots for typing.
 */
@Composable
fun TypingWaveAnimation(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
    dotSize: Dp = 4.dp,
    dotColor: Color = BuddysTheme.colors.primaryRed
) {
    val transition = rememberInfiniteTransition(label = "typingWave")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until dotCount) {
            val dy by transition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = i * 120, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .offset(y = dy.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/**
 * Floating Typing Indicator Pill above Composer.
 */
@Composable
fun TypingIndicatorPill(
    displayName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BuddysTheme.colors.surface)
            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypingWaveAnimation(dotSize = 4.dp, dotColor = BuddysTheme.colors.primaryRed)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$displayName is typing...",
            style = MaterialTheme.typography.labelSmall.copy(
                color = BuddysTheme.colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

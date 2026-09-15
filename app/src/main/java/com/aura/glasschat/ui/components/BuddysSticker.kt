package com.aura.glasschat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.glasschat.ui.theme.BuddysTheme

enum class StickerType {
    BRAND_EMBLEM,
    SECURITY_SHIELD,
    SPIDER_WEB,
    // Backward-compatibility aliases
    DORAEMON_GADGET,
    SHINCHAN_PLAYFUL,
    HATTORI_NINJA,
    NOBITA_DREAMER,
    SPIDER_HERO
}

/**
 * Minimalist BUDDYS Brand Emblem & Badge.
 * Renders the original abstract spider mark or security shield in a clean circular container.
 */
@Composable
fun BuddysSticker(
    type: StickerType = StickerType.BRAND_EMBLEM,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    rotation: Float = 0f,
    alpha: Float = 1.0f
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(BuddysTheme.colors.surfaceSecondary)
            .border(1.dp, BuddysTheme.colors.border, CircleShape)
            .padding(size * 0.18f),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            StickerType.SECURITY_SHIELD -> {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Security",
                    tint = BuddysTheme.colors.primaryRed,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                BuddysSpiderEmblem(
                    modifier = Modifier.fillMaxSize(),
                    tint = BuddysTheme.colors.primaryRed
                )
            }
        }
    }
}


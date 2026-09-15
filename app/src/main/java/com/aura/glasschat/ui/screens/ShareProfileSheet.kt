package com.aura.glasschat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.data.model.User
import com.aura.glasschat.ui.components.AvatarView
import com.aura.glasschat.ui.components.BuddysButton
import com.aura.glasschat.ui.components.BuddysOutlinedButton
import com.aura.glasschat.ui.components.BuddysSpiderEmblem
import com.aura.glasschat.ui.theme.BuddysTheme
import com.aura.glasschat.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProfileSheet(
    user: User?,
    onDismiss: () -> Unit
) {
    if (user == null) return
    val context = LocalContext.current

    val qrBitmap: Bitmap? = remember(user.username) {
        val payload = QrCodeGenerator.getProfileDeepLink(user.username)
        QrCodeGenerator.generateQrBitmap(
            content = payload,
            size = 512,
            foregroundColor = android.graphics.Color.BLACK,
            backgroundColor = android.graphics.Color.WHITE
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BuddysTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BuddysTheme.colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with close icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 18.sp
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BuddysTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium BUDDYS Profile Share Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BuddysTheme.colors.surfaceSecondary)
                    .border(1.5.dp, BuddysTheme.colors.border, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // BUDDYS Brand Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BuddysSpiderEmblem(size = 20.dp, tint = BuddysTheme.colors.primaryRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Buddies",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = BuddysTheme.colors.primaryRed,
                                letterSpacing = 1.8.sp,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User Avatar
                    AvatarView(
                        imageUrl = user.avatarUrl,
                        displayName = user.displayName.ifBlank { user.username },
                        size = 68.dp,
                        isOnline = user.isOnline
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Display Name
                    Text(
                        text = user.displayName.ifBlank { "Buddy" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textPrimary,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // @username
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BuddysTheme.colors.primaryRed,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )

                    if (user.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = user.bio,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BuddysTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Code Frame
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Profile QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = BuddysTheme.colors.primaryRed,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Scan with Buddies camera to connect",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BuddysTheme.colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BuddysOutlinedButton(
                    text = "Copy Link",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val profileUrl = "https://buddys.app/${user.username}"
                        clipboard.setPrimaryClip(ClipData.newPlainText("Buddies Profile", profileUrl))
                        Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                )

                BuddysButton(
                    text = "Share Profile",
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Connect with ${user.displayName} on Buddies! @${user.username}\nhttps://buddys.app/${user.username}"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Profile"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

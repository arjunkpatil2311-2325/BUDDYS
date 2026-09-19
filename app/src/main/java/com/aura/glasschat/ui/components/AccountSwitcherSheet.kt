package com.aura.glasschat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.glasschat.data.model.SavedAccount
import com.aura.glasschat.ui.theme.BuddysTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    savedAccounts: List<SavedAccount>,
    currentUid: String,
    onDismiss: () -> Unit,
    onSwitchAccount: (SavedAccount) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (SavedAccount) -> Unit,
    onLogOutActiveAccount: (() -> Unit)? = null
) {
    var accountToRemove by remember { mutableStateOf<SavedAccount?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BuddysTheme.colors.surface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BuddysSpiderEmblem(
                        size = 24.dp,
                        tint = BuddysTheme.colors.primaryRed
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Accounts",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textPrimary,
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BuddysTheme.colors.surfaceSecondary
                    ) {
                        Text(
                            text = "${savedAccounts.size}",
                            color = BuddysTheme.colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(BuddysTheme.colors.surfaceSecondary, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BuddysTheme.colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Saved Accounts List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedAccounts, key = { it.uid }) { account ->
                    val isActive = account.uid == currentUid

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isActive) BuddysTheme.colors.surfaceSecondary else BuddysTheme.colors.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isActive) BuddysTheme.colors.primaryRed else BuddysTheme.colors.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isActive) {
                                    onSwitchAccount(account)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AvatarView(
                                    imageUrl = account.avatarUrl,
                                    displayName = account.displayName.ifBlank { account.username },
                                    size = 46.dp,
                                    isOnline = isActive
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = account.displayName.ifBlank { account.username },
                                        fontWeight = FontWeight.Bold,
                                        color = BuddysTheme.colors.textPrimary,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${account.username}",
                                        color = BuddysTheme.colors.textSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActive) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BuddysTheme.colors.primaryRed,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Active Account",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else {
                                    IconButton(
                                        onClick = { accountToRemove = account },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Account",
                                            tint = BuddysTheme.colors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Account Option
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, BuddysTheme.colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddAccount() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BuddysTheme.colors.surfaceSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Account",
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = "Add Buddies Account",
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.textPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // Optional Log Out Active Account
                if (onLogOutActiveAccount != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, BuddysTheme.colors.border.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLogOutActiveAccount() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BuddysTheme.colors.error.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Log Out",
                                        tint = BuddysTheme.colors.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = "Log Out Active Account",
                                    fontWeight = FontWeight.SemiBold,
                                    color = BuddysTheme.colors.error,
                                    fontSize = 14.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Remove Account Confirmation Dialog
    accountToRemove?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            containerColor = BuddysTheme.colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Remove Account",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary
                    )
                )
            },
            text = {
                Text(
                    text = "Remove @${acc.username} from this device? Your messages and profile will not be deleted, but you will need your password to log back in.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BuddysTheme.colors.textSecondary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = acc
                        accountToRemove = null
                        onRemoveAccount(toDelete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BuddysTheme.colors.error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRemove = null }) {
                    Text("Cancel", color = BuddysTheme.colors.textSecondary)
                }
            }
        )
    }
}


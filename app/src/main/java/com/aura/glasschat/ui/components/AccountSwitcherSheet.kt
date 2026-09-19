package com.aura.glasschat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
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
    onRemoveAccount: (SavedAccount) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141C),
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
                        modifier = Modifier.size(24.dp),
                        tint = BuddysTheme.colors.primaryRed
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Accounts",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${savedAccounts.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
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
                        color = if (isActive) Color(0xFF1E1E2C) else Color(0xFF181822),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isActive) BuddysTheme.colors.primaryRed else Color.White.copy(alpha = 0.1f)
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
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${account.username}",
                                        color = Color.White.copy(alpha = 0.55f),
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
                                        onClick = { onRemoveAccount(account) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Account",
                                            tint = Color.White.copy(alpha = 0.4f),
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
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
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Account",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = "Add Buddies Account",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

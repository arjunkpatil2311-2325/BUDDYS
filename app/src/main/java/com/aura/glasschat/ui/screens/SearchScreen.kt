package com.aura.glasschat.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.repository.FollowStatus
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.SearchUserItem
import com.aura.glasschat.ui.viewmodel.SearchViewModel

import android.widget.Toast
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scanner = remember(context) { GmsBarcodeScanning.getClient(context) }

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
                title = "Discover",
                onBack = onBack
            )

            // Search Input Box + QR Scanner Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BuddysSearchBar(
                        query = uiState.query,
                        onQueryChange = { viewModel.onQueryChanged(it) },
                        placeholder = "Search by @username or name..."
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        scanner.startScan()
                            .addOnSuccessListener { barcode: Barcode ->
                                val raw = barcode.rawValue
                                if (!raw.isNullOrBlank()) {
                                    viewModel.lookupQrUser(
                                        rawPayload = raw,
                                        onUserFound = { user ->
                                            onOpenProfile(user.uid)
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                            .addOnFailureListener { e: Exception ->
                                Toast.makeText(context, "Scan cancelled or failed", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR Code",
                        tint = BuddysTheme.colors.primaryRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Content / Results
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center),
                            color = BuddysTheme.colors.primaryRed,
                            strokeWidth = 3.dp
                        )
                    }
                    uiState.query.isBlank() -> {
                        BuddysEmptyState(
                            title = "Discover people on Buddies",
                            subtitle = "Search by @username or name to find and follow friends.",
                            icon = Icons.Default.Search,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp)
                        )
                    }
                    uiState.results.isEmpty() -> {
                        BuddysEmptyState(
                            title = "No users found",
                            subtitle = "We couldn't find any account matching '@${uiState.query}'.",
                            icon = Icons.Default.PersonSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = uiState.results,
                                key = { it.user.uid }
                            ) { searchItem ->
                                SearchResultUserCard(
                                    item = searchItem,
                                    onClick = { onOpenProfile(searchItem.user.uid) },
                                    onFollowClick = { viewModel.toggleFollow(searchItem.user) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultUserCard(
    item: SearchUserItem,
    onClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    val user = item.user
    val status = item.followStatus

    BuddysCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                imageUrl = user.avatarUrl,
                displayName = user.displayName.ifBlank { user.username },
                size = 46.dp,
                isOnline = user.isOnline
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName.ifBlank { user.username },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Follow Action Button
            when (status) {
                FollowStatus.FOLLOWING -> {
                    BuddysOutlinedButton(
                        text = "Following",
                        onClick = onFollowClick,
                        modifier = Modifier.height(34.dp)
                    )
                }
                FollowStatus.REQUESTED -> {
                    BuddysOutlinedButton(
                        text = "Requested",
                        onClick = onFollowClick,
                        modifier = Modifier.height(34.dp)
                    )
                }
                FollowStatus.NOT_FOLLOWING -> {
                    BuddysButton(
                        text = "Follow",
                        onClick = onFollowClick,
                        modifier = Modifier.height(34.dp)
                    )
                }
                else -> {
                    // MUTUAL, SELF, BLOCKED, etc.
                }
            }
        }
    }
}

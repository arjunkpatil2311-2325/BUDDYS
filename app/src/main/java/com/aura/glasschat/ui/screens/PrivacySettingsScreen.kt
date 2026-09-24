package com.aura.glasschat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.BuildConfig
import com.aura.glasschat.data.model.SavedAccount
import com.aura.glasschat.data.update.UpdateManifest
import com.aura.glasschat.security.AppLockManager
import com.aura.glasschat.security.BiometricAuthManager
import com.aura.glasschat.security.LockTimeout
import com.aura.glasschat.ui.components.AccountSwitcherSheet
import com.aura.glasschat.ui.components.BuddysButton
import com.aura.glasschat.ui.components.BuddysCard
import com.aura.glasschat.ui.components.BuddysOutlinedButton
import com.aura.glasschat.ui.components.BuddysTopBar
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.PrivacySettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenPinSetup: () -> Unit,
    onOpenSavedMessages: () -> Unit = {},
    onOpenCloseFriends: () -> Unit = {},
    onOpenStoryArchive: () -> Unit = {},
    onOpenStorageManager: () -> Unit = {},
    onOpenAppUpdates: () -> Unit = {},
    onOpenHiddenChats: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    viewModel: PrivacySettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val updateManager = remember { com.aura.glasschat.data.update.UpdateManager.getInstance(context) }
    val availableUpdate by updateManager.availableUpdate.collectAsState()
    var isCheckingUpdates by remember { mutableStateOf(false) }

    val accountManager = remember { com.aura.glasschat.data.repository.AccountManagerRepository.getInstance(context) }
    val authRepository = remember { com.aura.glasschat.data.repository.AuthRepository() }
    val currentUid = authRepository.currentUserId
    val savedAccounts by accountManager.savedAccounts.collectAsState(initial = emptyList())
    var showAccountSwitcherSheet by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val themeMode by ThemePreferences.themeMode.collectAsState()

    val appLockManager = remember { AppLockManager.getInstance(context) }
    val pinManager = appLockManager.pinManager

    val isPinSet by pinManager.isPinSet.collectAsState()
    val lockTimeout by pinManager.lockTimeout.collectAsState()
    val isBiometricEnabled by pinManager.isBiometricEnabled.collectAsState()
    val hideNotifContent by pinManager.hideNotificationContent.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = BuddysTheme.colors.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BuddysTopBar(
                title = "Settings & Privacy",
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // 0. PREMIUM FEATURES & DATA
                // ==========================================
                item {
                    Text(
                        text = "FEATURES & DATA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.primaryRed,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            // Close Friends
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenCloseFriends() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFF22A06B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Close Friends",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BuddysTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Manage private story audience",
                                        fontSize = 11.5.sp,
                                        color = BuddysTheme.colors.textSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            HorizontalDivider(color = BuddysTheme.colors.border)

                            // Saved Messages
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSavedMessages() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Saved Messages",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BuddysTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "View all starred notes & media",
                                        fontSize = 11.5.sp,
                                        color = BuddysTheme.colors.textSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            HorizontalDivider(color = BuddysTheme.colors.border)

                            // Story Archive
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenStoryArchive() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Story Archive",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BuddysTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Private past expired moments",
                                        fontSize = 11.5.sp,
                                        color = BuddysTheme.colors.textSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }



                            HorizontalDivider(color = BuddysTheme.colors.border)

                            // Storage Manager
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenStorageManager() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Storage & Data",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BuddysTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Manage cache and media downloads",
                                        fontSize = 11.5.sp,
                                        color = BuddysTheme.colors.textSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 1. PRIVACY PIN & APP LOCK
                // ==========================================
                item {
                    Text(
                        text = "PRIVACY LOCK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.primaryRed,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = BuddysTheme.colors.primaryRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Privacy PIN Lock",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                        )
                                        Text(
                                            text = if (isPinSet) "Protected with 4-digit PIN" else "PIN not configured",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BuddysTheme.colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                if (!isPinSet) {
                                    BuddysButton(
                                        text = "Set Up",
                                        onClick = onOpenPinSetup,
                                        modifier = Modifier.height(38.dp)
                                    )
                                } else {
                                    BuddysOutlinedButton(
                                        text = "Change",
                                        onClick = onOpenPinSetup,
                                        modifier = Modifier.height(38.dp)
                                    )
                                }
                            }

                            if (isPinSet) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = BuddysTheme.colors.border)
                                Spacer(modifier = Modifier.height(14.dp))

                                // Auto Lock Timeout Selection
                                Text(
                                    text = "Auto-Lock Timeout",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BuddysTheme.colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LockTimeout.entries.forEach { timeout ->
                                        val isSelected = lockTimeout == timeout
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { pinManager.setLockTimeout(timeout) }
                                                .background(
                                                    if (isSelected) BuddysTheme.colors.primaryRed
                                                    else BuddysTheme.colors.surfaceSecondary
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) BuddysTheme.colors.primaryRed
                                                    else BuddysTheme.colors.border,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (timeout) {
                                                    LockTimeout.IMMEDIATELY -> "Instant"
                                                    LockTimeout.AFTER_1_MIN -> "1 min"
                                                    LockTimeout.AFTER_5_MIN -> "5 min"
                                                    LockTimeout.AFTER_15_MIN -> "15 min"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSelected) BuddysTheme.colors.textOnPrimary else BuddysTheme.colors.textPrimary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.5.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Biometric Unlock Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = BuddysTheme.colors.textSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Biometric Unlock",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = BuddysTheme.colors.textPrimary
                                                )
                                            )
                                            Text(
                                                text = "Use fingerprint or face recognition",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = BuddysTheme.colors.textSecondary,
                                                    fontSize = 11.5.sp
                                                )
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isBiometricEnabled,
                                        onCheckedChange = { enable ->
                                            if (enable) {
                                                if (!BiometricAuthManager.isBiometricHardwareAvailable(context)) {
                                                    Toast.makeText(context, "Tip: Ensure fingerprint is set up in your device settings", Toast.LENGTH_LONG).show()
                                                }
                                                pinManager.setBiometricEnabled(true)
                                            } else {
                                                pinManager.setBiometricEnabled(false)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = BuddysTheme.colors.surface,
                                            checkedTrackColor = BuddysTheme.colors.primaryRed,
                                            uncheckedThumbColor = BuddysTheme.colors.textMuted,
                                            uncheckedTrackColor = BuddysTheme.colors.surfaceSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 2. NOTIFICATION PRIVACY
                // ==========================================
                item {
                    Text(
                        text = "NOTIFICATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.primaryRed,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.NotificationsOff,
                                        contentDescription = null,
                                        tint = BuddysTheme.colors.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Hide Message Previews",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                        )
                                        Text(
                                            text = if (hideNotifContent) "Notifications only show 'New message'" else "Notifications show message text",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BuddysTheme.colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                Switch(
                                    checked = hideNotifContent,
                                    onCheckedChange = { pinManager.setHideNotificationContent(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BuddysTheme.colors.surface,
                                        checkedTrackColor = BuddysTheme.colors.primaryRed,
                                        uncheckedThumbColor = BuddysTheme.colors.textMuted,
                                        uncheckedTrackColor = BuddysTheme.colors.surfaceSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 3. APPEARANCE / THEME SELECTION
                // ==========================================
                item {
                    Text(
                        text = "APPEARANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.primaryRed,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Theme Mode",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BuddysTheme.colors.textPrimary
                                        )
                                    )
                                    Text(
                                        text = "Choose light, dark, or system mode",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BuddysTheme.colors.textSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppThemeMode.entries.forEach { mode ->
                                    val isSelected = themeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { ThemePreferences.setThemeMode(mode) }
                                            .background(
                                                if (isSelected) BuddysTheme.colors.primaryRed else BuddysTheme.colors.surfaceSecondary
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) BuddysTheme.colors.primaryRed else BuddysTheme.colors.border,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (mode) {
                                                AppThemeMode.SYSTEM -> "System"
                                                AppThemeMode.LIGHT -> "Light"
                                                AppThemeMode.DARK -> "Dark"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isSelected) BuddysTheme.colors.textOnPrimary else BuddysTheme.colors.textPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 4. ACCOUNT PRIVACY
                // ==========================================
                item {
                    Text(
                        text = "ACCOUNT PRIVACY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.primaryRed,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (uiState.isPrivate) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Private Account",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                        )
                                        Text(
                                            text = if (uiState.isPrivate)
                                                "Only approved buddies can see your moments"
                                            else
                                                "Anyone on Buddies can follow you directly",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BuddysTheme.colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                Switch(
                                    checked = uiState.isPrivate,
                                    onCheckedChange = { viewModel.togglePrivateAccount(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BuddysTheme.colors.surface,
                                        checkedTrackColor = BuddysTheme.colors.primaryRed,
                                        uncheckedThumbColor = BuddysTheme.colors.textMuted,
                                        uncheckedTrackColor = BuddysTheme.colors.surfaceSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 5. SAFETY & COMMUNITY
                // ==========================================
                item {
                    Text(
                        text = "SAFETY & COMMUNITY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.primaryRed,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenRules() }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null,
                                        tint = BuddysTheme.colors.primaryRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Community Rules & Safety",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                        )
                                        Text(
                                            text = "Read our guidelines on respectful connections",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BuddysTheme.colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 6. BLOCKED ACCOUNTS
                // ==========================================
                if (uiState.blockedUsers.isNotEmpty()) {
                    item {
                        Text(
                            text = "BLOCKED ACCOUNTS (${uiState.blockedUsers.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.primaryRed,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )

                        BuddysCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.blockedUsers.forEach { blockedUser ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "User ${blockedUser.blockedUid.take(8)}...",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                        )
                                        BuddysOutlinedButton(
                                            text = "Unblock",
                                            onClick = { viewModel.unblockUser(blockedUser.blockedUid) },
                                            modifier = Modifier.height(34.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 7. ACCOUNTS & LOGINS
                // ==========================================
                item {
                    Text(
                        text = "ACCOUNTS & LOGINS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.primaryRed,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Switch Account Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showAccountSwitcherSheet = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwitchAccount,
                                        contentDescription = null,
                                        tint = BuddysTheme.colors.primaryRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Switch Account",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                        )
                                        Text(
                                            text = "${savedAccounts.size} saved account${if (savedAccounts.size != 1) "s" else ""}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BuddysTheme.colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            HorizontalDivider(color = BuddysTheme.colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                            // Add Account Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        authRepository.logOut()
                                        onLoggedOut()
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = BuddysTheme.colors.primaryAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Add Buddies Account",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = BuddysTheme.colors.textPrimary
                                        )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 8. APP VERSION & UPDATES
                // ==========================================
                item {
                    Text(
                        text = "APP VERSION & UPDATES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.primaryRed,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenAppUpdates() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = BuddysTheme.colors.primaryRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "App Updates",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = BuddysTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = "Current version ${BuildConfig.VERSION_NAME}",
                                            fontSize = 12.sp,
                                            color = BuddysTheme.colors.textSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (availableUpdate != null) {
                                        Surface(
                                            color = BuddysTheme.colors.primaryRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Update available",
                                                color = BuddysTheme.colors.primaryRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = BuddysTheme.colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showAccountSwitcherSheet) {
            AccountSwitcherSheet(
                savedAccounts = savedAccounts,
                currentUid = currentUid,
                onDismiss = { showAccountSwitcherSheet = false },
                onSwitchAccount = { target ->
                    showAccountSwitcherSheet = false
                    coroutineScope.launch {
                        val res = accountManager.switchAccount(target.uid, authRepository)
                        if (res.isSuccess) {
                            Toast.makeText(context, "Switched to @${target.username} ✨", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "Could not switch: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onAddAccount = {
                    showAccountSwitcherSheet = false
                    authRepository.logOut()
                    onLoggedOut()
                },
                onRemoveAccount = { target ->
                    accountManager.removeAccount(target.uid)
                    Toast.makeText(context, "Removed @${target.username}", Toast.LENGTH_SHORT).show()
                },
                onLogOutActiveAccount = {
                    showAccountSwitcherSheet = false
                    viewModel.signOut()
                    onLoggedOut()
                }
            )
        }
    }
}

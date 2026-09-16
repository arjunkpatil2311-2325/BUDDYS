package com.aura.glasschat.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aura.glasschat.data.model.Message
import com.aura.glasschat.security.AppLockManager
import com.aura.glasschat.security.PinVerificationResult
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.ChatViewModel
import com.aura.glasschat.util.ChatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    otherUserId: String,
    onBack: () -> Unit,
    onStartAudioCall: (otherUid: String, otherName: String, otherAvatarUrl: String?) -> Unit = { _, _, _ -> },
    onStartVideoCall: (otherUid: String, otherName: String, otherAvatarUrl: String?) -> Unit = { _, _, _ -> },
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val otherUser = uiState.otherUser
    val currentUserId = uiState.currentUserId
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isSearching by remember { mutableStateOf(false) }

    val appLockManager = remember { AppLockManager.getInstance(context) }
    val pinManager = appLockManager.pinManager
    val isPinSet by pinManager.isPinSet.collectAsState()
    var chatLockPinInput by remember { mutableStateOf("") }
    var chatLockPinError by remember { mutableStateOf<String?>(null) }

    // Audio Permission Launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startAudioRecording()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is needed to record voice messages",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
        }
    }

    // Wallpaper Photo Picker Launcher
    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onBackgroundSelectedForPreview(uri)
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val uri = saveBitmapToTempUri(context, bitmap)
            if (uri != null) {
                viewModel.onImageSelected(uri)
            }
        }
    }

    LaunchedEffect(chatId, otherUserId) {
        viewModel.initChat(chatId, otherUserId)
    }

    DisposableEffect(chatId) {
        viewModel.setScreenActive(true)
        onDispose {
            viewModel.setScreenActive(false)
        }
    }

    // Auto scroll to bottom on new message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show error toast if any
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearErrorMessage()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(BuddysTheme.colors.surface)
                    .border(
                        androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border),
                        RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
                    )
            ) {
                if (uiState.isSelectionMode) {
                    // MULTI-SELECTION TOP BAR
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.exitSelectionMode() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit selection",
                                tint = BuddysTheme.colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "${uiState.selectedMessageIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.textPrimary,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Forward Action
                        IconButton(
                            onClick = { viewModel.openForwardDialog() },
                            enabled = uiState.selectedMessageIds.isNotEmpty(),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Forward selected",
                                tint = BuddysTheme.colors.primaryRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Save / Star Action
                        IconButton(
                            onClick = {
                                val selectedMsgs = uiState.messages.filter { uiState.selectedMessageIds.contains(it.id) }
                                for (msg in selectedMsgs) {
                                    viewModel.saveMessage(msg)
                                }
                                Toast.makeText(context, "Saved to Stars", Toast.LENGTH_SHORT).show()
                                viewModel.exitSelectionMode()
                            },
                            enabled = uiState.selectedMessageIds.isNotEmpty(),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Save selected",
                                tint = BuddysTheme.colors.primaryRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Delete for Me Action
                        IconButton(
                            onClick = { viewModel.deleteSelectedMessagesForMe() },
                            enabled = uiState.selectedMessageIds.isNotEmpty(),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = BuddysTheme.colors.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else if (uiState.isSearchModeActive) {
                    // DEDICATED SEARCH MODE TOP BAR
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.exitSearchMode() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit search",
                                tint = BuddysTheme.colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BuddysTheme.colors.surfaceSecondary)
                                .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search in conversation...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = BuddysTheme.colors.textMuted,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textPrimary,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(BuddysTheme.colors.primaryRed)
                            )
                        }

                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setSearchQuery("") },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = BuddysTheme.colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // NORMAL TOP BAR
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.pauseVoiceMessage()
                                onBack()
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = BuddysTheme.colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        AvatarView(
                            imageUrl = otherUser?.avatarUrl,
                            displayName = otherUser?.displayName ?: "Friend",
                            size = 38.dp,
                            isOnline = otherUser?.isOnline == true
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = otherUser?.displayName ?: "Chat",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BuddysTheme.colors.textPrimary,
                                    fontSize = 15.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val headerStatus = when {
                                uiState.isOtherUserTyping -> "typing..."
                                uiState.isOtherUserInChat -> "● In this chat"
                                otherUser?.isOnline == true -> "● Active now"
                                else -> ChatUtils.formatLastActive(false, otherUser?.lastSeen)
                            }
                            Text(
                                text = headerStatus,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = when {
                                        uiState.isOtherUserTyping || uiState.isOtherUserInChat || otherUser?.isOnline == true -> BuddysTheme.colors.success
                                        else -> BuddysTheme.colors.textMuted
                                    },
                                    fontWeight = if (uiState.isOtherUserTyping || uiState.isOtherUserInChat) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp
                                )
                            )
                        }

                        // Top Action Buttons: Call, Video, Search, More
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Voice Call Button
                            IconButton(
                                onClick = {
                                    val targetName = otherUser?.displayName?.ifBlank { otherUser.username } ?: "Buddy"
                                    onStartAudioCall(otherUserId, targetName, otherUser?.avatarUrl)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Audio Call",
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Video Call Button
                            IconButton(
                                onClick = {
                                    val targetName = otherUser?.displayName?.ifBlank { otherUser.username } ?: "Buddy"
                                    onStartVideoCall(otherUserId, targetName, otherUser?.avatarUrl)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video Call",
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // In-Chat Search Icon
                            IconButton(
                                onClick = { viewModel.enterSearchMode() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = BuddysTheme.colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // More Menu (Media/Links/Files, Pinned Messages, Chat Background)
                            Box {
                                var showMoreMenu by remember { mutableStateOf(false) }

                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = BuddysTheme.colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier
                                        .background(BuddysTheme.colors.surface)
                                        .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Media, Links & Files",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = BuddysTheme.colors.textPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PermMedia,
                                                contentDescription = null,
                                                tint = BuddysTheme.colors.primaryRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.openMediaLinksFilesSheet()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Pinned Messages",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = BuddysTheme.colors.textPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = null,
                                                tint = BuddysTheme.colors.primaryRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.openPinnedSheet()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Chat Background",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = BuddysTheme.colors.textPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Wallpaper,
                                                contentDescription = null,
                                                tint = BuddysTheme.colors.primaryRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.openThemeSheet()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (uiState.isChatLocked) "Unlock Chat (PIN Lock)" else "Lock Chat (PIN Protected)",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = BuddysTheme.colors.textPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (uiState.isChatLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = BuddysTheme.colors.primaryRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            if (!uiState.isChatLocked && !isPinSet) {
                                                Toast.makeText(context, "Please set a Privacy PIN in Profile first", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.toggleLockChat()
                                            }
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (uiState.isChatHidden) "Unhide Chat" else "Hide Chat",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = BuddysTheme.colors.textPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (uiState.isChatHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = BuddysTheme.colors.primaryRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.toggleHideChat()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isChatLocked && !uiState.isChatUnlockedForSession && isPinSet) {
                // PIN Locked Screen Gate
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BuddysTheme.colors.background)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BuddysCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(BuddysTheme.colors.surfaceSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Locked Conversation",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BuddysTheme.colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter your Privacy PIN to access messages in this chat.",
                                style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedTextField(
                                value = chatLockPinInput,
                                onValueChange = { input ->
                                    if (input.length <= 6 && input.all { ch -> ch.isDigit() }) {
                                        chatLockPinInput = input
                                        chatLockPinError = null
                                        if (pinManager.verifyPin(input) is PinVerificationResult.Success) {
                                            viewModel.unlockChatForSession()
                                            chatLockPinInput = ""
                                        }
                                    }
                                },
                                singleLine = true,
                                isError = chatLockPinError != null,
                                label = { Text("Privacy PIN", color = BuddysTheme.colors.textSecondary) },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BuddysTheme.colors.primaryRed,
                                    unfocusedBorderColor = BuddysTheme.colors.border,
                                    focusedTextColor = BuddysTheme.colors.textPrimary,
                                    unfocusedTextColor = BuddysTheme.colors.textPrimary,
                                    focusedContainerColor = BuddysTheme.colors.surfaceSecondary,
                                    unfocusedContainerColor = BuddysTheme.colors.surfaceSecondary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (chatLockPinError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = chatLockPinError ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.error)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            BuddysButton(
                                text = "Unlock Chat",
                                onClick = {
                                    val result = pinManager.verifyPin(chatLockPinInput)
                                    if (result is PinVerificationResult.Success) {
                                        viewModel.unlockChatForSession()
                                        chatLockPinInput = ""
                                    } else if (result is PinVerificationResult.Throttled) {
                                        chatLockPinError = "Too many attempts. Wait ${result.remainingSeconds}s"
                                    } else {
                                        chatLockPinError = "Incorrect PIN"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Search Results Summary Banner (in Search Mode)
                if (uiState.isSearchModeActive && uiState.searchQuery.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.searchResults.isEmpty()) "No results found" else "${uiState.searchResults.size} ${if (uiState.searchResults.size == 1) "result" else "results"} found",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (uiState.searchResults.isEmpty()) BuddysTheme.colors.textMuted else BuddysTheme.colors.primaryRed,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Pinned Message Bar below TopBar
            val pinnedMessagesList = remember(uiState.pinnedMessageIds, uiState.messages) {
                uiState.messages.filter { uiState.pinnedMessageIds.contains(it.id) }
            }
            if (!uiState.isSearchModeActive && pinnedMessagesList.isNotEmpty()) {
                val latestPinned = pinnedMessagesList.last()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border))
                        .clickable {
                            viewModel.jumpToMessage(latestPinned.id)
                            val idx = uiState.visibleMessages.indexOfFirst { it.id == latestPinned.id }
                            if (idx != -1) {
                                coroutineScope.launch { listState.animateScrollToItem(idx) }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = BuddysTheme.colors.primaryRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (pinnedMessagesList.size > 1) "Pinned Messages (${pinnedMessagesList.size})" else "Pinned Message",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BuddysTheme.colors.primaryRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        )
                        val snippet = when {
                            latestPinned.isImageMessage -> "📷 Photo"
                            latestPinned.isVoiceMessage -> "🎤 Voice message"
                            else -> latestPinned.content
                        }
                        Text(
                            text = snippet,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BuddysTheme.colors.textPrimary,
                                fontSize = 12.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openPinnedSheet() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "View all pinned",
                            tint = BuddysTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Messages Area Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Custom Chat Wallpaper Background (if configured)
                val currentBgPath = uiState.chatBackgroundPath
                if (!currentBgPath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(currentBgPath),
                        contentDescription = "Chat Wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Readability overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (BuddysTheme.colors.isDark) {
                                    Color.Black.copy(alpha = 0.65f)
                                } else {
                                    Color.White.copy(alpha = 0.50f)
                                }
                            )
                    )
                }

                val messages = uiState.visibleMessages
                val lastSeenOutgoingMessageId = remember(messages) {
                    messages.findLast { it.senderId == currentUserId && it.status == "SEEN" }?.id
                }
                val latestOutgoingMessageId = remember(messages) {
                    messages.findLast { it.senderId == currentUserId }?.id
                }
                val isLatestSeen = (lastSeenOutgoingMessageId != null && lastSeenOutgoingMessageId == latestOutgoingMessageId)
                val hasWallpaper = !uiState.chatBackgroundPath.isNullOrBlank()

                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            BuddysSpiderEmblem(size = 40.dp, tint = BuddysTheme.colors.primaryRed)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Start a private chat",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BuddysTheme.colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Say hi to ${otherUser?.displayName ?: "your buddy"}!",
                                style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        itemsIndexed(
                            items = messages,
                            key = { _, msg -> msg.id }
                        ) { index, message ->
                            val isOutgoing = message.senderId == currentUserId
                            val isSelected = uiState.selectedMessageIds.contains(message.id)
                            val isThisPlaying = uiState.playbackState.currentMessageId == message.id && uiState.playbackState.isPlaying
                            val isThisBuffering = uiState.playbackState.currentMessageId == message.id && uiState.playbackState.isBuffering
                            val currentPbPos = if (uiState.playbackState.currentMessageId == message.id) uiState.playbackState.currentPositionMs else 0L

                            // Spacing: 4dp consecutive from same sender, 12dp different sender
                            val prevMessage = if (index > 0) messages[index - 1] else null
                            val topSpacing = if (prevMessage == null) 0.dp else if (prevMessage.senderId == message.senderId) 4.dp else 12.dp

                            if (topSpacing > 0.dp) {
                                Spacer(modifier = Modifier.height(topSpacing))
                            }

                            // Unread Messages Divider
                            if (message.id == uiState.firstUnreadMessageId && uiState.unreadCount > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(BuddysTheme.colors.border)
                                    )
                                    Text(
                                        text = "  ${uiState.unreadCount} NEW MESSAGES  ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BuddysTheme.colors.primaryRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.8.sp
                                        )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(BuddysTheme.colors.border)
                                    )
                                }
                            }

                            val showSeenReceipt = isOutgoing && isLatestSeen && (message.id == lastSeenOutgoingMessageId)

                            NostalgicMessageItem(
                                message = message,
                                isOutgoing = isOutgoing,
                                showSeenReceipt = showSeenReceipt,
                                hasWallpaper = hasWallpaper,
                                isHighlighted = (message.id == uiState.highlightedMessageId),
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = isSelected,
                                isPlayingVoice = isThisPlaying,
                                isBufferingVoice = isThisBuffering,
                                voicePlaybackPosMs = currentPbPos,
                                playbackSpeed = uiState.playbackState.speed,
                                onToggleSpeed = {
                                    viewModel.togglePlaybackSpeed()
                                },
                                onVoicePlayPauseClick = {
                                    if (isThisPlaying) {
                                        viewModel.pauseVoiceMessage()
                                    } else {
                                        viewModel.playVoiceMessage(message)
                                    }
                                },
                                onVoiceSeek = { pos ->
                                    viewModel.seekVoiceMessage(pos)
                                },
                                onImageClick = { url ->
                                    viewModel.openFullScreenImage(url)
                                },
                                onLongClick = {
                                    if (!uiState.isSelectionMode) {
                                        viewModel.openMessageMenu(message)
                                    }
                                },
                                onToggleSelect = {
                                    viewModel.toggleMessageSelection(message.id)
                                },
                                onReactionClick = { emoji ->
                                    viewModel.toggleReaction(message, emoji)
                                },
                                onReplyCardClick = { replyId ->
                                    val targetIndex = messages.indexOfFirst { it.id == replyId }
                                    if (targetIndex != -1) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(targetIndex)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // "In this chat" Avatar Indicator
            AnimatedVisibility(visible = uiState.isOtherUserInChat && !uiState.isOtherUserTyping) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarView(
                        imageUrl = otherUser?.avatarUrl,
                        displayName = otherUser?.displayName ?: "Buddy",
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${otherUser?.displayName?.ifBlank { otherUser.username } ?: "Buddy"} is in this chat",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BuddysTheme.colors.success,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            // Floating Typing Pill
            AnimatedVisibility(visible = uiState.isOtherUserTyping) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 6.dp)
                ) {
                    TypingIndicatorPill(displayName = otherUser?.displayName ?: "Friend")
                }
            }

            // Uploading Media Progress Pill
            AnimatedVisibility(visible = uiState.isUploadingMedia) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(BuddysTheme.colors.surfaceSecondary)
                            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = BuddysTheme.colors.primaryRed,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sending media...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BuddysTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Selected Image Preview Banner
            AnimatedVisibility(visible = uiState.selectedImageUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BuddysTheme.colors.surface)
                        .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = uiState.selectedImageUri,
                        contentDescription = "Selected Image Preview",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Photo attached",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.textPrimary
                            )
                        )
                        Text(
                            text = "Add a caption or send directly",
                            style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = { viewModel.cancelSelectedImage() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel image",
                            tint = BuddysTheme.colors.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Reply Preview Banner
            AnimatedVisibility(visible = uiState.replyingToMessage != null) {
                uiState.replyingToMessage?.let { replyMsg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BuddysTheme.colors.surfaceSecondary)
                            .border(1.dp, BuddysTheme.colors.primaryRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = BuddysTheme.colors.primaryRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${replyMsg.senderName.ifBlank { "message" }}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BuddysTheme.colors.primaryRed
                                )
                            )
                            val previewText = when {
                                replyMsg.isImageMessage -> "📷 Image"
                                replyMsg.isVoiceMessage -> "🎤 Voice message · ${ChatUtils.formatDuration(replyMsg.durationMs)}"
                                else -> replyMsg.content
                            }
                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.cancelReply() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = BuddysTheme.colors.textSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Edit Preview Banner
            AnimatedVisibility(visible = uiState.editingMessage != null) {
                uiState.editingMessage?.let {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BuddysTheme.colors.surfaceSecondary)
                            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Editing message...",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BuddysTheme.colors.textPrimary),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.cancelEdit() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = BuddysTheme.colors.textSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // ==========================================
            // COMPOSER CONTAINER
            // ==========================================
            when {
                // 1. RECORDING AUDIO MODE
                uiState.isRecordingAudio -> {
                    if (uiState.isRecordingLocked) {
                        // Locked Recording Bar: Trash | Live Waveform Equalizer | Timer | Pause/Resume | Send
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(BuddysTheme.colors.surface)
                                .border(1.dp, BuddysTheme.colors.primaryRed.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cancel / Trash
                            IconButton(
                                onClick = { viewModel.cancelAudioRecording() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancel Recording",
                                    tint = BuddysTheme.colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Live Waveform Visualizer
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val amplitudes = uiState.liveWaveformAmplitudes
                                amplitudes.forEach { amp ->
                                    val barHeight = (4.dp + (amp * 22).dp).coerceIn(4.dp, 26.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (uiState.isRecordingPaused) BuddysTheme.colors.textMuted
                                                else BuddysTheme.colors.primaryRed
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Timer
                            Text(
                                text = ChatUtils.formatDuration(uiState.recordingDurationSec * 1000L),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (uiState.isRecordingPaused) BuddysTheme.colors.textSecondary else BuddysTheme.colors.primaryRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Pause / Resume Button
                            IconButton(
                                onClick = {
                                    if (uiState.isRecordingPaused) {
                                        viewModel.resumeAudioRecording()
                                    } else {
                                        viewModel.pauseAudioRecording()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isRecordingPaused) Icons.Default.Mic else Icons.Default.Pause,
                                    contentDescription = if (uiState.isRecordingPaused) "Resume" else "Pause",
                                    tint = BuddysTheme.colors.primaryRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Send Button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BuddysTheme.colors.primaryRed)
                                    .clickable { viewModel.stopAudioRecording() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Recording",
                                    tint = BuddysTheme.colors.textOnPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // Unlocked Active Recording Bar: Cancel | Pulsing Red Dot | Live Equalizer | Timer | Lock button | Stop/Send
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(BuddysTheme.colors.softRed)
                                .border(1.dp, BuddysTheme.colors.primaryRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cancel
                            IconButton(
                                onClick = { viewModel.cancelAudioRecording() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancel",
                                    tint = BuddysTheme.colors.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Pulsing dot
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val dotAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(BuddysTheme.colors.primaryRed.copy(alpha = dotAlpha))
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = ChatUtils.formatDuration(uiState.recordingDurationSec * 1000L),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BuddysTheme.colors.textPrimary,
                                    fontSize = 13.sp
                                )
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Lock Button
                            TextButton(
                                onClick = { viewModel.lockAudioRecording() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock",
                                    tint = BuddysTheme.colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Lock",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BuddysTheme.colors.textSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Done / Send
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BuddysTheme.colors.primaryRed)
                                    .clickable { viewModel.stopAudioRecording() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done recording",
                                    tint = BuddysTheme.colors.textOnPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 2. VOICE NOTE PREVIEW MODE (Recorded, awaiting user decision)
                uiState.recordedVoiceFile != null -> {
                    val isPreviewPlaying = uiState.playbackState.currentMessageId == "preview_voice" && uiState.playbackState.isPlaying
                    val previewPosition = if (uiState.playbackState.currentMessageId == "preview_voice") uiState.playbackState.currentPositionMs else 0L

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BuddysTheme.colors.surface)
                            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Delete recording button
                        IconButton(
                            onClick = { viewModel.deleteVoicePreview() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete voice",
                                tint = BuddysTheme.colors.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Play / Pause preview button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BuddysTheme.colors.surfaceSecondary)
                                .clickable {
                                    if (isPreviewPlaying) {
                                        viewModel.pauseVoicePreview()
                                    } else {
                                        viewModel.playVoicePreview()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play preview",
                                tint = BuddysTheme.colors.primaryRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Duration Text
                        Text(
                            text = if (isPreviewPlaying) {
                                "${ChatUtils.formatDuration(previewPosition)} / ${ChatUtils.formatDuration(uiState.recordedVoiceDurationMs)}"
                            } else {
                                "🎤 ${ChatUtils.formatDuration(uiState.recordedVoiceDurationMs)}"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BuddysTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Send Voice Message Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BuddysTheme.colors.primaryRed)
                                .clickable(enabled = !uiState.isUploadingMedia) {
                                    viewModel.sendVoiceMessage()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send voice message",
                                tint = BuddysTheme.colors.textOnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 3. NORMAL TEXT / IMAGE COMPOSER MODE
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BuddysTheme.colors.surface)
                            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment Plus Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BuddysTheme.colors.surfaceSecondary)
                                .clickable {
                                    viewModel.openAttachmentMenu()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attachment",
                                tint = BuddysTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Input TextField
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        ) {
                            if (uiState.inputText.isEmpty()) {
                                Text(
                                    text = if (uiState.selectedImageUri != null) "Add a caption..." else "Message...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = BuddysTheme.colors.textMuted,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = uiState.inputText,
                                onValueChange = { viewModel.onInputTextChanged(it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textPrimary,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(BuddysTheme.colors.primaryRed)
                            )
                        }

                        val hasText = uiState.inputText.isNotBlank()
                        val hasImage = uiState.selectedImageUri != null

                        if (hasText || hasImage || uiState.editingMessage != null) {
                            // Send / Save Button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BuddysTheme.colors.primaryRed)
                                    .clickable(enabled = !uiState.isUploadingMedia && !uiState.isSending) {
                                        if (hasImage) {
                                            viewModel.sendSelectedImage()
                                        } else {
                                            viewModel.sendMessage()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = BuddysTheme.colors.textOnPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // Voice Microphone Button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BuddysTheme.colors.surfaceSecondary)
                                    .clickable {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            viewModel.startAudioRecording()
                                        } else {
                                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record voice message",
                                    tint = BuddysTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Attachment Bottom Sheet Dialog
        if (uiState.showAttachmentMenu) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissAttachmentMenu() },
                containerColor = BuddysTheme.colors.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Share media",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textPrimary
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Gallery Option
                        AttachmentMenuItem(
                            icon = Icons.Default.PhotoLibrary,
                            label = "Gallery",
                            backgroundColor = BuddysTheme.colors.surfaceSecondary,
                            tint = BuddysTheme.colors.primaryRed
                        ) {
                            viewModel.dismissAttachmentMenu()
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }

                        // Camera Option
                        AttachmentMenuItem(
                            icon = Icons.Default.CameraAlt,
                            label = "Camera",
                            backgroundColor = BuddysTheme.colors.surfaceSecondary,
                            tint = BuddysTheme.colors.primaryRed
                        ) {
                            viewModel.dismissAttachmentMenu()
                            cameraLauncher.launch(null)
                        }
                    }
                }
            }
        }

        // Custom Chat Background / Theme Bottom Sheet
        if (uiState.showThemeSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissThemeSheet() },
                containerColor = BuddysTheme.colors.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Chat Background",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BuddysTheme.colors.textPrimary,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize the background wallpaper for this conversation.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.textSecondary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Mini Chat Wallpaper Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(16.dp))
                            .background(BuddysTheme.colors.background)
                    ) {
                        val currentSavedPath = uiState.chatBackgroundPath
                        val previewModel: Any? = when {
                            uiState.previewBackgroundUri != null -> uiState.previewBackgroundUri
                            !currentSavedPath.isNullOrBlank() -> File(currentSavedPath)
                            else -> null
                        }

                        if (previewModel != null) {
                            AsyncImage(
                                model = previewModel,
                                contentDescription = "Wallpaper preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Readability overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (BuddysTheme.colors.isDark) Color.Black.copy(alpha = 0.45f)
                                        else Color.White.copy(alpha = 0.30f)
                                    )
                            )
                        }

                        // Mock Message Bubbles to illustrate contrast
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Incoming Mock Bubble
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 4.dp))
                                    .background(BuddysTheme.colors.surfaceSecondary)
                                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 4.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Hey! How does this wallpaper look?",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BuddysTheme.colors.textPrimary,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            // Outgoing Mock Bubble
                            Row(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 4.dp))
                                    .background(BuddysTheme.colors.primaryRed)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Looks super clean and readable 🔥",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BuddysTheme.colors.textOnPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buttons Area
                    if (uiState.previewBackgroundUri != null) {
                        // Staged for application: Show Apply & Cancel Preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelBackgroundPreview() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BuddysTheme.colors.textSecondary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { viewModel.applyChatBackground() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BuddysTheme.colors.primaryRed,
                                    contentColor = BuddysTheme.colors.textOnPrimary
                                )
                            ) {
                                Text("Apply", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Normal sheet actions: Choose from gallery / Remove background
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    wallpaperPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BuddysTheme.colors.primaryRed,
                                    contentColor = BuddysTheme.colors.textOnPrimary
                                )
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose from Gallery", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                            }
                        }

                        if (!uiState.chatBackgroundPath.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.removeChatBackground() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = BuddysTheme.colors.error)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove Background", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Full Screen Image Viewer Modal
        if (uiState.fullScreenImageUrl != null) {
            FullScreenImageViewer(
                imageUrl = uiState.fullScreenImageUrl,
                onDismiss = { viewModel.closeFullScreenImage() }
            )
        }

        // Message Long-Press Context Dialog
        uiState.activeMenuMessage?.let { message ->
            val isSender = message.senderId == currentUserId

            AlertDialog(
                onDismissRequest = { viewModel.closeMessageMenu() },
                containerColor = BuddysTheme.colors.surface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    // Quick Emoji Reaction Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val reactions = listOf("❤️", "😂", "👍", "😮", "😢", "🔥")
                        reactions.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.toggleReaction(message, emoji)
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Reply
                        MenuActionRow(icon = Icons.AutoMirrored.Filled.Reply, title = "Reply", color = BuddysTheme.colors.primaryRed) {
                            viewModel.startReply(message)
                        }

                        // Copy Text (for text messages or image captions)
                        if (!message.isVoiceMessage && message.content.isNotBlank() && message.content != "[Image]") {
                            MenuActionRow(icon = Icons.Default.ContentCopy, title = "Copy text", color = BuddysTheme.colors.textPrimary) {
                                clipboardManager.setText(AnnotatedString(message.content))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                viewModel.closeMessageMenu()
                            }
                        }

                        // Pin / Unpin message
                        val isPinned = uiState.pinnedMessageIds.contains(message.id)
                        if (isPinned) {
                            MenuActionRow(icon = Icons.Default.PushPin, title = "Unpin message", color = BuddysTheme.colors.textPrimary) {
                                viewModel.unpinMessage(message.id)
                                viewModel.closeMessageMenu()
                            }
                        } else {
                            MenuActionRow(icon = Icons.Default.PushPin, title = "Pin message", color = BuddysTheme.colors.primaryRed) {
                                viewModel.pinMessage(message.id)
                                viewModel.closeMessageMenu()
                            }
                        }

                        // Save / Star Message
                        MenuActionRow(icon = Icons.Default.Bookmark, title = "Save to Stars", color = BuddysTheme.colors.primaryRed) {
                            viewModel.saveMessage(message)
                            Toast.makeText(context, "Saved message", Toast.LENGTH_SHORT).show()
                        }

                        // Remind Me
                        var showReminderDialog by remember { mutableStateOf(false) }
                        MenuActionRow(icon = Icons.Default.Alarm, title = "Remind me...", color = BuddysTheme.colors.textPrimary) {
                            showReminderDialog = true
                        }

                        if (showReminderDialog) {
                            AlertDialog(
                                onDismissRequest = { showReminderDialog = false },
                                title = { Text("Set Message Reminder") },
                                text = {
                                    Column {
                                        TextButton(onClick = {
                                            viewModel.remindMe(message, 15)
                                            showReminderDialog = false
                                            Toast.makeText(context, "Reminder set for 15 minutes", Toast.LENGTH_SHORT).show()
                                        }) { Text("In 15 minutes") }
                                        TextButton(onClick = {
                                            viewModel.remindMe(message, 60)
                                            showReminderDialog = false
                                            Toast.makeText(context, "Reminder set for 1 hour", Toast.LENGTH_SHORT).show()
                                        }) { Text("In 1 hour") }
                                        TextButton(onClick = {
                                            viewModel.remindMe(message, 1440)
                                            showReminderDialog = false
                                            Toast.makeText(context, "Reminder set for tomorrow", Toast.LENGTH_SHORT).show()
                                        }) { Text("Tomorrow") }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showReminderDialog = false }) { Text("Cancel") }
                                }
                            )
                        }

                        // Forward
                        MenuActionRow(icon = Icons.AutoMirrored.Filled.Reply, title = "Forward", color = BuddysTheme.colors.textPrimary) {
                            viewModel.enterSelectionMode(message.id)
                            viewModel.openForwardDialog()
                        }

                        // Multi-Select
                        MenuActionRow(icon = Icons.Default.Checklist, title = "Select multiple", color = BuddysTheme.colors.textPrimary) {
                            viewModel.enterSelectionMode(message.id)
                        }

                        // Edit (if sender and text message)
                        if (isSender && !message.isUnsent && !message.isImageMessage && !message.isVoiceMessage) {
                            MenuActionRow(icon = Icons.Default.Edit, title = "Edit message", color = BuddysTheme.colors.primaryRed) {
                                viewModel.startEdit(message)
                            }
                        }

                        // Unsend (if sender)
                        if (isSender && !message.isUnsent) {
                            MenuActionRow(icon = Icons.AutoMirrored.Filled.Undo, title = "Unsend message", color = BuddysTheme.colors.error) {
                                viewModel.unsendMessage(message)
                            }
                        }

                        // Delete For Me
                        MenuActionRow(icon = Icons.Default.Delete, title = "Delete for me", color = BuddysTheme.colors.error) {
                            viewModel.deleteMessageForMe(message)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.closeMessageMenu() }) {
                        Text("Done", color = BuddysTheme.colors.primaryRed, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Media, Links & Files Modal Sheet
        if (uiState.showMediaLinksFilesSheet) {
            MediaLinksFilesSheet(
                mediaMessages = uiState.mediaMessages,
                linkItems = uiState.linkItems,
                voiceMessages = uiState.voiceMessages,
                onDismiss = { viewModel.dismissMediaLinksFilesSheet() },
                onImageClick = { url -> viewModel.openFullScreenImage(url) },
                onPlayVoiceClick = { msg -> viewModel.playVoiceMessage(msg) }
            )
        }

        // Pinned Messages Modal Sheet
        if (uiState.showPinnedSheet) {
            val allPinned = remember(uiState.pinnedMessageIds, uiState.messages) {
                uiState.messages.filter { uiState.pinnedMessageIds.contains(it.id) }
            }
            PinnedMessagesSheet(
                pinnedMessages = allPinned,
                onDismiss = { viewModel.dismissPinnedSheet() },
                onJumpToMessage = { msgId ->
                    viewModel.dismissPinnedSheet()
                    viewModel.jumpToMessage(msgId)
                    val targetIdx = uiState.visibleMessages.indexOfFirst { it.id == msgId }
                    if (targetIdx != -1) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(targetIdx)
                        }
                    }
                },
                onUnpinMessage = { msgId ->
                    val msg = uiState.messages.find { it.id == msgId }
                    if (msg != null) viewModel.togglePinMessage(msg)
                }
            )
        }

        // Forward Messages Dialog
        if (uiState.showForwardDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissForwardDialog() },
                title = {
                    Text(
                        text = "Forward to...",
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary
                    )
                },
                text = {
                    var forwardQuery by remember { mutableStateOf("") }
                    val filteredFriends = remember(forwardQuery, uiState.friends) {
                        if (forwardQuery.isBlank()) uiState.friends
                        else uiState.friends.filter {
                            it.displayName.contains(forwardQuery, ignoreCase = true) ||
                            it.username.contains(forwardQuery, ignoreCase = true)
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                        OutlinedTextField(
                            value = forwardQuery,
                            onValueChange = { forwardQuery = it },
                            placeholder = { Text("Search friends...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        if (filteredFriends.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No friends found", color = BuddysTheme.colors.textMuted, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(filteredFriends.size) { idx ->
                                    val friend = filteredFriends[idx]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                viewModel.forwardSelectedMessages(friend.chatId.ifBlank { friend.friendUid })
                                                Toast.makeText(context, "Forwarded to ${friend.displayName.ifBlank { friend.username }}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarView(
                                            imageUrl = friend.avatarUrl,
                                            displayName = friend.displayName.ifBlank { friend.username },
                                            size = 36.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = friend.displayName.ifBlank { friend.username },
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                            if (friend.username.isNotBlank()) {
                                                Text(
                                                    text = "@${friend.username}",
                                                    fontSize = 12.sp,
                                                    color = BuddysTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Forward",
                                            tint = BuddysTheme.colors.primaryRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissForwardDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun AttachmentMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = BuddysTheme.colors.textPrimary
            )
        )
    }
}

@Composable
private fun MenuActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(color = color, fontWeight = FontWeight.SemiBold)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NostalgicMessageItem(
    message: Message,
    isOutgoing: Boolean,
    showSeenReceipt: Boolean,
    hasWallpaper: Boolean,
    isHighlighted: Boolean = false,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isPlayingVoice: Boolean,
    isBufferingVoice: Boolean,
    voicePlaybackPosMs: Long,
    playbackSpeed: Float = 1.0f,
    onToggleSpeed: () -> Unit = {},
    onVoicePlayPauseClick: () -> Unit,
    onVoiceSeek: (Long) -> Unit,
    onImageClick: (String) -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onReactionClick: (String) -> Unit,
    onReplyCardClick: (String) -> Unit
) {
    val density = LocalDensity.current
    val maxSwipeOffsetPx = with(density) { 70.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val formattedTime = remember(message.timestamp) {
        ChatUtils.formatMessageTime(message.timestamp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .semantics {
                contentDescription = "${if (isOutgoing) "Sent message" else "Received message"}: ${message.content.ifBlank { message.type }}, sent at $formattedTime"
            }
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        coroutineScope.launch {
                            delay(1000)
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-maxSwipeOffsetPx, 0f)
                            offsetX.snapTo(newOffset)
                        }
                    }
                )
            }
    ) {
        // Timestamp revealed at the right edge when dragging left
        val revealProgress = (kotlin.math.abs(offsetX.value) / maxSwipeOffsetPx).coerceIn(0f, 1f)
        if (revealProgress > 0.05f && formattedTime.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .alpha(revealProgress)
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BuddysTheme.colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Message Row shifted by drag offset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
            horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = BuddysTheme.colors.primaryRed)
                )
            }

            Column(
                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                // Quoted Reply Bubble Header
                if (!message.replyToText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isOutgoing) BuddysTheme.colors.primaryRed.copy(alpha = 0.85f)
                                else BuddysTheme.colors.surfaceSecondary
                            )
                            .border(
                                1.dp,
                                if (isOutgoing) BuddysTheme.colors.primaryRed else BuddysTheme.colors.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { message.replyToMessageId?.let { onReplyCardClick(it) } }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(
                                text = message.replyToSenderName ?: "Reply",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutgoing) BuddysTheme.colors.textOnPrimary else BuddysTheme.colors.primaryRed
                                )
                            )
                            Text(
                                text = message.replyToText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isOutgoing) BuddysTheme.colors.textOnPrimary.copy(alpha = 0.85f) else BuddysTheme.colors.textSecondary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                val highlightBorderWidth by animateDpAsState(
                    targetValue = if (isHighlighted) 2.dp else 1.dp,
                    animationSpec = tween(300),
                    label = "highlightBorder"
                )

                val outgoingBubbleColor = if (BuddysTheme.colors.isDark) Color(0xFFC71D25) else BuddysTheme.colors.primaryRed
                val incomingBubbleColor = if (hasWallpaper) {
                    if (BuddysTheme.colors.isDark) Color(0xFF161820).copy(alpha = 0.95f) else Color(0xFFFFFFFF).copy(alpha = 0.95f)
                } else {
                    if (BuddysTheme.colors.isDark) Color(0xFF161820) else Color(0xFFF1F3F7)
                }

                val bubbleShape = if (message.isMissedCallMessage) {
                    RoundedCornerShape(12.dp)
                } else {
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 16.dp
                    )
                }

                // Message Bubble
                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(
                            color = when {
                                message.isMissedCallMessage -> if (BuddysTheme.colors.isDark) Color(0xFF1C171A) else Color(0xFFFDF0F1)
                                isHighlighted && !isOutgoing -> BuddysTheme.colors.softRed.copy(alpha = 0.5f)
                                message.isUnsent -> BuddysTheme.colors.softRed.copy(alpha = 0.25f)
                                isOutgoing -> outgoingBubbleColor
                                else -> incomingBubbleColor
                            }
                        )
                        .border(
                            width = highlightBorderWidth,
                            color = when {
                                message.isMissedCallMessage -> Color(0xFFE5454C).copy(alpha = 0.4f)
                                isHighlighted -> BuddysTheme.colors.primaryRed
                                message.isUnsent -> BuddysTheme.colors.softRed.copy(alpha = 0.6f)
                                isOutgoing -> Color.Transparent
                                else -> if (BuddysTheme.colors.isDark) Color(0xFF232632) else Color(0xFFE2E4EB)
                            },
                            shape = bubbleShape
                        )
                        .combinedClickable(
                            onClick = {
                                if (message.isImageMessage && !message.isUnsent && !message.mediaUrl.isNullOrBlank()) {
                                    onImageClick(message.mediaUrl)
                                }
                            },
                            onLongClick = onLongClick
                        )
                        .padding(
                            if (message.isImageMessage && !message.isUnsent) PaddingValues(4.dp) else PaddingValues(horizontal = 13.dp, vertical = 7.dp)
                        )
                ) {
                    when {
                        message.isUnsent -> {
                            val unsentText = when {
                                message.isImageMessage -> "🖼️ This message was unsent"
                                message.isVoiceMessage -> "🎤 This message was unsent"
                                else -> "This message was unsent"
                            }
                            Text(
                                text = unsentText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.error,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 13.5.sp
                                )
                            )
                        }

                        // MISSED CALL CARD
                        message.isMissedCallMessage -> {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneMissed,
                                    contentDescription = "Missed Call",
                                    tint = BuddysTheme.colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = message.content.ifBlank { "Missed Audio/Video Call" },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = BuddysTheme.colors.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Text(
                                        text = "Tap to call back",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BuddysTheme.colors.primaryRed,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        // STORY REPLY
                        message.isStoryReplyMessage -> {
                            Column {
                                if (!message.storyReplyPreviewUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = message.storyReplyPreviewUrl,
                                        contentDescription = "Story preview",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = "Replied to story",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isOutgoing) BuddysTheme.colors.textOnPrimary.copy(alpha = 0.7f) else BuddysTheme.colors.primaryRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    )
                                )
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isOutgoing) BuddysTheme.colors.textOnPrimary else BuddysTheme.colors.textPrimary,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }

                        // IMAGE MESSAGE
                        message.isImageMessage && !message.mediaUrl.isNullOrBlank() -> {
                            Column {
                                AsyncImage(
                                    model = message.mediaUrl,
                                    contentDescription = "Image message",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 140.dp, max = 260.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                if (message.content.isNotBlank() && message.content != "[Image]") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isOutgoing) BuddysTheme.colors.textOnPrimary else BuddysTheme.colors.textPrimary,
                                            fontSize = 14.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // VOICE MESSAGE
                        message.isVoiceMessage && !message.mediaUrl.isNullOrBlank() -> {
                            VoiceMessageBubble(
                                messageId = message.id,
                                durationMs = message.durationMs,
                                isPlaying = isPlayingVoice,
                                isBuffering = isBufferingVoice,
                                currentPositionMs = voicePlaybackPosMs,
                                isOutgoing = isOutgoing,
                                playbackSpeed = playbackSpeed,
                                onPlayPauseClick = onVoicePlayPauseClick,
                                onSeek = onVoiceSeek,
                                onToggleSpeed = onToggleSpeed
                            )
                        }

                        // TEXT MESSAGE
                        else -> {
                            Column {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isOutgoing) BuddysTheme.colors.textOnPrimary else BuddysTheme.colors.textPrimary,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    )
                                )

                                if (message.isEdited) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Edited",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isOutgoing) BuddysTheme.colors.textOnPrimary.copy(alpha = 0.7f) else BuddysTheme.colors.textMuted,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Grouped Emoji Reactions
                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        val grouped = message.reactions.values.groupingBy { it }.eachCount()
                        grouped.forEach { (emoji, count) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BuddysTheme.colors.surface)
                                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(10.dp))
                                    .clickable { onReactionClick(emoji) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (count > 1) "$emoji $count" else emoji,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = BuddysTheme.colors.textPrimary)
                                )
                            }
                        }
                    }
                }

                // Single "Seen" Receipt at Bottom of Outgoing Thread
                if (showSeenReceipt) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Seen",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BuddysTheme.colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

private fun saveBitmapToTempUri(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val cacheDir = File(context.cacheDir, "camera_images").apply {
            if (!exists()) mkdirs()
        }
        val file = File(cacheDir, "cam_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

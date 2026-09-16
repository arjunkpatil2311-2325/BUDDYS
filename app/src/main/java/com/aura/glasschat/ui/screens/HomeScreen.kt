package com.aura.glasschat.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.glasschat.data.model.Chat
import com.aura.glasschat.data.model.Friend
import com.aura.glasschat.data.model.User
import com.aura.glasschat.data.model.UserStories
import com.aura.glasschat.data.repository.FollowRepository
import com.aura.glasschat.data.repository.FollowStatus
import com.aura.glasschat.data.update.UpdateManifest
import com.aura.glasschat.ui.components.*
import com.aura.glasschat.ui.theme.*
import com.aura.glasschat.ui.viewmodel.HomeViewModel
import com.aura.glasschat.ui.viewmodel.ProfileViewModel
import com.aura.glasschat.ui.viewmodel.SearchUserItem
import com.aura.glasschat.ui.viewmodel.SearchViewModel
import com.aura.glasschat.ui.viewmodel.StoryViewModel
import com.aura.glasschat.util.ChatUtils

enum class HomeBottomTab {
    CHATS, UPDATES, CALLS, PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChat: (chatId: String, otherUserId: String) -> Unit,
    onOpenAddFriend: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenEditProfile: () -> Unit = {},
    onOpenPrivacySettings: () -> Unit = {},
    onOpenFollowers: (userId: String) -> Unit = {},
    onOpenFollowing: (userId: String) -> Unit = {},
    onOpenPublicProfile: (userId: String) -> Unit = {},
    onOpenCreateStory: () -> Unit = {},
    onOpenStoryViewer: (userId: String) -> Unit = {},
    onOpenSavedMessages: () -> Unit = {},
    onOpenCloseFriends: () -> Unit = {},
    onOpenStoryArchive: () -> Unit = {},
    onOpenHiddenChats: () -> Unit = {},
    onOpenStorageManager: () -> Unit = {},
    onOpenCall: (otherUserId: String, otherName: String, isVideo: Boolean) -> Unit = { _, _, _ -> },
    onLoggedOut: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    storyViewModel: StoryViewModel = viewModel(),
    followRepository: FollowRepository = remember { FollowRepository() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val storyUiState by storyViewModel.uiState.collectAsState()
    val currentUser = uiState.currentUser
    val authRepository = remember { com.aura.glasschat.data.repository.AuthRepository() }
    val currentUid = (currentUser?.uid ?: "").ifBlank { authRepository.currentUserId }
    val unreadNotifs by followRepository.observeUnreadNotificationCount(currentUid).collectAsState(initial = 0)
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(HomeBottomTab.CHATS) }
    var selectedChatForMenu by remember { mutableStateOf<Chat?>(null) }
    var chatFilterChip by remember { mutableStateOf("ALL") } // ALL, UNREAD, CLOSE_FRIENDS, GROUPS, PINNED

    val appLockManager = remember { com.aura.glasschat.security.AppLockManager.getInstance(context) }
    val pinManager = appLockManager.pinManager
    val isPinSet by pinManager.isPinSet.collectAsState()
    var chatToUnlockWithPin by remember { mutableStateOf<Chat?>(null) }
    var lockPinInput by remember { mutableStateOf("") }
    var lockPinError by remember { mutableStateOf<String?>(null) }

    // Profile photo picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profileViewModel.onPhotoSelected(context, uri)
        }
    }

    val profileUiState by profileViewModel.uiState.collectAsState()
    LaunchedEffect(profileUiState.isLoggedOut) {
        if (profileUiState.isLoggedOut) {
            onLoggedOut()
        }
    }

    // Helper to perform actual chat navigation
    fun performNavigation(chat: Chat) {
        val otherUid = chat.getOtherParticipantUid(currentUid).ifBlank {
            chat.participants.firstOrNull { it != currentUid } ?: ""
        }
        val validChatId = chat.chatId.ifBlank {
            if (currentUid.isNotBlank() && otherUid.isNotBlank()) ChatUtils.getDeterministicChatId(currentUid, otherUid) else ""
        }
        if (validChatId.isNotBlank() && otherUid.isNotBlank()) {
            onOpenChat(validChatId, otherUid)
        } else if (otherUid.isNotBlank()) {
            val fallbackChatId = ChatUtils.getDeterministicChatId(currentUid, otherUid)
            onOpenChat(fallbackChatId, otherUid)
        }
    }

    // Helper to safely navigate to chat with PIN challenge if locked
    fun openChatSafe(chat: Chat) {
        if (chat.isLocked(currentUid) && isPinSet) {
            chatToUnlockWithPin = chat
            lockPinInput = ""
            lockPinError = null
        } else {
            performNavigation(chat)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BuddysTheme.colors.background
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val totalUnreadChats = uiState.chats.count { it.hasUnread(currentUid) }
                val hasUnreadUpdates = storyUiState.activeUserStories.any { it.hasUnreadFor(currentUid) }
                val missedCallsCount = uiState.callHistory.count { it.status == "MISSED" || (it.status == "REJECTED" && it.callerUid != currentUid) }
                BuddysBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> selectedTab = tab },
                    userAvatarUrl = currentUser?.avatarUrl,
                    userDisplayName = currentUser?.displayName ?: "Me",
                    unreadChatsCount = totalUnreadChats,
                    hasUnreadUpdates = hasUnreadUpdates,
                    missedCallsCount = missedCallsCount
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
            ) {
                when (selectedTab) {
                    // ===================================================
                    // TAB 1: CHATS (PRIMARY MESSAGING HUB)
                    // ===================================================
                    HomeBottomTab.CHATS -> {
                        val totalUnreadChats = uiState.chats.count { it.hasUnread(currentUid) }
                        val categories = listOf(
                            "ALL" to ("All" to null),
                            "UNREAD" to ("Unread" to totalUnreadChats),
                            "CLOSE_FRIENDS" to ("Close Friends" to null),
                            "GROUPS" to ("Groups" to null),
                            "PINNED" to ("Pinned" to null)
                        )

                        val chatsToDisplay = uiState.filteredChats.filter { chat ->
                            val otherUid = chat.getOtherParticipantUid(currentUid)
                            when (chatFilterChip) {
                                "UNREAD" -> chat.hasUnread(currentUid)
                                "PINNED" -> chat.isPinned(currentUid)
                                "CLOSE_FRIENDS" -> currentUser?.closeFriends?.contains(otherUid) == true
                                "GROUPS" -> chat.participants.size > 2
                                else -> true
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Offline State Banner
                                if (!uiState.isOnline) {
                                    Surface(
                                        color = Color(0xFFD92D35),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Waiting for network connection...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // 1. Top Header: [Avatar] [Brand]  [Search] [Add] [Notifications]
                                TopBuddysHeader(
                                    userAvatarUrl = currentUser?.avatarUrl,
                                    userDisplayName = currentUser?.displayName ?: "Me",
                                    unreadNotificationCount = unreadNotifs,
                                    onProfileClick = { selectedTab = HomeBottomTab.PROFILE },
                                    onSearchClick = onOpenSearch,
                                    onActivityClick = onOpenNotifications,
                                    onAddFriendClick = onOpenAddFriend
                                )

                                // 2. Profile Notes & Stories Header (Instagram/Telegram carousel)
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // User's own note bubble + story avatar
                                    item {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(72.dp).clickable { viewModel.openNoteDialog() }
                                        ) {
                                            Box(contentAlignment = Alignment.TopCenter) {
                                                Surface(
                                                    color = BuddysTheme.colors.surface,
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border),
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                ) {
                                                    val noteDisplay = if (currentUser?.isNoteActive == true && !currentUser.note.isNullOrBlank()) currentUser.note else "+ Thought"
                                                    Text(
                                                        text = noteDisplay,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (currentUser?.isNoteActive == true) BuddysTheme.colors.textPrimary else BuddysTheme.colors.primaryRed,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            AvatarView(
                                                imageUrl = currentUser?.avatarUrl,
                                                displayName = currentUser?.displayName ?: "Me",
                                                size = 52.dp,
                                                isOnline = true
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Your note",
                                                fontSize = 11.sp,
                                                color = BuddysTheme.colors.textSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // Active Stories
                                    items(storyUiState.activeUserStories, key = { it.userId }) { userStory ->
                                        if (userStory.userId != currentUid) {
                                            val isCloseFriend = currentUser?.closeFriends?.contains(userStory.userId) == true
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(64.dp).clickable { onOpenStoryViewer(userStory.userId) }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .border(
                                                            2.dp,
                                                            if (isCloseFriend) Brush.linearGradient(listOf(Color(0xFF22A06B), Color(0xFF28B772)))
                                                            else if (userStory.hasUnreadFor(currentUid)) StoryRingGradient
                                                            else Brush.linearGradient(listOf(BuddysTheme.colors.border, BuddysTheme.colors.border)),
                                                            CircleShape
                                                        )
                                                        .padding(3.dp)
                                                ) {
                                                    AvatarView(
                                                        imageUrl = userStory.userAvatarUrl,
                                                        displayName = userStory.userDisplayName,
                                                        size = 50.dp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = userStory.userDisplayName.ifBlank { userStory.username },
                                                    fontSize = 11.sp,
                                                    color = BuddysTheme.colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // 3. Filter Category Pills
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(categories) { (code, pair) ->
                                        val (label, count) = pair
                                        BuddysTabPill(
                                            text = label,
                                            isSelected = chatFilterChip == code,
                                            onClick = { chatFilterChip = code },
                                            count = count,
                                            trailingEmoji = if (code == "CLOSE_FRIENDS") "⭐" else null
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // 4. Hidden Chats Tile (if any exist)
                                if (uiState.hiddenChatsCount > 0) {
                                    Surface(
                                        color = BuddysTheme.colors.surfaceSecondary,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .clickable { onOpenHiddenChats() }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = BuddysTheme.colors.primaryRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Hidden Chats",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = BuddysTheme.colors.textPrimary
                                                )
                                                Text(
                                                    text = "${uiState.hiddenChatsCount} conversations protected with PIN",
                                                    fontSize = 11.sp,
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

                                // 5. Chat Feed List
                                if (chatsToDisplay.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BuddysEmptyState(
                                            title = if (chatFilterChip == "ALL") "Welcome to Buddies" else "No chats in this filter",
                                            subtitle = if (chatFilterChip == "ALL") "Find your buddies, share moments, and chat securely." else "Try choosing another filter tab or start a new chat.",
                                            icon = Icons.AutoMirrored.Filled.Chat,
                                            actionText = if (chatFilterChip != "ALL") "View all chats" else null,
                                            onActionClick = { chatFilterChip = "ALL" }
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentPadding = PaddingValues(top = 2.dp, bottom = 80.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        items(
                                            items = chatsToDisplay,
                                            key = { it.chatId }
                                        ) { chat ->
                                            val otherUid = chat.getOtherParticipantUid(currentUid)
                                            val hasStory = storyUiState.activeUserStories.any { it.userId == otherUid }
                                            PremiumChatRow(
                                                chat = chat,
                                                currentUid = currentUid,
                                                hasActiveStory = hasStory,
                                                onClick = { openChatSafe(chat) },
                                                onLongClick = { selectedChatForMenu = chat },
                                                onCameraClick = onOpenCreateStory
                                            )
                                        }
                                    }
                                }
                            }

                            // 6. 3D Floating Action Button (FAB) for New Chat
                            ThreeDFloatingButton(
                                onClick = onOpenAddFriend,
                                icon = Icons.Default.Edit,
                                contentDescription = "New Chat",
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 20.dp, bottom = 20.dp)
                            )
                        }
                    }

                    // ===================================================
                    // TAB 2: UPDATES (24-HOUR STORIES & MOMENTS)
                    // ===================================================
                    HomeBottomTab.UPDATES -> {
                        val otherStories = storyUiState.activeUserStories.filter { it.userId != currentUid }
                        val myStories = storyUiState.activeUserStories.find { it.userId == currentUid }
                        val hasMyStory = myStories != null && myStories.stories.isNotEmpty()

                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 80.dp)
                            ) {
                                // Top Header: Updates + Story Archive Action
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Updates",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BuddysTheme.colors.textPrimary,
                                            fontSize = 24.sp
                                        )
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = onOpenStoryArchive,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(BuddysTheme.colors.surfaceSecondary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = "Story Archive",
                                                tint = BuddysTheme.colors.textPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = onOpenCreateStory,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(BuddysTheme.colors.primaryRed)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = "Create Story",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                val updateManager = remember { com.aura.glasschat.data.update.UpdateManager.getInstance(context) }
                                val availableUpdate by updateManager.availableUpdate.collectAsState()

                                if (availableUpdate != null) {
                                    UpdateAvailableBanner(
                                        manifest = availableUpdate!!,
                                        onUpdateClick = { updateManager.requestUpdatePrompt(it) },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }

                                // "Status / My Story" Section
                                Text(
                                    text = "Status",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BuddysTheme.colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )

                                Surface(
                                    color = BuddysTheme.colors.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clickable {
                                            if (hasMyStory) onOpenStoryViewer(currentUid)
                                            else onOpenCreateStory()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .then(
                                                    if (hasMyStory) Modifier.border(2.5.dp, StoryRingGradient, CircleShape).padding(2.5.dp)
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AvatarView(
                                                imageUrl = currentUser?.avatarUrl,
                                                displayName = currentUser?.displayName ?: "Me",
                                                size = 50.dp
                                            )
                                            if (!hasMyStory) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(BuddysTheme.colors.primaryRed)
                                                        .border(1.5.dp, BuddysTheme.colors.surface, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "My Status",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                            Text(
                                                text = if (hasMyStory) "Tap to view status updates" else "Tap to add status update",
                                                fontSize = 12.5.sp,
                                                color = BuddysTheme.colors.textSecondary
                                            )
                                        }

                                        IconButton(onClick = onOpenCreateStory) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Camera",
                                                tint = BuddysTheme.colors.primaryRed,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Recent Updates from Buddies
                                Text(
                                    text = "Recent Updates",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BuddysTheme.colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )

                                if (otherStories.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BuddysEmptyState(
                                            title = "No recent updates",
                                            subtitle = "When your buddies share 24-hour stories, they'll show up here.",
                                            icon = Icons.Default.AutoAwesome
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        otherStories.forEach { userStory ->
                                            val isCloseFriend = currentUser?.closeFriends?.contains(userStory.userId) == true
                                            Surface(
                                                color = BuddysTheme.colors.surface,
                                                shape = RoundedCornerShape(14.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onOpenStoryViewer(userStory.userId) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(52.dp)
                                                            .border(
                                                                2.5.dp,
                                                                if (isCloseFriend) Brush.linearGradient(listOf(Color(0xFF22A06B), Color(0xFF28B772)))
                                                                else if (userStory.hasUnreadFor(currentUid)) StoryRingGradient
                                                                else Brush.linearGradient(listOf(BuddysTheme.colors.border, BuddysTheme.colors.border)),
                                                                CircleShape
                                                            )
                                                            .padding(2.5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        AvatarView(
                                                            imageUrl = userStory.userAvatarUrl,
                                                            displayName = userStory.userDisplayName,
                                                            size = 46.dp
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(14.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = userStory.userDisplayName.ifBlank { userStory.username },
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 15.sp,
                                                                color = BuddysTheme.colors.textPrimary
                                                            )
                                                            if (isCloseFriend) {
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Icon(
                                                                    imageVector = Icons.Default.Stars,
                                                                    contentDescription = "Close Friend",
                                                                    tint = Color(0xFF22A06B),
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = "${userStory.stories.size} ${if (userStory.stories.size == 1) "story" else "stories"} · Active 24h",
                                                            fontSize = 12.5.sp,
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
                                }
                            }

                            // 3D Camera Floating Action Button
                            ThreeDFloatingButton(
                                onClick = onOpenCreateStory,
                                icon = Icons.Default.CameraAlt,
                                contentDescription = "Post Story",
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 20.dp, bottom = 20.dp)
                            )
                        }
                    }

                    // ===================================================
                    // TAB 3: CALLS (VOICE & VIDEO CALL CENTER)
                    // ===================================================
                    HomeBottomTab.CALLS -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Top Header: Calls
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Calls",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BuddysTheme.colors.textPrimary,
                                            fontSize = 24.sp
                                        )
                                    )
                                    IconButton(
                                        onClick = onOpenAddFriend,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(BuddysTheme.colors.surfaceSecondary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Call a Buddy",
                                            tint = BuddysTheme.colors.textPrimary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }

                                if (uiState.callHistory.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BuddysEmptyState(
                                            title = "No call history",
                                            subtitle = "Start secure audio and video calls with your buddies.",
                                            icon = Icons.Default.Phone,
                                            actionText = "Find buddies",
                                            onActionClick = onOpenAddFriend
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(uiState.callHistory, key = { it.callId }) { call ->
                                            val isOutgoing = call.callerUid == currentUid
                                            val otherUid = if (isOutgoing) call.receiverUid else call.callerUid
                                            val otherName = if (isOutgoing) call.receiverName else call.callerName
                                            val otherAvatar = if (isOutgoing) call.receiverAvatarUrl else call.callerAvatarUrl
                                            val isMissed = call.status == "MISSED" || (call.status == "REJECTED" && !isOutgoing)

                                            Surface(
                                                color = BuddysTheme.colors.surface,
                                                shape = RoundedCornerShape(14.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AvatarView(
                                                        imageUrl = otherAvatar,
                                                        displayName = otherName.ifBlank { "Buddy" },
                                                        size = 46.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = otherName.ifBlank { "Buddy" },
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isMissed) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textPrimary,
                                                            fontSize = 15.sp
                                                        )
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                                                                contentDescription = null,
                                                                tint = if (isMissed) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textSecondary,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = if (isMissed) "Missed" else if (isOutgoing) "Outgoing" else "Incoming",
                                                                color = if (isMissed) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textSecondary,
                                                                fontSize = 12.5.sp
                                                            )
                                                            Text(
                                                                text = " · ${call.createdAt?.let { ChatUtils.formatTimestamp(it) } ?: "Recent"}",
                                                                color = BuddysTheme.colors.textMuted,
                                                                fontSize = 12.5.sp
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            onOpenCall(otherUid, otherName, call.isVideo)
                                                        },
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .clip(CircleShape)
                                                            .background(BuddysTheme.colors.surfaceSecondary)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                                                            contentDescription = "Call Back",
                                                            tint = BuddysTheme.colors.primaryRed,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ===================================================
                    // TAB 4: PROFILE / YOU (ACCOUNT & SETTINGS HUB)
                    // ===================================================
                    HomeBottomTab.PROFILE -> {
                        val profileUser = profileUiState.user ?: currentUser

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 80.dp)
                        ) {
                            // Top Bar: You
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "You",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BuddysTheme.colors.textPrimary,
                                        fontSize = 24.sp
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { profileUser?.let { onOpenPublicProfile(it.uid) } },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(BuddysTheme.colors.surfaceSecondary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = "QR Code",
                                            tint = BuddysTheme.colors.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onOpenEditProfile,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(BuddysTheme.colors.surfaceSecondary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Profile",
                                            tint = BuddysTheme.colors.textPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onOpenPrivacySettings,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(BuddysTheme.colors.surfaceSecondary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = BuddysTheme.colors.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            val updateManager = remember { com.aura.glasschat.data.update.UpdateManager.getInstance(context) }
                            val availableUpdate by updateManager.availableUpdate.collectAsState()

                            if (availableUpdate != null) {
                                UpdateAvailableBanner(
                                    manifest = availableUpdate!!,
                                    onUpdateClick = { updateManager.requestUpdatePrompt(it) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            // Profile Hero Card
                            Surface(
                                color = BuddysTheme.colors.surface,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        AvatarView(
                                            imageUrl = profileUser?.avatarUrl,
                                            displayName = profileUser?.displayName ?: "User",
                                            size = 80.dp,
                                            isOnline = true
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(BuddysTheme.colors.primaryRed)
                                                .clickable { profileViewModel.openPhotoOptions() }
                                                .border(2.dp, BuddysTheme.colors.surface, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Change Photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = profileUser?.displayName ?: "Buddies User",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = BuddysTheme.colors.textPrimary
                                    )

                                    if (!profileUser?.username.isNullOrBlank()) {
                                        Text(
                                            text = "@${profileUser?.username}",
                                            fontSize = 13.5.sp,
                                            color = BuddysTheme.colors.primaryRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (!profileUser?.bio.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = profileUser?.bio ?: "",
                                            fontSize = 13.sp,
                                            color = BuddysTheme.colors.textSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Follower / Following Stats
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { profileUser?.let { onOpenFollowers(it.uid) } }
                                        ) {
                                            Text(
                                                text = "${profileUser?.followerCount ?: 0}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                            Text(
                                                text = "Followers",
                                                fontSize = 12.sp,
                                                color = BuddysTheme.colors.textSecondary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(30.dp)
                                                .background(BuddysTheme.colors.divider)
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { profileUser?.let { onOpenFollowing(it.uid) } }
                                        ) {
                                            Text(
                                                text = "${profileUser?.followingCount ?: 0}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = BuddysTheme.colors.textPrimary
                                            )
                                            Text(
                                                text = "Following",
                                                fontSize = 12.sp,
                                                color = BuddysTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Features & Hub List
                            Text(
                                text = "Features & Tools",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BuddysTheme.colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )

                            Surface(
                                color = BuddysTheme.colors.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    ProfileHubItem(
                                        icon = Icons.Default.Bookmark,
                                        iconTint = Color(0xFF0084FF),
                                        title = "Saved Messages",
                                        subtitle = "Personal cloud bookmarks & notes",
                                        onClick = onOpenSavedMessages
                                    )
                                    HorizontalDivider(color = BuddysTheme.colors.divider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                                    ProfileHubItem(
                                        icon = Icons.Default.Stars,
                                        iconTint = Color(0xFF22A06B),
                                        title = "Close Friends",
                                        subtitle = "Manage private story audience",
                                        onClick = onOpenCloseFriends
                                    )
                                    HorizontalDivider(color = BuddysTheme.colors.divider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                                    ProfileHubItem(
                                        icon = Icons.Default.History,
                                        iconTint = BuddysTheme.colors.primaryRed,
                                        title = "Story Archive",
                                        subtitle = "View your past 24h stories",
                                        onClick = onOpenStoryArchive
                                    )
                                    HorizontalDivider(color = BuddysTheme.colors.divider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                                    ProfileHubItem(
                                        icon = Icons.Default.Settings,
                                        iconTint = Color(0xFFEAA11A),
                                        title = "Settings & Privacy",
                                        subtitle = "Privacy PIN, Biometrics, App Lock & Hidden chats",
                                        onClick = onOpenPrivacySettings
                                    )
                                    HorizontalDivider(color = BuddysTheme.colors.divider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                                    ProfileHubItem(
                                        icon = Icons.Default.Storage,
                                        iconTint = Color(0xFFA855F7),
                                        title = "Storage & Data",
                                        subtitle = "Manage cache and media downloads",
                                        onClick = onOpenStorageManager
                                    )
                                    HorizontalDivider(color = BuddysTheme.colors.divider.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                                    ProfileHubItem(
                                        icon = Icons.Default.PersonAdd,
                                        iconTint = BuddysTheme.colors.primaryRed,
                                        title = "Pair Buddy Code",
                                        subtitle = "Connect using 15-minute temporary codes",
                                        onClick = onOpenAddFriend
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Account Section
                            Surface(
                                color = BuddysTheme.colors.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BuddysTheme.colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    ProfileHubItem(
                                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                                        iconTint = BuddysTheme.colors.error,
                                        title = "Sign Out",
                                        subtitle = "Log out from this device",
                                        onClick = { profileViewModel.signOut() }
                                    )
                                }
                            }
                        }

                        if (profileUiState.showPhotoOptions) {
                            AlertDialog(
                                onDismissRequest = { profileViewModel.dismissPhotoOptions() },
                                containerColor = BuddysTheme.colors.surface,
                                shape = RoundedCornerShape(24.dp),
                                title = { Text("Profile Picture", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    profileViewModel.dismissPhotoOptions()
                                                    galleryLauncher.launch("image/*")
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BuddysTheme.colors.primaryRed, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Text("Choose from gallery", fontWeight = FontWeight.SemiBold, color = BuddysTheme.colors.textPrimary)
                                        }
                                        if (!profileUser?.avatarUrl.isNullOrBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { profileViewModel.removePhoto() }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = BuddysTheme.colors.error, modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Text("Remove photo", fontWeight = FontWeight.SemiBold, color = BuddysTheme.colors.error)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { profileViewModel.dismissPhotoOptions() }) {
                                        Text("Cancel", color = BuddysTheme.colors.textSecondary)
                                    }
                                }
                            )
                        }
                    }
                }

                // Incoming Call Banner Overlay
                IncomingCallOverlay(
                    incomingCall = uiState.incomingCall,
                    onAccept = { incoming ->
                        onOpenCall(incoming.callerUid, incoming.callerName, incoming.isVideo)
                    },
                    onDecline = { incoming ->
                        viewModel.declineIncomingCall(incoming)
                    }
                )
            }
        }


        // ==========================================
        // CHAT CONTEXT MENU DIALOG (PIN / MUTE / LOCK / HIDE)
        // ==========================================
        selectedChatForMenu?.let { chat ->
            val isPinned = chat.isPinned(currentUid)
            val isMuted = chat.isMuted(currentUid)
            val isLocked = chat.isLocked(currentUid)
            val isHidden = chat.isHidden(currentUid)
            val otherInfo = chat.getOtherParticipantInfo(currentUid)

            AlertDialog(
                onDismissRequest = { selectedChatForMenu = null },
                containerColor = BuddysTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarView(imageUrl = otherInfo.avatarUrl, displayName = otherInfo.displayName, size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = otherInfo.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.textPrimary
                            )
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Pin / Unpin
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.togglePinChat(chat)
                                    selectedChatForMenu = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = BuddysTheme.colors.primaryRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = if (isPinned) "Unpin conversation" else "Pin to top",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Mute / Unmute
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.toggleMuteChat(chat)
                                    selectedChatForMenu = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.NotificationsNone else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = BuddysTheme.colors.primaryRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = if (isMuted) "Unmute notifications" else "Mute notifications",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Lock / Unlock Chat (PIN Protected)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.toggleLockChat(chat)
                                    selectedChatForMenu = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = BuddysTheme.colors.primaryRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = if (isLocked) "Unlock conversation" else "Lock conversation (PIN)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Hide / Unhide Chat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.toggleHideChat(chat)
                                    selectedChatForMenu = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = BuddysTheme.colors.primaryRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = if (isHidden) "Unhide conversation" else "Hide conversation",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedChatForMenu = null }) {
                        Text("Done", color = BuddysTheme.colors.primaryRed, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // ==========================================
        // PIN CHALLENGE DIALOG FOR LOCKED CHAT
        // ==========================================
        chatToUnlockWithPin?.let { chat ->
            val otherInfo = chat.getOtherParticipantInfo(currentUid)
            AlertDialog(
                onDismissRequest = {
                    chatToUnlockWithPin = null
                    lockPinInput = ""
                    lockPinError = null
                },
                containerColor = BuddysTheme.colors.surface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BuddysTheme.colors.primaryRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Locked Chat",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BuddysTheme.colors.textPrimary
                            )
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Enter your Privacy PIN to unlock conversation with ${otherInfo.displayName}.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BuddysTheme.colors.textSecondary,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = lockPinInput,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { ch -> ch.isDigit() }) {
                                    lockPinInput = input
                                    lockPinError = null
                                    if (pinManager.verifyPin(input) is com.aura.glasschat.security.PinVerificationResult.Success) {
                                        val target = chatToUnlockWithPin
                                        chatToUnlockWithPin = null
                                        lockPinInput = ""
                                        target?.let { performNavigation(it) }
                                    }
                                }
                            },
                            singleLine = true,
                            isError = lockPinError != null,
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
                        if (lockPinError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = lockPinError ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = BuddysTheme.colors.error)
                            )
                        }
                    }
                },
                confirmButton = {
                    BuddysButton(
                        text = "Unlock",
                        onClick = {
                            val result = pinManager.verifyPin(lockPinInput)
                            if (result is com.aura.glasschat.security.PinVerificationResult.Success) {
                                val target = chatToUnlockWithPin
                                chatToUnlockWithPin = null
                                lockPinInput = ""
                                target?.let { performNavigation(it) }
                            } else if (result is com.aura.glasschat.security.PinVerificationResult.Throttled) {
                                lockPinError = "Too many attempts. Wait ${result.remainingSeconds}s"
                            } else {
                                lockPinError = "Incorrect PIN"
                            }
                        }
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            chatToUnlockWithPin = null
                            lockPinInput = ""
                            lockPinError = null
                        }
                    ) {
                        Text("Cancel", color = BuddysTheme.colors.textSecondary)
                    }
                }
            )
        }

        // ==========================================
        // 24-HOUR THOUGHT NOTE DIALOG
        // ==========================================
        if (uiState.showNoteDialog) {
            var noteInput by remember { mutableStateOf(currentUser?.note ?: "") }
            val charCountColor = when {
                noteInput.length >= 60 -> BuddysTheme.colors.primaryRed
                noteInput.length >= 50 -> Color(0xFFE5A00D)
                else -> BuddysTheme.colors.textMuted
            }

            AlertDialog(
                onDismissRequest = { viewModel.closeNoteDialog() },
                containerColor = BuddysTheme.colors.surface,
                shape = RoundedCornerShape(18.dp),
                title = {
                    Text(
                        text = if (currentUser?.isNoteActive == true) "Edit Thought Note" else "Share a Thought",
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Share what's on your mind with your buddies. Disappears automatically after 24 hours.",
                            fontSize = 12.5.sp,
                            color = BuddysTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(BuddysTheme.colors.surfaceSecondary)
                                .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (noteInput.isEmpty()) {
                                Text(
                                    text = "What's on your mind? (max 60 chars)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = BuddysTheme.colors.textMuted,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = noteInput,
                                onValueChange = { if (it.length <= 60) noteInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = BuddysTheme.colors.textPrimary,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(BuddysTheme.colors.primaryRed)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${noteInput.length}/60",
                            fontSize = 11.5.sp,
                            fontWeight = if (noteInput.length >= 50) FontWeight.Bold else FontWeight.Normal,
                            color = charCountColor,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                },
                confirmButton = {
                    BuddysButton(
                        text = "Share",
                        onClick = {
                            viewModel.saveNote(noteInput.trim())
                        },
                        enabled = noteInput.isNotBlank()
                    )
                },
                dismissButton = {
                    Row {
                        if (currentUser?.isNoteActive == true) {
                            TextButton(onClick = { viewModel.deleteNote() }) {
                                Text("Delete", color = BuddysTheme.colors.primaryRed)
                            }
                        }
                        TextButton(onClick = { viewModel.closeNoteDialog() }) {
                            Text("Cancel", color = BuddysTheme.colors.textSecondary)
                        }
                    }
                }
            )
        }
    }
}

// ====================================================================
// SEARCH USER ROW ITEM
// ====================================================================
@Composable
private fun SearchUserRow(
    item: SearchUserItem,
    onClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BuddysTheme.colors.surface)
            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(
            imageUrl = item.user.avatarUrl,
            displayName = item.user.displayName,
            size = 50.dp,
            isOnline = item.user.isOnline
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.user.displayName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${item.user.username}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BuddysTheme.colors.primaryRed,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        when (item.followStatus) {
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
            else -> {}
        }
    }
}

// ====================================================================
// TOP BUDDYS HEADER (SNAPCHAT STYLE: [Avatar] [⌕]  Chat  [🔔] [👤+] [⋮])
// ====================================================================
@Composable
private fun TopBuddysHeader(
    userAvatarUrl: String?,
    userDisplayName: String,
    unreadNotificationCount: Int,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    onActivityClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onMoreClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = BuddysTheme.colors.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BuddysTheme.colors.border.copy(alpha = 0.8f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action Cluster: 3D Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThreeDAvatar(
                    imageUrl = userAvatarUrl,
                    displayName = userDisplayName,
                    size = 38.dp,
                    isOnline = true,
                    onClick = onProfileClick
                )

                ThreeDIconButton(
                    onClick = onSearchClick,
                    icon = Icons.Default.Search,
                    contentDescription = "Search",
                    size = 36.dp,
                    iconSize = 18.dp
                )
            }

            // Center Brand Title: Emblem + BUDDYS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BuddysSpiderEmblem(size = 18.dp, tint = BuddysTheme.colors.primaryRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Buddies",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 19.sp,
                        letterSpacing = 1.5.sp
                    )
                )
            }

            // Right Action Cluster: Notifications, Add Friend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    ThreeDIconButton(
                        onClick = onActivityClick,
                        icon = Icons.Default.NotificationsNone,
                        contentDescription = "Notifications",
                        size = 36.dp,
                        iconSize = 18.dp
                    )
                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BuddysTheme.colors.primaryRed)
                        )
                    }
                }

                ThreeDIconButton(
                    onClick = onAddFriendClick,
                    icon = Icons.Default.PersonAdd,
                    contentDescription = "Add Buddy",
                    size = 36.dp,
                    iconSize = 17.dp
                )
            }
        }
    }
}

// ====================================================================
// STORIES / STATUS SECTION
// ====================================================================
@Composable
private fun StoriesSection(
    currentUser: com.aura.glasschat.data.model.User?,
    myStories: com.aura.glasschat.data.model.UserStories?,
    activeUserStories: List<com.aura.glasschat.data.model.UserStories>,
    currentUid: String,
    onOpenCreateStory: () -> Unit,
    onOpenStoryViewer: (userId: String) -> Unit,
    onOpenAddFriend: () -> Unit
) {
    val otherUserStories = activeUserStories.filter { it.userId != currentUid }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. My Story Card
        item(key = "my_story") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(68.dp)
            ) {
                BuddysStoryRing(
                    imageUrl = currentUser?.avatarUrl,
                    displayName = currentUser?.displayName ?: "Me",
                    size = 64.dp,
                    hasUnreadStory = myStories != null && myStories.stories.isNotEmpty(),
                    isSelf = true,
                    onClick = {
                        if (myStories != null && myStories.stories.isNotEmpty()) {
                            onOpenStoryViewer(currentUid)
                        } else {
                            onOpenCreateStory()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your story",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 2. Active Stories from Buddys
        if (otherUserStories.isNotEmpty()) {
            items(
                items = otherUserStories,
                key = { "story_${it.userId}" }
            ) { userStory ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(68.dp)
                ) {
                    BuddysStoryRing(
                        imageUrl = userStory.userAvatarUrl,
                        displayName = userStory.userDisplayName.ifBlank { userStory.username },
                        size = 64.dp,
                        hasUnreadStory = userStory.hasUnreadFor(currentUid),
                        isSelf = false,
                        onClick = { onOpenStoryViewer(userStory.userId) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = userStory.userDisplayName.split(" ").firstOrNull() ?: userStory.username,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BuddysTheme.colors.textPrimary,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // Minimalist Add Buddies Story Circle
            item(key = "empty_stories_nudge") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clickable { onOpenAddFriend() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(BuddysTheme.colors.surfaceSecondary)
                            .border(1.5.dp, BuddysTheme.colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add buddies",
                            tint = BuddysTheme.colors.primaryRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add buddies",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = BuddysTheme.colors.textSecondary,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ====================================================================
// QUICK SEARCH BAR (CAPSULE)
// ====================================================================
@Composable
private fun QuickSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(BuddysTheme.colors.surfaceSecondary)
            .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(23.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = BuddysTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search buddies, chats & stories...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BuddysTheme.colors.textMuted,
                        fontSize = 13.5.sp
                    )
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 13.5.sp
                ),
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(BuddysTheme.colors.primaryRed)
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = BuddysTheme.colors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BuddysTheme.colors.surface)
                    .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BuddysTheme.colors.primaryRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// ====================================================================
// SECTION HEADER
// ====================================================================
@Composable
private fun SectionHeader(
    title: String,
    badgeText: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BuddysTheme.colors.textSecondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.5.sp
                )
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BuddysTheme.colors.surfaceSecondary)
                        .border(1.dp, BuddysTheme.colors.border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BuddysTheme.colors.primaryRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BuddysTheme.colors.primaryRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

// ====================================================================
// BUDDY CARD ITEM (YOUR BUDDYS ROW)
// ====================================================================
@Composable
private fun BuddyCardItem(
    friend: Friend,
    onClick: () -> Unit
) {
    BuddysCard(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp)
        ) {
            AvatarView(
                imageUrl = friend.avatarUrl,
                displayName = friend.displayName,
                size = 48.dp,
                isOnline = friend.isOnline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = friend.displayName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${friend.username.ifBlank { "buddy" }}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BuddysTheme.colors.textSecondary,
                    fontSize = 10.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (friend.isOnline) BuddysTheme.colors.softRed else BuddysTheme.colors.surfaceSecondary)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (friend.isOnline) "online" else "Buddys",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (friend.isOnline) BuddysTheme.colors.primaryRed else BuddysTheme.colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp
                    )
                )
            }
        }
    }
}

// ====================================================================
// PREMIUM SNAPCHAT CHAT ROW
// ====================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PremiumChatRow(
    chat: Chat,
    currentUid: String,
    hasActiveStory: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCameraClick: () -> Unit = {}
) {
    val otherInfo = chat.getOtherParticipantInfo(currentUid)
    val hasUnread = chat.hasUnread(currentUid)
    val isPinned = chat.isPinned(currentUid)
    val isMuted = chat.isMuted(currentUid)
    val isLocked = chat.isLocked(currentUid)
    val isSentByMe = chat.lastMessageSenderId == currentUid

    val isMediaMessage = chat.lastMessage.contains("[Image]", ignoreCase = true) ||
            chat.lastMessage.contains("photo", ignoreCase = true) ||
            chat.lastMessage.contains("snap", ignoreCase = true)
    val isVoiceMessage = chat.lastMessage.contains("Voice message", ignoreCase = true) ||
            chat.lastMessage.contains("🎤", ignoreCase = true)

    val (deliveryType, statusLabel) = when {
        isSentByMe -> {
            val type = if (isMediaMessage) ChatDeliveryType.MEDIA_DELIVERED
            else if (isVoiceMessage) ChatDeliveryType.VOICE_DELIVERED
            else ChatDeliveryType.TEXT_DELIVERED
            type to "Delivered"
        }
        hasUnread -> {
            val type = if (isMediaMessage) ChatDeliveryType.SNAP_RECEIVED
            else if (isVoiceMessage) ChatDeliveryType.VOICE_SENT
            else ChatDeliveryType.CHAT_RECEIVED
            val label = if (isMediaMessage) "New Snap"
            else if (isVoiceMessage) "New Audio"
            else "New Chat"
            type to label
        }
        else -> {
            val type = if (isMediaMessage) ChatDeliveryType.SNAP_OPENED
            else if (isVoiceMessage) ChatDeliveryType.VOICE_OPENED
            else ChatDeliveryType.CHAT_OPENED
            val label = if (isMediaMessage) "Received" else "Opened"
            type to label
        }
    }

    val timeFormatted = ChatUtils.formatSnapchatTime(chat.lastMessageTimestamp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (hasUnread) BuddysTheme.colors.surfaceSecondary.copy(alpha = 0.5f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 3D Avatar with Story Ring / Bitmoji styling
        Box(
            modifier = Modifier
                .size(50.dp)
                .then(
                    if (hasActiveStory) Modifier.border(2.dp, StoryRingGradient, CircleShape).padding(2.dp)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            ThreeDAvatar(
                imageUrl = otherInfo.avatarUrl,
                displayName = otherInfo.displayName,
                size = 46.dp,
                isOnline = false
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Center Text Column (Name + Streak, Subtitle Delivery Status)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Top Line: Display Name + Emoji / Streak indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = otherInfo.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = BuddysTheme.colors.textPrimary,
                        fontSize = 15.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Pinned Indicator
                if (isPinned) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Pinned",
                        tint = BuddysTheme.colors.primaryRed,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Locked Indicator
                if (isLocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = BuddysTheme.colors.primaryRed,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Muted Indicator
                if (isMuted) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Muted",
                        tint = BuddysTheme.colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Bottom Line: Delivery Icon + Status Text + Time + Streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BuddysChatStatusIcon(
                    type = deliveryType,
                    size = 11.dp
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (hasUnread) BuddysTheme.colors.textPrimary else BuddysTheme.colors.textSecondary,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                )

                Text(
                    text = " · ",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textMuted,
                        fontSize = 13.sp
                    )
                )

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BuddysTheme.colors.textMuted,
                        fontSize = 12.5.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. Right Quick Action: 3D Camera Snap Button [📷]
        ThreeDIconButton(
            onClick = onCameraClick,
            icon = Icons.Outlined.PhotoCamera,
            contentDescription = "Quick Snap",
            size = 36.dp,
            iconSize = 17.dp,
            containerColor = BuddysTheme.colors.surfaceSecondary.copy(alpha = 0.7f),
            tint = BuddysTheme.colors.textSecondary
        )
    }
}

// ====================================================================
// EMPTY HOME STATE
// ====================================================================
@Composable
private fun EmptyHomeState(
    isSearching: Boolean,
    onFindBuddysClick: () -> Unit,
    onPairFriendClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            BuddysSpiderEmblem(size = 56.dp, tint = BuddysTheme.colors.primaryRed)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSearching) "No matches found" else "Welcome to Buddies",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSearching) "Try searching a different name or username." else "Find your friends, share 24h stories, and chat securely.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BuddysTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 13.5.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BuddysButton(
                    text = "Find Buddys",
                    onClick = onFindBuddysClick,
                    leadingIcon = Icons.Default.Search,
                    modifier = Modifier.height(42.dp)
                )

                BuddysOutlinedButton(
                    text = "Pair Code",
                    onClick = onPairFriendClick,
                    leadingIcon = Icons.Default.GroupAdd,
                    modifier = Modifier.height(42.dp)
                )
            }
        }
    }
}



// ====================================================================
// CREATE ACTION SHEET CONTENT
// ====================================================================
@Composable
private fun CreateActionSheetContent(
    onNewChatClick: () -> Unit,
    onAddBuddyClick: () -> Unit,
    onDiscoverBuddysClick: () -> Unit,
    onCreateStoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Create",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BuddysTheme.colors.textPrimary,
                fontSize = 18.sp
            )
        )
        Spacer(modifier = Modifier.height(18.dp))

        ActionSheetRow(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "New Conversation",
            subtitle = "Message your existing buddies",
            badgeColor = BuddysTheme.colors.surfaceSecondary,
            iconTint = BuddysTheme.colors.primaryRed,
            onClick = onNewChatClick
        )

        ActionSheetRow(
            icon = Icons.Default.GroupAdd,
            title = "Add a Buddy",
            subtitle = "Pair using a 15-minute temporary code",
            badgeColor = BuddysTheme.colors.surfaceSecondary,
            iconTint = BuddysTheme.colors.primaryRed,
            onClick = onAddBuddyClick
        )

        ActionSheetRow(
            icon = Icons.Default.CameraAlt,
            title = "Create Story",
            subtitle = "Share a 24-hour photo or video story",
            badgeColor = BuddysTheme.colors.surfaceSecondary,
            iconTint = BuddysTheme.colors.primaryRed,
            onClick = onCreateStoryClick
        )

        ActionSheetRow(
            icon = Icons.Default.Search,
            title = "Discover People",
            subtitle = "Search usernames and follow new people",
            badgeColor = BuddysTheme.colors.surfaceSecondary,
            iconTint = BuddysTheme.colors.primaryRed,
            onClick = onDiscoverBuddysClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ActionSheetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(badgeColor)
                .border(1.dp, BuddysTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BuddysTheme.colors.textPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BuddysTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = BuddysTheme.colors.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ProfileHubItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = BuddysTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = BuddysTheme.colors.textSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = BuddysTheme.colors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

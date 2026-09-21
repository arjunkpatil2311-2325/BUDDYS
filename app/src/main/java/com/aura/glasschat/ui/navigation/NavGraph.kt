package com.aura.glasschat.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aura.glasschat.data.model.UserStories
import com.aura.glasschat.ui.screens.*
import com.aura.glasschat.ui.viewmodel.CallViewModel
import com.aura.glasschat.ui.viewmodel.StoryViewModel

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object AddFriend : Screen("add_friend")
    data object TapToBuddy : Screen("tap_to_buddy")
    data object Search : Screen("search")
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    data object PrivacySettings : Screen("privacy_settings")
    data object PrivacyPinSetup : Screen("privacy_pin_setup")
    data object CommunityRules : Screen("community_rules")
    data object PublicProfile : Screen("public_profile/{userId}") {
        fun createRoute(userId: String): String = "public_profile/$userId"
    }
    data object Followers : Screen("followers/{userId}") {
        fun createRoute(userId: String): String = "followers/$userId"
    }
    data object Following : Screen("following/{userId}") {
        fun createRoute(userId: String): String = "following/$userId"
    }
    data object Chat : Screen("chat/{chatId}/{otherUserId}") {
        fun createRoute(chatId: String, otherUserId: String): String = "chat/$chatId/$otherUserId"
    }
    data object CreateStory : Screen("create_story")
    data object StoryViewer : Screen("story_viewer/{userId}") {
        fun createRoute(userId: String): String = "story_viewer/$userId"
    }
    data object SavedMessages : Screen("saved_messages")
    data object CloseFriends : Screen("close_friends")
    data object StoryArchive : Screen("story_archive")
    data object HiddenChats : Screen("hidden_chats")
    data object StorageManager : Screen("storage_manager")
    data object AppUpdates : Screen("app_updates")
    data object Call : Screen("call/{otherUserId}/{otherName}/{callType}") {
        fun createRoute(otherUserId: String, otherName: String, callType: String, avatarUrl: String? = null): String {
            val encodedName = try { java.net.URLEncoder.encode(otherName, "UTF-8") } catch (_: Exception) { otherName }
            return "call/$otherUserId/$encodedName/$callType"
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(240)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(animationSpec = tween(200))
        }
    ) {
        composable(Screen.Auth.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenChat = { chatId, otherUserId ->
                    if (chatId.isNotBlank() && otherUserId.isNotBlank()) {
                        navController.navigate(Screen.Chat.createRoute(chatId, otherUserId))
                    }
                },
                onOpenAddFriend = {
                    navController.navigate(Screen.AddFriend.route)
                },
                onOpenProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onOpenSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onOpenNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onOpenEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onOpenPrivacySettings = {
                    navController.navigate(Screen.PrivacySettings.route)
                },
                onOpenFollowers = { userId ->
                    navController.navigate(Screen.Followers.createRoute(userId))
                },
                onOpenFollowing = { userId ->
                    navController.navigate(Screen.Following.createRoute(userId))
                },
                onOpenPublicProfile = { userId ->
                    navController.navigate(Screen.PublicProfile.createRoute(userId))
                },
                onOpenCreateStory = {
                    navController.navigate(Screen.CreateStory.route)
                },
                onOpenStoryViewer = { userId ->
                    navController.navigate(Screen.StoryViewer.createRoute(userId))
                },
                onOpenSavedMessages = {
                    navController.navigate(Screen.SavedMessages.route)
                },
                onOpenCloseFriends = {
                    navController.navigate(Screen.CloseFriends.route)
                },
                onOpenStoryArchive = {
                    navController.navigate(Screen.StoryArchive.route)
                },
                onOpenHiddenChats = {
                    navController.navigate(Screen.HiddenChats.route)
                },
                onOpenStorageManager = {
                    navController.navigate(Screen.StorageManager.route)
                },
                onOpenCall = { otherUserId, otherName, isVideo ->
                    navController.navigate(Screen.Call.createRoute(otherUserId, otherName, if (isVideo) "video" else "audio"))
                },
                onLoggedOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { userId ->
                    navController.navigate(Screen.PublicProfile.createRoute(userId))
                }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { userId ->
                    navController.navigate(Screen.PublicProfile.createRoute(userId))
                }
            )
        }

        composable(Screen.AddFriend.route) {
            AddFriendScreen(
                onBack = { navController.popBackStack() },
                onPairingSuccess = { chatId, otherUserId ->
                    navController.navigate(Screen.Chat.createRoute(chatId, otherUserId)) {
                        popUpTo(Screen.AddFriend.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TapToBuddy.route) {
            TapToBuddyScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPairingCode = {
                    navController.navigate(Screen.AddFriend.route)
                },
                onNavigateToChat = { chatId, otherUserId ->
                    navController.navigate(Screen.Chat.createRoute(chatId, otherUserId)) {
                        popUpTo(Screen.TapToBuddy.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.PublicProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            PublicProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onOpenFollowers = { targetId ->
                    navController.navigate(Screen.Followers.createRoute(targetId))
                },
                onOpenFollowing = { targetId ->
                    navController.navigate(Screen.Following.createRoute(targetId))
                },
                onOpenChat = { chatId, otherUserId ->
                    navController.navigate(Screen.Chat.createRoute(chatId, otherUserId))
                }
            )
        }

        composable(
            route = Screen.Followers.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            FollowersScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onOpenProfile = { targetId ->
                    navController.navigate(Screen.PublicProfile.createRoute(targetId))
                }
            )
        }

        composable(
            route = Screen.Following.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            FollowingScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onOpenProfile = { targetId ->
                    navController.navigate(Screen.PublicProfile.createRoute(targetId))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            ChatScreen(
                chatId = chatId,
                otherUserId = otherUserId,
                onBack = { navController.popBackStack() },
                onStartAudioCall = { targetUid, name, avatar ->
                    navController.navigate(Screen.Call.createRoute(targetUid, name, "AUDIO", avatar))
                },
                onStartVideoCall = { targetUid, name, avatar ->
                    navController.navigate(Screen.Call.createRoute(targetUid, name, "VIDEO", avatar))
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onOpenPrivacySettings = {
                    navController.navigate(Screen.PrivacySettings.route)
                },
                onOpenFollowers = { userId ->
                    navController.navigate(Screen.Followers.createRoute(userId))
                },
                onOpenFollowing = { userId ->
                    navController.navigate(Screen.Following.createRoute(userId))
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenRules = {
                    navController.navigate(Screen.CommunityRules.route)
                },
                onOpenPinSetup = {
                    navController.navigate(Screen.PrivacyPinSetup.route)
                },
                onOpenSavedMessages = {
                    navController.navigate(Screen.SavedMessages.route)
                },
                onOpenCloseFriends = {
                    navController.navigate(Screen.CloseFriends.route)
                },
                onOpenStoryArchive = {
                    navController.navigate(Screen.StoryArchive.route)
                },
                onOpenStorageManager = {
                    navController.navigate(Screen.StorageManager.route)
                },
                onOpenAppUpdates = {
                    navController.navigate(Screen.AppUpdates.route)
                },
                onOpenHiddenChats = {
                    navController.navigate(Screen.HiddenChats.route)
                },
                onLoggedOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PrivacyPinSetup.route) {
            PrivacyPinSetupScreen(
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CommunityRules.route) {
            CommunityRulesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateStory.route) {
            StoryStudioScreen(
                onBack = { navController.popBackStack() },
                onStoryPosted = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.StoryViewer.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val storyViewModel: StoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val storyUiState by storyViewModel.uiState.collectAsState()
            val userStories = storyUiState.activeUserStories.find { it.userId == userId }
                ?: UserStories(userId = userId, username = "", userDisplayName = "", userAvatarUrl = null, stories = emptyList())

            StoryViewerScreen(
                userStories = userStories,
                onClose = { navController.popBackStack() },
                onOpenChatWithReply = { chatId, otherUid ->
                    navController.navigate(Screen.Chat.createRoute(chatId, otherUid))
                },
                viewModel = storyViewModel
            )
        }

        composable(Screen.SavedMessages.route) {
            SavedMessagesScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChat = { chatId, _ ->
                    // Find participants or open chat
                    navController.navigate("chat/$chatId/peer")
                }
            )
        }

        composable(Screen.CloseFriends.route) {
            CloseFriendsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StoryArchive.route) {
            StoryArchiveScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HiddenChats.route) {
            HiddenChatsScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { chatId, otherUserId ->
                    navController.navigate(Screen.Chat.createRoute(chatId, otherUserId))
                }
            )
        }

        composable(Screen.StorageManager.route) {
            StorageManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppUpdates.route) {
            AppUpdatesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherName") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val rawName = backStackEntry.arguments?.getString("otherName") ?: "Buddy"
            val otherName = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (_: Exception) { rawName }
            val callTypeStr = backStackEntry.arguments?.getString("callType") ?: "AUDIO"
            val isVideo = callTypeStr == "VIDEO"

            val callViewModel: CallViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            LaunchedEffect(otherUserId, callTypeStr) {
                callViewModel.startOrAnswerCall(
                    otherUserId = otherUserId,
                    otherName = otherName,
                    otherAvatarUrl = null,
                    isVideo = isVideo
                )
            }

            CallScreen(
                onEndCall = { navController.popBackStack() },
                viewModel = callViewModel
            )
        }
    }
}

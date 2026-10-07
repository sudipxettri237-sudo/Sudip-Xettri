package com.example.user

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.common.data.*
import com.example.common.models.Conversation
import com.example.common.models.UserProfile
import com.example.user.auth.UserSignInScreen
import com.example.user.chat.ChatConversationScreen
import com.example.user.chat.ChatListScreen
import com.example.user.explore.ExploreScreen
import com.example.user.feed.FeedScreen
import com.example.user.notifications.NotificationsScreen
import com.example.user.profile.ProfileScreen
import com.example.user.settings.UserSettingsScreen
import kotlinx.coroutines.launch

sealed class UserNavDestination {
    data object Feed : UserNavDestination()
    data object Explore : UserNavDestination()
    data object ChatList : UserNavDestination()
    data object Notifications : UserNavDestination()
    data object MyProfile : UserNavDestination()
    data class UserProfileView(val uid: String) : UserNavDestination()
    data class ActiveChat(val conversation: Conversation) : UserNavDestination()
    data object Settings : UserNavDestination()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserApp(
    onLaunchAdminApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { FirestoreProvider.get(context) }
    val authRepository = remember { AuthRepository(db) }
    val postRepository = remember { PostRepository(db) }
    val chatRepository = remember { ChatRepository(db) }
    val socialRepository = remember { SocialRepository(db) }

    val currentFirebaseUser by authRepository.authStateFlow.collectAsStateWithLifecycle(initialValue = authRepository.currentFirebaseUser)
    var currentProfile by remember { mutableStateOf<UserProfile?>(null) }
    var currentDestination by remember { mutableStateOf<UserNavDestination>(UserNavDestination.Feed) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }

    // Synchronize user profile when signed in
    LaunchedEffect(currentFirebaseUser?.uid) {
        val user = currentFirebaseUser
        if (user != null) {
            currentProfile = authRepository.ensureUserProfileCreated(user)
        } else {
            currentProfile = null
        }
    }

    if (currentFirebaseUser == null || currentProfile == null) {
        UserSignInScreen(
            authRepository = authRepository,
            onSignInSuccess = {
                // Profile will load in LaunchedEffect
            }
        )
        return
    }

    val profile = currentProfile!!

    // Handle Back Press in sub-screens
    BackHandler(enabled = currentDestination !is UserNavDestination.Feed) {
        when (currentDestination) {
            is UserNavDestination.ActiveChat -> currentDestination = UserNavDestination.ChatList
            is UserNavDestination.UserProfileView -> currentDestination = UserNavDestination.Feed
            is UserNavDestination.Settings -> currentDestination = UserNavDestination.MyProfile
            else -> {
                selectedBottomTab = 0
                currentDestination = UserNavDestination.Feed
            }
        }
    }

    val isTopLevelScreen = currentDestination in listOf(
        UserNavDestination.Feed,
        UserNavDestination.Explore,
        UserNavDestination.ChatList,
        UserNavDestination.Notifications,
        UserNavDestination.MyProfile
    )

    Scaffold(
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedBottomTab == 0,
                        onClick = {
                            selectedBottomTab = 0
                            currentDestination = UserNavDestination.Feed
                        },
                        icon = { Icon(Icons.Default.DynamicFeed, contentDescription = "Feed") },
                        label = { Text("Feed") },
                        modifier = Modifier.testTag("nav_feed")
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 1,
                        onClick = {
                            selectedBottomTab = 1
                            currentDestination = UserNavDestination.Explore
                        },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                        label = { Text("Explore") },
                        modifier = Modifier.testTag("nav_explore")
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 2,
                        onClick = {
                            selectedBottomTab = 2
                            currentDestination = UserNavDestination.ChatList
                        },
                        icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Messages") },
                        label = { Text("Messages") },
                        modifier = Modifier.testTag("nav_chat")
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 3,
                        onClick = {
                            selectedBottomTab = 3
                            currentDestination = UserNavDestination.Notifications
                        },
                        icon = { Icon(Icons.Default.NotificationsNone, contentDescription = "Alerts") },
                        label = { Text("Alerts") },
                        modifier = Modifier.testTag("nav_notifications")
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 4,
                        onClick = {
                            selectedBottomTab = 4
                            currentDestination = UserNavDestination.MyProfile
                        },
                        icon = { Icon(Icons.Default.PersonOutline, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (val dest = currentDestination) {
            is UserNavDestination.Feed -> {
                FeedScreen(
                    currentProfile = profile,
                    postRepository = postRepository,
                    socialRepository = socialRepository,
                    onNavigateToProfile = { uid ->
                        currentDestination = if (uid == profile.uid) UserNavDestination.MyProfile else UserNavDestination.UserProfileView(uid)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UserNavDestination.Explore -> {
                ExploreScreen(
                    currentProfile = profile,
                    socialRepository = socialRepository,
                    chatRepository = chatRepository,
                    onNavigateToProfile = { uid ->
                        currentDestination = if (uid == profile.uid) UserNavDestination.MyProfile else UserNavDestination.UserProfileView(uid)
                    },
                    onOpenChat = { otherUser ->
                        coroutineScope.launch {
                            val convId = chatRepository.getOrCreateConversation(
                                currentUserId = profile.uid,
                                currentUserName = profile.displayName,
                                currentUserPhoto = profile.photoURL,
                                otherUserId = otherUser.uid,
                                otherUserName = otherUser.displayName,
                                otherUserPhoto = otherUser.photoURL
                            )
                            currentDestination = UserNavDestination.ActiveChat(
                                Conversation(
                                    conversationId = convId,
                                    participantIds = listOf(profile.uid, otherUser.uid),
                                    participantNames = mapOf(profile.uid to profile.displayName, otherUser.uid to otherUser.displayName),
                                    participantPhotos = mapOf(profile.uid to profile.photoURL, otherUser.uid to otherUser.photoURL)
                                )
                            )
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UserNavDestination.ChatList -> {
                ChatListScreen(
                    currentProfile = profile,
                    chatRepository = chatRepository,
                    onSelectConversation = { conv ->
                        currentDestination = UserNavDestination.ActiveChat(conv)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UserNavDestination.Notifications -> {
                NotificationsScreen(
                    currentProfile = profile,
                    socialRepository = socialRepository,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UserNavDestination.MyProfile -> {
                ProfileScreen(
                    userId = profile.uid,
                    currentUserProfile = profile,
                    authRepository = authRepository,
                    postRepository = postRepository,
                    onOpenSettings = { currentDestination = UserNavDestination.Settings },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UserNavDestination.UserProfileView -> {
                ProfileScreen(
                    userId = dest.uid,
                    currentUserProfile = profile,
                    authRepository = authRepository,
                    postRepository = postRepository,
                    onOpenSettings = { },
                    onBack = { currentDestination = UserNavDestination.Feed },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UserNavDestination.ActiveChat -> {
                ChatConversationScreen(
                    conversation = dest.conversation,
                    currentProfile = profile,
                    chatRepository = chatRepository,
                    onBack = { currentDestination = UserNavDestination.ChatList },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            is UserNavDestination.Settings -> {
                UserSettingsScreen(
                    currentProfile = profile,
                    authRepository = authRepository,
                    onBack = { currentDestination = UserNavDestination.MyProfile },
                    onOpenAdminPanel = onLaunchAdminApp,
                    onSignedOut = {
                        selectedBottomTab = 0
                        currentDestination = UserNavDestination.Feed
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

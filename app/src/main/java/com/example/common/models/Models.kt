package com.example.common.models

import com.google.firebase.Timestamp

enum class UserRole(val value: String) {
    USER("user"),
    MODERATOR("moderator"),
    ADMIN("admin");

    companion object {
        fun from(value: String?): UserRole = when (value?.lowercase()) {
            "admin" -> ADMIN
            "moderator" -> MODERATOR
            else -> USER
        }
    }
}

enum class AccountStatus(val value: String) {
    ACTIVE("active"),
    SUSPENDED("suspended"),
    BANNED("banned");

    companion object {
        fun from(value: String?): AccountStatus = when (value?.lowercase()) {
            "suspended" -> SUSPENDED
            "banned" -> BANNED
            else -> ACTIVE
        }
    }
}

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val photoURL: String = "",
    val coverURL: String = "",
    val bio: String = "",
    val role: String = "user",
    val status: String = "active",
    val isOnline: Boolean = false,
    val lastSeen: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val followerCount: Long = 0L,
    val followingCount: Long = 0L
) {
    val userRole: UserRole get() = UserRole.from(role)
    val accountStatus: AccountStatus get() = AccountStatus.from(status)
}

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoURL: String = "",
    val content: String = "",
    val mediaUrls: String = "", // Comma-separated or URL
    val mediaType: String = "none", // none, image, video
    val visibility: String = "public", // public, followers, hidden
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    val shareCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isLikedByMe: Boolean = false,
    val isSavedByMe: Boolean = false
)

data class Comment(
    val commentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoURL: String = "",
    val content: String = "",
    val createdAt: Timestamp? = null
)

data class Story(
    val storyId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoURL: String = "",
    val mediaUrl: String = "",
    val caption: String = "",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

data class Conversation(
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val updatedAt: Timestamp? = null,
    val unreadCount: Long = 0L
) {
    fun getOtherParticipantName(currentUid: String): String {
        val otherId = participantIds.firstOrNull { it != currentUid } ?: return "User"
        return participantNames[otherId] ?: "User"
    }

    fun getOtherParticipantPhoto(currentUid: String): String {
        val otherId = participantIds.firstOrNull { it != currentUid } ?: return ""
        return participantPhotos[otherId] ?: ""
    }
}

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false
)

data class NotificationItem(
    val notificationId: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "like", // like, comment, follow, message, announcement
    val targetId: String = "",
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)

data class Report(
    val reportId: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val targetType: String = "post", // user, post, comment
    val targetId: String = "",
    val targetSummary: String = "",
    val reason: String = "",
    val status: String = "pending", // pending, reviewed, resolved, rejected
    val moderatorNotes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class AdminLog(
    val logId: String = "",
    val adminUid: String = "",
    val adminName: String = "",
    val action: String = "",
    val targetId: String = "",
    val targetType: String = "",
    val details: String = "",
    val timestamp: Timestamp? = null
)

data class AppSetting(
    val settingId: String = "global",
    val maintenanceMode: Boolean = false,
    val registrationEnabled: Boolean = true,
    val postingEnabled: Boolean = true,
    val messagingEnabled: Boolean = true,
    val moderationEnabled: Boolean = true,
    val updatedAt: Timestamp? = null
)

data class DashboardStats(
    val totalUsers: Long = 0L,
    val onlineUsers: Long = 0L,
    val newUsersToday: Long = 0L,
    val totalPosts: Long = 0L,
    val totalComments: Long = 0L,
    val totalReports: Long = 0L,
    val pendingReports: Long = 0L,
    val suspendedUsers: Long = 0L,
    val bannedUsers: Long = 0L
)

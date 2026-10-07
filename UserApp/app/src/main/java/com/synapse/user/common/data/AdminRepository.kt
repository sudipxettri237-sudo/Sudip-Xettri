package com.example.common.data

import com.example.common.models.AdminLog
import com.example.common.models.AppSetting
import com.example.common.models.Comment
import com.example.common.models.DashboardStats
import com.example.common.models.Post
import com.example.common.models.Report
import com.example.common.models.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class AdminRepository(private val db: FirebaseFirestore) {

    suspend fun getDashboardStats(): DashboardStats {
        val usersSnap = db.collection("users").get().await()
        val totalUsers = usersSnap.size().toLong()
        var onlineUsers = 0L
        var suspendedUsers = 0L
        var bannedUsers = 0L
        var newToday = 0L

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val startOfDay = cal.time

        for (doc in usersSnap.documents) {
            if (doc.getBoolean("isOnline") == true) onlineUsers++
            val status = doc.getString("status")
            if (status == "suspended") suspendedUsers++
            if (status == "banned") bannedUsers++
            val created = doc.getTimestamp("createdAt")
            if (created != null && created.toDate().after(startOfDay)) newToday++
        }

        val postsSnap = db.collection("posts").get().await()
        val totalPosts = postsSnap.size().toLong()

        val reportsSnap = db.collection("reports").get().await()
        val totalReports = reportsSnap.size().toLong()
        val pendingReports = reportsSnap.documents.count { it.getString("status") == "pending" }.toLong()

        return DashboardStats(
            totalUsers = totalUsers,
            onlineUsers = onlineUsers,
            newUsersToday = newToday,
            totalPosts = totalPosts,
            totalReports = totalReports,
            pendingReports = pendingReports,
            suspendedUsers = suspendedUsers,
            bannedUsers = bannedUsers
        )
    }

    suspend fun getAllUsers(query: String = "", filterStatus: String = "all", filterRole: String = "all"): List<UserProfile> {
        val snapshot = db.collection("users").limit(100).get().await()
        var list = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.username.lowercase().contains(q) ||
                it.displayName.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.uid.lowercase().contains(q)
            }
        }
        if (filterStatus != "all") {
            list = list.filter { it.status.equals(filterStatus, ignoreCase = true) }
        }
        if (filterRole != "all") {
            list = list.filter { it.role.equals(filterRole, ignoreCase = true) }
        }
        return list
    }

    suspend fun updateUserStatus(
        targetUid: String,
        newStatus: String,
        adminUid: String,
        adminName: String,
        reason: String = ""
    ): Result<Unit> {
        return try {
            db.collection("users").document(targetUid).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logAdminAction(
                adminUid = adminUid,
                adminName = adminName,
                action = "UPDATE_STATUS_$newStatus".uppercase(),
                targetId = targetUid,
                targetType = "USER",
                details = "Status changed to $newStatus. Reason: $reason"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(
        targetUid: String,
        newRole: String,
        adminUid: String,
        adminName: String
    ): Result<Unit> {
        return try {
            db.collection("users").document(targetUid).update(
                mapOf(
                    "role" to newRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logAdminAction(
                adminUid = adminUid,
                adminName = adminName,
                action = "ROLE_CHANGED_TO_${newRole.uppercase()}",
                targetId = targetUid,
                targetType = "USER",
                details = "Assigned role: $newRole"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllPosts(query: String = ""): List<Post> {
        val snapshot = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()

        var list = snapshot.documents.mapNotNull { doc ->
            val p = doc.toObject(Post::class.java)
            p?.copy(postId = doc.id)
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.content.lowercase().contains(q) ||
                it.authorName.lowercase().contains(q) ||
                it.postId.lowercase().contains(q)
            }
        }
        return list
    }

    suspend fun setPostVisibility(
        postId: String,
        visibility: String,
        adminUid: String,
        adminName: String,
        reason: String = ""
    ): Result<Unit> {
        return try {
            db.collection("posts").document(postId).update("visibility", visibility).await()
            logAdminAction(
                adminUid = adminUid,
                adminName = adminName,
                action = if (visibility == "hidden") "HIDE_POST" else "RESTORE_POST",
                targetId = postId,
                targetType = "POST",
                details = "Visibility set to $visibility. Note: $reason"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String, adminUid: String, adminName: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId).delete().await()
            logAdminAction(
                adminUid = adminUid,
                adminName = adminName,
                action = "DELETE_POST",
                targetId = postId,
                targetType = "POST",
                details = "Permanently deleted by admin"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReports(statusFilter: String = "all"): List<Report> {
        var query = db.collection("reports").orderBy("createdAt", Query.Direction.DESCENDING)
        if (statusFilter != "all") {
            query = query.whereEqualTo("status", statusFilter)
        }
        val snapshot = query.limit(50).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val r = doc.toObject(Report::class.java)
            r?.copy(reportId = doc.id)
        }
    }

    suspend fun updateReport(
        reportId: String,
        newStatus: String,
        notes: String,
        adminUid: String,
        adminName: String
    ): Result<Unit> {
        return try {
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to newStatus,
                    "moderatorNotes" to notes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logAdminAction(
                adminUid = adminUid,
                adminName = adminName,
                action = "REPORT_${newStatus.uppercase()}",
                targetId = reportId,
                targetType = "REPORT",
                details = "Report marked $newStatus. Notes: $notes"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAdminLogs(): Flow<List<AdminLog>> = callbackFlow {
        val query = db.collection("adminLogs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val log = doc.toObject(AdminLog::class.java)
                    log?.copy(logId = doc.id)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun logAdminAction(
        adminUid: String,
        adminName: String,
        action: String,
        targetId: String,
        targetType: String,
        details: String
    ) {
        try {
            val logRef = db.collection("adminLogs").document()
            val data = mapOf(
                "logId" to logRef.id,
                "adminUid" to adminUid,
                "adminName" to adminName,
                "action" to action,
                "targetId" to targetId,
                "targetType" to targetType,
                "details" to details,
                "timestamp" to FieldValue.serverTimestamp()
            )
            logRef.set(data).await()
        } catch (_: Exception) {}
    }

    suspend fun sendAnnouncement(
        title: String,
        message: String,
        targetUid: String? = null,
        adminUid: String,
        adminName: String
    ): Result<Unit> {
        return try {
            if (targetUid != null && targetUid.isNotBlank()) {
                val notifRef = db.collection("notifications").document()
                val data = mapOf(
                    "notificationId" to notifRef.id,
                    "recipientId" to targetUid,
                    "senderId" to adminUid,
                    "senderName" to "Synapse Admin",
                    "type" to "announcement",
                    "targetId" to "",
                    "title" to title,
                    "message" to message,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                notifRef.set(data).await()
            } else {
                // Broadcast to users
                val users = db.collection("users").limit(50).get().await()
                for (doc in users.documents) {
                    val notifRef = db.collection("notifications").document()
                    val data = mapOf(
                        "notificationId" to notifRef.id,
                        "recipientId" to doc.id,
                        "senderId" to adminUid,
                        "senderName" to "Synapse System Announcement",
                        "type" to "announcement",
                        "targetId" to "",
                        "title" to title,
                        "message" to message,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    notifRef.set(data).await()
                }
            }
            logAdminAction(
                adminUid = adminUid,
                adminName = adminName,
                action = "BROADCAST_ANNOUNCEMENT",
                targetId = targetUid ?: "ALL",
                targetType = "NOTIFICATION",
                details = "Title: $title"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAppSettings(): AppSetting {
        return try {
            val doc = db.collection("appSettings").document("global").get().await()
            if (doc.exists()) {
                doc.toObject(AppSetting::class.java) ?: AppSetting()
            } else {
                val defaultSetting = AppSetting()
                db.collection("appSettings").document("global").set(defaultSetting).await()
                defaultSetting
            }
        } catch (e: Exception) {
            AppSetting()
        }
    }

    suspend fun updateAppSettings(
        setting: AppSetting,
        adminUid: String,
        adminName: String
    ): Result<Unit> {
        return try {
            val map = mapOf(
                "settingId" to "global",
                "maintenanceMode" to setting.maintenanceMode,
                "registrationEnabled" to setting.registrationEnabled,
                "postingEnabled" to setting.postingEnabled,
                "messagingEnabled" to setting.messagingEnabled,
                "moderationEnabled" to setting.moderationEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("appSettings").document("global").set(map, SetOptions.merge()).await()
            logAdminAction(
                adminUid = adminUid,
                adminName = adminName,
                action = "UPDATE_APP_SETTINGS",
                targetId = "global",
                targetType = "CONFIG",
                details = "Maintenance: ${setting.maintenanceMode}, Reg: ${setting.registrationEnabled}, Post: ${setting.postingEnabled}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

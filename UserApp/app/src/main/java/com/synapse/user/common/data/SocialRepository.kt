package com.example.common.data

import com.example.common.models.NotificationItem
import com.example.common.models.Report
import com.example.common.models.Story
import com.example.common.models.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class SocialRepository(private val db: FirebaseFirestore) {

    suspend fun searchUsers(query: String): List<UserProfile> {
        if (query.isBlank()) {
            val snapshot = db.collection("users").limit(20).get().await()
            return snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
        }
        val clean = query.trim().lowercase()
        val snapshot = db.collection("users")
            .orderBy("username")
            .startAt(clean)
            .endAt(clean + "\uf8ff")
            .limit(20)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
    }

    fun observeStories(): Flow<List<Story>> = callbackFlow {
        val query = db.collection("stories")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val story = doc.toObject(Story::class.java)
                    story?.copy(storyId = doc.id)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun createStory(
        authorId: String,
        authorName: String,
        authorPhotoURL: String,
        mediaUrl: String,
        caption: String
    ): Result<Unit> {
        return try {
            val storyRef = db.collection("stories").document()
            val expiresMillis = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
            val data = mapOf(
                "storyId" to storyRef.id,
                "authorId" to authorId,
                "authorName" to authorName,
                "authorPhotoURL" to authorPhotoURL,
                "mediaUrl" to mediaUrl,
                "caption" to caption,
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to Timestamp(Date(expiresMillis))
            )
            storyRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> = callbackFlow {
        val query = db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val item = doc.toObject(NotificationItem::class.java)
                    item?.copy(notificationId = doc.id)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun markNotificationRead(notificationId: String) {
        try {
            db.collection("notifications").document(notificationId).update("isRead", true).await()
        } catch (_: Exception) {}
    }

    suspend fun createReport(
        reporterId: String,
        reporterName: String,
        targetType: String,
        targetId: String,
        targetSummary: String,
        reason: String
    ): Result<Unit> {
        return try {
            val reportRef = db.collection("reports").document()
            val data = mapOf(
                "reportId" to reportRef.id,
                "reporterId" to reporterId,
                "reporterName" to reporterName,
                "targetType" to targetType,
                "targetId" to targetId,
                "targetSummary" to targetSummary,
                "reason" to reason,
                "status" to "pending",
                "moderatorNotes" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            reportRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

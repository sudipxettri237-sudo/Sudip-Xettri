package com.example.common.data

import com.example.common.models.ChatMessage
import com.example.common.models.Conversation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val db: FirebaseFirestore) {

    fun observeConversations(currentUserId: String): Flow<List<Conversation>> = callbackFlow {
        val query = db.collection("conversations")
            .whereArrayContains("participantIds", currentUserId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val conv = doc.toObject(Conversation::class.java)
                    conv?.copy(conversationId = doc.id)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val query = db.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val msgs = snapshot.documents.mapNotNull { doc ->
                    val msg = doc.toObject(ChatMessage::class.java)
                    msg?.copy(messageId = doc.id)
                }
                trySend(msgs)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getOrCreateConversation(
        currentUserId: String,
        currentUserName: String,
        currentUserPhoto: String,
        otherUserId: String,
        otherUserName: String,
        otherUserPhoto: String
    ): String {
        // Look for existing conversation with these two participants
        val query = db.collection("conversations")
            .whereArrayContains("participantIds", currentUserId)
            .get()
            .await()

        val existing = query.documents.firstOrNull { doc ->
            val participants = doc.get("participantIds") as? List<*>
            participants?.contains(otherUserId) == true
        }

        if (existing != null) {
            return existing.id
        }

        // Create new conversation
        val newRef = db.collection("conversations").document()
        val data = mapOf(
            "conversationId" to newRef.id,
            "participantIds" to listOf(currentUserId, otherUserId),
            "participantNames" to mapOf(currentUserId to currentUserName, otherUserId to otherUserName),
            "participantPhotos" to mapOf(currentUserId to currentUserPhoto, otherUserId to otherUserPhoto),
            "lastMessage" to "Started conversation",
            "lastSenderId" to currentUserId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        newRef.set(data).await()
        return newRef.id
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String,
        mediaUrl: String = ""
    ): Result<Unit> {
        return try {
            val msgRef = db.collection("conversations").document(conversationId)
                .collection("messages").document()

            val msgData = mapOf(
                "messageId" to msgRef.id,
                "senderId" to senderId,
                "senderName" to senderName,
                "text" to text,
                "mediaUrl" to mediaUrl,
                "isRead" to false,
                "timestamp" to FieldValue.serverTimestamp()
            )
            msgRef.set(msgData).await()

            // Update conversation snippet
            db.collection("conversations").document(conversationId).set(
                mapOf(
                    "lastMessage" to text.ifEmpty { "Photo" },
                    "lastSenderId" to senderId,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

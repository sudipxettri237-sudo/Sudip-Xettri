package com.example.common.data

import com.example.common.models.Comment
import com.example.common.models.Post
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository(private val db: FirebaseFirestore) {

    fun observeFeed(currentUserId: String?): Flow<List<Post>> = callbackFlow {
        val query = db.collection("posts")
            .whereEqualTo("visibility", "public")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val posts = snapshot.documents.mapNotNull { doc ->
                    val post = doc.toObject(Post::class.java)
                    post?.copy(postId = doc.id)
                }
                trySend(posts)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getFeedOnce(currentUserId: String?): List<Post> {
        val snapshot = db.collection("posts")
            .whereEqualTo("visibility", "public")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val post = doc.toObject(Post::class.java)
            post?.copy(postId = doc.id)
        }
    }

    suspend fun getUserPosts(userId: String): List<Post> {
        val snapshot = db.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val post = doc.toObject(Post::class.java)
            post?.copy(postId = doc.id)
        }
    }

    suspend fun createPost(
        authorId: String,
        authorName: String,
        authorUsername: String,
        authorPhotoURL: String,
        content: String,
        mediaUrl: String = "",
        mediaType: String = "none"
    ): Result<String> {
        return try {
            val postRef = db.collection("posts").document()
            val postMap = mapOf(
                "postId" to postRef.id,
                "authorId" to authorId,
                "authorName" to authorName,
                "authorUsername" to authorUsername,
                "authorPhotoURL" to authorPhotoURL,
                "content" to content,
                "mediaUrls" to mediaUrl,
                "mediaType" to mediaType,
                "visibility" to "public",
                "likeCount" to 0L,
                "commentCount" to 0L,
                "shareCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            postRef.set(postMap).await()
            Result.success(postRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String, uid: String): Result<Boolean> {
        return try {
            val likeDocRef = db.collection("posts").document(postId)
                .collection("likes").document(uid)
            val likeSnapshot = likeDocRef.get().await()

            val postRef = db.collection("posts").document(postId)
            if (likeSnapshot.exists()) {
                // Unlike
                likeDocRef.delete().await()
                postRef.update("likeCount", FieldValue.increment(-1)).await()
                Result.success(false)
            } else {
                // Like
                likeDocRef.set(mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
                postRef.update("likeCount", FieldValue.increment(1)).await()
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasUserLiked(postId: String, uid: String): Boolean {
        return try {
            val likeDoc = db.collection("posts").document(postId)
                .collection("likes").document(uid).get().await()
            likeDoc.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val query = db.collection("posts").document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val comments = snapshot.documents.mapNotNull { doc ->
                    val c = doc.toObject(Comment::class.java)
                    c?.copy(commentId = doc.id)
                }
                trySend(comments)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(
        postId: String,
        authorId: String,
        authorName: String,
        authorPhotoURL: String,
        content: String
    ): Result<Unit> {
        return try {
            val commentRef = db.collection("posts").document(postId)
                .collection("comments").document()
            val commentData = mapOf(
                "commentId" to commentRef.id,
                "authorId" to authorId,
                "authorName" to authorName,
                "authorPhotoURL" to authorPhotoURL,
                "content" to content,
                "createdAt" to FieldValue.serverTimestamp()
            )
            commentRef.set(commentData).await()
            db.collection("posts").document(postId).update("commentCount", FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setPostVisibility(postId: String, visibility: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId).update("visibility", visibility).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

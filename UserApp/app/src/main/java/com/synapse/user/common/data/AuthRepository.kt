package com.example.common.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.common.models.UserProfile
import com.example.common.models.UserRole
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val db: FirebaseFirestore,
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            trySend(fbAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(context)
            val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")

            ensureUserProfileCreated(user)
            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In was cancelled by user: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun ensureUserProfileCreated(user: FirebaseUser): UserProfile {
        val userDoc = db.collection("users").document(user.uid).get().await()
        if (userDoc.exists()) {
            val profile = userDoc.toObject(UserProfile::class.java) ?: UserProfile(uid = user.uid)
            // Update online presence
            db.collection("users").document(user.uid).update(
                mapOf(
                    "isOnline" to true,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return profile
        }

        // If this is the first user ever, assign admin role so user can access admin dashboard
        val countSnapshot = db.collection("users").limit(2).get().await()
        val isFirstUser = countSnapshot.isEmpty

        val initialRole = if (isFirstUser) "admin" else "user"
        val username = (user.email?.substringBefore("@") ?: "user_${user.uid.take(5)}")
            .lowercase().replace("[^a-z0-9_]".toRegex(), "")

        val profileData = mapOf(
            "uid" to user.uid,
            "displayName" to (user.displayName ?: "Synapse Explorer"),
            "username" to username,
            "email" to (user.email ?: ""),
            "photoURL" to (user.photoUrl?.toString() ?: ""),
            "coverURL" to "",
            "bio" to "Welcome to my Synapse universe! ✨",
            "role" to initialRole,
            "status" to "active",
            "isOnline" to true,
            "followerCount" to 0L,
            "followingCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastSeen" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(user.uid).set(profileData, SetOptions.merge()).await()
        return UserProfile(
            uid = user.uid,
            displayName = user.displayName ?: "Synapse Explorer",
            username = username,
            email = user.email ?: "",
            photoURL = user.photoUrl?.toString() ?: "",
            role = initialRole,
            status = "active",
            isOnline = true
        )
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.toObject(UserProfile::class.java)
    }

    suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
            val updateMap = mutableMapOf<String, Any>(
                "displayName" to profile.displayName,
                "bio" to profile.bio,
                "photoURL" to profile.photoURL,
                "coverURL" to profile.coverURL,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updateMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update online status: ${e.message}")
        }
    }

    suspend fun signOut() {
        setOnlineStatus(false)
        auth.signOut()
    }
}

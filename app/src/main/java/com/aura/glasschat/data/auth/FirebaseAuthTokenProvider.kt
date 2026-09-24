package com.aura.glasschat.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Interface defining the token retrieval contract for authenticated services.
 */
interface AuthTokenProvider {
    suspend fun getToken(forceRefresh: Boolean = false): String?
    fun getCurrentUserId(): String?
    fun getCurrentUser(): FirebaseUser?
}

/**
 * Thread-safe provider for Firebase Auth ID tokens.
 * Handles automatic and forced token refresh to prevent token expiration issues
 * during long-running app sessions.
 */
class FirebaseAuthTokenProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthTokenProvider {

    companion object {
        private const val TAG = "BUDDYS_AUTH_TOKEN"

        @Volatile
        private var instance: FirebaseAuthTokenProvider? = null

        fun getInstance(): FirebaseAuthTokenProvider {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthTokenProvider().also { instance = it }
            }
        }
    }

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Retrieves the Firebase ID token for the current user.
     *
     * @param forceRefresh If true, forces a network token refresh from Firebase Auth servers,
     *                     bypassing the local cache. Used when downstream services report HTTP 401.
     */
    override suspend fun getToken(forceRefresh: Boolean): String? {
        val user = auth.currentUser
        if (user == null) {
            Log.w(TAG, "Cannot get ID token: No current authenticated Firebase user.")
            return null
        }

        return try {
            val result = user.getIdToken(forceRefresh).await()
            val token = result?.token
            if (token != null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Successfully retrieved token for user ${user.uid.take(6)}... (forced=$forceRefresh)")
                }
            } else {
                Log.w(TAG, "Firebase token result was null for user ${user.uid.take(6)}...")
            }
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve Firebase ID token (forced=$forceRefresh): ${e.message}", e)
            null
        }
    }
}

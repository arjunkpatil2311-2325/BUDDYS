package com.aura.glasschat.data.repository

import com.aura.glasschat.data.model.User
import com.aura.glasschat.util.UsernameUtils
import com.aura.glasschat.util.UsernameValidationResult
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    companion object {
        fun validateUsername(input: String): Result<String> {
            return when (val res = UsernameUtils.validate(input)) {
                is UsernameValidationResult.Valid -> Result.success(res.normalized)
                is UsernameValidationResult.Invalid -> Result.failure(IllegalArgumentException(res.reason))
            }
        }
    }

    fun validateUsernameInput(input: String): Result<String> = validateUsername(input)

    suspend fun checkUsernameAvailable(normalizedUsername: String): Boolean {
        val normalized = UsernameUtils.normalize(normalizedUsername)
        if (UsernameUtils.isReserved(normalized)) return false
        return try {
            val doc = firestore.collection("usernames").document(normalized).get().await()
            !doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Creates Firebase Auth account with email & password.
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after sign up")
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically claims a username and sets up the complete user profile in Firestore.
     */
    suspend fun completeOnboarding(
        uid: String,
        email: String,
        displayName: String,
        username: String,
        avatarUrl: String? = null,
        bio: String = ""
    ): Result<User> {
        val validation = UsernameUtils.validate(username)
        if (validation is UsernameValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validation.reason))
        }
        val cleanUsername = (validation as UsernameValidationResult.Valid).normalized

        return try {
            val now = Timestamp.now()
            val user = User(
                uid = uid,
                email = email.trim(),
                username = cleanUsername,
                displayName = displayName.trim().ifBlank { cleanUsername },
                avatarUrl = avatarUrl,
                bio = bio.trim(),
                statusMessage = bio.trim().ifBlank { "Connected via Buddies ✨" },
                isPrivate = false,
                followerCount = 0,
                followingCount = 0,
                postsCount = 0,
                onboardingCompleted = true,
                createdAt = now,
                lastSeen = now,
                isOnline = true
            )

            val usernameDocRef = firestore.collection("usernames").document(cleanUsername)
            val userDocRef = firestore.collection("users").document(uid)

            firestore.runTransaction { transaction ->
                val usernameSnapshot = transaction.get(usernameDocRef)
                if (usernameSnapshot.exists()) {
                    val existingUid = usernameSnapshot.getString("uid")
                    if (existingUid != uid) {
                        throw IllegalStateException("Username '@$cleanUsername' is already taken.")
                    }
                }

                transaction.set(
                    usernameDocRef,
                    mapOf(
                        "uid" to uid,
                        "username" to cleanUsername,
                        "createdAt" to now
                    )
                )
                transaction.set(userDocRef, user.toMap(), SetOptions.merge())
            }.await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after login")

            firestore.collection("users")
                .document(firebaseUser.uid)
                .update(
                    mapOf(
                        "isOnline" to true,
                        "lastSeen" to Timestamp.now()
                    )
                ).await()

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates with Google ID token.
     * Returns the existing profile if complete, or a pending User representation if onboarding is needed.
     */
    suspend fun signInWithGoogle(idToken: String): Result<Pair<FirebaseUser, Boolean>> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after Google sign in")

            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val userSnapshot = userDocRef.get().await()

            val isOnboardingComplete = if (userSnapshot.exists()) {
                val userObj = userSnapshot.toObject(User::class.java)
                val completed = userObj?.onboardingCompleted == true && !userObj.username.isNullOrBlank()
                if (completed) {
                    userDocRef.update(
                        mapOf(
                            "isOnline" to true,
                            "lastSeen" to Timestamp.now()
                        )
                    ).await()
                }
                completed
            } else {
                false
            }

            Result.success(Pair(firebaseUser, isOnboardingComplete))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                throw IllegalArgumentException("Please enter a valid email address.")
            }
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        val uid = currentUserId
        if (uid.isNotEmpty()) {
            try {
                firestore.collection("users")
                    .document(uid)
                    .update(
                        mapOf(
                            "isOnline" to false,
                            "lastSeen" to Timestamp.now()
                        )
                    ).await()
            } catch (_: Exception) {}
        }
        auth.signOut()
    }
}

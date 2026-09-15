package com.aura.glasschat.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val bio: String = "",
    val statusMessage: String = "Connected via Buddies ✨",
    val pronouns: String = "",
    val link: String = "",
    val gender: String = "",
    val isPrivate: Boolean = false,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val onboardingCompleted: Boolean = false,
    val usernameChangedAt: Timestamp? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastSeen: Timestamp? = null,
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,

    // Phase 3 Upgrades:
    val note: String? = null,
    val noteCreatedAt: Timestamp? = null,
    val onlinePrivacy: String = "EVERYONE", // EVERYONE, FRIENDS, NOBODY
    val lastSeenPrivacy: String = "EVERYONE", // EVERYONE, FRIENDS, NOBODY
    val notificationPrivacy: String = "SHOW_ALL", // SHOW_ALL, SENDER_ONLY, HIDE_PREVIEW
    val closeFriends: List<String> = emptyList()
) {
    val isNoteActive: Boolean
        get() {
            if (note.isNullOrBlank() || noteCreatedAt == null) return false
            val expiryTimeSec = noteCreatedAt.seconds + 86400 // 24 hours
            return Timestamp.now().seconds < expiryTimeSec
        }

    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "username" to username,
        "displayName" to displayName,
        "avatarUrl" to avatarUrl,
        "bio" to bio,
        "statusMessage" to statusMessage,
        "pronouns" to pronouns,
        "link" to link,
        "gender" to gender,
        "isPrivate" to isPrivate,
        "followerCount" to followerCount,
        "followingCount" to followingCount,
        "postsCount" to postsCount,
        "onboardingCompleted" to onboardingCompleted,
        "usernameChangedAt" to usernameChangedAt,
        "createdAt" to (createdAt ?: Timestamp.now()),
        "lastSeen" to (lastSeen ?: Timestamp.now()),
        "isOnline" to isOnline,
        "note" to note,
        "noteCreatedAt" to noteCreatedAt,
        "onlinePrivacy" to onlinePrivacy,
        "lastSeenPrivacy" to lastSeenPrivacy,
        "notificationPrivacy" to notificationPrivacy,
        "closeFriends" to closeFriends
    )
}

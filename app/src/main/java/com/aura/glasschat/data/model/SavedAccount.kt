package com.aura.glasschat.data.model

data class SavedAccount(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val authProvider: String = "EMAIL", // "EMAIL", "GOOGLE"
    val encryptedSessionToken: String? = null,
    val lastActiveAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "username" to username,
        "displayName" to displayName,
        "avatarUrl" to avatarUrl,
        "authProvider" to authProvider,
        "encryptedSessionToken" to encryptedSessionToken,
        "lastActiveAt" to lastActiveAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): SavedAccount {
            return SavedAccount(
                uid = map["uid"] as? String ?: "",
                email = map["email"] as? String ?: "",
                username = map["username"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                avatarUrl = map["avatarUrl"] as? String,
                authProvider = map["authProvider"] as? String ?: "EMAIL",
                encryptedSessionToken = map["encryptedSessionToken"] as? String,
                lastActiveAt = (map["lastActiveAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

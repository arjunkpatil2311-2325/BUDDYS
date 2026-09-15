package com.aura.glasschat.util

import com.google.firebase.Timestamp
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

object ChatUtils {

    private val secureRandom = SecureRandom()
    private const val ALPHANUMERIC_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    /**
     * Generates a deterministic chat ID from two user IDs.
     * Guarantees that chat(userA, userB) == chat(userB, userA).
     */
    fun getDeterministicChatId(uid1: String, uid2: String): String {
        require(uid1.isNotBlank() && uid2.isNotBlank()) { "User IDs must not be blank" }
        require(uid1 != uid2) { "Cannot create a 1-to-1 chat with oneself" }

        val sorted = listOf(uid1, uid2).sorted()
        return "chat_${sorted[0]}_${sorted[1]}"
    }

    /**
     * Generates a cryptographically random, temporary pairing code.
     * Example: AURA-9K8X2P
     */
    fun generateSecurePairingCode(length: Int = 6): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            val randomIndex = secureRandom.nextInt(ALPHANUMERIC_CHARS.length)
            sb.append(ALPHANUMERIC_CHARS[randomIndex])
        }
        return "AURA-$sb"
    }

    /**
     * Normalizes an entered code (uppercases and adds prefix if missing).
     */
    fun normalizePairingCode(input: String): String {
        val cleaned = input.trim().uppercase()
        return if (cleaned.startsWith("AURA-")) {
            cleaned
        } else if (cleaned.startsWith("AURA")) {
            "AURA-" + cleaned.removePrefix("AURA").trimStart('-')
        } else {
            "AURA-$cleaned"
        }
    }

    fun formatTimestamp(timestamp: Timestamp?): String = formatMessageTime(timestamp)

    /**
     * Formats elapsed timestamp in Snapchat-style (e.g. 7h, 24m, 2d, 1w, now).
     */
    fun formatSnapchatTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "now"
        val diffSec = (System.currentTimeMillis() - timestamp.toDate().time) / 1000
        return when {
            diffSec < 60 -> "now"
            diffSec < 3600 -> "${diffSec / 60}m"
            diffSec < 86400 -> "${diffSec / 3600}h"
            diffSec < 604800 -> "${diffSec / 86400}d"
            else -> "${diffSec / 604800}w"
        }
    }

    /**
     * Formats timestamp for chat message list.
     */
    fun formatMessageTime(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val date = timestamp.toDate()
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = date }

        return if (now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) &&
            now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
        ) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        } else if (now.get(Calendar.WEEK_OF_YEAR) == msgCal.get(Calendar.WEEK_OF_YEAR) &&
            now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
        ) {
            SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
        }
    }

    /**
     * Formats last active timestamp for user profiles and header subtitles.
     */
    fun formatLastActive(isOnline: Boolean, lastSeen: Timestamp?): String {
        if (isOnline) return "Active now"
        if (lastSeen == null) return "Offline"

        val diffSec = (System.currentTimeMillis() - lastSeen.toDate().time) / 1000
        return when {
            diffSec < 60 -> "Active just now"
            diffSec < 3600 -> "Active ${diffSec / 60}m ago"
            diffSec < 86400 -> "Active ${diffSec / 3600}h ago"
            diffSec < 172800 -> "Active yesterday"
            else -> "Active " + SimpleDateFormat("MMM d", Locale.getDefault()).format(lastSeen.toDate())
        }
    }

    /**
     * Formats Seen status for message bubbles.
     */
    fun formatSeenStatus(seenAt: Timestamp?): String {
        if (seenAt == null) return "Seen"
        val diffSec = (System.currentTimeMillis() - seenAt.toDate().time) / 1000
        return when {
            diffSec < 60 -> "Seen just now"
            diffSec < 3600 -> "Seen ${diffSec / 60}m ago"
            diffSec < 86400 -> "Seen ${diffSec / 3600}h ago"
            else -> "Seen " + SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(seenAt.toDate())
        }
    }

    /**
     * Formats duration in ms to mm:ss or m:ss (e.g. 0:07, 1:23).
     */
    fun formatDuration(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0) return "0:00"
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.getDefault(), "%d:%02d", min, sec)
    }

    /**
     * Translates Storage (Supabase & Firebase) exceptions into user-friendly and actionable error messages.
     * Prevents misleading "Check your connection" messages when the actual failure is permissions,
     * missing bucket, file size, or authentication.
     */
    fun getFriendlyStorageErrorMessage(e: Throwable): String {
        val message = e.message.orEmpty()
        val cause = e.cause?.message.orEmpty()
        val combined = "$message $cause".lowercase()

        return when {
            combined.contains("permission") || combined.contains("unauthorized") || combined.contains("not authorized") || combined.contains("401") || combined.contains("403") || combined.contains("-13021") ->
                "Upload failed: permission denied. Please verify your account."
            combined.contains("unauthenticated") || combined.contains("user is not authenticated") || combined.contains("-13020") ->
                "Please sign in to upload media."
            combined.contains("quota") || combined.contains("exceeded") || combined.contains("413") || combined.contains("-13040") ->
                "Media file exceeds storage limits. Please choose a smaller photo."
            combined.contains("network") || combined.contains("timeout") || combined.contains("unable to resolve host") || combined.contains("-13000") ->
                "Upload couldn't reach media servers. Please check your internet connection."
            combined.contains("cancelled") || combined.contains("-13040") ->
                "Upload was cancelled."
            combined.contains("object does not exist") || combined.contains("bucket") || combined.contains("404") || combined.contains("-13010") ->
                "Media upload failed: Storage destination not found. Please try again."
            else ->
                e.localizedMessage?.takeIf { it.isNotBlank() && !it.contains("java.lang") } ?: "Media upload failed. Please try again."
        }
    }

    /**
     * Translates Cloud Firestore exceptions into user-friendly and actionable error messages.
     */
    fun getFriendlyFirestoreErrorMessage(e: Throwable): String {
        val combined = "${e.message.orEmpty()} ${e.cause?.message.orEmpty()}".lowercase()
        return when {
            combined.contains("permission_denied") || combined.contains("permission denied") ->
                "You don't have permission to perform this action."
            combined.contains("unauthenticated") ->
                "Please sign in to continue."
            combined.contains("not_found") || combined.contains("not found") ->
                "The requested conversation or item could not be found."
            combined.contains("unavailable") || combined.contains("network") ->
                "Network unavailable. Reconnecting to Buddies..."
            else ->
                e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Action failed. Please try again."
        }
    }
}

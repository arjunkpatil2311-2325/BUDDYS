package com.aura.glasschat.util

sealed class UsernameValidationResult {
    data class Valid(val normalized: String) : UsernameValidationResult()
    data class Invalid(val reason: String) : UsernameValidationResult()
}

object UsernameUtils {

    private val RESERVED_USERNAMES = setOf(
        "admin",
        "administrator",
        "support",
        "help",
        "buddys",
        "official",
        "moderator",
        "mod",
        "system",
        "root",
        "null",
        "undefined",
        "aura",
        "glass",
        "buddys_official"
    )

    private val USERNAME_REGEX = Regex("^[a-z_][a-z0-9_]{2,29}$")

    /**
     * Normalizes a username by trimming whitespace, removing leading '@', and converting to lowercase.
     */
    fun normalize(input: String): String {
        return input.trim().lowercase().removePrefix("@")
    }

    /**
     * Validates a username according to Buddies rules:
     * - 3–30 characters
     * - Letters A-Z, numbers 0-9, and underscore (_)
     * - Cannot begin with a number
     * - Cannot be a reserved system name
     */
    fun validate(input: String): UsernameValidationResult {
        val normalized = normalize(input)

        if (normalized.isEmpty()) {
            return UsernameValidationResult.Invalid("Username cannot be empty.")
        }

        if (normalized.length < 3) {
            return UsernameValidationResult.Invalid("Username must be at least 3 characters.")
        }

        if (normalized.length > 30) {
            return UsernameValidationResult.Invalid("Username cannot exceed 30 characters.")
        }

        if (normalized.first().isDigit()) {
            return UsernameValidationResult.Invalid("Username cannot begin with a number.")
        }

        if (!USERNAME_REGEX.matches(normalized)) {
            return UsernameValidationResult.Invalid("Username can only contain letters, numbers, and underscores.")
        }

        if (RESERVED_USERNAMES.contains(normalized)) {
            return UsernameValidationResult.Invalid("This username is reserved by Buddies.")
        }

        return UsernameValidationResult.Valid(normalized)
    }

    fun isReserved(normalizedUsername: String): Boolean {
        return RESERVED_USERNAMES.contains(normalize(normalizedUsername))
    }
}

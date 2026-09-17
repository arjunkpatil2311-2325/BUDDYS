package com.aura.glasschat.auth

import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {

    private fun validateEmail(email: String): Boolean {
        return email.isNotBlank() && email.contains("@") && email.contains(".")
    }

    private fun validatePassword(password: String): Pair<Boolean, String?> {
        return when {
            password.isBlank() -> false to "Password cannot be empty"
            password.length < 6 -> false to "Password must be at least 6 characters."
            else -> true to null
        }
    }

    @Test
    fun `test email validation logic`() {
        assertTrue(validateEmail("user@example.com"))
        assertTrue(validateEmail("buddies_fan@aura.app"))
        assertTrue(validateEmail("john.doe@sub.domain.co"))

        assertFalse(validateEmail(""))
        assertFalse(validateEmail("   "))
        assertFalse(validateEmail("invalidemail"))
        assertFalse(validateEmail("missing_at_sign.com"))
        assertFalse(validateEmail("missing_dot@domain"))
    }

    @Test
    fun `test password validation rules`() {
        val (valid1, err1) = validatePassword("securePass123")
        assertTrue(valid1)
        assertNull(err1)

        val (valid2, err2) = validatePassword("123456")
        assertTrue(valid2)
        assertNull(err2)

        val (validShort, errShort) = validatePassword("12345")
        assertFalse(validShort)
        assertEquals("Password must be at least 6 characters.", errShort)

        val (validEmpty, errEmpty) = validatePassword("")
        assertFalse(validEmpty)
        assertEquals("Password cannot be empty", errEmpty)
    }

    @Test
    fun `test display name fallback to username when blank`() {
        val username = "john_doe"
        val customDisplayName = "John Doe"
        val blankDisplayName = "   "

        val resolvedCustom = customDisplayName.ifBlank { username }
        val resolvedFallback = blankDisplayName.ifBlank { username }

        assertEquals("John Doe", resolvedCustom)
        assertEquals("john_doe", resolvedFallback)
    }
}

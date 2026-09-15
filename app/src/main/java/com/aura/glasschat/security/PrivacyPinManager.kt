package com.aura.glasschat.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockTimeout(val label: String, val durationMs: Long) {
    IMMEDIATELY("Immediately", 0L),
    AFTER_1_MIN("After 1 minute", 60_000L),
    AFTER_5_MIN("After 5 minutes", 300_000L),
    AFTER_15_MIN("After 15 minutes", 900_000L);

    companion object {
        fun fromName(name: String?): LockTimeout {
            return entries.find { it.name == name } ?: IMMEDIATELY
        }
    }
}

/**
 * Local-only, Keystore/Salted-SHA256 backed Privacy PIN Manager.
 * Raw PIN is NEVER stored, NEVER logged, NEVER transmitted to Firebase.
 */
class PrivacyPinManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPinSet = MutableStateFlow(hasPin())
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _lockTimeout = MutableStateFlow(getLockTimeout())
    val lockTimeout: StateFlow<LockTimeout> = _lockTimeout.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _hideNotificationContent = MutableStateFlow(getHideNotificationContent())
    val hideNotificationContent: StateFlow<Boolean> = _hideNotificationContent.asStateFlow()

    fun hasPin(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    /**
     * Set a new Privacy PIN. Stores salted hash + salt.
     */
    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val hash = hashPin(pin, salt)

        prefs.edit()
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_COOLDOWN_UNTIL, 0L)
            .apply()

        _isPinSet.value = true
        return true
    }

    /**
     * Verifies the entered PIN against stored salted hash.
     * Returns:
     * - Success: Correct PIN (failed attempts reset)
     * - Incorrect: Failed verification with remaining attempts
     * - Throttled: User must wait for backoff cooldown
     */
    fun verifyPin(pin: String): PinVerificationResult {
        if (isThrottled()) {
            val remainingSec = (getCooldownUntil() - System.currentTimeMillis()) / 1000
            return PinVerificationResult.Throttled(remainingSec.coerceAtLeast(1))
        }

        val storedHashBase64 = prefs.getString(KEY_PIN_HASH, null) ?: return PinVerificationResult.NoPinSet
        val storedSaltBase64 = prefs.getString(KEY_PIN_SALT, null) ?: return PinVerificationResult.NoPinSet

        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val computedHash = hashPin(pin, salt)
        val computedHashBase64 = Base64.encodeToString(computedHash, Base64.NO_WRAP)

        return if (storedHashBase64 == computedHashBase64) {
            resetFailedAttempts()
            PinVerificationResult.Success
        } else {
            val newFailCount = recordFailedAttempt()
            if (newFailCount >= MAX_ATTEMPTS_BEFORE_COOLDOWN) {
                val cooldownDuration = calculateCooldownMs(newFailCount)
                val cooldownUntil = System.currentTimeMillis() + cooldownDuration
                prefs.edit().putLong(KEY_COOLDOWN_UNTIL, cooldownUntil).apply()
                PinVerificationResult.Throttled(cooldownDuration / 1000)
            } else {
                PinVerificationResult.Incorrect(
                    attemptsRemaining = (MAX_ATTEMPTS_BEFORE_COOLDOWN - newFailCount).coerceAtLeast(0)
                )
            }
        }
    }

    fun isThrottled(): Boolean {
        val cooldownUntil = getCooldownUntil()
        return cooldownUntil > System.currentTimeMillis()
    }

    fun getCooldownRemainingSeconds(): Long {
        val remaining = (getCooldownUntil() - System.currentTimeMillis()) / 1000
        return remaining.coerceAtLeast(0)
    }

    fun resetPin(newPin: String): Boolean {
        return setPin(newPin)
    }

    fun removePin() {
        prefs.edit().clear().apply()
        _isPinSet.value = false
    }

    fun setLockTimeout(timeout: LockTimeout) {
        prefs.edit().putString(KEY_LOCK_TIMEOUT, timeout.name).apply()
        _lockTimeout.value = timeout
    }

    fun getLockTimeout(): LockTimeout {
        val name = prefs.getString(KEY_LOCK_TIMEOUT, LockTimeout.IMMEDIATELY.name)
        return LockTimeout.fromName(name)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setHideNotificationContent(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_NOTIFS, hide).apply()
        _hideNotificationContent.value = hide
    }

    fun getHideNotificationContent(): Boolean {
        return prefs.getBoolean(KEY_HIDE_NOTIFS, true)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(pin.toByteArray(Charsets.UTF_8))
    }

    private fun getCooldownUntil(): Long {
        return prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
    }

    private fun recordFailedAttempt(): Int {
        val count = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, count).apply()
        return count
    }

    private fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_COOLDOWN_UNTIL, 0L)
            .apply()
    }

    private fun calculateCooldownMs(failCount: Int): Long {
        return when {
            failCount <= 5 -> 30_000L // 30 sec
            failCount <= 8 -> 60_000L // 1 min
            else -> 300_000L          // 5 min
        }
    }

    companion object {
        private const val PREFS_NAME = "buddys_privacy_pin_secure_store"
        private const val KEY_PIN_HASH = "pin_salted_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val KEY_COOLDOWN_UNTIL = "pin_cooldown_until"
        private const val KEY_LOCK_TIMEOUT = "pin_lock_timeout"
        private const val KEY_BIOMETRIC_ENABLED = "pin_biometric_enabled"
        private const val KEY_HIDE_NOTIFS = "pin_hide_notifications"

        const val MAX_ATTEMPTS_BEFORE_COOLDOWN = 5
    }
}

sealed class PinVerificationResult {
    object Success : PinVerificationResult()
    data class Incorrect(val attemptsRemaining: Int) : PinVerificationResult()
    data class Throttled(val remainingSeconds: Long) : PinVerificationResult()
    object NoPinSet : PinVerificationResult()
}

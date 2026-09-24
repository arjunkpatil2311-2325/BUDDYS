package com.aura.glasschat.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.aura.glasschat.data.model.SavedAccount
import com.aura.glasschat.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AccountManagerRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _savedAccounts = MutableStateFlow<List<SavedAccount>>(emptyList())
    val savedAccounts: Flow<List<SavedAccount>> = _savedAccounts.asStateFlow()

    init {
        loadAccounts()
    }

    companion object {
        private const val PREFS_NAME = "buddys_multi_accounts_v1"
        private const val KEY_SAVED_ACCOUNTS = "saved_accounts_json"
        private const val KEY_ACTIVE_UID = "active_account_uid"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "BuddiesAccountMasterKey_v1"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        @Volatile
        private var instance: AccountManagerRepository? = null

        fun getInstance(context: Context): AccountManagerRepository {
            return instance ?: synchronized(this) {
                instance ?: AccountManagerRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secretKey = getOrCreateSecretKey()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return ""
            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    @Synchronized
    private fun loadAccounts(): List<SavedAccount> {
        val rawJson = prefs.getString(KEY_SAVED_ACCOUNTS, "[]") ?: "[]"
        val list = mutableListOf<SavedAccount>()
        try {
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedAccount(
                        uid = obj.optString("uid"),
                        email = obj.optString("email"),
                        username = obj.optString("username"),
                        displayName = obj.optString("displayName"),
                        avatarUrl = if (obj.has("avatarUrl") && !obj.isNull("avatarUrl")) obj.getString("avatarUrl") else null,
                        authProvider = obj.optString("authProvider", "EMAIL"),
                        encryptedSessionToken = if (obj.has("encryptedSessionToken") && !obj.isNull("encryptedSessionToken")) obj.getString("encryptedSessionToken") else null,
                        lastActiveAt = obj.optLong("lastActiveAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}

        _savedAccounts.value = list
        return list
    }

    @Synchronized
    private fun persistAccounts(accounts: List<SavedAccount>) {
        val array = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject().apply {
                put("uid", acc.uid)
                put("email", acc.email)
                put("username", acc.username)
                put("displayName", acc.displayName)
                put("avatarUrl", acc.avatarUrl)
                put("authProvider", acc.authProvider)
                put("encryptedSessionToken", acc.encryptedSessionToken)
                put("lastActiveAt", acc.lastActiveAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_ACCOUNTS, array.toString()).apply()
        _savedAccounts.value = accounts
    }

    fun getSavedAccounts(): List<SavedAccount> {
        return _savedAccounts.value
    }

    fun getActiveAccountUid(): String {
        return prefs.getString(KEY_ACTIVE_UID, "") ?: ""
    }

    fun setActiveAccountUid(uid: String) {
        prefs.edit().putString(KEY_ACTIVE_UID, uid).apply()
    }

    fun saveAccount(user: User, authProvider: String = "EMAIL", sessionSecret: String? = null) {
        if (user.uid.isBlank()) return
        val current = getSavedAccounts().toMutableList()
        val existingIndex = current.indexOfFirst { it.uid == user.uid }

        val encryptedToken = if (!sessionSecret.isNullOrBlank()) encrypt(sessionSecret) else {
            existingIndex.takeIf { it != -1 }?.let { current[it].encryptedSessionToken }
        }

        val account = SavedAccount(
            uid = user.uid,
            email = user.email,
            username = user.username,
            displayName = user.displayName.ifBlank { user.username },
            avatarUrl = user.avatarUrl,
            authProvider = authProvider,
            encryptedSessionToken = encryptedToken,
            lastActiveAt = System.currentTimeMillis()
        )

        if (existingIndex != -1) {
            current[existingIndex] = account
        } else {
            current.add(account)
        }

        persistAccounts(current)
        setActiveAccountUid(user.uid)
    }

    fun saveSessionSecret(uid: String, email: String, sessionSecret: String) {
        if (uid.isBlank()) return
        val current = getSavedAccounts().toMutableList()
        val index = current.indexOfFirst { it.uid == uid }
        val encrypted = encrypt(sessionSecret)
        if (index != -1) {
            current[index] = current[index].copy(
                email = email.ifBlank { current[index].email },
                encryptedSessionToken = encrypted,
                lastActiveAt = System.currentTimeMillis()
            )
        } else {
            current.add(
                SavedAccount(
                    uid = uid,
                    email = email,
                    encryptedSessionToken = encrypted,
                    lastActiveAt = System.currentTimeMillis()
                )
            )
        }
        persistAccounts(current)
        setActiveAccountUid(uid)
    }

    fun updateAccountProfile(uid: String, username: String, displayName: String, avatarUrl: String?) {
        if (uid.isBlank()) return
        val current = getSavedAccounts().toMutableList()
        val index = current.indexOfFirst { it.uid == uid }
        if (index != -1) {
            val old = current[index]
            current[index] = old.copy(
                username = username,
                displayName = displayName.ifBlank { username },
                avatarUrl = avatarUrl,
                lastActiveAt = System.currentTimeMillis()
            )
            persistAccounts(current)
        }
    }

    fun removeAccount(uid: String) {
        val current = getSavedAccounts().filter { it.uid != uid }
        persistAccounts(current)
        if (getActiveAccountUid() == uid) {
            val next = current.firstOrNull()?.uid ?: ""
            setActiveAccountUid(next)
        }
    }

    /**
     * Finds next saved account to cycle to (e.g. on profile double tap).
     */
    fun getNextAccount(currentUid: String): SavedAccount? {
        val list = getSavedAccounts()
        if (list.size <= 1) return null
        val currentIndex = list.indexOfFirst { it.uid == currentUid }
        if (currentIndex == -1) return list.firstOrNull()
        val nextIndex = (currentIndex + 1) % list.size
        return list[nextIndex]
    }

    /**
     * Switches session to the target account.
     */
    suspend fun switchAccount(
        targetUid: String,
        authRepository: AuthRepository = AuthRepository()
    ): Result<SavedAccount> {
        val accounts = getSavedAccounts()
        val target = accounts.find { it.uid == targetUid }
            ?: return Result.failure(IllegalArgumentException("Account not found"))

        val currentUid = authRepository.currentUserId
        if (currentUid == targetUid) {
            setActiveAccountUid(targetUid)
            return Result.success(target)
        }

        // If encrypted credentials exist, re-authenticate silently
        val decryptedSecret = target.encryptedSessionToken?.let { decrypt(it) }
        if (!decryptedSecret.isNullOrBlank() && target.email.isNotBlank()) {
            val loginResult = authRepository.logIn(target.email, decryptedSecret)
            if (loginResult.isFailure) {
                return Result.failure(loginResult.exceptionOrNull() ?: Exception("Failed to switch to @${target.username}"))
            }
        } else {
            return Result.failure(Exception("No saved credentials for @${target.username}. Please re-add account."))
        }

        // Clear media storage cache to avoid token/URL bleeding between accounts
        SupabaseMediaStorageRepository.getInstance().clearCache()

        setActiveAccountUid(targetUid)
        val updated = accounts.map {
            if (it.uid == targetUid) it.copy(lastActiveAt = System.currentTimeMillis()) else it
        }
        persistAccounts(updated)
        return Result.success(target)
    }

    /**
     * Cycles to the next saved account in one call.
     */
    suspend fun cycleToNextAccount(
        currentUid: String,
        authRepository: AuthRepository = AuthRepository()
    ): Result<SavedAccount?> {
        val next = getNextAccount(currentUid) ?: return Result.success(null)
        val switchRes = switchAccount(next.uid, authRepository)
        return switchRes.map { it }
    }
}

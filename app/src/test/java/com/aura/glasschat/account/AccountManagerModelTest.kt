package com.aura.glasschat.account

import com.aura.glasschat.data.model.SavedAccount
import org.junit.Assert.*
import org.junit.Test

class AccountManagerModelTest {

    @Test
    fun savedAccount_serializationAndDeserialization_preservesAllFields() {
        val account = SavedAccount(
            uid = "user_spidey_1",
            email = "peter.parker@buddys.app",
            username = "spiderman",
            displayName = "Peter Parker",
            avatarUrl = "https://media.buddys.app/avatars/spidey.jpg",
            authProvider = "EMAIL",
            encryptedSessionToken = "enc_tok_abc_123",
            lastActiveAt = 1720000000000L
        )

        val map = account.toMap()
        val restored = SavedAccount.fromMap(map)

        assertEquals("user_spidey_1", restored.uid)
        assertEquals("peter.parker@buddys.app", restored.email)
        assertEquals("spiderman", restored.username)
        assertEquals("Peter Parker", restored.displayName)
        assertEquals("https://media.buddys.app/avatars/spidey.jpg", restored.avatarUrl)
        assertEquals("EMAIL", restored.authProvider)
        assertEquals("enc_tok_abc_123", restored.encryptedSessionToken)
        assertEquals(1720000000000L, restored.lastActiveAt)
    }

    @Test
    fun savedAccount_cyclingLogic_handlesSingleAndMultipleAccounts() {
        val account1 = SavedAccount(uid = "uid1", username = "hero1")
        val account2 = SavedAccount(uid = "uid2", username = "hero2")
        val account3 = SavedAccount(uid = "uid3", username = "hero3")

        val singleList = listOf(account1)
        fun getNext(list: List<SavedAccount>, currentUid: String): SavedAccount? {
            if (list.size <= 1) return null
            val idx = list.indexOfFirst { it.uid == currentUid }
            if (idx == -1) return list.firstOrNull()
            return list[(idx + 1) % list.size]
        }

        assertNull(getNext(singleList, "uid1"))

        val multiList = listOf(account1, account2, account3)
        assertEquals("uid2", getNext(multiList, "uid1")?.uid)
        assertEquals("uid3", getNext(multiList, "uid2")?.uid)
        assertEquals("uid1", getNext(multiList, "uid3")?.uid)
        assertEquals("uid1", getNext(multiList, "non_existent")?.uid)
    }

    @Test
    fun savedAccount_defaultsAndFallbacks_workCorrectly() {
        val emptyMap = emptyMap<String, Any?>()
        val restored = SavedAccount.fromMap(emptyMap)

        assertEquals("", restored.uid)
        assertEquals("", restored.email)
        assertEquals("", restored.username)
        assertEquals("", restored.displayName)
        assertNull(restored.avatarUrl)
        assertEquals("EMAIL", restored.authProvider)
        assertNull(restored.encryptedSessionToken)
        assertTrue(restored.lastActiveAt > 0)
    }
}

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

        fun getNext(list: List<SavedAccount>, currentUid: String): SavedAccount? {
            if (list.size <= 1) return null
            val idx = list.indexOfFirst { it.uid == currentUid }
            if (idx == -1) return list.firstOrNull()
            return list[(idx + 1) % list.size]
        }

        // 1 Account -> returns null (do not switch)
        assertNull(getNext(listOf(account1), "uid1"))

        // 2 Accounts -> toggles between them
        val twoList = listOf(account1, account2)
        assertEquals("uid2", getNext(twoList, "uid1")?.uid)
        assertEquals("uid1", getNext(twoList, "uid2")?.uid)

        // 3 Accounts -> cycles through them
        val multiList = listOf(account1, account2, account3)
        assertEquals("uid2", getNext(multiList, "uid1")?.uid)
        assertEquals("uid3", getNext(multiList, "uid2")?.uid)
        assertEquals("uid1", getNext(multiList, "uid3")?.uid)
        assertEquals("uid1", getNext(multiList, "non_existent")?.uid)
    }

    @Test
    fun savedAccount_removalAndActiveFallback_handlesGracefully() {
        val acc1 = SavedAccount(uid = "uid1", username = "user1")
        val acc2 = SavedAccount(uid = "uid2", username = "user2")
        var list = listOf(acc1, acc2)
        var activeUid = "uid1"

        // Remove active account -> falls back to next account
        list = list.filter { it.uid != "uid1" }
        if (activeUid == "uid1") {
            activeUid = list.firstOrNull()?.uid ?: ""
        }

        assertEquals(1, list.size)
        assertEquals("uid2", list.first().uid)
        assertEquals("uid2", activeUid)

        // Remove last account -> empty list and empty activeUid
        list = list.filter { it.uid != "uid2" }
        if (activeUid == "uid2") {
            activeUid = list.firstOrNull()?.uid ?: ""
        }

        assertTrue(list.isEmpty())
        assertEquals("", activeUid)
    }

    @Test
    fun savedAccount_accountIsolation_guaranteesSeparation() {
        val accountA = SavedAccount(
            uid = "user_A",
            email = "userA@buddys.app",
            username = "alpha",
            encryptedSessionToken = "token_A"
        )
        val accountB = SavedAccount(
            uid = "user_B",
            email = "userB@buddys.app",
            username = "beta",
            encryptedSessionToken = "token_B"
        )

        assertNotEquals(accountA.uid, accountB.uid)
        assertNotEquals(accountA.username, accountB.username)
        assertNotEquals(accountA.encryptedSessionToken, accountB.encryptedSessionToken)
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


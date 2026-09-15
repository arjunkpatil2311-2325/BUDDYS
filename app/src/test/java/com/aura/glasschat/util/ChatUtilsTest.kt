package com.aura.glasschat.util

import org.junit.Assert.*
import org.junit.Test

class ChatUtilsTest {

    @Test
    fun getDeterministicChatId_generatesConsistentIdRegardlessOfOrder() {
        val uid1 = "user_alpha_123"
        val uid2 = "user_beta_456"

        val chatId1 = ChatUtils.getDeterministicChatId(uid1, uid2)
        val chatId2 = ChatUtils.getDeterministicChatId(uid2, uid1)

        assertEquals(chatId1, chatId2)
        assertEquals("chat_user_alpha_123_user_beta_456", chatId1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getDeterministicChatId_throwsOnSelfPairing() {
        val uid = "user_123"
        ChatUtils.getDeterministicChatId(uid, uid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getDeterministicChatId_throwsOnBlankUid() {
        ChatUtils.getDeterministicChatId("", "user_123")
    }

    @Test
    fun generateSecurePairingCode_producesValidFormat() {
        val code = ChatUtils.generateSecurePairingCode(6)
        assertTrue(code.startsWith("AURA-"))
        assertEquals(11, code.length) // "AURA-" (5) + 6 alphanumeric chars
    }

    @Test
    fun normalizePairingCode_correctlyPrefixesAndUppercases() {
        assertEquals("AURA-7K9M2P", ChatUtils.normalizePairingCode("7k9m2p"))
        assertEquals("AURA-7K9M2P", ChatUtils.normalizePairingCode("aura-7k9m2p"))
        assertEquals("AURA-7K9M2P", ChatUtils.normalizePairingCode("AURA-7K9M2P"))
    }
}

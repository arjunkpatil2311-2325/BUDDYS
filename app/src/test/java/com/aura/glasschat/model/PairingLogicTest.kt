package com.aura.glasschat.model

import com.aura.glasschat.data.model.PairingCode
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class PairingLogicTest {

    @Test
    fun pairingCode_isValid_whenNotUsedAndNotExpired() {
        val futureDate = Date(System.currentTimeMillis() + 15 * 60 * 1000)
        val code = PairingCode(
            codeId = "code_1",
            code = "AURA-7K9M2P",
            ownerUid = "user_A",
            createdAt = Timestamp.now(),
            expiresAt = Timestamp(futureDate),
            used = false,
            usedBy = null
        )

        assertFalse(code.isExpired())
        assertTrue(code.isValid())
    }

    @Test
    fun pairingCode_isInvalid_whenExpired() {
        val pastDate = Date(System.currentTimeMillis() - 1000)
        val code = PairingCode(
            codeId = "code_2",
            code = "AURA-7K9M2P",
            ownerUid = "user_A",
            createdAt = Timestamp.now(),
            expiresAt = Timestamp(pastDate),
            used = false,
            usedBy = null
        )

        assertTrue(code.isExpired())
        assertFalse(code.isValid())
    }

    @Test
    fun pairingCode_isInvalid_whenUsed() {
        val futureDate = Date(System.currentTimeMillis() + 15 * 60 * 1000)
        val code = PairingCode(
            codeId = "code_3",
            code = "AURA-7K9M2P",
            ownerUid = "user_A",
            createdAt = Timestamp.now(),
            expiresAt = Timestamp(futureDate),
            used = true,
            usedBy = "user_B"
        )

        assertFalse(code.isExpired())
        assertFalse(code.isValid())
    }
}

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

    @Test
    fun nearbyPairingPayload_encodeAndDecodeCompactEndpointName_success() {
        val uid = "LZD4vVHzCbP2gS96rJ0sjWxF5Wr2"
        val username = "alice_qa"
        val displayName = "Alice QA"

        val encoded = com.aura.glasschat.data.nearby.NearbyPairingPayload.encodeCompactEndpointName(
            uid = uid,
            username = username,
            displayName = displayName
        )

        assertTrue("Encoded endpoint name must start with B1|", encoded.startsWith("B1|"))
        assertTrue("Encoded endpoint name must be <= 131 bytes for Nearby limit", encoded.toByteArray(Charsets.UTF_8).size <= 131)

        val decoded = com.aura.glasschat.data.nearby.NearbyPairingPayload.decodeCompactEndpointName(encoded)
        assertNotNull("Decoded result should not be null", decoded)
        assertEquals(uid, decoded?.first)
        assertEquals(username, decoded?.second)
        assertEquals(displayName, decoded?.third)
    }

    @Test
    fun nearbyPairingPayload_decodeCompactEndpointName_handlesInvalid() {
        assertNull(com.aura.glasschat.data.nearby.NearbyPairingPayload.decodeCompactEndpointName("InvalidHeader"))
        assertNull(com.aura.glasschat.data.nearby.NearbyPairingPayload.decodeCompactEndpointName("B1|"))
        assertNull(com.aura.glasschat.data.nearby.NearbyPairingPayload.decodeCompactEndpointName(""))
    }
}


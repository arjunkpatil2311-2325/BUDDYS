package com.aura.glasschat.model

import com.aura.glasschat.data.model.*
import com.aura.glasschat.data.repository.RelationshipState
import com.aura.glasschat.util.UsernameUtils
import com.aura.glasschat.util.UsernameValidationResult
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class UsernameAndFollowTest {

    @Test
    fun `test valid usernames pass validation`() {
        val validUsernames = listOf("rahul", "rahul_123", "a_b_c", "super123", "user_name_99", "_hidden_")
        for (username in validUsernames) {
            val result = UsernameUtils.validate(username)
            assertTrue("Expected valid for $username", result is UsernameValidationResult.Valid)
            assertEquals(username.lowercase(), (result as UsernameValidationResult.Valid).normalized)
        }
    }

    @Test
    fun `test usernames with leading @ are normalized properly`() {
        val result = UsernameUtils.validate("@rahul_dev")
        assertTrue(result is UsernameValidationResult.Valid)
        assertEquals("rahul_dev", (result as UsernameValidationResult.Valid).normalized)
    }

    @Test
    fun `test uppercase characters are normalized to lowercase`() {
        val result = UsernameUtils.validate("RahulSharma_99")
        assertTrue(result is UsernameValidationResult.Valid)
        assertEquals("rahulsharma_99", (result as UsernameValidationResult.Valid).normalized)
    }

    @Test
    fun `test usernames differing only by case produce identical normalized output`() {
        val u1 = UsernameUtils.normalize("@Arjun123")
        val u2 = UsernameUtils.normalize("arjun123")
        val u3 = UsernameUtils.normalize("ARJUN123")
        assertEquals("arjun123", u1)
        assertEquals(u1, u2)
        assertEquals(u2, u3)
    }

    @Test
    fun `test username starting with number is rejected`() {
        val result = UsernameUtils.validate("12arjun")
        assertTrue(result is UsernameValidationResult.Invalid)
        assertTrue((result as UsernameValidationResult.Invalid).reason.contains("cannot begin with a number"))
    }

    @Test
    fun `test reserved usernames are rejected`() {
        val reservedList = listOf("admin", "administrator", "support", "help", "buddys", "official", "moderator", "mod", "system", "root", "null", "undefined")
        for (name in reservedList) {
            val result = UsernameUtils.validate(name)
            assertTrue("Expected reserved rejection for $name", result is UsernameValidationResult.Invalid)
            assertTrue((result as UsernameValidationResult.Invalid).reason.contains("reserved"))
        }
    }

    @Test
    fun `test short usernames are rejected`() {
        val result = UsernameUtils.validate("ab")
        assertTrue(result is UsernameValidationResult.Invalid)
        assertTrue((result as UsernameValidationResult.Invalid).reason.contains("at least 3 characters"))
    }

    @Test
    fun `test overly long usernames exceeding 30 characters are rejected`() {
        val longUsername = "a_very_long_username_exceeding_thirty_chars_long_limit"
        val result = UsernameUtils.validate(longUsername)
        assertTrue(result is UsernameValidationResult.Invalid)
        assertTrue((result as UsernameValidationResult.Invalid).reason.contains("cannot exceed 30 characters"))
    }

    @Test
    fun `test invalid characters in username are rejected`() {
        val invalidUsernames = listOf("rahul!", "rahul sharma", "rahul-sharma", "rahul@home", "user.name", "rahul#1")
        for (username in invalidUsernames) {
            val result = UsernameUtils.validate(username)
            assertTrue("Expected failure for $username", result is UsernameValidationResult.Invalid)
        }
    }

    @Test
    fun `test User model backward compatibility with defaults`() {
        val time = Timestamp(Date(1000000))
        val user = User(
            uid = "user123",
            email = "user@example.com",
            username = "rahul",
            displayName = "Rahul Sharma",
            bio = "Nostalgic vibes",
            createdAt = time,
            lastSeen = time
        )

        assertFalse(user.isPrivate)
        assertFalse(user.onboardingCompleted)
        assertEquals(0, user.followerCount)
        assertEquals(0, user.followingCount)
        assertEquals(0, user.postsCount)
        assertEquals("Nostalgic vibes", user.bio)

        val map = user.toMap()
        assertEquals(false, map["isPrivate"])
        assertEquals(false, map["onboardingCompleted"])
        assertEquals(0, map["followerCount"])
        assertEquals(0, map["followingCount"])
        assertEquals(0, map["postsCount"])
        assertEquals("rahul", map["username"])
        assertEquals("user@example.com", map["email"])
        assertEquals("Nostalgic vibes", map["bio"])
    }

    @Test
    fun `test Follow model mapping`() {
        val time = Timestamp(Date(1000000))
        val follow = Follow(
            uid = "user_abc",
            username = "priya",
            displayName = "Priya Rao",
            avatarUrl = "https://example.com/avatar.jpg",
            followedAt = time
        )

        val map = follow.toMap()
        assertEquals("user_abc", map["uid"])
        assertEquals("priya", map["username"])
        assertEquals("Priya Rao", map["displayName"])
        assertEquals("https://example.com/avatar.jpg", map["avatarUrl"])
        assertEquals(time, map["followedAt"])
    }

    @Test
    fun `test FollowRequest model mapping and status`() {
        val time = Timestamp(Date(1000000))
        val request = FollowRequest(
            requestId = "req_123_456",
            requesterUid = "user_123",
            requesterUsername = "rahul",
            requesterDisplayName = "Rahul",
            targetUid = "user_456",
            status = "PENDING",
            createdAt = time
        )

        val map = request.toMap()
        assertEquals("req_123_456", map["requestId"])
        assertEquals("user_123", map["requesterUid"])
        assertEquals("user_456", map["targetUid"])
        assertEquals("PENDING", map["status"])
        assertEquals(time, map["createdAt"])
    }

    @Test
    fun `test AppNotification model mapping`() {
        val time = Timestamp(Date(1000000))
        val notification = AppNotification(
            id = "notif_999",
            recipientUid = "target_uid",
            actorUid = "actor_uid",
            actorUsername = "rahul",
            actorDisplayName = "Rahul",
            type = "FOLLOW",
            isRead = false,
            createdAt = time
        )

        val map = notification.toMap()
        assertEquals("notif_999", map["id"])
        assertEquals("target_uid", map["recipientUid"])
        assertEquals("actor_uid", map["actorUid"])
        assertEquals("FOLLOW", map["type"])
        assertEquals(false, map["isRead"])
        assertEquals(time, map["createdAt"])
    }

    @Test
    fun `test Report model mapping`() {
        val time = Timestamp(Date(1000000))
        val report = Report(
            reportId = "rep_001",
            reporterUid = "reporter_1",
            reportedUid = "bad_actor",
            reportedUsername = "spammer_99",
            reason = "Spam",
            details = "Sending automated promotional messages",
            createdAt = time
        )

        val map = report.toMap()
        assertEquals("rep_001", map["reportId"])
        assertEquals("reporter_1", map["reporterUid"])
        assertEquals("bad_actor", map["reportedUid"])
        assertEquals("spammer_99", map["reportedUsername"])
        assertEquals("Spam", map["reason"])
        assertEquals("Sending automated promotional messages", map["details"])
    }

    @Test
    fun `test RelationshipState enum contains mutual Buddys state`() {
        assertEquals(7, RelationshipState.values().size)
        assertTrue(RelationshipState.values().contains(RelationshipState.SELF))
        assertTrue(RelationshipState.values().contains(RelationshipState.NOT_FOLLOWING))
        assertTrue(RelationshipState.values().contains(RelationshipState.FOLLOWING))
        assertTrue(RelationshipState.values().contains(RelationshipState.REQUESTED))
        assertTrue(RelationshipState.values().contains(RelationshipState.FOLLOWED_BY))
        assertTrue(RelationshipState.values().contains(RelationshipState.MUTUAL))
        assertTrue(RelationshipState.values().contains(RelationshipState.BLOCKED))
    }
}

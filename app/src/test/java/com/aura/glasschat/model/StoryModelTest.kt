package com.aura.glasschat.model

import com.aura.glasschat.data.model.Story
import com.aura.glasschat.data.model.UserStories
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class StoryModelTest {

    @Test
    fun testStoryCreationAndExpiration() {
        val now = Timestamp.now()
        val expiredTimestamp = Timestamp(now.seconds - 100, 0)
        val activeTimestamp = Timestamp(now.seconds + 86400, 0)

        val expiredStory = Story(
            id = "story_1",
            userId = "user_1",
            mediaUrl = "https://example.com/story1.jpg",
            caption = "Old Memory",
            expiresAt = expiredTimestamp
        )

        val activeStory = Story(
            id = "story_2",
            userId = "user_1",
            mediaUrl = "https://example.com/story2.jpg",
            caption = "Today's fun!",
            expiresAt = activeTimestamp
        )

        assertTrue(expiredStory.isExpired)
        assertFalse(activeStory.isExpired)
    }

    @Test
    fun testStoryViewedTracking() {
        val story = Story(
            id = "story_123",
            userId = "user_1",
            mediaUrl = "https://example.com/story.jpg",
            viewedBy = listOf("viewer_A", "viewer_B")
        )

        assertTrue(story.isViewedBy("viewer_A"))
        assertTrue(story.isViewedBy("viewer_B"))
        assertFalse(story.isViewedBy("viewer_C"))
    }

    @Test
    fun testUserStoriesUnreadComputation() {
        val story1 = Story(id = "s1", userId = "user_A", viewedBy = listOf("viewer_1"))
        val story2 = Story(id = "s2", userId = "user_A", viewedBy = emptyList())

        val userStories = UserStories(
            userId = "user_A",
            username = "arjun",
            userDisplayName = "Arjun",
            userAvatarUrl = null,
            stories = listOf(story1, story2)
        )

        // viewer_1 has seen s1 but not s2
        assertTrue(userStories.hasUnreadFor("viewer_1"))

        // viewer_2 has seen neither
        assertTrue(userStories.hasUnreadFor("viewer_2"))

        // Both seen
        val allSeenStories = UserStories(
            userId = "user_A",
            username = "arjun",
            userDisplayName = "Arjun",
            userAvatarUrl = null,
            stories = listOf(story1.copy(viewedBy = listOf("viewer_1", "viewer_2")), story2.copy(viewedBy = listOf("viewer_1", "viewer_2")))
        )
        assertFalse(allSeenStories.hasUnreadFor("viewer_1"))
        assertFalse(allSeenStories.hasUnreadFor("viewer_2"))
    }

    @Test
    fun testStorySerializationMap() {
        val now = Timestamp.now()
        val story = Story(
            id = "story_xyz",
            userId = "user_xyz",
            username = "nostalgic_kid",
            userDisplayName = "Nostalgic Kid",
            userAvatarUrl = "https://example.com/avatar.jpg",
            mediaUrl = "https://example.com/image.jpg",
            caption = "Maggi & Cartoon Network!",
            createdAt = now,
            expiresAt = Timestamp(now.seconds + 86400, 0),
            viewedBy = listOf("uid1")
        )

        val map = story.toMap()
        assertEquals("story_xyz", map["id"])
        assertEquals("user_xyz", map["userId"])
        assertEquals("nostalgic_kid", map["username"])
        assertEquals("Maggi & Cartoon Network!", map["caption"])
        assertEquals(listOf("uid1"), map["viewedBy"])
    }
}

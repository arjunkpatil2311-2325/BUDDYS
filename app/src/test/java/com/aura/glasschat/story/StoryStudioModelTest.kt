package com.aura.glasschat.story

import com.aura.glasschat.data.model.Story
import com.aura.glasschat.data.model.StoryStickerItem
import com.aura.glasschat.data.model.StoryTextOverlay
import com.aura.glasschat.data.model.StoryViewerEntry
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class StoryStudioModelTest {

    @Test
    fun storyTextOverlay_serializationAndDeserialization_preservesAllFields() {
        val overlay = StoryTextOverlay(
            id = "txt-123",
            text = "Neon Spidey ✨",
            style = "NEON",
            color = 0xFFFF2A42,
            backgroundColor = 0xCC121216,
            alignment = "CENTER",
            x = 0.5f,
            y = 0.4f,
            scale = 1.25f,
            rotation = 15.0f
        )

        val map = overlay.toMap()
        val restored = StoryTextOverlay.fromMap(map)

        assertEquals("txt-123", restored.id)
        assertEquals("Neon Spidey ✨", restored.text)
        assertEquals("NEON", restored.style)
        assertEquals(0xFFFF2A42, restored.color)
        assertEquals(0xCC121216, restored.backgroundColor)
        assertEquals(0.5f, restored.x, 0.001f)
        assertEquals(0.4f, restored.y, 0.001f)
        assertEquals(1.25f, restored.scale, 0.001f)
        assertEquals(15.0f, restored.rotation, 0.001f)
    }

    @Test
    fun storyStickerItem_serializationAndDeserialization_preservesDataMap() {
        val sticker = StoryStickerItem(
            id = "stk-456",
            type = "LOCATION",
            data = mapOf("name" to "Spider HQ"),
            x = 0.3f,
            y = 0.7f,
            scale = 1.5f,
            rotation = -5f
        )

        val map = sticker.toMap()
        val restored = StoryStickerItem.fromMap(map)

        assertEquals("stk-456", restored.id)
        assertEquals("LOCATION", restored.type)
        assertEquals("Spider HQ", restored.data["name"])
        assertEquals(0.3f, restored.x, 0.001f)
        assertEquals(0.7f, restored.y, 0.001f)
    }

    @Test
    fun storyViewerEntry_serializationAndDeserialization_preservesDetails() {
        val viewer = StoryViewerEntry(
            uid = "user-999",
            username = "spider_fan",
            displayName = "Peter",
            avatarUrl = "https://example.com/peter.jpg",
            reaction = "🔥"
        )

        val map = viewer.toMap()
        val restored = StoryViewerEntry.fromMap(map)

        assertEquals("user-999", restored.uid)
        assertEquals("spider_fan", restored.username)
        assertEquals("Peter", restored.displayName)
        assertEquals("https://example.com/peter.jpg", restored.avatarUrl)
        assertEquals("🔥", restored.reaction)
    }

    @Test
    fun story_visibilityAndExpiration_worksCorrectly() {
        val now = Timestamp.now()
        val activeStory = Story(
            id = "s-1",
            userId = "author-1",
            expiresAt = Timestamp(now.seconds + 3600, 0),
            audience = "CLOSE_FRIENDS",
            closeFriends = listOf("friend-1", "friend-2")
        )

        assertFalse(activeStory.isExpired)
        assertTrue(activeStory.isVisibleTo("author-1")) // Author can always see
        assertTrue(activeStory.isVisibleTo("friend-1"))
        assertFalse(activeStory.isVisibleTo("stranger-3"))

        val expiredStory = Story(
            id = "s-2",
            userId = "author-2",
            expiresAt = Timestamp(now.seconds - 100, 0),
            audience = "EVERYONE"
        )
        assertTrue(expiredStory.isExpired)
    }
}

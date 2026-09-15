package com.aura.glasschat.data.supabase

import com.aura.glasschat.BuildConfig

object SupabaseConfig {
    const val DEFAULT_PROJECT_URL = "https://sjinrlvwiwzdvszixyzh.supabase.co"
    const val DEFAULT_BUCKET = "buddys-media"

    // Default expiration for private bucket signed URLs:
    // Avatars: 7 days
    // Chat media & voice notes: 7 days
    // Stories: 24 hours (matching 24-hour story lifecycle)
    const val AVATAR_SIGNED_URL_EXPIRY_SECONDS = 7 * 24 * 60 * 60 // 7 days
    const val CHAT_MEDIA_SIGNED_URL_EXPIRY_SECONDS = 7 * 24 * 60 * 60 // 7 days
    const val STORY_SIGNED_URL_EXPIRY_SECONDS = 24 * 60 * 60 // 24 hours

    val projectUrl: String
        get() = BuildConfig.SUPABASE_URL.ifBlank { DEFAULT_PROJECT_URL }

    val bucketName: String
        get() = BuildConfig.SUPABASE_BUCKET.ifBlank { DEFAULT_BUCKET }

    /**
     * Retrieves the Supabase Publishable / Anon key.
     * Only client-safe anon key is used here.
     */
    val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY
}

package com.aura.glasschat.data.repository

import android.util.Log
import com.aura.glasschat.data.supabase.SupabaseConfig
import com.aura.glasschat.data.supabase.SupabaseStorageClient
import java.io.File

interface MediaStorageRepository {
    suspend fun uploadProfilePicture(userId: String, imageBytes: ByteArray): Result<String>
    suspend fun uploadChatImage(chatId: String, messageId: String, imageBytes: ByteArray, senderId: String = ""): Result<String>
    suspend fun uploadChatVoice(chatId: String, messageId: String, audioFile: File, senderId: String = ""): Result<String>
    suspend fun uploadStoryMedia(storyId: String, userId: String, imageBytes: ByteArray, audience: String = "EVERYONE"): Result<String>
    suspend fun uploadPostMedia(postId: String, userId: String, imageBytes: ByteArray): Result<String>
    suspend fun deleteChatMedia(chatId: String, messageId: String): Result<Unit>
    suspend fun deleteStoryMedia(storyId: String): Result<Unit>
    suspend fun deletePostMedia(postId: String): Result<Unit>
    suspend fun deleteProfilePicture(userId: String): Result<Unit>
    suspend fun resolveMediaUrl(rawUrlOrPath: String, expiresInSeconds: Int = SupabaseConfig.STORY_SIGNED_URL_EXPIRY_SECONDS): String
    fun clearCache()
}

class SupabaseMediaStorageRepository(
    private val storageClient: SupabaseStorageClient = SupabaseStorageClient()
) : MediaStorageRepository {

    companion object {
        private const val TAG = "BUDDYS_STORAGE_REPO"

        @Volatile
        private var instance: SupabaseMediaStorageRepository? = null

        // In-memory cache for resolved signed URLs to avoid repeated network signing requests
        // Key: cleanPath, Value: Pair(signedUrl, expiryTimestampMillis)
        private val signedUrlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

        fun getInstance(): SupabaseMediaStorageRepository {
            return instance ?: synchronized(this) {
                instance ?: SupabaseMediaStorageRepository().also { instance = it }
            }
        }
    }

    override fun clearCache() {
        signedUrlCache.clear()
    }

    /**
     * Resolves a raw storage path or potentially expired signed URL into a fresh valid signed URL.
     * Guarantees 100% compatibility for old story records, Highlights, and archived stories.
     */
    override suspend fun resolveMediaUrl(rawUrlOrPath: String, expiresInSeconds: Int): String {
        if (rawUrlOrPath.isBlank()) return ""

        // If it's a local URI or external non-Supabase URL, return as-is
        if (rawUrlOrPath.startsWith("file:") || rawUrlOrPath.startsWith("content:")) {
            return rawUrlOrPath
        }

        val cleanPath = extractStoragePath(rawUrlOrPath)
        if (cleanPath.isBlank()) {
            return rawUrlOrPath
        }

        val now = System.currentTimeMillis()
        val cached = signedUrlCache[cleanPath]
        if (cached != null && cached.second > now + 60_000L) {
            return cached.first
        }

        val signResult = storageClient.createSignedUrl(cleanPath, expiresInSeconds)
        return signResult.fold(
            onSuccess = { freshSignedUrl ->
                val expiryMillis = now + (expiresInSeconds * 1000L)
                signedUrlCache[cleanPath] = Pair(freshSignedUrl, expiryMillis)
                freshSignedUrl
            },
            onFailure = { error ->
                Log.w(TAG, "Failed to resolve fresh signed URL for path '$cleanPath': ${error.message}")
                rawUrlOrPath
            }
        )
    }

    private fun extractStoragePath(raw: String): String {
        val trimmed = raw.trim()
        val bucket = SupabaseConfig.bucketName

        // Case 1: Full Supabase URL e.g. .../object/sign/buddys-media/stories/123/img.jpg?token=...
        val bucketIndex = trimmed.indexOf("$bucket/")
        if (bucketIndex != -1) {
            val afterBucket = trimmed.substring(bucketIndex + bucket.length + 1)
            val queryIndex = afterBucket.indexOf('?')
            return if (queryIndex != -1) afterBucket.substring(0, queryIndex) else afterBucket
        }

        // Case 2: Direct relative storage path e.g. stories/123/img.jpg or profile_pictures/abc/profile.jpg
        val knownPrefixes = listOf("stories/", "posts/", "profile_pictures/", "chat_media/", "voice_messages/")
        for (prefix in knownPrefixes) {
            val prefixIndex = trimmed.indexOf(prefix)
            if (prefixIndex != -1) {
                val pathSegment = trimmed.substring(prefixIndex)
                val queryIndex = pathSegment.indexOf('?')
                return if (queryIndex != -1) pathSegment.substring(0, queryIndex) else pathSegment
            }
        }

        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) "" else trimmed.trimStart('/')
    }

    /**
     * Uploads user avatar to: profile_pictures/{userId}/profile.jpg
     * Returns signed access URL for private bucket.
     */
    override suspend fun uploadProfilePicture(userId: String, imageBytes: ByteArray): Result<String> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Image data is empty"))
        }

        val storagePath = "profile_pictures/$userId/profile.jpg"
        val uploadResult = storageClient.uploadBytes(
            path = storagePath,
            bytes = imageBytes,
            contentType = "image/jpeg",
            upsert = true
        )

        return uploadResult.fold(
            onSuccess = { path ->
                // Generate long-lived signed URL for private bucket
                val signedUrlResult = storageClient.createSignedUrl(
                    path = path,
                    expiresInSeconds = SupabaseConfig.AVATAR_SIGNED_URL_EXPIRY_SECONDS
                )
                signedUrlResult
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to upload profile picture for user $userId", error)
                Result.failure(error)
            }
        )
    }

    /**
     * Uploads chat image to: chat_media/{chatId}/{messageId}/{senderId}_image.jpg
     * Returns signed access URL for private bucket.
     */
    override suspend fun uploadChatImage(
        chatId: String,
        messageId: String,
        imageBytes: ByteArray,
        senderId: String
    ): Result<String> {
        if (chatId.isBlank() || messageId.isBlank()) {
            return Result.failure(IllegalArgumentException("Chat ID and Message ID cannot be blank"))
        }
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Image bytes cannot be empty"))
        }

        val resolvedSender = if (senderId.isNotBlank()) senderId else (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
        val filename = if (resolvedSender.isNotBlank()) "${resolvedSender}_image.jpg" else "image.jpg"
        val storagePath = "chat_media/$chatId/$messageId/$filename"
        val uploadResult = storageClient.uploadBytes(
            path = storagePath,
            bytes = imageBytes,
            contentType = "image/jpeg",
            upsert = true
        )

        return uploadResult.fold(
            onSuccess = { path ->
                storageClient.createSignedUrl(
                    path = path,
                    expiresInSeconds = SupabaseConfig.CHAT_MEDIA_SIGNED_URL_EXPIRY_SECONDS
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to upload chat image: $storagePath", error)
                Result.failure(error)
            }
        )
    }

    /**
     * Uploads voice recording to: voice_messages/{chatId}/{messageId}/{senderId}_voice.m4a
     * Returns signed access URL for private bucket.
     */
    override suspend fun uploadChatVoice(
        chatId: String,
        messageId: String,
        audioFile: File,
        senderId: String
    ): Result<String> {
        if (chatId.isBlank() || messageId.isBlank()) {
            return Result.failure(IllegalArgumentException("Chat ID and Message ID cannot be blank"))
        }
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return Result.failure(IllegalArgumentException("Audio file is missing or empty"))
        }

        val resolvedSender = if (senderId.isNotBlank()) senderId else (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
        val filename = if (resolvedSender.isNotBlank()) "${resolvedSender}_voice.m4a" else "voice.m4a"
        val storagePath = "voice_messages/$chatId/$messageId/$filename"
        val uploadResult = storageClient.uploadFile(
            path = storagePath,
            file = audioFile,
            contentType = "audio/mp4",
            upsert = true
        )

        return uploadResult.fold(
            onSuccess = { path ->
                storageClient.createSignedUrl(
                    path = path,
                    expiresInSeconds = SupabaseConfig.CHAT_MEDIA_SIGNED_URL_EXPIRY_SECONDS
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to upload voice note: $storagePath", error)
                Result.failure(error)
            }
        )
    }

    /**
     * Uploads 24-hour story media to: stories/{storyId}/{userId}_image.jpg
     * Returns 24-hour signed access URL for private bucket.
     */
    override suspend fun uploadStoryMedia(
        storyId: String,
        userId: String,
        imageBytes: ByteArray,
        audience: String
    ): Result<String> {
        if (storyId.isBlank() || userId.isBlank()) {
            return Result.failure(IllegalArgumentException("Story ID and User ID cannot be blank"))
        }
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Story image bytes cannot be empty"))
        }

        val storagePath = "stories/$storyId/${userId}_image.jpg"
        val uploadResult = storageClient.uploadBytes(
            path = storagePath,
            bytes = imageBytes,
            contentType = "image/jpeg",
            upsert = true
        )

        return uploadResult.fold(
            onSuccess = { path ->
                storageClient.createSignedUrl(
                    path = path,
                    expiresInSeconds = SupabaseConfig.STORY_SIGNED_URL_EXPIRY_SECONDS
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to upload story media: $storagePath", error)
                Result.failure(error)
            }
        )
    }

    /**
     * Uploads post media to: posts/{postId}/{userId}_image.jpg
     * Returns signed access URL for private bucket.
     */
    override suspend fun uploadPostMedia(
        postId: String,
        userId: String,
        imageBytes: ByteArray
    ): Result<String> {
        if (postId.isBlank() || userId.isBlank()) {
            return Result.failure(IllegalArgumentException("Post ID and User ID cannot be blank"))
        }
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Post image bytes cannot be empty"))
        }

        val storagePath = "posts/$postId/${userId}_image.jpg"
        val uploadResult = storageClient.uploadBytes(
            path = storagePath,
            bytes = imageBytes,
            contentType = "image/jpeg",
            upsert = true
        )

        return uploadResult.fold(
            onSuccess = { path ->
                storageClient.createSignedUrl(
                    path = path,
                    expiresInSeconds = SupabaseConfig.POST_MEDIA_SIGNED_URL_EXPIRY_SECONDS
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to upload post media: $storagePath", error)
                Result.failure(error)
            }
        )
    }

    /**
     * Deletes chat media associated with a message.
     */
    override suspend fun deleteChatMedia(chatId: String, messageId: String): Result<Unit> {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val paths = mutableListOf(
            "chat_media/$chatId/$messageId/image.jpg",
            "voice_messages/$chatId/$messageId/voice.m4a"
        )
        if (currentUid.isNotBlank()) {
            paths.add("chat_media/$chatId/$messageId/${currentUid}_image.jpg")
            paths.add("voice_messages/$chatId/$messageId/${currentUid}_voice.m4a")
        }
        return storageClient.deleteObjects(paths)
    }

    /**
     * Deletes story media file.
     */
    override suspend fun deleteStoryMedia(storyId: String): Result<Unit> {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val paths = mutableListOf("stories/$storyId/image.jpg")
        if (currentUid.isNotBlank()) {
            paths.add("stories/$storyId/${currentUid}_image.jpg")
        }
        return storageClient.deleteObjects(paths)
    }

    /**
     * Deletes post media file.
     */
    override suspend fun deletePostMedia(postId: String): Result<Unit> {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val paths = mutableListOf("posts/$postId/image.jpg")
        if (currentUid.isNotBlank()) {
            paths.add("posts/$postId/${currentUid}_image.jpg")
        }
        return storageClient.deleteObjects(paths)
    }

    /**
     * Deletes user profile picture.
     */
    override suspend fun deleteProfilePicture(userId: String): Result<Unit> {
        return storageClient.deleteObjects(listOf("profile_pictures/$userId/profile.jpg"))
    }
}

package com.aura.glasschat.data.supabase

import android.util.Log
import com.aura.glasschat.BuildConfig
import com.aura.glasschat.data.auth.AuthTokenProvider
import com.aura.glasschat.data.auth.FirebaseAuthTokenProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Native Android client for Supabase Storage REST API and Edge Function authentication bridge.
 * Enforces zero-trust token verification via media-auth Edge Function for private bucket buddys-media.
 * Automatically handles token refresh on 401 expiration.
 */
class SupabaseStorageClient(
    private val projectUrl: String = SupabaseConfig.projectUrl,
    private val bucketName: String = SupabaseConfig.bucketName,
    private val publishableKey: String = SupabaseConfig.publishableKey,
    private val tokenProvider: AuthTokenProvider = FirebaseAuthTokenProvider.getInstance(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    companion object {
        private const val TAG = "BUDDYS_SUPABASE"
    }

    private val baseUrl: String
        get() = projectUrl.trimEnd('/') + "/storage/v1"

    private val functionsUrl: String
        get() = projectUrl.trimEnd('/') + "/functions/v1"

    /**
     * Calls the media-auth Supabase Edge Function to securely authorize operations.
     * Automatically performs a single forced token refresh and retry if HTTP 401 is received.
     */
    private suspend fun callMediaAuth(
        action: String,
        path: String,
        contentType: String? = null,
        expiresInSeconds: Int? = null,
        forceTokenRefresh: Boolean = false
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        val firebaseToken = tokenProvider.getToken(forceRefresh = forceTokenRefresh)
        if (firebaseToken.isNullOrBlank()) {
            Log.e(TAG, "Cannot call media-auth: Firebase ID token is unavailable.")
            return@withContext Result.failure(
                SupabaseStorageException(401, "User is not authenticated. Please sign in.")
            )
        }

        try {
            val edgeUrl = "$functionsUrl/media-auth"
            val jsonPayload = JSONObject().apply {
                put("firebaseToken", firebaseToken)
                put("action", action)
                put("path", path)
                if (contentType != null) put("contentType", contentType)
                if (expiresInSeconds != null) put("expiresIn", expiresInSeconds)
            }.toString()

            val edgeRequest = Request.Builder()
                .url(edgeUrl)
                .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
                .addHeader("apikey", publishableKey)
                .addHeader("Authorization", "Bearer $publishableKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(edgeRequest).execute()
            val responseBody = response.body?.string().orEmpty()

            // If token expired (HTTP 401) and we haven't retried yet, force refresh token and retry once
            if (response.code == 401 && !forceTokenRefresh) {
                Log.w(TAG, "media-auth returned 401 (token expired/invalid). Forcing token refresh and retrying...")
                return@withContext callMediaAuth(
                    action = action,
                    path = path,
                    contentType = contentType,
                    expiresInSeconds = expiresInSeconds,
                    forceTokenRefresh = true
                )
            }

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                Result.success(json)
            } else {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                Log.e(TAG, "media-auth Edge Function failed [HTTP ${response.code}]: $errorMsg (action=$action, path=$path)")
                Result.failure(SupabaseStorageException(response.code, errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "media-auth network error for action=$action, path=$path", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads in-memory byte array to private bucket buddys-media using a signed upload URL from media-auth.
     */
    suspend fun uploadBytes(
        path: String,
        bytes: ByteArray,
        contentType: String = "image/jpeg",
        upsert: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPath = path.trimStart('/')
        if (bytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Byte array is empty"))
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Requesting signed upload URL for bytes: path=$cleanPath, size=${bytes.size} bytes, type=$contentType")
        }

        // 1. Authorize and obtain signed upload URL via media-auth
        val authResult = callMediaAuth(
            action = "upload",
            path = cleanPath,
            contentType = contentType
        )

        val authJson = authResult.getOrElse { error ->
            Log.e(TAG, "Upload authorization failed for path=$cleanPath: ${error.message}")
            return@withContext Result.failure(error)
        }

        val rawUploadUrl = authJson.optString("uploadUrl", "")
        if (rawUploadUrl.isBlank()) {
            return@withContext Result.failure(
                SupabaseStorageException(500, "Missing upload URL from media-auth response")
            )
        }

        val fullUploadUrl = if (rawUploadUrl.startsWith("http")) rawUploadUrl else projectUrl.trimEnd('/') + rawUploadUrl

        // 2. PUT byte payload directly to signed upload URL
        try {
            val mediaType = contentType.toMediaTypeOrNull()
            val requestBody = bytes.toRequestBody(mediaType)

            val uploadReq = Request.Builder()
                .url(fullUploadUrl)
                .put(requestBody)
                .addHeader("Content-Type", contentType)
                .build()

            val uploadResp = httpClient.newCall(uploadReq).execute()
            val uploadBody = uploadResp.body?.string().orEmpty()

            if (uploadResp.isSuccessful) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Signed upload successful: $cleanPath")
                }
                Result.success(cleanPath)
            } else {
                val errorMsg = parseErrorMessage(uploadResp.code, uploadBody)
                Log.e(TAG, "Upload to signed URL failed [HTTP ${uploadResp.code}]: $errorMsg")
                Result.failure(SupabaseStorageException(uploadResp.code, errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during byte upload to signed URL: $cleanPath", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads a local file to private bucket buddys-media using a signed upload URL from media-auth.
     */
    suspend fun uploadFile(
        path: String,
        file: File,
        contentType: String = "audio/mp4",
        upsert: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPath = path.trimStart('/')
        if (!file.exists() || file.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("File is empty or does not exist"))
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Requesting signed upload URL for file: path=$cleanPath, size=${file.length()} bytes, type=$contentType")
        }

        // 1. Authorize and obtain signed upload URL via media-auth
        val authResult = callMediaAuth(
            action = "upload",
            path = cleanPath,
            contentType = contentType
        )

        val authJson = authResult.getOrElse { error ->
            Log.e(TAG, "File upload authorization failed for path=$cleanPath: ${error.message}")
            return@withContext Result.failure(error)
        }

        val rawUploadUrl = authJson.optString("uploadUrl", "")
        if (rawUploadUrl.isBlank()) {
            return@withContext Result.failure(
                SupabaseStorageException(500, "Missing upload URL from media-auth response")
            )
        }

        val fullUploadUrl = if (rawUploadUrl.startsWith("http")) rawUploadUrl else projectUrl.trimEnd('/') + rawUploadUrl

        // 2. PUT file payload directly to signed upload URL
        try {
            val mediaType = contentType.toMediaTypeOrNull()
            val requestBody = file.asRequestBody(mediaType)

            val uploadReq = Request.Builder()
                .url(fullUploadUrl)
                .put(requestBody)
                .addHeader("Content-Type", contentType)
                .build()

            val uploadResp = httpClient.newCall(uploadReq).execute()
            val uploadBody = uploadResp.body?.string().orEmpty()

            if (uploadResp.isSuccessful) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Signed file upload successful: $cleanPath")
                }
                Result.success(cleanPath)
            } else {
                val errorMsg = parseErrorMessage(uploadResp.code, uploadBody)
                Log.e(TAG, "File upload to signed URL failed [HTTP ${uploadResp.code}]: $errorMsg")
                Result.failure(SupabaseStorageException(uploadResp.code, errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during file upload to signed URL: $cleanPath", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a signed URL for reading private bucket objects via media-auth Edge Function.
     */
    suspend fun createSignedUrl(
        path: String,
        expiresInSeconds: Int = SupabaseConfig.CHAT_MEDIA_SIGNED_URL_EXPIRY_SECONDS
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPath = path.trimStart('/')

        val authResult = callMediaAuth(
            action = "sign",
            path = cleanPath,
            expiresInSeconds = expiresInSeconds
        )

        val authJson = authResult.getOrElse { error ->
            return@withContext Result.failure(error)
        }

        val rawSignedUrl = authJson.optString("signedUrl", "")
        if (rawSignedUrl.isBlank()) {
            return@withContext Result.failure(
                SupabaseStorageException(500, "Missing signedUrl in media-auth response")
            )
        }

        val fullUrl = if (rawSignedUrl.startsWith("http")) {
            rawSignedUrl
        } else if (rawSignedUrl.startsWith("/storage/v1")) {
            projectUrl.trimEnd('/') + rawSignedUrl
        } else {
            baseUrl + "/" + rawSignedUrl.trimStart('/')
        }

        Result.success(fullUrl)
    }

    /**
     * Deletes one or multiple objects by prefixes:
     * DELETE /storage/v1/object/{bucket}
     */
    suspend fun deleteObjects(paths: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        if (paths.isEmpty()) return@withContext Result.success(Unit)
        val url = "$baseUrl/object/$bucketName"

        try {
            val jsonArray = JSONArray()
            for (p in paths) {
                jsonArray.put(p.trimStart('/'))
            }
            val jsonPayload = JSONObject().apply {
                put("prefixes", jsonArray)
            }.toString()

            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .delete(requestBody)
                .addHeader("apikey", publishableKey)
                .addHeader("Authorization", "Bearer $publishableKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val responseBody = response.body?.string().orEmpty()
                val errorMsg = parseErrorMessage(response.code, responseBody)
                Result.failure(SupabaseStorageException(response.code, errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(code: Int, responseBody: String): String {
        return try {
            if (responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                json.optString("error", json.optString("message", json.optString("msg", responseBody)))
            } else {
                "HTTP $code error"
            }
        } catch (_: Exception) {
            responseBody.ifBlank { "HTTP $code error" }
        }
    }
}

class SupabaseStorageException(
    val statusCode: Int,
    message: String,
    cause: Throwable? = null
) : IOException("Supabase Storage Error [HTTP $statusCode]: $message", cause)

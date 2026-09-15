package com.aura.glasschat.data.supabase

import android.util.Log
import com.aura.glasschat.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
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
 * Native Android client for Supabase Storage REST API.
 * Uses only publishable anon key with private bucket buddys-media.
 */
class SupabaseStorageClient(
    private val projectUrl: String = SupabaseConfig.projectUrl,
    private val bucketName: String = SupabaseConfig.bucketName,
    private val publishableKey: String = SupabaseConfig.publishableKey,
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

    private suspend fun getFirebaseToken(): String? {
        return try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve Firebase ID token", e)
            null
        }
    }

    /**
     * Uploads in-memory byte array to Supabase Storage.
     * Uses Edge Function token bridge when available, falling back to direct upload.
     */
    suspend fun uploadBytes(
        path: String,
        bytes: ByteArray,
        contentType: String = "image/jpeg",
        upsert: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPath = path.trimStart('/')
        val firebaseToken = getFirebaseToken()

        if (firebaseToken != null) {
            try {
                val edgeUrl = "$functionsUrl/media-auth"
                val jsonPayload = JSONObject().apply {
                    put("firebaseToken", firebaseToken)
                    put("action", "upload")
                    put("path", cleanPath)
                    put("contentType", contentType)
                }.toString()

                val edgeRequest = Request.Builder()
                    .url(edgeUrl)
                    .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
                    .addHeader("apikey", publishableKey)
                    .addHeader("Authorization", "Bearer $publishableKey")
                    .build()

                val edgeResponse = httpClient.newCall(edgeRequest).execute()
                val edgeBody = edgeResponse.body?.string().orEmpty()

                if (edgeResponse.isSuccessful && edgeBody.isNotBlank()) {
                    val json = JSONObject(edgeBody)
                    val rawUploadUrl = json.optString("uploadUrl", "")
                    if (rawUploadUrl.isNotBlank()) {
                        val fullUploadUrl = if (rawUploadUrl.startsWith("http")) rawUploadUrl else projectUrl.trimEnd('/') + rawUploadUrl
                        val uploadReq = Request.Builder()
                            .url(fullUploadUrl)
                            .put(bytes.toRequestBody(contentType.toMediaTypeOrNull()))
                            .addHeader("Content-Type", contentType)
                            .build()
                        val uploadResp = httpClient.newCall(uploadReq).execute()
                        if (uploadResp.isSuccessful) {
                            return@withContext Result.success(cleanPath)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Edge function upload bridge failed, falling back: ${e.message}")
            }
        }

        // Direct upload fallback
        val url = "$baseUrl/object/$bucketName/$cleanPath"
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Uploading bytes to Supabase: bucket=$bucketName, path=$cleanPath, size=${bytes.size} bytes")
        }

        try {
            val mediaType = contentType.toMediaTypeOrNull()
            val requestBody = bytes.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", publishableKey)
                .addHeader("Authorization", "Bearer $publishableKey")
                .addHeader("Content-Type", contentType)

            if (upsert) {
                requestBuilder.addHeader("x-upsert", "true")
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Supabase upload successful: $cleanPath")
                }
                Result.success(cleanPath)
            } else {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                Log.e(TAG, "Supabase upload failed [HTTP ${response.code}]: $errorMsg")
                Result.failure(SupabaseStorageException(response.code, errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase upload network/IO error", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads a local file stream to Supabase Storage.
     * Uses Edge Function token bridge when available, falling back to direct upload.
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

        val firebaseToken = getFirebaseToken()
        if (firebaseToken != null) {
            try {
                val edgeUrl = "$functionsUrl/media-auth"
                val jsonPayload = JSONObject().apply {
                    put("firebaseToken", firebaseToken)
                    put("action", "upload")
                    put("path", cleanPath)
                    put("contentType", contentType)
                }.toString()

                val edgeRequest = Request.Builder()
                    .url(edgeUrl)
                    .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
                    .addHeader("apikey", publishableKey)
                    .addHeader("Authorization", "Bearer $publishableKey")
                    .build()

                val edgeResponse = httpClient.newCall(edgeRequest).execute()
                val edgeBody = edgeResponse.body?.string().orEmpty()

                if (edgeResponse.isSuccessful && edgeBody.isNotBlank()) {
                    val json = JSONObject(edgeBody)
                    val rawUploadUrl = json.optString("uploadUrl", "")
                    if (rawUploadUrl.isNotBlank()) {
                        val fullUploadUrl = if (rawUploadUrl.startsWith("http")) rawUploadUrl else projectUrl.trimEnd('/') + rawUploadUrl
                        val uploadReq = Request.Builder()
                            .url(fullUploadUrl)
                            .put(file.asRequestBody(contentType.toMediaTypeOrNull()))
                            .addHeader("Content-Type", contentType)
                            .build()
                        val uploadResp = httpClient.newCall(uploadReq).execute()
                        if (uploadResp.isSuccessful) {
                            return@withContext Result.success(cleanPath)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Edge function file upload bridge failed, falling back: ${e.message}")
            }
        }

        // Direct upload fallback
        val url = "$baseUrl/object/$bucketName/$cleanPath"
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Uploading file to Supabase: bucket=$bucketName, path=$cleanPath, size=${file.length()} bytes")
        }

        try {
            val mediaType = contentType.toMediaTypeOrNull()
            val requestBody = file.asRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", publishableKey)
                .addHeader("Authorization", "Bearer $publishableKey")
                .addHeader("Content-Type", contentType)

            if (upsert) {
                requestBuilder.addHeader("x-upsert", "true")
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Supabase file upload successful: $cleanPath")
                }
                Result.success(cleanPath)
            } else {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                Log.e(TAG, "Supabase file upload failed [HTTP ${response.code}]: $errorMsg")
                Result.failure(SupabaseStorageException(response.code, errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase file upload error", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a signed URL for reading private bucket objects.
     * Uses Edge Function token bridge when available, falling back to direct sign endpoint.
     */
    suspend fun createSignedUrl(
        path: String,
        expiresInSeconds: Int = SupabaseConfig.CHAT_MEDIA_SIGNED_URL_EXPIRY_SECONDS
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPath = path.trimStart('/')
        val firebaseToken = getFirebaseToken()

        if (firebaseToken != null) {
            try {
                val edgeUrl = "$functionsUrl/media-auth"
                val jsonPayload = JSONObject().apply {
                    put("firebaseToken", firebaseToken)
                    put("action", "sign")
                    put("path", cleanPath)
                    put("expiresIn", expiresInSeconds)
                }.toString()

                val edgeRequest = Request.Builder()
                    .url(edgeUrl)
                    .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
                    .addHeader("apikey", publishableKey)
                    .addHeader("Authorization", "Bearer $publishableKey")
                    .build()

                val edgeResponse = httpClient.newCall(edgeRequest).execute()
                val edgeBody = edgeResponse.body?.string().orEmpty()

                if (edgeResponse.isSuccessful && edgeBody.isNotBlank()) {
                    val json = JSONObject(edgeBody)
                    val rawSignedUrl = json.optString("signedUrl", "")
                    if (rawSignedUrl.isNotBlank()) {
                        val fullUrl = if (rawSignedUrl.startsWith("http")) rawSignedUrl else projectUrl.trimEnd('/') + rawSignedUrl
                        return@withContext Result.success(fullUrl)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Edge function sign bridge failed, falling back: ${e.message}")
            }
        }

        // Direct sign fallback
        val url = "$baseUrl/object/sign/$bucketName/$cleanPath"
        try {
            val jsonPayload = JSONObject().apply {
                put("expiresIn", expiresInSeconds)
            }.toString()

            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", publishableKey)
                .addHeader("Authorization", "Bearer $publishableKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val rawSignedUrl = json.optString("signedURL", "")
                if (rawSignedUrl.isNotBlank()) {
                    val fullUrl = if (rawSignedUrl.startsWith("http")) {
                        rawSignedUrl
                    } else if (rawSignedUrl.startsWith("/storage/v1")) {
                        projectUrl.trimEnd('/') + rawSignedUrl
                    } else {
                        baseUrl + "/" + rawSignedUrl.trimStart('/')
                    }
                    Result.success(fullUrl)
                } else {
                    Result.failure(SupabaseStorageException(response.code, "Missing signedURL in response"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.code, responseBody)
                Log.e(TAG, "Failed to create signed URL [HTTP ${response.code}]: $errorMsg")
                Result.failure(SupabaseStorageException(response.code, errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating signed URL", e)
            Result.failure(e)
        }
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
                json.optString("message", json.optString("error", responseBody))
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

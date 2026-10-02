package com.aura.glasschat.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatHistoryMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

sealed class AiResponseResult {
    data class Success(val reply: String, val model: String) : AiResponseResult()
    data class Error(val message: String, val canRetry: Boolean = true) : AiResponseResult()
}

class BuddysAiRepository {

    companion object {
        private const val TAG = "BuddysAiRepository"
        // Secure server-side endpoint on Buddys backend (OpenAI secret key is kept securely on the server)
        const val AI_ENDPOINT_URL = "https://buddys01.vercel.app/api/ai/chat"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        @Volatile
        private var instance: BuddysAiRepository? = null

        fun getInstance(): BuddysAiRepository {
            return instance ?: synchronized(this) {
                instance ?: BuddysAiRepository().also { instance = it }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Sends conversation history to the secure Buddys AI backend endpoint.
     * The backend applies the system prompt and securely calls OpenAI with server-side keys.
     */
    suspend fun getAiResponse(history: List<ChatHistoryMessage>): AiResponseResult {
        return withContext(Dispatchers.IO) {
            try {
                if (history.isEmpty()) {
                    return@withContext AiResponseResult.Error("No messages provided.")
                }

                // Build JSON payload
                val messagesArray = JSONArray()
                for (msg in history.takeLast(20)) {
                    if (msg.content.isNotBlank()) {
                        val obj = JSONObject().apply {
                            put("role", if (msg.role == "user") "user" else "assistant")
                            put("content", msg.content.trim())
                        }
                        messagesArray.put(obj)
                    }
                }

                val payloadJson = JSONObject().apply {
                    put("messages", messagesArray)
                }.toString()

                val request = Request.Builder()
                    .url(AI_ENDPOINT_URL)
                    .header("Accept", "application/json")
                    .header("User-Agent", "Buddies-Android-App")
                    .post(payloadJson.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        Log.e(TAG, "AI request failed with code ${response.code}: $responseBody")
                        val errMsg = try {
                            val errJson = JSONObject(responseBody)
                            errJson.optString("error", "Server returned status ${response.code}")
                        } catch (_: Exception) {
                            "Connection issue (${response.code}). Please try again."
                        }
                        return@withContext AiResponseResult.Error(errMsg)
                    }

                    if (responseBody.isBlank()) {
                        return@withContext AiResponseResult.Error("Empty response from AI server.")
                    }

                    val json = JSONObject(responseBody)
                    val reply = json.optString("reply", "").trim()
                    val model = json.optString("model", "gpt-4o-mini")

                    if (reply.isNotBlank()) {
                        AiResponseResult.Success(reply = reply, model = model)
                    } else {
                        val err = json.optString("error", "Could not generate response.")
                        AiResponseResult.Error(err)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error calling AI backend", e)
                AiResponseResult.Error("Network connection error. Check your internet connection and retry.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error calling AI backend", e)
                AiResponseResult.Error("Something went wrong (${e.localizedMessage ?: "Unknown error"}).")
            }
        }
    }
}

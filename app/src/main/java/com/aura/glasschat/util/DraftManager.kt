package com.aura.glasschat.util

import android.content.Context
import android.content.SharedPreferences

class DraftManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "buddys_chat_drafts"
        private const val KEY_PREFIX = "draft_"
    }

    /**
     * Gets the saved draft for a specific conversation.
     */
    fun getDraft(chatId: String): String {
        if (chatId.isBlank()) return ""
        return prefs.getString(KEY_PREFIX + chatId, "") ?: ""
    }

    /**
     * Saves a draft for a specific conversation. If text is blank, removes the draft.
     */
    fun saveDraft(chatId: String, text: String) {
        if (chatId.isBlank()) return
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            clearDraft(chatId)
        } else {
            prefs.edit().putString(KEY_PREFIX + chatId, text).apply()
        }
    }

    /**
     * Clears the draft for a specific conversation.
     */
    fun clearDraft(chatId: String) {
        if (chatId.isBlank()) return
        prefs.edit().remove(KEY_PREFIX + chatId).apply()
    }
}

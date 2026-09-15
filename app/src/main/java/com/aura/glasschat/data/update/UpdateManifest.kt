package com.aura.glasschat.data.update

import org.json.JSONObject

/**
 * Model representing the online update release manifest from /update.json
 */
data class UpdateManifest(
    val latestVersion: String,
    val versionCode: Int,
    val apkUrl: String,
    val apkFileName: String,
    val fileSize: String,
    val releaseDate: String,
    val releaseNotes: List<String>,
    val isMandatory: Boolean,
    val minimumSupportedVersionCode: Int
) {
    companion object {
        fun fromJson(jsonStr: String): UpdateManifest {
            val json = JSONObject(jsonStr)
            val notesArray = json.optJSONArray("releaseNotes")
            val notesList = mutableListOf<String>()
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    notesList.add(notesArray.optString(i, ""))
                }
            }

            return UpdateManifest(
                latestVersion = json.optString("latestVersion", ""),
                versionCode = json.optInt("versionCode", 0),
                apkUrl = json.optString("apkUrl", ""),
                apkFileName = json.optString("apkFileName", "BUDDYS.apk"),
                fileSize = json.optString("fileSize", ""),
                releaseDate = json.optString("releaseDate", ""),
                releaseNotes = notesList.filter { it.isNotBlank() },
                isMandatory = json.optBoolean("isMandatory", false),
                minimumSupportedVersionCode = json.optInt("minimumSupportedVersionCode", 1)
            )
        }
    }
}

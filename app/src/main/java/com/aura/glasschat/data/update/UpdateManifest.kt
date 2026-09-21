package com.aura.glasschat.data.update

import android.os.Build
import org.json.JSONObject

/**
 * Model representing architecture-specific APK asset metadata.
 */
data class AbiAsset(
    val apkUrl: String,
    val apkFileName: String,
    val fileSize: String
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("apkUrl", apkUrl)
        json.put("apkFileName", apkFileName)
        json.put("fileSize", fileSize)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): AbiAsset {
            return AbiAsset(
                apkUrl = json.optString("apkUrl", ""),
                apkFileName = json.optString("apkFileName", ""),
                fileSize = json.optString("fileSize", "")
            )
        }
    }
}

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
    val minimumSupportedVersionCode: Int,
    val abis: Map<String, AbiAsset> = emptyMap()
) {
    /**
     * Resolves the best-matched APK asset for the current device's CPU architecture,
     * falling back to the universal APK if an exact architecture asset is not found.
     */
    fun getAssetForDevice(supportedAbis: Array<String> = Build.SUPPORTED_ABIS): AbiAsset {
        if (abis.isNotEmpty()) {
            for (abi in supportedAbis) {
                val asset = abis[abi]
                if (asset != null && asset.apkUrl.isNotBlank()) {
                    return asset
                }
            }
        }
        return AbiAsset(
            apkUrl = apkUrl,
            apkFileName = apkFileName,
            fileSize = fileSize
        )
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("latestVersion", latestVersion)
        json.put("versionCode", versionCode)
        json.put("apkUrl", apkUrl)
        json.put("apkFileName", apkFileName)
        json.put("fileSize", fileSize)
        json.put("releaseDate", releaseDate)
        val notesArray = org.json.JSONArray()
        releaseNotes.forEach { notesArray.put(it) }
        json.put("releaseNotes", notesArray)
        json.put("isMandatory", isMandatory)
        json.put("minimumSupportedVersionCode", minimumSupportedVersionCode)

        if (abis.isNotEmpty()) {
            val abisObj = JSONObject()
            for ((abiKey, abiAsset) in abis) {
                abisObj.put(abiKey, abiAsset.toJson())
            }
            json.put("abis", abisObj)
        }

        return json.toString()
    }

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

            val abisMap = mutableMapOf<String, AbiAsset>()
            val abisJson = json.optJSONObject("abis")
            if (abisJson != null) {
                val keys = abisJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val assetJson = abisJson.optJSONObject(key)
                    if (assetJson != null) {
                        abisMap[key] = AbiAsset.fromJson(assetJson)
                    }
                }
            }

            return UpdateManifest(
                latestVersion = json.optString("latestVersion", ""),
                versionCode = json.optInt("versionCode", 0),
                apkUrl = json.optString("apkUrl", ""),
                apkFileName = json.optString("apkFileName", "app-universal-debug.apk"),
                fileSize = json.optString("fileSize", ""),
                releaseDate = json.optString("releaseDate", ""),
                releaseNotes = notesList.filter { it.isNotBlank() },
                isMandatory = json.optBoolean("isMandatory", false),
                minimumSupportedVersionCode = json.optInt("minimumSupportedVersionCode", 1),
                abis = abisMap
            )
        }
    }
}

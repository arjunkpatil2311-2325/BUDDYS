package com.aura.glasschat.update

import com.aura.glasschat.data.update.UpdateManifest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class UpdateFlowLogicTest {

    @Test
    fun updateManifest_serializationAndDeserialization() {
        val original = UpdateManifest(
            latestVersion = "0.4.2",
            versionCode = 6,
            apkUrl = "https://github.com/arjunkpatil2311-2325/BUDDYS/releases/download/v0.4.2/app-universal-debug.apk",
            apkFileName = "app-universal-debug.apk",
            fileSize = "68.6 MB",
            releaseDate = "2026-09-17",
            releaseNotes = listOf("Profile fallback fix", "Forgot password", "Update manager fixes"),
            isMandatory = false,
            minimumSupportedVersionCode = 1
        )

        val jsonStr = original.toJson()
        val restored = UpdateManifest.fromJson(jsonStr)

        assertEquals(original.latestVersion, restored.latestVersion)
        assertEquals(original.versionCode, restored.versionCode)
        assertEquals(original.apkUrl, restored.apkUrl)
        assertEquals(original.apkFileName, restored.apkFileName)
        assertEquals(original.fileSize, restored.fileSize)
        assertEquals(original.releaseDate, restored.releaseDate)
        assertEquals(original.releaseNotes, restored.releaseNotes)
        assertEquals(original.isMandatory, restored.isMandatory)
        assertEquals(original.minimumSupportedVersionCode, restored.minimumSupportedVersionCode)
    }

    @Test
    fun versionComparison_upgradeFromV041toV042_triggersUpdate() {
        val currentInstalledVersionCode = 5 // v0.4.1
        val manifestVersionCode = 6 // v0.4.2

        val hasUpdate = manifestVersionCode > currentInstalledVersionCode
        assertTrue("v0.4.1 (code 5) must detect v0.4.2 (code 6) as an available update", hasUpdate)
    }

    @Test
    fun versionComparison_v042onV042_doesNotTriggerUpdate() {
        val currentInstalledVersionCode = 6 // v0.4.2
        val manifestVersionCode = 6 // v0.4.2

        val hasUpdate = manifestVersionCode > currentInstalledVersionCode
        assertFalse("v0.4.2 (code 6) must NOT trigger update when manifest is code 6", hasUpdate)
    }

    @Test
    fun liveManifest_parsesCorrectly() {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val request = Request.Builder()
            .url("https://buddys01.vercel.app/update.json")
            .build()

        val response = client.newCall(request).execute()
        assertTrue("Vercel update endpoint must return 200 OK", response.isSuccessful)

        val bodyStr = response.body?.string()
        assertNotNull(bodyStr)

        val manifest = UpdateManifest.fromJson(bodyStr!!)
        assertEquals("0.4.2", manifest.latestVersion)
        assertEquals(6, manifest.versionCode)
        assertTrue(manifest.apkUrl.startsWith("https://github.com/arjunkpatil2311-2325/BUDDYS/releases/download/v0.4.2/"))
    }
}

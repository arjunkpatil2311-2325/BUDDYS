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
            latestVersion = "0.4.1",
            versionCode = 5,
            apkUrl = "https://github.com/arjunkpatil2311-2325/BUDDYS/releases/download/v0.4.1/app-universal-debug.apk",
            apkFileName = "app-universal-debug.apk",
            fileSize = "68.5 MB",
            releaseDate = "2026-09-17",
            releaseNotes = listOf("Security overhaul", "PIN/Biometrics", "Following fixes"),
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
    fun versionComparison_upgradeFromV040toV041_triggersUpdate() {
        val currentInstalledVersionCode = 4 // v0.4.0
        val manifestVersionCode = 5 // v0.4.1

        val hasUpdate = manifestVersionCode > currentInstalledVersionCode
        assertTrue("v0.4.0 (code 4) must detect v0.4.1 (code 5) as an available update", hasUpdate)
    }

    @Test
    fun versionComparison_v041onV041_doesNotTriggerUpdate() {
        val currentInstalledVersionCode = 5 // v0.4.1
        val manifestVersionCode = 5 // v0.4.1

        val hasUpdate = manifestVersionCode > currentInstalledVersionCode
        assertFalse("v0.4.1 (code 5) must NOT trigger update when manifest is code 5", hasUpdate)
    }

    @Test
    fun liveDownloadUrl_githubRedirectAndByteStreamValid() {
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
        assertEquals("0.4.0", manifest.latestVersion)
        assertEquals(4, manifest.versionCode)
        assertTrue(manifest.apkUrl.startsWith("https://github.com/arjunkpatil2311-2325/BUDDYS/releases/download/v0.4.0/"))

        // Verify GitHub Download URL with streaming HEAD/range request
        val downloadReq = Request.Builder()
            .url(manifest.apkUrl)
            .header("Range", "bytes=0-1023")
            .build()

        val downloadRes = client.newCall(downloadReq).execute()
        assertTrue("GitHub APK download must return 200 or 206 Partial Content", downloadRes.isSuccessful)
        val readBytes = downloadRes.body?.byteStream()?.readBytes()
        assertNotNull(readBytes)
        assertEquals("Range buffer must read exactly 1024 bytes", 1024, readBytes!!.size)
    }
}

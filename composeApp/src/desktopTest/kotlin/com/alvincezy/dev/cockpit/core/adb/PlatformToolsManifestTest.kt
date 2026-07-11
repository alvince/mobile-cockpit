package com.alvincezy.dev.cockpit.core.adb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlatformToolsManifestTest {

    @Test
    fun parsesVersionAndChecksums() {
        val json = """
            {
              "version": "35.0.1",
              "sha256": {
                "darwin": "aaaa",
                "windows": "bbbb",
                "linux": "cccc"
              }
            }
        """.trimIndent()

        val manifest = PlatformToolsManifest.parse(json)
        assertEquals("35.0.1", manifest.version)
        assertEquals("aaaa", manifest.sha256For(AdbPlatform.MACOS))
        assertEquals("bbbb", manifest.sha256For(AdbPlatform.WINDOWS_X64))
        assertEquals("cccc", manifest.sha256For(AdbPlatform.LINUX_X64))
    }

    @Test
    fun placeholderTreatedAsMissing() {
        val json = """
            {
              "version": "35.0.1",
              "sha256": { "darwin": "PLACEHOLDER", "windows": "", "linux": "cccc" }
            }
        """.trimIndent()

        val manifest = PlatformToolsManifest.parse(json)
        assertNull(manifest.sha256For(AdbPlatform.MACOS))
        assertNull(manifest.sha256For(AdbPlatform.WINDOWS_X64))
        assertEquals("cccc", manifest.sha256For(AdbPlatform.LINUX_X64))
    }

    @Test
    fun loadsBundledResource() {
        val manifest = PlatformToolsManifest.load()
        assertEquals("35.0.1", manifest.version)
    }
}

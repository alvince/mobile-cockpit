package com.alvincezy.dev.cockpit.core.adb

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopAdbConfigStoreTest {

    private val configPath = "/home/user/.cockpit/config.properties".toPath()

    private fun store(fs: FakeFileSystem) = DesktopAdbConfigStore(fs, configPath)

    @Test
    fun returnsDefaultsWhenMissing() {
        val fs = FakeFileSystem()
        val config = store(fs).load()

        assertEquals(true, config.autoDownload)
        assertEquals(3, config.retryCount)
        assertEquals(1000L, config.retryBaseDelayMs)
        assertEquals(MeteredNetworkBehavior.ASK, config.meteredNetworkBehavior)
        assertEquals(2, config.maxCacheVersions)
        assertNull(config.adbPath)
    }

    @Test
    fun savesAndReloadsRoundTrip() {
        val fs = FakeFileSystem()
        val s = store(fs)
        val original = AdbConfig(
            adbPath = "/opt/adb",
            autoDownload = false,
            retryCount = 5,
            retryBaseDelayMs = 2000,
            meteredNetworkBehavior = MeteredNetworkBehavior.BLOCK,
            maxCacheVersions = 4,
        )
        s.save(original)

        val loaded = s.load()
        assertEquals(original, loaded)
    }

    @Test
    fun updateAppliesTransform() {
        val fs = FakeFileSystem()
        val s = store(fs)
        s.save(AdbConfig())

        val updated = s.update { it.copy(autoDownload = false, adbPath = "/x/adb") }

        assertEquals(false, updated.autoDownload)
        assertEquals("/x/adb", updated.adbPath)
        assertEquals(false, s.load().autoDownload)
    }

    @Test
    fun ignoresMalformedLines() {
        val fs = FakeFileSystem()
        fs.createDirectories(configPath.parent!!)
        fs.write(configPath) {
            writeUtf8(
                """
                # comment
                garbage-without-equals
                autoDownload=false
                retryCount=notanumber
                """.trimIndent()
            )
        }

        val config = store(fs).load()
        assertEquals(false, config.autoDownload)
        // Falls back to default when unparsable.
        assertEquals(3, config.retryCount)
    }
}

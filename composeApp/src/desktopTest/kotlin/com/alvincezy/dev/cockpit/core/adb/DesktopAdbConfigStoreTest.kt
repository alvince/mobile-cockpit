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

        assertNull(config.adbPath)
    }

    @Test
    fun savesAndReloadsRoundTrip() {
        val fs = FakeFileSystem()
        val s = store(fs)
        val original = AdbConfig(adbPath = "/opt/adb")
        s.save(original)

        val loaded = s.load()
        assertEquals(original, loaded)
    }

    @Test
    fun updateAppliesTransform() {
        val fs = FakeFileSystem()
        val s = store(fs)
        s.save(AdbConfig())

        val updated = s.update { it.copy(adbPath = "/x/adb") }

        assertEquals("/x/adb", updated.adbPath)
        assertEquals("/x/adb", s.load().adbPath)
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
                adb.path=/opt/adb
                """.trimIndent()
            )
        }

        val config = store(fs).load()
        assertEquals("/opt/adb", config.adbPath)
    }

    @Test
    fun blankAdbPathTreatedAsUnset() {
        val fs = FakeFileSystem()
        fs.createDirectories(configPath.parent!!)
        fs.write(configPath) { writeUtf8("adb.path=\n") }

        assertNull(store(fs).load().adbPath)
    }
}

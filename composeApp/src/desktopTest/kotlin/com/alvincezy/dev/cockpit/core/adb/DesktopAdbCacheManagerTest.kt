package com.alvincezy.dev.cockpit.core.adb

import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopAdbCacheManagerTest {

    private val toolsDir = "/data/tools/platform-tools".toPath()
    private val platform = AdbPlatform.LINUX_X64

    private fun manager(fs: FakeFileSystem) =
        DesktopAdbCacheManager(fs, toolsDir, platform)

    /** Builds a platform-tools style zip with `platform-tools/adb` inside. */
    private fun writeArchive(fs: FakeFileSystem, path: Path, adbContent: String = "#!/bin/sh\n") {
        val bytes = ByteArrayOutputStream().also { baos ->
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry("platform-tools/"))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("platform-tools/adb"))
                zip.write(adbContent.encodeToByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        path.parent?.let { fs.createDirectories(it) }
        fs.write(path) { write(bytes) }
    }

    @Test
    fun installUnzipsAndReturnsAdbPath() {
        val fs = FakeFileSystem()
        val archive = "/tmp/platform-tools.zip".toPath()
        writeArchive(fs, archive)

        val adb = manager(fs).install(archive, "35.0.1", expectedSha256 = null)

        assertEquals(toolsDir / "35.0.1" / "platform-tools" / "adb", adb)
        assertTrue(fs.exists(adb))
    }

    @Test
    fun cachedAdbReturnsInstalledPath() {
        val fs = FakeFileSystem()
        val archive = "/tmp/platform-tools.zip".toPath()
        writeArchive(fs, archive)
        val m = manager(fs)
        m.install(archive, "35.0.1", null)

        val cached = m.cachedAdb("35.0.1")
        assertTrue(cached != null && fs.exists(cached))
    }

    @Test
    fun cachedAdbNullWhenAbsent() {
        val fs = FakeFileSystem()
        assertNull(manager(fs).cachedAdb("35.0.1"))
    }

    @Test
    fun installFailsOnSha256Mismatch() {
        val fs = FakeFileSystem()
        val archive = "/tmp/platform-tools.zip".toPath()
        writeArchive(fs, archive)

        assertFailsWith<AdbVerificationException> {
            manager(fs).install(archive, "35.0.1", expectedSha256 = "deadbeef")
        }
    }

    @Test
    fun trimCacheKeepsMostRecent() {
        val fs = FakeFileSystem()
        val m = manager(fs)
        listOf("34.0.0", "35.0.0", "35.0.1").forEach { v ->
            val archive = "/tmp/$v.zip".toPath()
            writeArchive(fs, archive)
            m.install(archive, v, null)
        }

        m.trimCache(2)

        val remaining = fs.list(toolsDir)
            .filter { fs.metadataOrNull(it)?.isDirectory == true }
            .map { it.name }
        assertEquals(2, remaining.size)
    }

    @Test
    fun validateCacheFalseWhenMissing() {
        val fs = FakeFileSystem()
        assertFalse(manager(fs).validateCache("35.0.1"))
    }
}

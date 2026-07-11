package com.alvincezy.dev.cockpit.core.adb

import java.io.File
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okio.Path
import okio.fakefilesystem.FakeFileSystem

class DesktopAdbPathResolverTest {

    private val manifest = PlatformToolsManifest("35.0.1", mapOf("linux" to "cccc"))

    private class StubCache(val cached: Path? = null) : AdbCacheManager {
        override fun cachedAdb(version: String): Path? = cached
        override fun install(archive: Path, version: String, expectedSha256: String?): Path =
            error("unused")
        override fun validateCache(version: String): Boolean = cached != null
        override fun trimCache(keep: Int) {}
    }

    private class StubDownloader : AdbDownloader {
        override fun download(version: String, platform: AdbPlatform, destination: Path):
            Flow<DownloadProgress> = emptyFlow()
    }

    private fun resolver(
        fs: FakeFileSystem = FakeFileSystem(),
        config: AdbConfig = AdbConfig(),
        cache: AdbCacheManager = StubCache(),
        env: (String) -> String? = { null },
        path: List<String> = emptyList(),
    ): DesktopAdbPathResolver {
        val store = object : AdbConfigStore {
            override fun load() = config
            override fun save(config: AdbConfig) {}
            override fun update(transform: (AdbConfig) -> AdbConfig) = transform(config)
        }
        return DesktopAdbPathResolver(
            configStore = store,
            cacheManager = cache,
            downloader = StubDownloader(),
            fileSystem = fs,
            manifest = manifest,
            platform = AdbPlatform.LINUX_X64,
            env = env,
            pathEntries = { path },
        )
    }

    private fun createRealExecutable(): File {
        val f = File.createTempFile("adb", "")
        f.writeText("#!/bin/sh\n")
        f.setExecutable(true)
        f.deleteOnExit()
        return f
    }

    @Test
    fun resolvesFromSystemPath() = runTest {
        val exe = createRealExecutable()
        // executableName is "adb"; place a file named adb in a temp dir
        val dir = File(System.getProperty("java.io.tmpdir"), "adbpathtest-${System.nanoTime()}")
        dir.mkdirs()
        val adb = File(dir, "adb").apply { writeText("#!/bin/sh\n"); setExecutable(true); deleteOnExit() }
        dir.deleteOnExit()

        val resolution = resolver(path = listOf(dir.absolutePath)).resolve()

        val system = assertIs<AdbResolution.System>(resolution)
        assertEquals(AdbSource.SystemPath, system.source)
        assertTrue(system.path.toString().endsWith("adb"))
        exe.delete()
    }

    @Test
    fun resolvesFromCockpitEnvVar() = runTest {
        val dir = File(System.getProperty("java.io.tmpdir"), "adbenvtest-${System.nanoTime()}")
        dir.mkdirs()
        val adb = File(dir, "adb").apply { writeText("#!/bin/sh\n"); setExecutable(true); deleteOnExit() }
        dir.deleteOnExit()

        val resolution = resolver(env = { key ->
            if (key == "COCKPIT_ADB_PATH") adb.absolutePath else null
        }).resolve()

        val system = assertIs<AdbResolution.System>(resolution)
        assertEquals(AdbSource.EnvironmentVariable, system.source)
    }

    @Test
    fun fallsThroughInvalidConfigToNextPriority() = runTest {
        val dir = File(System.getProperty("java.io.tmpdir"), "adbfalltest-${System.nanoTime()}")
        dir.mkdirs()
        val adb = File(dir, "adb").apply { writeText("#!/bin/sh\n"); setExecutable(true); deleteOnExit() }
        dir.deleteOnExit()

        val resolution = resolver(
            config = AdbConfig(adbPath = "/nonexistent/adb"),
            path = listOf(dir.absolutePath),
        ).resolve()

        val system = assertIs<AdbResolution.System>(resolution)
        assertEquals(AdbSource.SystemPath, system.source)
    }

    @Test
    fun missingWhenNothingFound() = runTest {
        val resolution = resolver().resolve()
        assertIs<AdbResolution.Missing.NoSystemAdb>(resolution)
    }

    @Test
    fun resolveOrDownloadDisabledWhenAutoDownloadOff() = runTest {
        val flow = resolver(config = AdbConfig(autoDownload = false)).resolveOrDownload()
        val emissions = mutableListOf<AdbResolution>()
        flow.collect { emissions.add(it) }
        assertTrue(emissions.any { it is AdbResolution.Missing.DisabledByUser })
    }
}

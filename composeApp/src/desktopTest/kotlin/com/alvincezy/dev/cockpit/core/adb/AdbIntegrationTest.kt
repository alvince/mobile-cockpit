package com.alvincezy.dev.cockpit.core.adb

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.fakefilesystem.FakeFileSystem
import org.junit.Assume.assumeTrue

/**
 * End-to-end integration test that exercises the real ADB resolution + execution
 * chain against a system adb. Skipped automatically when no adb is available so it
 * stays CI-safe.
 */
class AdbIntegrationTest {

    private val manifest = PlatformToolsManifest("35.0.1", emptyMap())

    private class NoopCache : AdbCacheManager {
        override fun cachedAdb(version: String): Path? = null
        override fun install(archive: Path, version: String, expectedSha256: String?): Path =
            error("unused")
        override fun validateCache(version: String): Boolean = false
        override fun trimCache(keep: Int) {}
    }

    private class NoopDownloader : AdbDownloader {
        override fun download(version: String, platform: AdbPlatform, destination: Path):
            Flow<DownloadProgress> = emptyFlow()
    }

    private fun buildStack(): Triple<AdbPathResolver, AdbCommandExecutor, AdbDeviceDetector> {
        val configStore = object : AdbConfigStore {
            override fun load() = AdbConfig()
            override fun save(config: AdbConfig) {}
            override fun update(transform: (AdbConfig) -> AdbConfig) = transform(AdbConfig())
        }
        val platform = DesktopPaths.currentPlatform() ?: AdbPlatform.LINUX_X64
        val resolver = DesktopAdbPathResolver(
            configStore = configStore,
            cacheManager = NoopCache(),
            downloader = NoopDownloader(),
            fileSystem = FakeFileSystem(),
            manifest = manifest,
            platform = platform,
        )
        val executor = DesktopAdbCommandExecutor(resolver)
        val detector = DesktopAdbDeviceDetector(executor)
        return Triple(resolver, executor, detector)
    }

    @Test
    fun resolvesSystemAdb() = runTest {
        val (resolver, _, _) = buildStack()
        val resolution = resolver.resolve()
        assumeTrue("No system adb available; skipping", resolution is AdbResolution.System)

        val system = resolution as AdbResolution.System
        println("Resolved adb: ${system.path} (source=${system.source})")
        assertTrue(system.path.toString().contains("adb"))
    }

    @Test
    fun executesAdbVersion() = runTest {
        val (resolver, executor, _) = buildStack()
        assumeTrue("No system adb available; skipping", resolver.resolve() is AdbResolution.System)

        val output = executor.exec(AdbCommandRequest(args = listOf("version"))).getOrThrow()
        println("adb version exit=${output.exitCode}\n${output.stdout}")
        assertTrue(output.exitCode == 0)
        assertTrue(output.stdout.contains("Android Debug Bridge", ignoreCase = true))
    }

    @Test
    fun detectsDevices() = runTest {
        val (resolver, _, detector) = buildStack()
        assumeTrue("No system adb available; skipping", resolver.resolve() is AdbResolution.System)

        val devices = detector.detect().getOrThrow()
        println("Detected ${devices.size} device(s): $devices")
        // No assertion on count (may be zero); success is that parsing did not throw.
        assertTrue(devices.size >= 0)
    }
}

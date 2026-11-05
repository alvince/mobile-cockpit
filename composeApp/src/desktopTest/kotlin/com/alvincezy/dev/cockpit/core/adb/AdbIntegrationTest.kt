package com.alvincezy.dev.cockpit.core.adb

import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * End-to-end integration test that exercises the real ADB resolution + execution
 * chain against an adb available on the machine (system or bundled). Skipped
 * automatically when no adb is available so it stays CI-safe.
 */
class AdbIntegrationTest {

    private fun buildStack(): Triple<AdbPathResolver, AdbCommandExecutor, AdbDeviceDetector> {
        val configStore = object : AdbConfigStore {
            override fun load() = AdbConfig()
            override fun save(config: AdbConfig) {}
            override fun update(transform: (AdbConfig) -> AdbConfig) = transform(AdbConfig())
        }
        val platform = DesktopPaths.currentPlatform() ?: AdbPlatform.LINUX_X64
        val resolver = DesktopAdbPathResolver(
            configStore = configStore,
            fileSystem = FakeFileSystem(),
            platform = platform,
            bundledDir = null,
        )
        val executor = DesktopAdbCommandExecutor(resolver)
        val detector = DesktopAdbDeviceDetector(executor)
        return Triple(resolver, executor, detector)
    }

    private fun hasAdb(resolution: AdbResolution): Boolean =
        resolution is AdbResolution.System || resolution is AdbResolution.Bundled

    @Test
    fun resolvesAdb() = runTest {
        val (resolver, _, _) = buildStack()
        val resolution = resolver.resolve()
        assumeTrue("No adb available; skipping", hasAdb(resolution))

        println("Resolved adb: $resolution")
        assertTrue(resolution.toString().contains("adb"))
    }

    @Test
    fun executesAdbVersion() = runTest {
        val (resolver, executor, _) = buildStack()
        assumeTrue("No adb available; skipping", hasAdb(resolver.resolve()))

        val output = executor.exec(AdbCommandRequest(args = listOf("version"))).getOrThrow()
        println("adb version exit=${output.exitCode}\n${output.stdout}")
        assertTrue(output.exitCode == 0)
        assertTrue(output.stdout.contains("Android Debug Bridge", ignoreCase = true))
    }

    @Test
    fun detectsDevices() = runTest {
        val (resolver, _, detector) = buildStack()
        assumeTrue("No adb available; skipping", hasAdb(resolver.resolve()))

        val devices = detector.detect().getOrThrow()
        println("Detected ${devices.size} device(s): $devices")
        // No assertion on count (may be zero); success is that parsing did not throw.
        assertTrue(devices.size >= 0)
    }
}

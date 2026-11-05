package com.alvincezy.dev.cockpit.core.adb

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopAdbPathResolverTest {

    private fun resolver(
        fs: FakeFileSystem = FakeFileSystem(),
        config: AdbConfig = AdbConfig(),
        bundledDir: Path? = null,
        runtimeDir: Path? = "/runtime/bin".toPath(),
        isExecutable: (Path) -> Boolean = { false },
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
            fileSystem = fs,
            platform = AdbPlatform.LINUX_X64,
            bundledDir = bundledDir,
            runtimeDir = runtimeDir,
            isExecutable = isExecutable,
            env = env,
            pathEntries = { path },
        )
    }

    /** A real, executable file on the actual filesystem, named "adb". */
    private fun realAdbDir(prefix: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "$prefix-${System.nanoTime()}").apply {
            mkdirs()
            deleteOnExit()
        }
        File(dir, "adb").apply {
            writeText("#!/bin/sh\n")
            setExecutable(true)
            deleteOnExit()
        }
        return dir
    }

    @Test
    fun resolvesFromSystemPath() = runTest {
        val dir = realAdbDir("adbpathtest")

        val resolution = resolver(path = listOf(dir.absolutePath)).resolve()

        val system = assertIs<AdbResolution.System>(resolution)
        assertEquals(AdbSource.SystemPath, system.source)
        assertTrue(system.path.toString().endsWith("adb"))
    }

    @Test
    fun resolvesFromCockpitEnvVar() = runTest {
        val dir = realAdbDir("adbenvtest")
        val adb = File(dir, "adb").absolutePath

        val resolution = resolver(env = { key ->
            if (key == "COCKPIT_ADB_PATH") adb else null
        }).resolve()

        val system = assertIs<AdbResolution.System>(resolution)
        assertEquals(AdbSource.EnvironmentVariable, system.source)
    }

    @Test
    fun fallsThroughInvalidConfigToNextPriority() = runTest {
        val dir = realAdbDir("adbfalltest")

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
    fun bundledFallbackWhenNoSystemAdb() = runTest {
        val fs = FakeFileSystem()
        val bundledDir = "/bundle/adb".toPath()
        fs.createDirectories(bundledDir)
        fs.write(bundledDir / "adb") { writeUtf8("") }

        val resolution = resolver(
            fs = fs,
            bundledDir = bundledDir,
            isExecutable = { it.toString().endsWith("/adb") },
        ).resolve()

        val bundled = assertIs<AdbResolution.Bundled>(resolution)
        // Materialized into the writable runtime dir, not run from the bundle.
        assertEquals("/runtime/bin/adb".toPath(), bundled.path)
        assertNull(bundled.version)
        assertTrue(fs.exists("/runtime/bin/adb".toPath()))
    }

    @Test
    fun systemAdbTakesPriorityOverBundled() = runTest {
        val dir = realAdbDir("adbsystemfirst")
        val fs = FakeFileSystem()
        val bundledDir = "/bundle/adb".toPath()
        fs.createDirectories(bundledDir)
        fs.write(bundledDir / "adb") { writeUtf8("") }

        val resolution = resolver(
            fs = fs,
            bundledDir = bundledDir,
            isExecutable = { true },
            path = listOf(dir.absolutePath),
        ).resolve()

        val system = assertIs<AdbResolution.System>(resolution)
        assertEquals(AdbSource.SystemPath, system.source)
    }

    @Test
    fun readsBundledVersionFromVersionTxt() = runTest {
        val fs = FakeFileSystem()
        val bundledDir = "/bundle/adb".toPath()
        fs.createDirectories(bundledDir)
        fs.write(bundledDir / "adb") { writeUtf8("") }
        fs.write(bundledDir / "version.txt") {
            writeUtf8("Android Debug Bridge version 1.0.41\nVersion 35.0.1\n")
        }

        val resolution = resolver(
            fs = fs,
            bundledDir = bundledDir,
            isExecutable = { true },
        ).resolve()

        val bundled = assertIs<AdbResolution.Bundled>(resolution)
        assertEquals("Android Debug Bridge version 1.0.41", bundled.version)
    }

    @Test
    fun bundledSkippedWhenNotExecutable() = runTest {
        val fs = FakeFileSystem()
        val bundledDir = "/bundle/adb".toPath()
        fs.createDirectories(bundledDir)
        fs.write(bundledDir / "adb") { writeUtf8("") }

        val resolution = resolver(
            fs = fs,
            bundledDir = bundledDir,
            isExecutable = { false },
        ).resolve()

        assertIs<AdbResolution.Missing.NoSystemAdb>(resolution)
    }

    /**
     * jpackage drops the executable bit from app resources, so the bundled adb must be
     * copied to a writable dir and marked executable there, leaving the bundle intact.
     * Uses the real filesystem because the executable bit is what's under test.
     */
    @Test
    fun materializesBundledAdbIntoWritableDir() = runTest {
        val root = Files.createTempDirectory("adb-materialize").toFile().apply { deleteOnExit() }
        val bundledDir = File(root, "bundled").apply { mkdirs() }
        val runtimeDir = File(root, "runtime")
        val bundledAdb = File(bundledDir, "adb").apply {
            writeText("#!/bin/sh\n")
            setExecutable(false, false)
        }
        File(bundledDir, "version.txt").writeText("Android Debug Bridge version 1.0.41\n")

        val resolution = DesktopAdbPathResolver(
            configStore = object : AdbConfigStore {
                override fun load() = AdbConfig()
                override fun save(config: AdbConfig) {}
                override fun update(transform: (AdbConfig) -> AdbConfig) = transform(AdbConfig())
            },
            fileSystem = FileSystem.SYSTEM,
            platform = AdbPlatform.LINUX_X64,
            bundledDir = bundledDir.absolutePath.toPath(),
            runtimeDir = runtimeDir.absolutePath.toPath(),
            env = { null },
            pathEntries = { emptyList() },
        ).resolve()

        val bundled = assertIs<AdbResolution.Bundled>(resolution)
        assertEquals("Android Debug Bridge version 1.0.41", bundled.version)
        assertTrue(File(runtimeDir, "adb").canExecute(), "materialized copy must be executable")
        assertFalse(bundledAdb.canExecute(), "the app bundle must not be modified in place")
    }
}

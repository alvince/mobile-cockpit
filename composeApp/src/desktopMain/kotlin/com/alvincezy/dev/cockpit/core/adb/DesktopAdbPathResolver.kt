package com.alvincezy.dev.cockpit.core.adb

import java.io.File
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Resolves an adb executable following this priority chain:
 *
 * 1. `adb.path` in `~/.cockpit/config.properties`
 * 2. `COCKPIT_ADB_PATH`
 * 3. `ANDROID_HOME` / `ANDROID_SDK_ROOT` -> `platform-tools/adb`
 * 4. system `PATH`
 * 5. the adb bundled inside the application package
 *
 * An invalid entry at any level is skipped and resolution continues to the next.
 */
class DesktopAdbPathResolver(
    private val configStore: AdbConfigStore,
    private val fileSystem: FileSystem,
    private val platform: AdbPlatform = DesktopPaths.currentPlatform() ?: AdbPlatform.LINUX_X64,
    private val bundledDir: Path? = DesktopPaths.bundledAdbDir(),
    private val runtimeDir: Path? = DesktopPaths.bundledRuntimeDir,
    private val env: (String) -> String? = System::getenv,
    private val pathEntries: () -> List<String> = {
        System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
    },
    private val isExecutable: (Path) -> Boolean = { path ->
        val file = File(path.toString())
        file.isFile && file.canExecute()
    },
) : AdbPathResolver {

    override suspend fun resolve(): AdbResolution {
        val searched = mutableListOf<String>()

        findSystemAdb(searched)?.let { return it }
        findBundled(searched)?.let { return it }

        return AdbResolution.Missing.NoSystemAdb(searched)
    }

    private fun findSystemAdb(searched: MutableList<String>): AdbResolution.System? {
        // 1. user config
        configStore.load().adbPath?.let { configured ->
            searched += configured
            validExecutable(configured)?.let {
                return AdbResolution.System(it, AdbSource.UserConfig)
            }
        }

        // 2. COCKPIT_ADB_PATH
        env("COCKPIT_ADB_PATH")?.let { custom ->
            searched += custom
            validExecutable(custom)?.let {
                return AdbResolution.System(it, AdbSource.EnvironmentVariable)
            }
        }

        // 3. ANDROID_HOME / ANDROID_SDK_ROOT
        for (key in listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
            env(key)?.let { sdk ->
                val candidate = "$sdk${File.separator}platform-tools${File.separator}${platform.executableName}"
                searched += candidate
                validExecutable(candidate)?.let {
                    return AdbResolution.System(it, AdbSource.AndroidSdk)
                }
            }
        }

        // 4. PATH
        for (dir in pathEntries()) {
            if (dir.isBlank()) continue
            val candidate = "$dir${File.separator}${platform.executableName}"
            validExecutable(candidate)?.let {
                return AdbResolution.System(it, AdbSource.SystemPath)
            }
        }

        return null
    }

    /** 5. the adb shipped inside the app package (`compose.application.resources.dir/adb`). */
    private fun findBundled(searched: MutableList<String>): AdbResolution.Bundled? {
        val dir = bundledDir ?: return null
        val source = dir / platform.executableName
        searched += source.toString()

        val executable = materializeBundled(dir, source) ?: return null
        return AdbResolution.Bundled(executable, readBundledVersion(dir))
    }

    /**
     * Copies the bundled adb (and any sibling files, e.g. the Windows USB DLLs)
     * into [runtimeDir] and marks it executable. jpackage drops the executable bit
     * from app resources, and the bundle itself may be read-only (running from a
     * DMG) or signed, so we never modify it in place. Re-copies only when the size
     * differs, which keeps repeated `resolve()` calls cheap.
     */
    private fun materializeBundled(dir: Path, source: Path): Path? {
        if (!fileSystem.exists(source)) return null
        val targetDir = runtimeDir ?: return null

        return runCatching {
            fileSystem.createDirectories(targetDir)
            fileSystem.list(dir)
                .filter { fileSystem.metadataOrNull(it)?.isRegularFile == true }
                .forEach { file ->
                    val target = targetDir / file.name
                    val sameSize = fileSystem.metadataOrNull(target)?.size ==
                        fileSystem.metadataOrNull(file)?.size
                    if (!sameSize) {
                        if (fileSystem.exists(target)) fileSystem.delete(target)
                        fileSystem.copy(file, target)
                    }
                }

            val executable = targetDir / platform.executableName
            File(executable.toString()).setExecutable(true, false)
            executable.takeIf { isExecutable(it) }
        }.getOrNull()
    }

    private fun readBundledVersion(dir: Path): String? =
        runCatching { fileSystem.read(dir / VERSION_FILE) { readUtf8() } }
            .getOrNull()
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() }

    private fun validExecutable(path: String): Path? {
        val file = File(path)
        return if (file.isFile && file.canExecute()) path.toPath() else null
    }

    private companion object {
        const val VERSION_FILE = "version.txt"
    }
}

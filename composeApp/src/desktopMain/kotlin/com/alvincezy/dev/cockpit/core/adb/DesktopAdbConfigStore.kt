package com.alvincezy.dev.cockpit.core.adb

import okio.FileSystem
import okio.Path

/**
 * Persists [AdbConfig] to a Java-properties style file using Okio.
 *
 * The format is intentionally simple `key=value` lines so it stays readable and
 * testable with a [okio.fakefilesystem.FakeFileSystem].
 */
class DesktopAdbConfigStore(
    private val fileSystem: FileSystem,
    private val configFile: Path = DesktopPaths.configFile,
) : AdbConfigStore {

    override fun load(): AdbConfig {
        if (!fileSystem.exists(configFile)) return AdbConfig()
        val props = runCatching {
            fileSystem.read(configFile) { readUtf8() }.let(::parse)
        }.getOrElse { return AdbConfig() }

        return AdbConfig(
            adbPath = props[KEY_ADB_PATH]?.takeIf { it.isNotBlank() },
        )
    }

    override fun save(config: AdbConfig) {
        configFile.parent?.let { fileSystem.createDirectories(it) }
        val text = buildString {
            config.adbPath?.let { appendLine("$KEY_ADB_PATH=$it") }
        }
        fileSystem.write(configFile) { writeUtf8(text) }
    }

    override fun update(transform: (AdbConfig) -> AdbConfig): AdbConfig {
        val updated = transform(load())
        save(updated)
        return updated
    }

    private fun parse(text: String): Map<String, String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
            .toMap()

    private companion object {
        const val KEY_ADB_PATH = "adb.path"
    }
}

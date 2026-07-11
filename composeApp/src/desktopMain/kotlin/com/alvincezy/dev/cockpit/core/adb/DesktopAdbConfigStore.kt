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

        val defaults = AdbConfig()
        return AdbConfig(
            adbPath = props[KEY_ADB_PATH]?.takeIf { it.isNotBlank() },
            autoDownload = props[KEY_AUTO_DOWNLOAD]?.toBooleanStrictOrNull() ?: defaults.autoDownload,
            retryCount = props[KEY_RETRY_COUNT]?.toIntOrNull() ?: defaults.retryCount,
            retryBaseDelayMs = props[KEY_RETRY_DELAY]?.toLongOrNull() ?: defaults.retryBaseDelayMs,
            meteredNetworkBehavior = props[KEY_METERED]
                ?.let { runCatching { MeteredNetworkBehavior.valueOf(it) }.getOrNull() }
                ?: defaults.meteredNetworkBehavior,
            maxCacheVersions = props[KEY_MAX_CACHE]?.toIntOrNull() ?: defaults.maxCacheVersions,
        )
    }

    override fun save(config: AdbConfig) {
        configFile.parent?.let { fileSystem.createDirectories(it) }
        val text = buildString {
            config.adbPath?.let { appendLine("$KEY_ADB_PATH=$it") }
            appendLine("$KEY_AUTO_DOWNLOAD=${config.autoDownload}")
            appendLine("$KEY_RETRY_COUNT=${config.retryCount}")
            appendLine("$KEY_RETRY_DELAY=${config.retryBaseDelayMs}")
            appendLine("$KEY_METERED=${config.meteredNetworkBehavior.name}")
            appendLine("$KEY_MAX_CACHE=${config.maxCacheVersions}")
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
        const val KEY_AUTO_DOWNLOAD = "autoDownload"
        const val KEY_RETRY_COUNT = "retryCount"
        const val KEY_RETRY_DELAY = "retryBaseDelayMs"
        const val KEY_METERED = "meteredNetworkBehavior"
        const val KEY_MAX_CACHE = "maxCacheVersions"
    }
}

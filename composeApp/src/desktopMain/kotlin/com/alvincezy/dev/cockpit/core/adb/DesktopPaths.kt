package com.alvincezy.dev.cockpit.core.adb

import okio.Path
import okio.Path.Companion.toPath

/**
 * Central resolution of Cockpit's on-disk locations on the desktop.
 *
 * The data directory defaults to `~/.cockpit` and can be overridden via the
 * `cockpit.data-dir` system property. The config file lives inside the data dir.
 */
object DesktopPaths {

    val dataDir: Path
        get() {
            val override = System.getProperty("cockpit.data-dir")?.takeIf { it.isNotBlank() }
            if (override != null) return override.toPath()
            val home = System.getProperty("user.home") ?: "."
            return home.toPath() / ".cockpit"
        }

    val configFile: Path
        get() = dataDir / "config.properties"

    val toolsDir: Path
        get() = dataDir / "tools" / "platform-tools"

    fun versionDir(version: String): Path = toolsDir / version

    /** Detects the current platform, or null if unsupported. */
    fun currentPlatform(): AdbPlatform? {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            os.contains("mac") || os.contains("darwin") -> AdbPlatform.MACOS
            os.contains("win") -> AdbPlatform.WINDOWS_X64
            os.contains("nux") || os.contains("nix") -> AdbPlatform.LINUX_X64
            else -> null
        }
    }
}

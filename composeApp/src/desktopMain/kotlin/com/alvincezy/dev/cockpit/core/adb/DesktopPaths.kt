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

    // Note: window state is unrelated to ADB, but DesktopPaths is the single entry
    // point for everything under ~/.cockpit, so it lives here for now. If more
    // non-ADB paths accrue, promote DesktopPaths to a shared core/paths package.
    val windowStateFile: Path
        get() = dataDir / "window.properties"

    /**
     * Writable directory where the bundled adb is materialized at runtime. jpackage
     * drops the executable bit from app resources, so the binary is copied here and
     * marked executable instead of mutating the (possibly signed, possibly read-only)
     * application bundle.
     */
    val bundledRuntimeDir: Path
        get() = dataDir / "bin"

    /**
     * Directory holding the adb bundled at build time, or null when the app is not
     * running from a Compose Desktop distribution (the system property is set for
     * both `run` and packaged apps).
     */
    fun bundledAdbDir(): Path? {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return resourcesDir.toPath() / "adb"
    }

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

package com.alvincezy.dev.cockpit.core.adb

/**
 * UI-facing state for the adb environment, driven by [AdbPathResolver].
 */
sealed class AdbEnvironmentState {
    data object Checking : AdbEnvironmentState()
    data class Downloading(val bytesRead: Long, val totalBytes: Long) : AdbEnvironmentState()
    data class Ready(val path: String, val source: AdbSource, val version: String? = null) : AdbEnvironmentState()
    data class Missing(val resolution: AdbResolution.Missing) : AdbEnvironmentState()
}

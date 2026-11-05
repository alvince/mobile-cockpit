package com.alvincezy.dev.cockpit.core.adb

/**
 * User-facing ADB configuration persisted in `~/.cockpit/config.properties`.
 */
data class AdbConfig(
    val adbPath: String? = null,
)

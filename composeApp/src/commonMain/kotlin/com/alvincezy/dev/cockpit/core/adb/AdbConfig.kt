package com.alvincezy.dev.cockpit.core.adb

/**
 * User-facing ADB configuration persisted in `~/.cockpit/config.properties`.
 */
data class AdbConfig(
    val adbPath: String? = null,
    val autoDownload: Boolean = true,
    val retryCount: Int = 3,
    val retryBaseDelayMs: Long = 1000,
    val meteredNetworkBehavior: MeteredNetworkBehavior = MeteredNetworkBehavior.ASK,
    val maxCacheVersions: Int = 2,
)

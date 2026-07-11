package com.alvincezy.dev.cockpit.core.adb

/**
 * Detects connected devices and manages wireless connections.
 */
interface AdbDeviceDetector {
    /** Runs `adb devices -l` and parses the result. */
    suspend fun detect(): Result<List<AdbDevice>>

    /** Connects to a device over TCP/IP, e.g. `adb connect 192.168.1.10:5555`. */
    suspend fun connect(host: String, port: Int = 5555): Result<AdbCommandOutput>

    /** Disconnects a previously connected TCP/IP device. */
    suspend fun disconnect(host: String, port: Int = 5555): Result<AdbCommandOutput>
}

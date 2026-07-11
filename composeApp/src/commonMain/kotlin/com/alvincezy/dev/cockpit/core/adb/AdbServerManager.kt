package com.alvincezy.dev.cockpit.core.adb

/**
 * Manages the adb server lifecycle.
 */
interface AdbServerManager {
    suspend fun startServer(): Result<Unit>
    suspend fun killServer(): Result<Unit>
    suspend fun isRunning(): Boolean
    suspend fun restartServer(): Result<Unit>
}

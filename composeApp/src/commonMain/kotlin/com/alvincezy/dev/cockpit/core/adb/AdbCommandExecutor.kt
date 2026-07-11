package com.alvincezy.dev.cockpit.core.adb

import kotlinx.coroutines.flow.Flow

/**
 * Executes adb commands. The executor is responsible for locating the adb binary
 * (via [AdbPathResolver]) and injecting the correct environment (e.g. PATH).
 */
interface AdbCommandExecutor {
    /** Runs a command to completion and returns its captured output. */
    suspend fun exec(request: AdbCommandRequest): Result<AdbCommandOutput>

    /** Runs a long-lived command and streams its output line by line. */
    fun execStream(request: AdbCommandRequest): Flow<StreamLine>
}

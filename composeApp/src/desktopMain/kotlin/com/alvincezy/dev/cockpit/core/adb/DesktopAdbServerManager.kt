package com.alvincezy.dev.cockpit.core.adb

/**
 * Manages the adb server lifecycle via the [AdbCommandExecutor].
 */
class DesktopAdbServerManager(
    private val executor: AdbCommandExecutor,
) : AdbServerManager {

    override suspend fun startServer(): Result<Unit> =
        executor.exec(AdbCommandRequest(args = listOf("start-server")))
            .mapCatching { it.requireSuccess() }

    override suspend fun killServer(): Result<Unit> =
        executor.exec(AdbCommandRequest(args = listOf("kill-server")))
            .mapCatching { it.requireSuccess() }

    override suspend fun isRunning(): Boolean =
        executor.exec(AdbCommandRequest(args = listOf("start-server")))
            .map { it.exitCode == 0 }
            .getOrDefault(false)

    override suspend fun restartServer(): Result<Unit> =
        killServer().mapCatching { startServer().getOrThrow() }

    private fun AdbCommandOutput.requireSuccess() {
        if (exitCode != 0) {
            throw AdbCommandException(exitCode, stderr.ifBlank { stdout })
        }
    }
}

class AdbCommandException(val exitCode: Int, message: String) :
    Exception("adb command failed (exit=$exitCode): $message")

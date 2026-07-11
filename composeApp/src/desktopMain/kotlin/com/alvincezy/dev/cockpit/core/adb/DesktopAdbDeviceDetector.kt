package com.alvincezy.dev.cockpit.core.adb

/**
 * Detects connected devices using `adb devices -l` and manages wireless connections.
 */
class DesktopAdbDeviceDetector(
    private val executor: AdbCommandExecutor,
) : AdbDeviceDetector {

    override suspend fun detect(): Result<List<AdbDevice>> =
        executor.exec(AdbCommandRequest(args = listOf("devices", "-l")))
            .mapCatching { output ->
                if (output.exitCode != 0) {
                    throw AdbCommandException(output.exitCode, output.stderr.ifBlank { output.stdout })
                }
                AdbDeviceParser.parse(output.stdout)
            }

    override suspend fun connect(host: String, port: Int): Result<AdbCommandOutput> =
        executor.exec(AdbCommandRequest(args = listOf("connect", "$host:$port")))

    override suspend fun disconnect(host: String, port: Int): Result<AdbCommandOutput> =
        executor.exec(AdbCommandRequest(args = listOf("disconnect", "$host:$port")))
}

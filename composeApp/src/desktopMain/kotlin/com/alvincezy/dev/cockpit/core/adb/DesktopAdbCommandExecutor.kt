package com.alvincezy.dev.cockpit.core.adb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

/**
 * Executes adb via [ProcessBuilder]. The adb binary is located lazily through
 * [AdbPathResolver]; when a cached/downloaded adb is used, its directory is
 * prepended to the child process PATH so scrcpy and adb agree on the binary.
 */
class DesktopAdbCommandExecutor(
    private val pathResolver: AdbPathResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AdbCommandExecutor {

    override suspend fun exec(request: AdbCommandRequest): Result<AdbCommandOutput> =
        withContext(ioDispatcher) {
            runCatching {
                val adb = resolveAdbPath()
                val process = buildProcess(adb, request).start()

                val stdout = StringBuilder()
                val stderr = StringBuilder()
                val outReader = Thread { process.inputStream.bufferedReader().copyInto(stdout) }
                val errReader = Thread { process.errorStream.bufferedReader().copyInto(stderr) }
                outReader.start()
                errReader.start()

                val finished = process.waitFor(request.timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    outReader.join(1000)
                    errReader.join(1000)
                    throw AdbTimeoutException(request.args, request.timeout.inWholeMilliseconds)
                }
                outReader.join()
                errReader.join()

                AdbCommandOutput(
                    exitCode = process.exitValue(),
                    stdout = stdout.toString(),
                    stderr = stderr.toString(),
                )
            }
        }

    override fun execStream(request: AdbCommandRequest): Flow<StreamLine> = callbackFlow {
        val adb = resolveAdbPath()
        val process = buildProcess(adb, request).start()

        val outReader = Thread {
            process.inputStream.bufferedReader().forEachLine { trySend(StreamLine.Output(it)) }
        }
        val errReader = Thread {
            process.errorStream.bufferedReader().forEachLine { trySend(StreamLine.Error(it)) }
        }
        val waiter = Thread {
            outReader.start()
            errReader.start()
            val code = process.waitFor()
            outReader.join()
            errReader.join()
            trySend(StreamLine.Completed(code))
            close()
        }
        waiter.start()

        awaitClose {
            if (process.isAlive) process.destroyForcibly()
        }
    }.flowOn(ioDispatcher)

    private suspend fun resolveAdbPath(): String {
        return when (val resolution = pathResolver.resolve()) {
            is AdbResolution.System -> resolution.path.toString()
            is AdbResolution.Cached -> resolution.path.toString()
            else -> throw AdbNotFoundException(resolution)
        }
    }

    private fun buildProcess(adb: String, request: AdbCommandRequest): ProcessBuilder {
        val command = buildList {
            add(adb)
            addAll(request.args)
        }
        val builder = ProcessBuilder(command)
        request.workingDir?.let { builder.directory(java.io.File(it.toString())) }

        val environment = builder.environment()
        // Ensure the resolved adb's own directory wins on PATH.
        val adbDir = java.io.File(adb).parentFile?.absolutePath
        if (adbDir != null) {
            val sep = java.io.File.pathSeparator
            environment["PATH"] = adbDir + sep + (environment["PATH"] ?: "")
        }
        request.env.forEach { (k, v) -> environment[k] = v }
        return builder
    }

    private fun BufferedReader.copyInto(target: StringBuilder) {
        use { reader ->
            var line = reader.readLine()
            while (line != null) {
                target.append(line).append('\n')
                line = reader.readLine()
            }
        }
    }
}

class AdbTimeoutException(args: List<String>, timeoutMs: Long) :
    Exception("adb ${args.joinToString(" ")} timed out after ${timeoutMs}ms")

class AdbNotFoundException(val resolution: AdbResolution) :
    Exception("adb binary could not be resolved: $resolution")

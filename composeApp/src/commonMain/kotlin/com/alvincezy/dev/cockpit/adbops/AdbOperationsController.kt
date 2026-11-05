package com.alvincezy.dev.cockpit.adbops

import com.alvincezy.dev.cockpit.core.adb.AdbCommandExecutor
import com.alvincezy.dev.cockpit.core.adb.AdbCommandRequest
import com.alvincezy.dev.cockpit.core.adb.AdbDeviceDetector
import com.alvincezy.dev.cockpit.core.adb.AdbEnvironmentState
import com.alvincezy.dev.cockpit.core.adb.AdbPathResolver
import com.alvincezy.dev.cockpit.core.adb.AdbResolution
import com.alvincezy.dev.cockpit.core.adb.AdbServerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the state and coordinates the common ADB operations exposed by the UI area:
 * environment resolution, server lifecycle, device discovery and wireless connect.
 *
 * This is a lightweight state holder (not an androidx ViewModel) so it needs no extra
 * lifecycle dependency; it manages its own [CoroutineScope].
 */
class AdbOperationsController(
    private val pathResolver: AdbPathResolver,
    private val serverManager: AdbServerManager,
    private val deviceDetector: AdbDeviceDetector,
    private val executor: AdbCommandExecutor,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(AdbOperationsState())
    val state: StateFlow<AdbOperationsState> = _state.asStateFlow()

    fun checkEnvironment() = runExclusive("检测 adb 环境") {
        _state.update { it.copy(environment = AdbEnvironmentState.Checking) }
        val env = when (val resolution = pathResolver.resolve()) {
            is AdbResolution.System ->
                AdbEnvironmentState.Ready(resolution.path.toString(), resolution.source)

            is AdbResolution.Bundled ->
                AdbEnvironmentState.Ready(
                    resolution.path.toString(),
                    com.alvincezy.dev.cockpit.core.adb.AdbSource.Bundled,
                    resolution.version,
                )

            is AdbResolution.Missing ->
                AdbEnvironmentState.Missing(resolution)
        }
        _state.update { it.copy(environment = env) }
        if (env is AdbEnvironmentState.Ready) {
            val running = serverManager.isRunning()
            _state.update { it.copy(serverRunning = running) }
        }
        messageFor(env)
    }

    fun startServer() = runExclusive("启动 adb server") {
        serverManager.startServer().getOrThrow()
        _state.update { it.copy(serverRunning = true) }
        "adb server 已启动"
    }

    fun killServer() = runExclusive("停止 adb server") {
        serverManager.killServer().getOrThrow()
        _state.update { it.copy(serverRunning = false, devices = emptyList()) }
        "adb server 已停止"
    }

    fun restartServer() = runExclusive("重启 adb server") {
        serverManager.restartServer().getOrThrow()
        _state.update { it.copy(serverRunning = true) }
        "adb server 已重启"
    }

    fun refreshDevices() = runExclusive("刷新设备列表") {
        val devices = deviceDetector.detect().getOrThrow()
        _state.update { it.copy(devices = devices) }
        "检测到 ${devices.size} 台设备"
    }

    fun connect(host: String, port: Int) = runExclusive("连接 $host:$port") {
        val output = deviceDetector.connect(host, port).getOrThrow()
        val devices = deviceDetector.detect().getOrDefault(_state.value.devices)
        _state.update { it.copy(devices = devices) }
        output.stdout.ifBlank { output.stderr }.trim().ifBlank { "已发送连接请求" }
    }

    fun disconnect(host: String, port: Int) = runExclusive("断开 $host:$port") {
        val output = deviceDetector.disconnect(host, port).getOrThrow()
        val devices = deviceDetector.detect().getOrDefault(emptyList())
        _state.update { it.copy(devices = devices) }
        output.stdout.ifBlank { output.stderr }.trim().ifBlank { "已断开连接" }
    }

    fun runCustom(argsLine: String) = runExclusive("adb $argsLine") {
        val args = argsLine.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (args.isEmpty()) return@runExclusive "请输入命令参数"
        val output = executor.exec(AdbCommandRequest(args = args)).getOrThrow()
        buildString {
            append("exit=${output.exitCode}")
            output.stdout.trim().takeIf { it.isNotEmpty() }?.let { append("\n$it") }
            output.stderr.trim().takeIf { it.isNotEmpty() }?.let { append("\n$it") }
        }
    }

    fun dispose() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    private fun messageFor(env: AdbEnvironmentState): String = when (env) {
        is AdbEnvironmentState.Ready -> "adb 就绪 (${env.source})"
        is AdbEnvironmentState.Missing -> "未找到 adb: ${env.resolution::class.simpleName}"
        AdbEnvironmentState.Checking -> "检测中…"
    }

    private fun runExclusive(action: String, block: suspend () -> String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, lastMessage = "$action…") }
        scope.launch {
            val message = runCatching { block() }
                .getOrElse { t -> "$action 失败: ${t.message ?: t::class.simpleName}" }
            _state.update { it.copy(busy = false, lastMessage = message) }
        }
    }
}

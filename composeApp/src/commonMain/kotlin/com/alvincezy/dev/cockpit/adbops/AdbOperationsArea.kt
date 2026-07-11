package com.alvincezy.dev.cockpit.adbops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alvincezy.dev.cockpit.core.adb.AdbDevice
import com.alvincezy.dev.cockpit.core.adb.AdbEnvironmentState
import org.koin.compose.koinInject

/**
 * Callbacks wired from the ADB operations area to its [AdbOperationsController].
 * Extracting them lets [AdbOperationsContent] stay stateless and previewable.
 */
class AdbOperationsActions(
    val onCheckEnvironment: () -> Unit = {},
    val onStartServer: () -> Unit = {},
    val onKillServer: () -> Unit = {},
    val onRestartServer: () -> Unit = {},
    val onRefreshDevices: () -> Unit = {},
    val onConnect: (host: String, port: Int) -> Unit = { _, _ -> },
    val onDisconnect: (host: String, port: Int) -> Unit = { _, _ -> },
    val onRunCustom: (argsLine: String) -> Unit = {},
) {
    companion object {
        fun from(controller: AdbOperationsController) = AdbOperationsActions(
            onCheckEnvironment = controller::checkEnvironment,
            onStartServer = controller::startServer,
            onKillServer = controller::killServer,
            onRestartServer = controller::restartServer,
            onRefreshDevices = controller::refreshDevices,
            onConnect = controller::connect,
            onDisconnect = controller::disconnect,
            onRunCustom = controller::runCustom,
        )
    }
}

/**
 * Self-contained UI area that wraps the common ADB operations: environment status,
 * server lifecycle, device discovery and wireless connect. Drop it anywhere in the
 * app; it obtains its [AdbOperationsController] via Koin and delegates rendering to
 * the stateless [AdbOperationsContent].
 */
@Composable
fun AdbOperationsArea(
    modifier: Modifier = Modifier,
    controller: AdbOperationsController = koinInject(),
) {
    val state by controller.state.collectAsState()

    LaunchedEffect(Unit) { controller.checkEnvironment() }
    DisposableEffect(controller) { onDispose { controller.dispose() } }

    AdbOperationsContent(
        state = state,
        actions = AdbOperationsActions.from(controller),
        modifier = modifier,
    )
}

/**
 * Stateless rendering of the ADB operations area. Takes the [state] and [actions]
 * as parameters so it can be rendered in isolation (previews, tests) without Koin.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdbOperationsContent(
    state: AdbOperationsState,
    actions: AdbOperationsActions,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ADB 操作", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(12.dp))
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                }
            }

            Spacer(Modifier.height(8.dp))
            EnvironmentRow(state.environment, state.serverRunning)

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = actions.onCheckEnvironment, enabled = !state.busy) {
                    Text("检测环境")
                }
                OutlinedButton(onClick = actions.onStartServer, enabled = !state.busy) {
                    Text("启动 Server")
                }
                OutlinedButton(onClick = actions.onKillServer, enabled = !state.busy) {
                    Text("停止 Server")
                }
                OutlinedButton(onClick = actions.onRestartServer, enabled = !state.busy) {
                    Text("重启 Server")
                }
                OutlinedButton(onClick = actions.onRefreshDevices, enabled = !state.busy) {
                    Text("刷新设备")
                }
            }

            Spacer(Modifier.height(12.dp))
            WirelessConnectRow(enabled = !state.busy, onConnect = actions.onConnect, onDisconnect = actions.onDisconnect)

            Spacer(Modifier.height(12.dp))
            CustomCommandRow(enabled = !state.busy, onRun = actions.onRunCustom)

            Spacer(Modifier.height(12.dp))
            DeviceList(state.devices)

            state.lastMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun EnvironmentRow(environment: AdbEnvironmentState, serverRunning: Boolean?) {
    val text = when (environment) {
        AdbEnvironmentState.Checking -> "环境: 检测中…"
        is AdbEnvironmentState.Downloading -> "环境: 下载中…"
        is AdbEnvironmentState.Ready -> buildString {
            append("环境: 就绪 · 来源=${environment.source}")
            environment.version?.let { append(" · 版本=$it") }
            append("\n路径: ${environment.path}")
        }
        is AdbEnvironmentState.Missing -> "环境: 未找到 adb (${environment.resolution::class.simpleName})"
    }
    Column {
        Text(text, style = MaterialTheme.typography.bodyMedium)
        val serverText = when (serverRunning) {
            true -> "Server: 运行中"
            false -> "Server: 已停止"
            null -> "Server: 未知"
        }
        Text(serverText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WirelessConnectRow(
    enabled: Boolean,
    onConnect: (String, Int) -> Unit,
    onDisconnect: (String, Int) -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5555") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("设备 IP") },
            singleLine = true,
            modifier = Modifier.width(180.dp),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = port,
            onValueChange = { port = it.filter(Char::isDigit) },
            label = { Text("端口") },
            singleLine = true,
            modifier = Modifier.width(100.dp),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            enabled = enabled && host.isNotBlank(),
            onClick = { onConnect(host.trim(), port.toIntOrNull() ?: 5555) },
        ) { Text("连接") }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            enabled = enabled && host.isNotBlank(),
            onClick = { onDisconnect(host.trim(), port.toIntOrNull() ?: 5555) },
        ) { Text("断开") }
    }
}

@Composable
private fun CustomCommandRow(enabled: Boolean, onRun: (String) -> Unit) {
    var command by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("自定义 adb 参数, 如: shell getprop ro.product.model") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            enabled = enabled && command.isNotBlank(),
            onClick = { onRun(command) },
        ) { Text("执行") }
    }
}

@Composable
private fun DeviceList(devices: List<AdbDevice>) {
    Column {
        Text("设备列表 (${devices.size})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        if (devices.isEmpty()) {
            Text("无已连接设备", style = MaterialTheme.typography.bodySmall)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                devices.forEach { device -> DeviceRow(device) }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: AdbDevice) {
    val detail = buildString {
        append(device.serial)
        append("  [${device.state}]")
        device.model?.let { append("  model=$it") }
        device.transport?.let { append("  transport=$it") }
    }
    Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

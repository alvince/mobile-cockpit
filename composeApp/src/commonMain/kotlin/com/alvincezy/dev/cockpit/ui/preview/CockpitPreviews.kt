package com.alvincezy.dev.cockpit.ui.preview

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.alvincezy.dev.cockpit.app.AppScaffold
import com.alvincezy.dev.cockpit.app.theme.AppTheme
import com.alvincezy.dev.cockpit.core.adb.AdbDevice
import com.alvincezy.dev.cockpit.core.adb.AdbEnvironmentState
import com.alvincezy.dev.cockpit.core.adb.AdbResolution
import com.alvincezy.dev.cockpit.core.adb.AdbSource
import com.alvincezy.dev.cockpit.adbops.AdbOperationsActions
import com.alvincezy.dev.cockpit.adbops.AdbOperationsContent
import com.alvincezy.dev.cockpit.adbops.AdbOperationsState

private val sampleDevices = listOf(
    AdbDevice(serial = "emulator-5554", state = "device", model = "Pixel_6", transport = "usb"),
    AdbDevice(serial = "192.168.1.10:5555", state = "device", model = "Pixel_5", transport = "192.168.1.10:5555"),
    AdbDevice(serial = "BADDEVICE", state = "unauthorized"),
)

@Preview
@Composable
fun AdbOperationsContentReadyPreview() {
    AppTheme {
        AdbOperationsContent(
            state = AdbOperationsState(
                environment = AdbEnvironmentState.Ready(
                    path = "/Users/Shared/Library/Android/sdk/platform-tools/adb",
                    source = AdbSource.AndroidSdk,
                ),
                serverRunning = true,
                devices = sampleDevices,
                lastMessage = "检测到 3 台设备",
            ),
            actions = AdbOperationsActions(),
        )
    }
}

@Preview
@Composable
fun AdbOperationsContentBusyPreview() {
    AppTheme {
        AdbOperationsContent(
            state = AdbOperationsState(
                environment = AdbEnvironmentState.Checking,
                serverRunning = null,
                busy = true,
                lastMessage = "检测 adb 环境…",
            ),
            actions = AdbOperationsActions(),
        )
    }
}

@Preview
@Composable
fun AdbOperationsContentMissingPreview() {
    AppTheme {
        AdbOperationsContent(
            state = AdbOperationsState(
                environment = AdbEnvironmentState.Missing(
                    AdbResolution.Missing.NoSystemAdb(searchedPaths = listOf("/usr/bin/adb")),
                ),
                serverRunning = false,
                lastMessage = "未找到 adb: NoSystemAdb",
            ),
            actions = AdbOperationsActions(),
        )
    }
}

@Preview
@Composable
fun AppScaffoldPreview() {
    AppScaffold { modifier ->
        AdbOperationsContent(
            state = AdbOperationsState(
                environment = AdbEnvironmentState.Ready(
                    path = "/opt/platform-tools/adb",
                    source = AdbSource.Cached,
                    version = "35.0.1",
                ),
                serverRunning = true,
                devices = sampleDevices.take(1),
            ),
            actions = AdbOperationsActions(),
            modifier = modifier,
        )
    }
}

package com.alvincezy.dev.cockpit.adbops

import com.alvincezy.dev.cockpit.core.adb.AdbDevice
import com.alvincezy.dev.cockpit.core.adb.AdbEnvironmentState

/**
 * Immutable UI state for the ADB operations area.
 */
data class AdbOperationsState(
    val environment: AdbEnvironmentState = AdbEnvironmentState.Checking,
    val serverRunning: Boolean? = null,
    val devices: List<AdbDevice> = emptyList(),
    val busy: Boolean = false,
    val lastMessage: String? = null,
)

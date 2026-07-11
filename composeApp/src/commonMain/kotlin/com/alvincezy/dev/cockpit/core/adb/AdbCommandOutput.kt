package com.alvincezy.dev.cockpit.core.adb

data class AdbCommandOutput(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

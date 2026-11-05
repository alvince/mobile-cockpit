package com.alvincezy.dev.cockpit.core.adb

enum class AdbPlatform(
    val identifier: String,
    val osName: String,
    val executableName: String,
) {
    MACOS("macos", "darwin", "adb"),
    WINDOWS_X64("windows-x64", "windows", "adb.exe"),
    LINUX_X64("linux-x64", "linux", "adb");
}

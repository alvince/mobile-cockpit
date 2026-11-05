package com.alvincezy.dev.cockpit.core.di

import com.alvincezy.dev.cockpit.core.adb.AdbCommandExecutor
import com.alvincezy.dev.cockpit.core.adb.AdbConfigStore
import com.alvincezy.dev.cockpit.core.adb.AdbDeviceDetector
import com.alvincezy.dev.cockpit.core.adb.AdbPathResolver
import com.alvincezy.dev.cockpit.core.adb.AdbServerManager
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbCommandExecutor
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbConfigStore
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbDeviceDetector
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbPathResolver
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbServerManager
import com.alvincezy.dev.cockpit.core.window.DesktopWindowStateStore
import com.alvincezy.dev.cockpit.core.window.WindowStateStore
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop ADB capability bindings. Registers the platform-backed ADB services
 * (config store, path resolver, executor, server, detector) plus their infra
 * dependency (FileSystem).
 */
actual fun platformModule(): Module = module {
    single<FileSystem> { FileSystem.SYSTEM }

    single<AdbConfigStore> { DesktopAdbConfigStore(get()) }
    single<WindowStateStore> { DesktopWindowStateStore(get()) }
    single<AdbPathResolver> { DesktopAdbPathResolver(get(), get()) }
    single<AdbCommandExecutor> { DesktopAdbCommandExecutor(get()) }
    single<AdbServerManager> { DesktopAdbServerManager(get()) }
    single<AdbDeviceDetector> { DesktopAdbDeviceDetector(get()) }
}

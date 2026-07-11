package com.alvincezy.dev.cockpit.core.di

import com.alvincezy.dev.cockpit.core.adb.AdbCacheManager
import com.alvincezy.dev.cockpit.core.adb.AdbCommandExecutor
import com.alvincezy.dev.cockpit.core.adb.AdbConfigStore
import com.alvincezy.dev.cockpit.core.adb.AdbDeviceDetector
import com.alvincezy.dev.cockpit.core.adb.AdbDownloader
import com.alvincezy.dev.cockpit.core.adb.AdbPathResolver
import com.alvincezy.dev.cockpit.core.adb.AdbServerManager
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbCacheManager
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbCommandExecutor
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbConfigStore
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbDeviceDetector
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbDownloader
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbPathResolver
import com.alvincezy.dev.cockpit.core.adb.DesktopAdbServerManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop ADB capability bindings. Registers the platform-backed ADB services
 * (config store, path resolver, downloader, cache, executor, server, detector)
 * plus their infra dependencies (FileSystem, HttpClient).
 */
actual fun platformModule(): Module = module {
    single<FileSystem> { FileSystem.SYSTEM }
    single<HttpClient> { HttpClient(Java) }

    single<AdbConfigStore> { DesktopAdbConfigStore(get()) }
    single<AdbCacheManager> { DesktopAdbCacheManager(get()) }
    single<AdbDownloader> { DesktopAdbDownloader(get(), get(), get()) }
    single<AdbPathResolver> { DesktopAdbPathResolver(get(), get(), get(), get()) }
    single<AdbCommandExecutor> { DesktopAdbCommandExecutor(get()) }
    single<AdbServerManager> { DesktopAdbServerManager(get()) }
    single<AdbDeviceDetector> { DesktopAdbDeviceDetector(get()) }
}

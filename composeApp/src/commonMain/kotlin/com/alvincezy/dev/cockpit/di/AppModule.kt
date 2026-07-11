package com.alvincezy.dev.cockpit.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin DI module for the Cockpit desktop application.
 *
 * Future services to register here:
 * - AdbClient / AdbCommandExecutor
 * - ScrcpyController
 * - DeviceConnectionManager
 * - SdkBridge (network/log/database/SP/MMKV/sandbox)
 */
val appModule = module {
    // Example placeholder service
    singleOf(::ApplicationInfoProvider)
}

/**
 * Simple placeholder providing basic application metadata.
 * Replace/extend with real services as the project evolves.
 */
class ApplicationInfoProvider {
    val name: String = "Cockpit"
    val version: String = "0.1.0-SNAPSHOT"
}

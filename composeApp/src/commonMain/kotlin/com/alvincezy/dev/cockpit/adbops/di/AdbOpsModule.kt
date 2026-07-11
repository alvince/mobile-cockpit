package com.alvincezy.dev.cockpit.adbops.di

import com.alvincezy.dev.cockpit.adbops.AdbOperationsController
import org.koin.dsl.module

/**
 * Koin module for the ADB operations feature (`adbops`). Provides the UI-facing
 * [AdbOperationsController], which depends on the ADB capability services bound
 * by the platform module (see `core.adb.di`).
 */
val adbOpsModule = module {
    factory { AdbOperationsController(get(), get(), get(), get()) }
}

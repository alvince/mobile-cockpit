package com.alvincezy.dev.cockpit.core.di

import com.alvincezy.dev.cockpit.adbops.di.adbOpsModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Platform-specific Koin module. The desktop actual registers the ADB capability
 * services (config store, path resolver, downloader, cache, executor, server, detector).
 */
expect fun platformModule(): Module

/**
 * Root Koin module for the Cockpit desktop application. Aggregates feature and
 * core-capability modules. `platformModule()` (the ADB capability bindings) is
 * added separately at startup in `Main.kt`.
 */
val appModule = module {
    singleOf(::ApplicationInfoProvider)

    includes(adbOpsModule)
}

/**
 * Simple placeholder providing basic application metadata.
 */
class ApplicationInfoProvider {
    val name: String = "Cockpit"
    val version: String = "0.1.0-SNAPSHOT"
}

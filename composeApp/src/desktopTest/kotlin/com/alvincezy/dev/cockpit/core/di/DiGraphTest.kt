package com.alvincezy.dev.cockpit.core.di

import com.alvincezy.dev.cockpit.adbops.AdbOperationsController
import com.alvincezy.dev.cockpit.core.window.WindowStateStore
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class DiGraphTest {

    @AfterTest
    fun tearDown() {
        runCatching { stopKoin() }
    }

    @Test
    fun resolvesAdbOperationsController() {
        val koin = startKoin {
            modules(appModule, platformModule())
        }.koin

        val controller = koin.get<AdbOperationsController>()
        assertNotNull(controller)
        controller.dispose()
    }

    @Test
    fun resolvesWindowStateStore() {
        val koin = startKoin {
            modules(appModule, platformModule())
        }.koin

        assertNotNull(koin.get<WindowStateStore>())
    }
}

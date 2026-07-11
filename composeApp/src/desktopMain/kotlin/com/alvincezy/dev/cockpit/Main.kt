package com.alvincezy.dev.cockpit

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alvincezy.dev.cockpit.app.App
import com.alvincezy.dev.cockpit.core.di.appModule
import com.alvincezy.dev.cockpit.core.di.platformModule
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule, platformModule())
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Cockpit",
        ) {
            App()
        }
    }
}

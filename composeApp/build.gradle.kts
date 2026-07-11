plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.okio)

                // Koin DI
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.java)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(libs.okio.fakefilesystem)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.alvincezy.dev.cockpit.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
            )
            packageName = "Cockpit"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icon.icns").takeIf { it.exists() })
                bundleID = "com.alvincezy.dev.cockpit"
                jvmArgs("-Dapple.awt.application.appearance=system")
            }

            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico").takeIf { it.exists() })
                menuGroup = "Cockpit"
                upgradeUuid = "b4f4c6e8-7c1d-4f3a-9b2e-5d8a7c6e9f01"
            }
        }
    }
}

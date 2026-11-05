import java.io.File

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
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(libs.okio.fakefilesystem)
            }
        }
    }
}

// --- Bundled adb -------------------------------------------------------------------
// The adb executable is staged from the *build machine* into appResources/<os>/adb
// at packaging time; it is never committed to the repository (.gitignore excludes
// appResources/<os>/). Only the license text under appResources/common/ is tracked.
// Compose copies appResourcesRootDir/{common,<os>,<os>-<arch>} into the app package.

val buildOs = System.getProperty("os.name").orEmpty()
val composeOsId = when {
    buildOs.startsWith("Mac", ignoreCase = true) -> "macos"
    buildOs.startsWith("Win", ignoreCase = true) -> "windows"
    else -> "linux"
}
val isWindowsBuild = composeOsId == "windows"
val hostAdbExecutable = if (isWindowsBuild) "adb.exe" else "adb"
val hostAdbRequiredFiles =
    if (isWindowsBuild) listOf("adb.exe", "AdbWinApi.dll", "AdbWinUsbApi.dll") else listOf("adb")

val bundledAdbDir = layout.projectDirectory.dir("appResources/$composeOsId/adb")

val prepareBundledAdb = tasks.register("prepareBundledAdb") {
    group = "compose desktop"
    description = "Stages adb from the build machine into appResources/<os>/adb for packaging."

    val adbPathProperty = providers.gradleProperty("adbPath")
    val cockpitAdbEnv = providers.environmentVariable("COCKPIT_ADB_PATH")
    val androidHomeEnv = providers.environmentVariable("ANDROID_HOME")
    val androidSdkRootEnv = providers.environmentVariable("ANDROID_SDK_ROOT")
    val pathEnv = providers.environmentVariable("PATH")

    val outputDir = bundledAdbDir.asFile
    val requiredFiles = hostAdbRequiredFiles
    val executableName = hostAdbExecutable
    val setExecutableBit = !isWindowsBuild

    // Always restage so a changed -PadbPath takes effect; the output dir is still
    // declared so configuration cache and task ordering work.
    outputs.dir(outputDir)
    outputs.upToDateWhen { false }

    doLast {
        fun executableIn(dir: File): File? = File(dir, executableName).takeIf { it.isFile }

        val candidates = buildList {
            adbPathProperty.orNull?.takeIf { it.isNotBlank() }?.let { add(File(it)) }
            cockpitAdbEnv.orNull?.takeIf { it.isNotBlank() }?.let { add(File(it)) }
            listOfNotNull(androidHomeEnv.orNull, androidSdkRootEnv.orNull)
                .filter { it.isNotBlank() }
                .forEach { add(File(File(it, "platform-tools"), executableName)) }
            pathEnv.orNull?.split(File.pathSeparator).orEmpty()
                .filter { it.isNotBlank() }
                .forEach { add(File(it, executableName)) }
        }

        // Accept either the executable itself or a directory containing it.
        val sourceDir = candidates.asSequence()
            .mapNotNull { candidate ->
                when {
                    candidate.isDirectory -> candidate.takeIf { executableIn(it) != null }
                    candidate.isFile -> candidate.parentFile?.takeIf { executableIn(it) != null }
                    else -> null
                }
            }
            .firstOrNull()

        if (sourceDir == null) {
            throw GradleException(
                "prepareBundledAdb: could not find '$executableName' on the build machine. " +
                    "Provide it via one of:\n" +
                    "  -PadbPath=/path/to/$executableName (or its directory)\n" +
                    "  COCKPIT_ADB_PATH=/path/to/$executableName\n" +
                    "  ANDROID_HOME / ANDROID_SDK_ROOT pointing at an Android SDK\n" +
                    "  $executableName available on PATH",
            )
        }

        val missing = requiredFiles.filter { !File(sourceDir, it).isFile }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "prepareBundledAdb: '$executableName' in $sourceDir is missing required " +
                    "sibling file(s): ${missing.joinToString()}. Windows adb needs " +
                    "AdbWinApi.dll and AdbWinUsbApi.dll next to adb.exe.",
            )
        }

        if (outputDir.exists()) outputDir.deleteRecursively()
        outputDir.mkdirs()
        requiredFiles.forEach { name ->
            File(sourceDir, name).copyTo(File(outputDir, name), overwrite = true)
        }

        val adb = File(outputDir, executableName)
        if (setExecutableBit && !adb.setExecutable(true, false)) {
            logger.warn("prepareBundledAdb: failed to set the executable bit on ${adb.absolutePath}")
        }

        runCatching {
            val process = ProcessBuilder(adb.absolutePath, "version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
        }.getOrNull()?.let { version ->
            File(outputDir, "version.txt").writeText("$version\n")
        }

        logger.lifecycle("prepareBundledAdb: staged $requiredFiles from $sourceDir -> $outputDir")
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

            // Ships appResources/{common,<os>}/ into the app package; the bundled
            // adb is located at runtime via `compose.application.resources.dir`.
            appResourcesRootDir.set(layout.projectDirectory.dir("appResources"))

            // JDK modules required at runtime by the jlink-trimmed image.
            // List generated via `./gradlew :composeApp:suggestRuntimeModules`.
            modules("java.instrument", "java.management", "jdk.unsupported")

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

// Ordering only: dev `run` must not require adb on the build machine.
tasks.matching { it.name == "prepareAppResources" }.configureEach {
    mustRunAfter(prepareBundledAdb)
}
// Packaging requires adb: depend on staging so a missing adb fails the build clearly.
tasks.matching {
    it.name.startsWith("package") || it.name == "createDistributable"
}.configureEach {
    dependsOn(prepareBundledAdb)
}

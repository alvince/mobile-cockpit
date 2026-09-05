package com.alvincezy.dev.cockpit.core.adb

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.File

/**
 * Resolves an adb executable following the priority chain defined in the plan:
 *
 * 1. `adb.path` in `~/.cockpit/config.properties`
 * 2. `COCKPIT_ADB_PATH`
 * 3. `ANDROID_HOME` / `ANDROID_SDK_ROOT` -> `platform-tools/adb`
 * 4. system `PATH`
 * 5. cached download
 * 6. trigger a fresh download
 *
 * An invalid entry at any level is skipped and resolution continues to the next.
 */
class DesktopAdbPathResolver(
    private val configStore: AdbConfigStore,
    private val cacheManager: AdbCacheManager,
    private val downloader: AdbDownloader,
    private val fileSystem: FileSystem,
    private val manifest: PlatformToolsManifest = PlatformToolsManifest.load(),
    private val platform: AdbPlatform = DesktopPaths.currentPlatform() ?: AdbPlatform.LINUX_X64,
    private val env: (String) -> String? = System::getenv,
    private val pathEntries: () -> List<String> = {
        System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
    },
) : AdbPathResolver {

    override suspend fun resolve(): AdbResolution {
        val searched = mutableListOf<String>()

        findSystemAdb(searched)?.let { return it }

        cacheManager.cachedAdb(manifest.version)?.let {
            return AdbResolution.Cached(it, manifest.version)
        }

        return AdbResolution.Missing.NoSystemAdb(searched)
    }

    override fun resolveOrDownload(): Flow<AdbResolution> = flow {
        when (val resolution = resolve()) {
            is AdbResolution.System, is AdbResolution.Cached, is AdbResolution.Downloading ->
                emit(resolution)

            is AdbResolution.Missing -> {
                val config = configStore.load()
                if (!config.autoDownload) {
                    emit(AdbResolution.Missing.DisabledByUser("autoDownload is disabled"))
                    return@flow
                }
                emit(AdbResolution.Downloading(null))

                val version = manifest.version
                val archive = DesktopPaths.toolsDir / platform.zipName(version)
                var failure: Throwable? = null

                downloader.download(version, platform, archive).collect { progress ->
                    when (progress) {
                        is DownloadProgress.Started -> emit(AdbResolution.Downloading(0f))
                        is DownloadProgress.InProgress -> {
                            val fraction = if (progress.totalBytes > 0) {
                                progress.bytesRead.toFloat() / progress.totalBytes
                            } else {
                                null
                            }
                            emit(AdbResolution.Downloading(fraction))
                        }

                        is DownloadProgress.Failed -> failure = progress.error
                        is DownloadProgress.Done -> Unit
                    }
                }

                if (failure != null) {
                    emit(AdbResolution.Missing.DownloadFailed(failure!!))
                    return@flow
                }

                val installed = runCatching {
                    cacheManager.install(archive, version, manifest.sha256For(platform))
                }.getOrElse {
                    emit(AdbResolution.Missing.VerificationFailed(archive))
                    return@flow
                }
                runCatching { if (fileSystem.exists(archive)) fileSystem.delete(archive) }
                cacheManager.trimCache(config.maxCacheVersions)

                if (cacheManager.validateCache(version)) {
                    emit(AdbResolution.Cached(installed, version))
                } else {
                    emit(AdbResolution.Missing.CacheCorrupted(installed))
                }
            }
        }
    }

    private fun findSystemAdb(searched: MutableList<String>): AdbResolution.System? {
        // 1. user config
        configStore.load().adbPath?.let { configured ->
            searched += configured
            validExecutable(configured)?.let {
                return AdbResolution.System(it, AdbSource.UserConfig)
            }
        }

        // 2. COCKPIT_ADB_PATH
        env("COCKPIT_ADB_PATH")?.let { custom ->
            searched += custom
            validExecutable(custom)?.let {
                return AdbResolution.System(it, AdbSource.EnvironmentVariable)
            }
        }

        // 3. ANDROID_HOME / ANDROID_SDK_ROOT
        for (key in listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
            env(key)?.let { sdk ->
                val candidate = "$sdk${File.separator}platform-tools${File.separator}${platform.executableName}"
                searched += candidate
                validExecutable(candidate)?.let {
                    return AdbResolution.System(it, AdbSource.AndroidSdk)
                }
            }
        }

        // 4. PATH
        for (dir in pathEntries()) {
            if (dir.isBlank()) continue
            val candidate = "$dir${File.separator}${platform.executableName}"
            validExecutable(candidate)?.let {
                return AdbResolution.System(it, AdbSource.SystemPath)
            }
        }

        return null
    }

    private fun validExecutable(path: String): Path? {
        val file = File(path)
        return if (file.isFile && file.canExecute()) path.toPath() else null
    }
}

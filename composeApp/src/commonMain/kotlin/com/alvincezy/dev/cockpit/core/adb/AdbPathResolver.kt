package com.alvincezy.dev.cockpit.core.adb

import kotlinx.coroutines.flow.Flow

/**
 * Resolves a usable adb executable following the priority chain:
 * user config -> COCKPIT_ADB_PATH -> ANDROID_HOME/ANDROID_SDK_ROOT -> PATH -> cache -> download.
 */
interface AdbPathResolver {
    /**
     * Resolves adb without triggering a download. Returns a terminal [AdbResolution]
     * ([AdbResolution.System], [AdbResolution.Cached] or [AdbResolution.Missing]).
     */
    suspend fun resolve(): AdbResolution

    /**
     * Resolves adb, downloading platform-tools on demand when nothing is found and
     * auto-download is enabled. Emits intermediate [AdbResolution.Downloading] states.
     */
    fun resolveOrDownload(): Flow<AdbResolution>
}

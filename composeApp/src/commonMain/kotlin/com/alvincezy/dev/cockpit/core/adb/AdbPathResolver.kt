package com.alvincezy.dev.cockpit.core.adb

/**
 * Resolves a usable adb executable following the priority chain:
 * user config -> COCKPIT_ADB_PATH -> ANDROID_HOME/ANDROID_SDK_ROOT -> PATH -> bundled adb.
 */
interface AdbPathResolver {
    /**
     * Resolves adb. Returns a terminal [AdbResolution]
     * ([AdbResolution.System], [AdbResolution.Bundled] or [AdbResolution.Missing]).
     */
    suspend fun resolve(): AdbResolution
}

package com.alvincezy.dev.cockpit.core.adb

import okio.Path

/**
 * Manages the on-disk cache of downloaded platform-tools under
 * `~/.cockpit/tools/platform-tools/<version>/`.
 */
interface AdbCacheManager {
    /** Returns the adb executable path for [version] if it is cached and valid, else null. */
    fun cachedAdb(version: String): Path?

    /**
     * Atomically installs a downloaded platform-tools archive into the versioned cache
     * directory and returns the resulting adb executable path.
     */
    fun install(archive: Path, version: String, expectedSha256: String?): Path

    /** Verifies that the cached version's files exist and are executable. */
    fun validateCache(version: String): Boolean

    /** Removes old cached versions, keeping at most [keep] most-recent versions. */
    fun trimCache(keep: Int)
}

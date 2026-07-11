package com.alvincezy.dev.cockpit.core.adb

import kotlinx.coroutines.flow.Flow
import okio.Path

/**
 * Downloads the official platform-tools archive for the current platform.
 */
interface AdbDownloader {
    /**
     * Downloads the platform-tools zip for [version] on [platform] to [destination],
     * emitting progress. The flow completes with [DownloadProgress.Done] on success or
     * [DownloadProgress.Failed] on error. Cancelling the collector aborts the download,
     * removes the partial file and releases any lock.
     */
    fun download(
        version: String,
        platform: AdbPlatform,
        destination: Path,
    ): Flow<DownloadProgress>
}

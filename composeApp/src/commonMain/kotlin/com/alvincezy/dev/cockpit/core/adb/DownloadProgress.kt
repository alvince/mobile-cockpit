package com.alvincezy.dev.cockpit.core.adb

import okio.Path

sealed class DownloadProgress {
    data class Started(val url: String) : DownloadProgress()
    data class InProgress(val bytesRead: Long, val totalBytes: Long) : DownloadProgress()
    data class Done(val path: Path) : DownloadProgress()
    data class Failed(val error: Throwable) : DownloadProgress()
}

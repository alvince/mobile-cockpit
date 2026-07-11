package com.alvincezy.dev.cockpit.core.adb

import okio.Path

sealed class AdbResolution {
    data class System(val path: Path, val source: AdbSource) : AdbResolution()
    data class Cached(val path: Path, val version: String) : AdbResolution()
    data class Downloading(val progress: Float?) : AdbResolution()
    sealed class Missing : AdbResolution() {
        data class NoSystemAdb(val searchedPaths: List<String>) : Missing()
        data class DownloadFailed(val cause: Throwable) : Missing()
        data class VerificationFailed(val path: Path) : Missing()
        data class CacheCorrupted(val path: Path) : Missing()
        data class DisabledByUser(val message: String) : Missing()
        data class PermissionDenied(val path: String) : Missing()
        data class Unknown(val reason: String) : Missing()
    }
}

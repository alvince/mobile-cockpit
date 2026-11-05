package com.alvincezy.dev.cockpit.core.adb

import okio.Path

sealed class AdbResolution {
    /** An adb executable found on the user's system or configured path. */
    data class System(val path: Path, val source: AdbSource) : AdbResolution()

    /** The adb shipped inside the application package. */
    data class Bundled(val path: Path, val version: String? = null) : AdbResolution()

    sealed class Missing : AdbResolution() {
        data class NoSystemAdb(val searchedPaths: List<String>) : Missing()
        data class PermissionDenied(val path: String) : Missing()
        data class Unknown(val reason: String) : Missing()
    }
}

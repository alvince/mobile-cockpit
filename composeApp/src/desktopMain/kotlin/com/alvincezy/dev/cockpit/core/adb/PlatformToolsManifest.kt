package com.alvincezy.dev.cockpit.core.adb

/**
 * The pinned platform-tools version and its per-OS SHA-256 checksums, loaded from
 * `platform-tools-manifest.json` on the classpath.
 */
data class PlatformToolsManifest(
    val version: String,
    val sha256: Map<String, String>,
) {
    /** Returns the checksum for [platform], or null if it is a placeholder / missing. */
    fun sha256For(platform: AdbPlatform): String? =
        sha256[platform.osName]?.takeIf { it.isNotBlank() && it != "PLACEHOLDER" }

    companion object {
        private const val RESOURCE = "/platform-tools-manifest.json"

        fun load(): PlatformToolsManifest {
            val text = PlatformToolsManifest::class.java.getResourceAsStream(RESOURCE)
                ?.bufferedReader()?.use { it.readText() }
                ?: error("Missing resource $RESOURCE")
            return parse(text)
        }

        /** Minimal parser for the fixed manifest shape (avoids a JSON dependency). */
        fun parse(json: String): PlatformToolsManifest {
            val version = STRING_VALUE.find(json.substringAfter("\"version\""))?.groupValues?.get(1)
                ?.takeIf { it.isNotBlank() }
                ?: error("manifest missing version")

            val shaBlock = json.substringAfter("\"sha256\"").substringAfter('{').substringBefore('}')
            val sha = ENTRY.findAll(shaBlock).associate { m ->
                m.groupValues[1] to m.groupValues[2]
            }
            return PlatformToolsManifest(version, sha)
        }

        private val STRING_VALUE = Regex(":\\s*\"([^\"]*)\"")
        private val ENTRY = Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"")
    }
}

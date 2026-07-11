package com.alvincezy.dev.cockpit.core.adb

/**
 * Reads and writes the persisted [AdbConfig].
 */
interface AdbConfigStore {
    fun load(): AdbConfig
    fun save(config: AdbConfig)
    fun update(transform: (AdbConfig) -> AdbConfig): AdbConfig
}

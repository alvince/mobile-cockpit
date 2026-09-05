package com.alvincezy.dev.cockpit.core.window

/**
 * Persists the main window's [WindowLayout] across launches. Mirrors the shape of
 * `AdbConfigStore`: synchronous load/save plus a read-modify-write [update].
 */
interface WindowStateStore {
    fun load(): WindowLayout
    fun save(layout: WindowLayout)
    fun update(transform: (WindowLayout) -> WindowLayout): WindowLayout
}

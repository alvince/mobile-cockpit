package com.alvincezy.dev.cockpit.core.window

import com.alvincezy.dev.cockpit.core.adb.DesktopPaths
import okio.FileSystem
import okio.Path

/**
 * Persists [WindowLayout] to a Java-properties style file using Okio, mirroring
 * `DesktopAdbConfigStore`. Format is simple `key=value` lines so it stays readable
 * and testable with a [okio.fakefilesystem.FakeFileSystem].
 *
 * Position lines (`window.x` / `window.y`) are only written when non-null, so a
 * never-saved position loads back as `null`.
 */
class DesktopWindowStateStore(
    private val fileSystem: FileSystem,
    private val stateFile: Path = DesktopPaths.windowStateFile,
) : WindowStateStore {

    override fun load(): WindowLayout {
        if (!fileSystem.exists(stateFile)) return WindowLayout()
        val props = runCatching {
            fileSystem.read(stateFile) { readUtf8() }.let(::parse)
        }.getOrElse { return WindowLayout() }

        val defaults = WindowLayout()
        return WindowLayout(
            width = props[KEY_WIDTH]?.toFloatOrNull() ?: defaults.width,
            height = props[KEY_HEIGHT]?.toFloatOrNull() ?: defaults.height,
            x = props[KEY_X]?.toFloatOrNull(),
            y = props[KEY_Y]?.toFloatOrNull(),
            maximized = props[KEY_MAXIMIZED]?.toBooleanStrictOrNull() ?: defaults.maximized,
            rememberSize = props[KEY_REMEMBER_SIZE]?.toBooleanStrictOrNull() ?: defaults.rememberSize,
            rememberPosition = props[KEY_REMEMBER_POSITION]?.toBooleanStrictOrNull() ?: defaults.rememberPosition,
            rememberMaximized = props[KEY_REMEMBER_MAXIMIZED]?.toBooleanStrictOrNull() ?: defaults.rememberMaximized,
        )
    }

    override fun save(layout: WindowLayout) {
        stateFile.parent?.let { fileSystem.createDirectories(it) }
        val text = buildString {
            appendLine("$KEY_WIDTH=${layout.width}")
            appendLine("$KEY_HEIGHT=${layout.height}")
            layout.x?.let { appendLine("$KEY_X=$it") }
            layout.y?.let { appendLine("$KEY_Y=$it") }
            appendLine("$KEY_MAXIMIZED=${layout.maximized}")
            appendLine("$KEY_REMEMBER_SIZE=${layout.rememberSize}")
            appendLine("$KEY_REMEMBER_POSITION=${layout.rememberPosition}")
            appendLine("$KEY_REMEMBER_MAXIMIZED=${layout.rememberMaximized}")
        }
        fileSystem.write(stateFile) { writeUtf8(text) }
    }

    override fun update(transform: (WindowLayout) -> WindowLayout): WindowLayout {
        val updated = transform(load())
        save(updated)
        return updated
    }

    private fun parse(text: String): Map<String, String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
            .toMap()

    private companion object {
        const val KEY_WIDTH = "window.width"
        const val KEY_HEIGHT = "window.height"
        const val KEY_X = "window.x"
        const val KEY_Y = "window.y"
        const val KEY_MAXIMIZED = "window.maximized"
        const val KEY_REMEMBER_SIZE = "remember.size"
        const val KEY_REMEMBER_POSITION = "remember.position"
        const val KEY_REMEMBER_MAXIMIZED = "remember.maximized"
    }
}

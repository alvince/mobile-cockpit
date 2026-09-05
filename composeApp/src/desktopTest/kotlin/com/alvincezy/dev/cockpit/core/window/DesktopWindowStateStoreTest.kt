package com.alvincezy.dev.cockpit.core.window

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopWindowStateStoreTest {

    private val statePath = "/home/user/.cockpit/window.properties".toPath()

    private fun store(fs: FakeFileSystem) = DesktopWindowStateStore(fs, statePath)

    @Test
    fun returnsDefaultsWhenMissing() {
        val fs = FakeFileSystem()
        val layout = store(fs).load()

        assertEquals(WindowLayout(), layout)
        assertNull(layout.x)
        assertNull(layout.y)
        assertTrue(layout.rememberSize && layout.rememberPosition && layout.rememberMaximized)
    }

    @Test
    fun savesAndReloadsRoundTrip() {
        val fs = FakeFileSystem()
        val s = store(fs)
        val original = WindowLayout(
            width = 1280f,
            height = 820f,
            x = 100f,
            y = 60f,
            maximized = true,
            rememberSize = true,
            rememberPosition = false,
            rememberMaximized = true,
        )
        s.save(original)

        assertEquals(original, s.load())
    }

    @Test
    fun updateAppliesTransform() {
        val fs = FakeFileSystem()
        val s = store(fs)
        s.save(WindowLayout())

        val updated = s.update { it.copy(rememberSize = false, width = 900f) }

        assertEquals(false, updated.rememberSize)
        assertEquals(900f, updated.width)
        assertEquals(false, s.load().rememberSize)
    }

    @Test
    fun ignoresMalformedLines() {
        val fs = FakeFileSystem()
        fs.createDirectories(statePath.parent!!)
        fs.write(statePath) {
            writeUtf8(
                """
                # comment
                garbage-without-equals
                remember.size=false
                window.width=notanumber
                """.trimIndent()
            )
        }

        val layout = store(fs).load()
        assertEquals(false, layout.rememberSize)
        // Falls back to default when unparsable.
        assertEquals(WindowLayout().width, layout.width)
    }

    @Test
    fun positionOmittedWhenNull() {
        val fs = FakeFileSystem()
        val s = store(fs)
        s.save(WindowLayout(x = null, y = null))

        val text = fs.read(statePath) { readUtf8() }
        assertTrue("window.x" !in text)
        assertTrue("window.y" !in text)

        val reloaded = s.load()
        assertNull(reloaded.x)
        assertNull(reloaded.y)
    }
}

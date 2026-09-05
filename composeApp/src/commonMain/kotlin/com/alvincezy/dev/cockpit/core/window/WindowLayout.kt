package com.alvincezy.dev.cockpit.core.window

/**
 * Persisted main-window geometry plus the user's "remember" preferences.
 *
 * All geometry is stored in dp (matching Compose's [androidx.compose.ui.unit.DpSize] /
 * [androidx.compose.ui.window.WindowPosition]). [x] / [y] are nullable so that a
 * never-saved position round-trips as `null` (→ platform-default placement) rather
 * than a bogus `0,0`.
 *
 * The three `remember*` flags default to `true`: out of the box the window remembers
 * its size, position and maximized state. Turning one off stops future persistence of
 * that dimension and falls back to a default on next launch (it does not erase the
 * already-stored value).
 */
data class WindowLayout(
    val width: Float = 1000f,
    val height: Float = 700f,
    val x: Float? = null,
    val y: Float? = null,
    val maximized: Boolean = false,
    val rememberSize: Boolean = true,
    val rememberPosition: Boolean = true,
    val rememberMaximized: Boolean = true,
)

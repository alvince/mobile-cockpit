package com.alvincezy.dev.cockpit

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.alvincezy.dev.cockpit.app.App
import com.alvincezy.dev.cockpit.app.theme.AppTheme
import com.alvincezy.dev.cockpit.core.di.appModule
import com.alvincezy.dev.cockpit.core.di.platformModule
import com.alvincezy.dev.cockpit.core.window.WindowLayout
import com.alvincezy.dev.cockpit.core.window.WindowStateStore
import com.alvincezy.dev.cockpit.preferences.PreferencesPanel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.updateAndGet
import org.jetbrains.skiko.hostOs
import org.koin.core.context.startKoin
import java.awt.Desktop
import java.awt.GraphicsEnvironment

/**
 * Quiet period the window geometry must hold before it is persisted: long enough
 * to coalesce the transient values emitted during a resize/move gesture, short
 * enough that the final geometry is saved promptly after the user lets go.
 */
private const val DEBOUNCE_MS = 600L

@OptIn(FlowPreview::class)
fun main() {
    val koin = startKoin {
        modules(appModule, platformModule())
    }.koin
    val store = koin.get<WindowStateStore>()
    val initial = store.load()

    application {
        // Single source of truth shared by the persistence collector and the
        // Preferences UI. Reading cur.remember* on every emission means toggle
        // changes take effect immediately without re-reading disk.
        val prefs = remember { MutableStateFlow(initial) }

        val windowState = rememberWindowState(
            size = if (initial.rememberSize) DpSize(initial.width.dp, initial.height.dp)
            else DpSize(1000.dp, 700.dp),
            position = restoredPosition(initial),
            placement = if (initial.rememberMaximized && initial.maximized) WindowPlacement.Maximized
            else WindowPlacement.Floating,
        )

        var showPrefs by remember { mutableStateOf(false) }

        // macOS: the system App menu provides "Preferences…" + ⌘, . Its handler
        // may fire off the Compose thread, so bridge through a SharedFlow that a
        // LaunchedEffect collects on the Compose dispatcher before touching state.
        if (hostOs.isMacOS) {
            val prefRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
            DisposableEffect(Unit) {
                val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
                if (desktop != null && desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
                    desktop.setPreferencesHandler { prefRequests.tryEmit(Unit) }
                }
                onDispose { }
            }
            LaunchedEffect(Unit) {
                prefRequests.collect { showPrefs = true }
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Cockpit",
        ) {
            // Non-macOS: no system App menu, so surface Preferences via a menu bar.
            if (!hostOs.isMacOS) {
                MenuBar {
                    Menu("Cockpit") {
                        Item(
                            "Preferences…",
                            shortcut = KeyShortcut(Key.Comma, ctrl = true),
                            onClick = { showPrefs = true },
                        )
                    }
                }
            }

            App()

            // Real-time persistence. The debounce coalesces the burst of
            // intermediate values Compose emits while the window settles after
            // first show (position cascade) and during a continuous user drag, so
            // only the final resting geometry reaches disk. Use ONLY the emitted
            // snapshot triple; do not re-read windowState.* here (it may have
            // moved during the debounce window).
            LaunchedEffect(Unit) {
                snapshotFlow { Triple(windowState.size, windowState.position, windowState.placement) }
                    .debounce(DEBOUNCE_MS)
                    .collect { (size, position, placement) ->
                        val saved = prefs.updateAndGet { cur ->
                            var n = cur
                            if (cur.rememberSize && placement == WindowPlacement.Floating && size.isSpecified) {
                                n = n.copy(width = size.width.value, height = size.height.value)
                            }
                            if (cur.rememberPosition && position is WindowPosition.Absolute) {
                                n = n.copy(x = position.x.value, y = position.y.value)
                            }
                            if (cur.rememberMaximized) {
                                n = n.copy(maximized = placement == WindowPlacement.Maximized)
                            }
                            n
                        }
                        store.save(saved)
                    }
            }
        }

        if (showPrefs) {
            val current by prefs.collectAsState()
            // Compute dialog position centered on the main window when opened.
            val dialogState = remember(showPrefs) {
                val pos = windowState.position
                val size = windowState.size
                val position = if (pos is WindowPosition.Absolute && size.isSpecified) {
                    WindowPosition.Absolute(
                        pos.x + (size.width - 420.dp) / 2f,
                        pos.y + size.height / 4f,
                    )
                } else {
                    WindowPosition(Alignment.Center)
                }
                DialogState(position = position, size = DpSize.Unspecified)
            }
            DialogWindow(
                onCloseRequest = { showPrefs = false },
                state = dialogState,
                title = "Preferences",
                resizable = false,
            ) {
                AppTheme {
                    PreferencesPanel(
                        layout = current,
                        onToggle = { updated ->
                            prefs.value = updated
                            store.save(updated)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Computes the restore position, honoring [WindowLayout.rememberPosition] and
 * guarding against a saved point that now lies outside every screen (e.g. an
 * external monitor was disconnected). Off-screen → platform default.
 */
private fun restoredPosition(layout: WindowLayout): WindowPosition {
    if (!layout.rememberPosition) return WindowPosition.PlatformDefault
    val x = layout.x ?: return WindowPosition.PlatformDefault
    val y = layout.y ?: return WindowPosition.PlatformDefault
    return if (isOnAnyScreen(x, y)) WindowPosition.Absolute(x.dp, y.dp)
    else WindowPosition.PlatformDefault
}

/** True if the dp point (x, y) falls within the union of all screen bounds. */
private fun isOnAnyScreen(x: Float, y: Float): Boolean = runCatching {
    if (GraphicsEnvironment.isHeadless()) return@runCatching true
    GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { device ->
        val b = device.defaultConfiguration.bounds
        x >= b.x && x < b.x + b.width && y >= b.y && y < b.y + b.height
    }
}.getOrDefault(true)

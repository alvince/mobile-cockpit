package com.alvincezy.dev.cockpit.developertools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alvincezy.dev.cockpit.adbops.AdbOperationsArea
import com.alvincezy.dev.cockpit.app.nav.SubSection

/**
 * Developer Tools feature panel. Reuses the shared ADB operations UI; [sub] is
 * accepted so the panel can emphasise commands vs. server control in a later
 * iteration.
 */
@Composable
fun DeveloperToolsPanel(
    sub: SubSection?,
    modifier: Modifier = Modifier,
) {
    AdbOperationsArea(modifier = modifier)
}

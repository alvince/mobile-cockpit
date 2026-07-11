package com.alvincezy.dev.cockpit.connection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alvincezy.dev.cockpit.adbops.AdbOperationsArea
import com.alvincezy.dev.cockpit.app.nav.SubSection

/**
 * Connection feature panel. Reuses the shared ADB operations UI; [sub] is accepted
 * so the panel can emphasise device vs. wireless flows in a later iteration.
 */
@Composable
fun ConnectionPanel(
    sub: SubSection?,
    modifier: Modifier = Modifier,
) {
    AdbOperationsArea(modifier = modifier)
}

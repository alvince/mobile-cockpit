package com.alvincezy.dev.cockpit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvincezy.dev.cockpit.app.nav.SubSection

/**
 * Presentation feature panel — scrcpy mirroring. Placeholder until the scrcpy
 * integration lands.
 */
@Composable
fun PresentationPanel(
    sub: SubSection?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("投屏 (scrcpy)", style = MaterialTheme.typography.titleLarge)
        Text(
            "Coming soon",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

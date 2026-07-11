package com.alvincezy.dev.cockpit.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvincezy.dev.cockpit.app.nav.Section
import com.alvincezy.dev.cockpit.app.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Home (card) state: a vertical list of [SectionCard]s. Selecting a card enters
 * the expanded Detail view for that [Section] via [onOpenSection].
 */
@Composable
fun HomeScreen(
    onOpenSection: (Section) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Cockpit Desktop Toolbox", style = MaterialTheme.typography.headlineMedium)
        Text(
            "调试 · 演示 · 操作",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionCard(
            section = Section.Connection,
            description = "连接与管理设备：USB / 无线配对",
            onClick = { onOpenSection(Section.Connection) },
        )
        SectionCard(
            section = Section.DeveloperTools,
            description = "ADB 命令与 server 生命周期管理",
            onClick = { onOpenSection(Section.DeveloperTools) },
        )
        SectionCard(
            section = Section.Presentation,
            description = "投屏演示 (scrcpy)",
            onClick = { onOpenSection(Section.Presentation) },
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    AppTheme {
        HomeScreen(onOpenSection = {})
    }
}

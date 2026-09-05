package com.alvincezy.dev.cockpit.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvincezy.dev.cockpit.app.theme.AppTheme
import com.alvincezy.dev.cockpit.core.window.WindowLayout
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Preferences panel: three switches controlling whether the window remembers its
 * size / position / maximized state. Pure presentation — [layout] is the current
 * value and every toggle emits a copy via [onToggle]; the caller owns persistence.
 */
@Composable
fun PreferencesPanel(
    layout: WindowLayout,
    onToggle: (WindowLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 420.dp).heightIn(max = 400.dp),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp)
                .heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("窗口", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(top = 4.dp))

            ToggleRow(
                label = "记住窗口大小",
                checked = layout.rememberSize,
                onCheckedChange = { onToggle(layout.copy(rememberSize = it)) },
            )
            ToggleRow(
                label = "记住窗口位置",
                checked = layout.rememberPosition,
                onCheckedChange = { onToggle(layout.copy(rememberPosition = it)) },
            )
            ToggleRow(
                label = "记住最大化状态",
                checked = layout.rememberMaximized,
                onCheckedChange = { onToggle(layout.copy(rememberMaximized = it)) },
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview
@Composable
private fun PreferencesPanelPreview() {
    AppTheme {
        PreferencesPanel(layout = WindowLayout(rememberPosition = false), onToggle = {})
    }
}

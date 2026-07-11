package com.alvincezy.dev.cockpit.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alvincezy.dev.cockpit.app.nav.Section
import com.alvincezy.dev.cockpit.app.nav.SubSection
import com.alvincezy.dev.cockpit.app.theme.AppTheme
import com.alvincezy.dev.cockpit.connection.ConnectionPanel
import com.alvincezy.dev.cockpit.developertools.DeveloperToolsPanel
import com.alvincezy.dev.cockpit.presentation.PresentationPanel
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Expanded (Detail) state: a left navigation rail listing the primary [Section]s
 * plus a "back to Home" action, and a right-hand panel that renders the currently
 * selected section's content.
 *
 * The selected section is kept as **local state** (seeded from [initialSection]),
 * so switching sections only recomposes the right pane — it does NOT re-navigate,
 * which previously replayed the full-window enter/exit transition and caused a
 * whole-window flicker. Only the panel content cross-fades.
 *
 * The rail wraps its content width (widest item) rather than using a fixed width.
 */
@Composable
fun DetailScaffold(
    initialSection: Section,
    initialSub: SubSection?,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var current by rememberSaveable(initialSection) { mutableStateOf(initialSection) }
    var currentSub by rememberSaveable(initialSection) { mutableStateOf(initialSub) }

    Row(modifier = modifier.fillMaxSize()) {
        NavRail(
            current = current,
            onSelectSection = { section ->
                current = section
                currentSub = section.defaultSub
            },
            onBackToHome = onBackToHome,
        )

        VerticalDivider()

        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = current,
                animationSpec = tween(220),
                modifier = Modifier.fillMaxSize(),
            ) { section ->
                DetailPanel(
                    section = section,
                    sub = if (section == current) currentSub else section.defaultSub,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * A content-width (wrap-content) navigation rail: a vertical column of items sized
 * to the widest entry via [IntrinsicSize.Max].
 */
@Composable
private fun NavRail(
    current: Section,
    onSelectSection: (Section) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(vertical = 12.dp, horizontal = 8.dp),
        ) {
            NavRailItem(
                label = "← Home",
                selected = false,
                onClick = onBackToHome,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Section.entries.forEach { entry ->
                NavRailItem(
                    label = entry.label,
                    selected = entry == current,
                    onClick = { onSelectSection(entry) },
                )
            }
        }
    }
}

@Composable
private fun NavRailItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background =
        if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
        textAlign = TextAlign.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/**
 * Dispatches the right-pane content for the given [section] / [sub].
 */
@Composable
private fun DetailPanel(
    section: Section,
    sub: SubSection?,
    modifier: Modifier = Modifier,
) {
    when (section) {
        Section.Connection -> ConnectionPanel(sub = sub, modifier = modifier.padding(24.dp))
        Section.DeveloperTools -> DeveloperToolsPanel(sub = sub, modifier = modifier.padding(24.dp))
        Section.Presentation -> PresentationPanel(sub = sub, modifier = modifier)
    }
}

// Uses the Presentation section so the preview needs no Koin-injected ADB panel.
@Preview
@Composable
private fun DetailScaffoldPreview() {
    AppTheme {
        DetailScaffold(
            initialSection = Section.Presentation,
            initialSub = Section.Presentation.defaultSub,
            onBackToHome = {},
        )
    }
}

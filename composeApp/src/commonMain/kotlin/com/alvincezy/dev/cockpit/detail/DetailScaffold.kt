package com.alvincezy.dev.cockpit.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
 * Left navigation rail for the expanded (Detail) state: a "back to Home" action
 * plus the primary [Section]s. Designed to be hosted in a fixed-width slot by the
 * morph layout, so it fills its given width rather than wrapping content.
 *
 * Selecting a section changes the current section only (via [onSelectSection]) —
 * it does not trigger any navigation/morph, so switching sections just cross-fades
 * the right pane (see [DetailPanelHost]) with no whole-window animation.
 */
@Composable
fun NavRail(
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
                .fillMaxWidth()
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
 * The right pane: renders the current [section] / [sub] content, cross-fading
 * when the section changes so only the panel content swaps (no window flicker).
 */
@Composable
fun DetailPanelHost(
    section: Section,
    sub: SubSection?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = section,
            animationSpec = tween(220),
            modifier = Modifier.fillMaxSize(),
        ) { target ->
            DetailPanel(
                section = target,
                sub = if (target == section) sub else target.defaultSub,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
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

// Previews the fully-expanded (Detail) layout. Uses Section.Presentation so the
// preview needs no Koin-injected ADB panel.
@Preview
@Composable
private fun DetailExpandedPreview() {
    AppTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            NavRail(
                current = Section.Presentation,
                onSelectSection = {},
                onBackToHome = {},
                modifier = Modifier.width(200.dp),
            )
            VerticalDivider()
            DetailPanelHost(
                section = Section.Presentation,
                sub = Section.Presentation.defaultSub,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

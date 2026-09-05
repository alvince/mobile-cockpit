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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.alvincezy.dev.cockpit.app.nav.Section
import com.alvincezy.dev.cockpit.app.nav.SubSection
import com.alvincezy.dev.cockpit.app.theme.AppTheme
import com.alvincezy.dev.cockpit.connection.ConnectionPanel
import com.alvincezy.dev.cockpit.developertools.DeveloperToolsPanel
import com.alvincezy.dev.cockpit.presentation.PresentationPanel
import org.jetbrains.compose.ui.tooling.preview.Preview

/** How long the fly-out stays open after the cursor leaves, so the pointer can
 *  cross the gap from a rail item into its pop-out without dismissing it. */
private const val FLYOUT_CLOSE_DELAY_MS = 150L

/**
 * Left navigation rail for the expanded (Detail) state: a "back to Home" action
 * plus the primary [Section]s. Designed to be hosted in a fixed-width slot by the
 * morph layout, so it fills its given width rather than wrapping content.
 *
 * Clicking a section enters it via [onSelectSection] (opening its [Section.defaultSub]),
 * which keeps the rail usable without a mouse. Hovering a section reveals a [Popup]
 * fly-out of that section's [SubSection]s; picking one calls [onSelectSub].
 *
 * Neither action triggers the morph — they only update state, so the right pane
 * cross-fades (see [DetailPanelHost]) with no whole-window animation.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavRail(
    current: Section,
    currentSub: SubSection?,
    onSelectSection: (Section) -> Unit,
    onSelectSub: (SubSection) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The section whose fly-out is currently shown, plus a "requested to close"
    // flag so a short grace period can keep it open while the cursor travels.
    var hovered by remember { mutableStateOf<Section?>(null) }
    var closeRequested by remember { mutableStateOf(false) }

    LaunchedEffect(closeRequested, hovered) {
        if (closeRequested && hovered != null) {
            kotlinx.coroutines.delay(FLYOUT_CLOSE_DELAY_MS)
            if (closeRequested) hovered = null
        }
    }

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
                var anchorWidthPx by remember { mutableStateOf(0) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { anchorWidthPx = it.size.width },
                ) {
                    NavRailItem(
                        label = entry.label,
                        selected = entry == current,
                        onClick = { onSelectSection(entry) },
                        modifier = Modifier
                            .onPointerEvent(PointerEventType.Enter) {
                                hovered = entry
                                closeRequested = false
                            }
                            .onPointerEvent(PointerEventType.Exit) {
                                closeRequested = true
                            },
                    )
                    if (hovered == entry) {
                        SubSectionFlyout(
                            section = entry,
                            currentSub = currentSub,
                            anchorWidthPx = anchorWidthPx,
                            onSelectSub = {
                                onSelectSub(it)
                                hovered = null
                                closeRequested = false
                            },
                            onEnter = { closeRequested = false },
                            onExit = { closeRequested = true },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fly-out [Popup] anchored to the right edge of a rail item, listing the
 * [section]'s sub-sections. It tracks its own pointer enter/exit so the fly-out
 * survives the cursor crossing over from the rail item.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SubSectionFlyout(
    section: Section,
    currentSub: SubSection?,
    anchorWidthPx: Int,
    onSelectSub: (SubSection) -> Unit,
    onEnter: () -> Unit,
    onExit: () -> Unit,
) {
    Popup(
        // The Popup positions relative to its parent (the anchor item Box), which
        // has its top-start at the item's top-left. Offsetting x by the anchor's
        // full width drops the fly-out just to the right of the rail, top-aligned
        // with the hovered item.
        offset = IntOffset(x = anchorWidthPx, y = 0),
        alignment = Alignment.TopStart,
    ) {
        Surface(
            modifier = Modifier
                .onPointerEvent(PointerEventType.Enter) { onEnter() }
                .onPointerEvent(PointerEventType.Exit) { onExit() },
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(8.dp),
        ) {
            // IntrinsicSize.Max sizes the column to its widest item, so the
            // fly-out is only as wide as the longest label (like a normal menu);
            // each item then fills that intrinsic width so backgrounds line up.
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(4.dp),
            ) {
                SubSection.forSection(section).forEach { item ->
                    NavRailItem(
                        label = item.label,
                        selected = item == currentSub,
                        onClick = { onSelectSub(item) },
                    )
                }
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
                currentSub = Section.Presentation.defaultSub,
                onSelectSection = {},
                onSelectSub = {},
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

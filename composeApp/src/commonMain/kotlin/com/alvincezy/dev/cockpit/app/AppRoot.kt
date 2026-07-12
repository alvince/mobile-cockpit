package com.alvincezy.dev.cockpit.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.alvincezy.dev.cockpit.app.nav.Section
import com.alvincezy.dev.cockpit.app.nav.SubSection
import com.alvincezy.dev.cockpit.detail.DetailPanelHost
import com.alvincezy.dev.cockpit.detail.NavRail
import com.alvincezy.dev.cockpit.home.HomeScreen
import kotlin.math.roundToInt

private const val MORPH_MS = 350

/** Fixed width of the collapsed navigation rail (the morph's target left width). */
private val RailWidth = 200.dp

/**
 * Main screen root. Instead of navigating between two destinations, it keeps a
 * single [expanded] flag and morphs the layout continuously:
 *
 * - **Home** (`expanded == false`, progress 0): the left region fills the whole
 *   window and shows the [HomeScreen] cards.
 * - **Detail** (`expanded == true`, progress 1): the left region has shrunk to a
 *   narrow [NavRail], and the [DetailPanelHost] has slid in from the right.
 *
 * A single [animateFloatAsState] `progress` drives the left region's width
 * ([lerp] full → rail), the right panel's slide-in offset + fade, and the
 * left content crossfade (Home cards fade out, rail fades in).
 *
 * Switching sections inside Detail only updates [section] (not [expanded]), so it
 * does not replay the morph — only the right pane cross-fades (see [DetailPanelHost]).
 */
@Composable
fun AppRoot() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var section by rememberSaveable { mutableStateOf(Section.Connection) }
    var sub by rememberSaveable(section) { mutableStateOf<SubSection?>(null) }

    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(MORPH_MS, easing = FastOutSlowInEasing),
        label = "morph",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val full = maxWidth
        val leftW = lerp(full, RailWidth, progress)
        val panelW = full - RailWidth

        Box(modifier = Modifier.fillMaxSize()) {
            // LEFT: resizes in place from full width down to the rail width.
            Box(
                modifier = Modifier
                    .width(leftW)
                    .fillMaxHeight()
                    .clipToBounds(),
            ) {
                // Home cards: visible while collapsing (mounted until fully expanded).
                if (progress < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 1f - progress },
                    ) {
                        HomeScreen(
                            onOpenSection = { selected ->
                                section = selected
                                sub = selected.defaultSub
                                expanded = true
                            },
                        )
                    }
                }
                // Nav rail: fades in as the region narrows (mounted once expanding begins).
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = progress },
                    ) {
                        NavRail(
                            current = section,
                            onSelectSection = { selected ->
                                section = selected
                                sub = selected.defaultSub
                            },
                            onBackToHome = { expanded = false },
                        )
                    }
                }
            }

            // RIGHT: details panel slides in from the right edge and fades with progress.
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .width(panelW)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .offset {
                            IntOffset(
                                x = (panelW.roundToPx() * (1f - progress)).roundToInt(),
                                y = 0,
                            )
                        }
                        .clipToBounds()
                        .graphicsLayer { alpha = progress },
                ) {
                    DetailPanelHost(
                        section = section,
                        sub = sub ?: section.defaultSub,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

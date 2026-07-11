package com.alvincezy.dev.cockpit.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.alvincezy.dev.cockpit.app.nav.Destination
import com.alvincezy.dev.cockpit.app.theme.AppTheme
import com.alvincezy.dev.cockpit.detail.DetailScaffold
import com.alvincezy.dev.cockpit.home.HomeScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val TRANSITION_MS = 350

/**
 * Root navigation host wiring the two main-screen states:
 * - [Destination.Home]   → card landing state ([HomeScreen])
 * - [Destination.Detail] → expanded two-pane state ([DetailScaffold])
 *
 * Transition: entering Detail slides the Home content out to the left (collapsing
 * toward the nav rail) while the Detail pane expands in from the right; going back
 * reverses the motion.
 */
@Composable
fun AppRoot() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.Home,
    ) {
        composable<Destination.Home>(
            // Home leaves toward the left when Detail opens; returns from the left on back.
            exitTransition = {
                slideOutHorizontally(tween(TRANSITION_MS)) { full -> -full / 3 } +
                    fadeOut(tween(TRANSITION_MS))
            },
            popEnterTransition = {
                slideInHorizontally(tween(TRANSITION_MS)) { full -> -full / 3 } +
                    fadeIn(tween(TRANSITION_MS))
            },
        ) {
            HomeScreen(
                onOpenSection = { section ->
                    navController.navigate(Destination.Detail.of(section, section.defaultSub))
                },
            )
        }

        composable<Destination.Detail>(
            // Detail expands in from the right; slides back out to the right on back.
            enterTransition = {
                slideInHorizontally(tween(TRANSITION_MS)) { full -> full } +
                    fadeIn(tween(TRANSITION_MS))
            },
            popExitTransition = {
                slideOutHorizontally(tween(TRANSITION_MS)) { full -> full } +
                    fadeOut(tween(TRANSITION_MS))
            },
        ) { backStackEntry ->
            val detail = backStackEntry.toRoute<Destination.Detail>()
            // Section switches inside Detail are handled by DetailScaffold's local state
            // (no re-navigation), so only the panel content changes — no window flicker.
            DetailScaffold(
                initialSection = detail.section,
                initialSub = detail.sub,
                onBackToHome = {
                    // Pop back to the existing Home entry so the reverse (pop) transition plays.
                    if (!navController.popBackStack(Destination.Home, inclusive = false)) {
                        navController.navigate(Destination.Home)
                    }
                },
            )
        }
    }
}

/**
 * Preview of the app's landing state. [AppRoot] itself wraps a [NavHost], which
 * depends on navigation runtime and cannot render in a static @Preview — so we
 * preview the start-destination content ([HomeScreen]) directly instead.
 */
@Preview
@Composable
private fun AppRootPreview() {
    AppTheme {
        HomeScreen(onOpenSection = {})
    }
}

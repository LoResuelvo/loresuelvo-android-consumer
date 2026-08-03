package com.loresuelvo.consumer.ui.components.bottomnav

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [LoResuelvoBottomBar]. Run on the JVM via
 * Robolectric so they participate in the unit-test task.
 *
 * The bar's contract is derived from the current route: the
 * `LoResuelvoNav` host passes the route from
 * `currentBackStackEntryAsState`, and the bar only renders on the
 * three primary destinations declared in [BottomDestination]. These
 * tests pin visibility + click so future changes to the host
 * wiring can't silently regress the bar.
 *
 * Note: the selection visual (the M3 active-indicator pill) is
 * covered manually on the device — the `ui-test:1.7.x` surface
 * (Compose BOM 2024.09) doesn't expose `onAllNodes(matcher)` /
 * `hasRole`, so the per-tab `Selected` semantics aren't reachable
 * from a test runner. The "exactly one item per label" assertion
 * pins the structural contract; the M3 NavigationBarItem handles
 * the visual selection indicator out of the box.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class LoResuelvoBottomBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(@androidx.annotation.StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id)

    @Test
    fun renders_three_tabs_on_a_primary_destination() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                LoResuelvoBottomBar(
                    currentRoute = BottomDestination.Inicio.route,
                    onNavigate = {},
                )
            }
        }
        // Every tab exposes its localised label as visible text.
        for (destination in BottomDestination.all) {
            composeTestRule.onNodeWithText(string(destination.labelRes))
                .assertExists()
        }
        // And exactly one node per label (the icon's content
        // description shares the label text, so `onAllNodesWithText`
        // would otherwise return 2). The assertion pins the
        // "one item per destination" invariant.
        for (destination in BottomDestination.all) {
            composeTestRule.onAllNodesWithText(text = string(destination.labelRes))
                .assertCountEquals(1)
        }
    }

    @Test
    fun hides_the_bar_on_non_primary_destinations() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                LoResuelvoBottomBar(
                    currentRoute = "welcome",  // not a BottomDestination
                    onNavigate = {},
                )
            }
        }
        // The bar returns early → no tab labels in the tree.
        for (destination in BottomDestination.all) {
            composeTestRule.onNodeWithText(string(destination.labelRes))
                .assertDoesNotExist()
        }
    }

    @Test
    fun hides_the_bar_when_current_route_is_null() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                LoResuelvoBottomBar(
                    currentRoute = null,
                    onNavigate = {},
                )
            }
        }
        for (destination in BottomDestination.all) {
            composeTestRule.onNodeWithText(string(destination.labelRes))
                .assertDoesNotExist()
        }
    }

    @Test
    fun clicking_a_tab_invokes_onNavigate_with_its_destination() {
        // `setContent` is single-shot per test, so the loop drives
        // three clicks against a single composition. The bar's
        // `currentRoute` stays pinned to the Inicio route — the host
        // is what would react to `onNavigate`, not the bar.
        val captured = mutableListOf<BottomDestination>()
        composeTestRule.setContent {
            LoresuelvoTheme {
                LoResuelvoBottomBar(
                    currentRoute = BottomDestination.Inicio.route,
                    onNavigate = { captured += it },
                )
            }
        }
        for (destination in BottomDestination.all) {
            captured.clear()
            composeTestRule.onNodeWithText(string(destination.labelRes))
                .performClick()
            assertEquals(listOf(destination), captured)
        }
    }
}

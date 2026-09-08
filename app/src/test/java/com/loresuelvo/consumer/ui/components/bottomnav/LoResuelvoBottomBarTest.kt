package com.loresuelvo.consumer.ui.components.bottomnav

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
 * Tag surfaces (see [LOWRESUELVO_BOTTOM_BAR_TAG] /
 * [BOTTOM_BAR_ITEM_TAG_PREFIX] in `BottomNavigationBar.kt`):
 *  - `lo-resuelvo-bottom-bar` — the outer Surface; presence pins
 *    the bar's visibility contract, absence pins the "hide on
 *    non-primary routes" contract.
 *  - `bottom-bar-item-{route}` — one per tab; targeted by the
 *    click test so the assertion does not depend on the
 *    localised copy of the tab label (the bar no longer paints
 *    labels; it's icon-only).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class LoResuelvoBottomBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
        // The outer Surface is present.
        composeTestRule.onNodeWithTag(LOWRESUELVO_BOTTOM_BAR_TAG).assertExists()
        // One click target per tab.
        BottomDestination.all.forEach { destination ->
            composeTestRule.onNodeWithTag(BOTTOM_BAR_ITEM_TAG_PREFIX + destination.route)
                .assertExists()
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
        composeTestRule.onAllNodesWithTag(LOWRESUELVO_BOTTOM_BAR_TAG).assertCountEquals(0)
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
        composeTestRule.onAllNodesWithTag(LOWRESUELVO_BOTTOM_BAR_TAG).assertCountEquals(0)
    }

    @Test
    fun clicking_a_tab_invokes_onNavigate_with_its_destination() {
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
            composeTestRule
                .onNodeWithTag(BOTTOM_BAR_ITEM_TAG_PREFIX + destination.route)
                .performClick()
            assertEquals(listOf(destination), captured)
        }
    }
}
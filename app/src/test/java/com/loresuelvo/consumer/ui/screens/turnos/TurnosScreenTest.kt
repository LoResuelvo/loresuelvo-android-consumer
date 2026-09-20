package com.loresuelvo.consumer.ui.screens.turnos

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [TurnosScreen].
 *
 * Landed minimally for scenario 01-VT: only the Loading branch
 * is pinned. The Ready / Error branches land with scenarios
 * 02-VT..14-VT — see [com.loresuelvo.consumer.bdd.home.VisualizeTurnsSteps]
 * for the BDD surface.
 *
 * Locale is pinned to `es-rAR` so the localised copy matches
 * the `values/strings.xml` strings the production app ships.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class TurnosScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loading_state_renders_top_app_bar_with_title() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnosScreen(state = TurnosUiState.Loading)
                }
            }
        }

        composeTestRule.onNodeWithTag(TURNOS_SCREEN_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TURNOS_TITLE_TAG).assertIsDisplayed()
    }
}

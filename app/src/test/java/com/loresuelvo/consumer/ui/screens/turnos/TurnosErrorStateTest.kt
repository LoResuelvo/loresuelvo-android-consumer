package com.loresuelvo.consumer.ui.screens.turnos

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for the [TurnosScreen] Error branch
 * (visualize-turns.feature scenarios 13-VT / 14-VT).
 *
 * Landed incrementally per scenario:
 *  - 13-VT → `error_network_state_renders_typed_copy_and_retry_cta`.
 *  - 14-VT → `error_server_state_renders_typed_copy_and_retry_cta`.
 *
 * Locale is pinned to `es-rAR` so the localised copy matches
 * the `values/strings.xml` strings the production app ships.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class TurnosErrorStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun localizedString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(resourceId)

    @Test
    fun error_network_state_renders_typed_copy_and_retry_cta() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnosScreen(
                        state = TurnosUiState.Error(
                            TurnosOutcome.Failure.Network(IOException("dns")),
                        ),
                        onRetryClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TURNOS_ERROR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TURNOS_ERROR_RETRY_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(
            localizedString(R.string.turnos_error_network),
        ).assertCountEquals(1)
    }

    @Test
    fun error_server_state_renders_typed_copy_and_retry_cta() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnosScreen(
                        state = TurnosUiState.Error(
                            TurnosOutcome.Failure.Server(code = 500, message = "boom"),
                        ),
                        onRetryClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TURNOS_ERROR_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(
            localizedString(R.string.turnos_error_server),
        ).assertCountEquals(1)
    }

    @Test
    fun retry_button_fires_the_callback() {
        var retries = 0
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnosScreen(
                        state = TurnosUiState.Error(
                            TurnosOutcome.Failure.Server(code = 500, message = "boom"),
                        ),
                        onRetryClick = { retries++ },
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TURNOS_ERROR_RETRY_TAG).performClick()
        assertEquals(1, retries)
    }
}

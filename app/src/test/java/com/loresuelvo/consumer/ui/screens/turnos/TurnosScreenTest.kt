package com.loresuelvo.consumer.ui.screens.turnos

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.auth.CalendarConnectionStatus
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.ui.components.turnocard.TURNO_CARD_TAG_PREFIX
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class TurnosScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun localizedString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(resourceId)

    @Test
    fun loading_state_renders_top_app_bar_and_spinner() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnosScreen(
                        state = TurnosUiState.Loading,
                        onRetryClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TURNOS_SCREEN_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TURNOS_TITLE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TURNOS_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun ready_state_with_items_renders_one_card_per_turno() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnosScreen(
                        state = TurnosUiState.Ready(
                            listOf(
                                sampleTurno(id = "1"),
                                sampleTurno(id = "2"),
                            ),
                        ),
                        onRetryClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TURNOS_LIST_TAG).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag(TURNO_CARD_TAG_PREFIX + "1")
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithTag(TURNO_CARD_TAG_PREFIX + "2")
            .assertCountEquals(1)
    }

    @Test
    fun ready_state_with_empty_list_renders_empty_state_copy() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnosScreen(
                        state = TurnosUiState.Ready(emptyList()),
                        onRetryClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TURNOS_EMPTY_TAG).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turnos_empty_title))
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turnos_empty_body))
            .assertCountEquals(1)
    }

    @Test
    fun disconnected_calendar_status_exposes_profile_connection_action() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    TurnosScreen(
                        state = TurnosUiState.Ready(
                            turnos = listOf(sampleTurno("1")),
                            calendarConnectionStatus = CalendarConnectionStatus.DISCONNECTED,
                        ),
                        onRetryClick = {},
                        onCalendarConnectionClick = { clicked = true },
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(CALENDAR_SYNC_BANNER_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CALENDAR_SYNC_ACTION_TAG).performClick()
        assertTrue(clicked)
    }

    private fun sampleTurno(id: String): Turno = Turno(
        id = id,
        serviceProposalId = "p-$id",
        status = TurnoStatus.Confirmed,
        counterpart = TurnoCounterpart(
            id = "$id-c",
            name = "Juan",
            surname = "Gómez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "Reparación",
        amountCents = 150_005_0L,
        scheduledOnEpochMillis = 1_783_540_200_000L,
    )
}

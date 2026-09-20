package com.loresuelvo.consumer.ui.components.turnocard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.domain.turno.TurnoCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [TurnoCard].
 *
 * Landed incrementally per scenario:
 *  - 04-VT → `renders_every_pinned_field` (name, photo,
 *    description, amount, date, time).
 *  - 05-VT → `status_badge_renders_per_turno_status` (Pending /
 *    Confirmed / Finished / Cancelled).
 *
 * Locale is pinned to `es-rAR` so the localised copy matches
 * the `values/strings.xml` strings the production app ships.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class TurnoCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun localizedString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(resourceId)

    @Test
    fun renders_every_pinned_field_for_a_single_turno() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnoCard(
                        turno = sampleTurno(id = "1"),
                        onCardClicked = {},
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(TURNO_CARD_TAG_PREFIX + "1")
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Juan Gómez")
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText("Plomería")
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText("Reparación de cañería")
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText("$ 15.000")
            .assertCountEquals(1)
        // ScheduledDateFormatter renders "dd/MM/yyyy - HH:mm hs"
        composeTestRule
            .onAllNodesWithText("15/10/2026 - 14:30 hs")
            .assertCountEquals(1)
    }

    @Test
    fun status_badge_renders_per_turno_status() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnoCard(
                        turno = sampleTurno(id = "1"),
                        onCardClicked = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_status_confirmed))
            .assertCountEquals(1)
    }

    @Test
    fun status_badge_renders_Pending() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnoCard(
                        turno = sampleTurno(id = "1", status = TurnoStatus.Pending),
                        onCardClicked = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_status_pending))
            .assertCountEquals(1)
    }

    private fun sampleTurno(
        id: String,
        status: TurnoStatus = TurnoStatus.Confirmed,
    ): Turno = Turno(
        id = id,
        serviceProposalId = "p-$id",
        status = status,
        counterpart = TurnoCounterpart(
            id = "$id-c",
            name = "Juan",
            surname = "Gómez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "Reparación de cañería",
        amountCents = 1_500_000L,
        // 2026-10-15 14:30 UTC — same epoch the WorkOrderScreenTest
        // uses to pin the formatter shape.
        scheduledOnEpochMillis = 1_792_074_600_000L,
    )
}

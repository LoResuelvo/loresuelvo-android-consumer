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
                        onDetailsClick = {},
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
                        onDetailsClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_status_confirmed))
            .assertCountEquals(1)
    }

    @Test
    fun status_badge_renders_Today_label_when_turno_is_scheduled_for_today_and_Confirmed() {
        val today = startOfTodayUtcMillis()
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnoCard(
                        turno = sampleTurno(
                            id = "1",
                            status = TurnoStatus.Confirmed,
                            scheduledOnEpochMillis = today,
                        ),
                        onDetailsClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_today_label))
            .assertCountEquals(1)
        // The "Confirmado" label must NOT be rendered when the
        // turno is today — the "(hoy)" pill replaces it.
        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_status_confirmed))
            .assertCountEquals(0)
    }

    @Test
    fun status_badge_keeps_Confirmado_label_when_turno_is_Confirmed_but_NOT_today() {
        val notToday = startOfTodayUtcMillis() + 24 * 60 * 60 * 1000L
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnoCard(
                        turno = sampleTurno(
                            id = "1",
                            status = TurnoStatus.Confirmed,
                            scheduledOnEpochMillis = notToday,
                        ),
                        onDetailsClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_status_confirmed))
            .assertCountEquals(1)
    }

    @Test
    fun status_badge_keeps_original_label_when_turno_is_today_but_NOT_Confirmed() {
        val today = startOfTodayUtcMillis()
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnoCard(
                        turno = sampleTurno(
                            id = "1",
                            status = TurnoStatus.Pending,
                            scheduledOnEpochMillis = today,
                        ),
                        onDetailsClick = {},
                    )
                }
            }
        }

        // "(hoy)" only replaces the badge when status is
        // Confirmed — a Pending turno scheduled for today still
        // renders the "Pendiente" badge.
        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_status_pending))
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_today_label))
            .assertCountEquals(0)
    }

    @Test
    fun status_badge_renders_Pending() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnoCard(
                        turno = sampleTurno(id = "1", status = TurnoStatus.Pending),
                        onDetailsClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_status_pending))
            .assertCountEquals(1)
    }

    @Test
    fun status_badge_renders_Finished() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnoCard(
                        turno = sampleTurno(id = "1", status = TurnoStatus.Finished),
                        onDetailsClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_status_finished))
            .assertCountEquals(1)
    }

    @Test
    fun status_badge_renders_Cancelled() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    TurnoCard(
                        turno = sampleTurno(id = "1", status = TurnoStatus.Cancelled),
                        onDetailsClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithText(localizedString(R.string.turno_status_cancelled))
            .assertCountEquals(1)
    }

    private fun sampleTurno(
        id: String,
        status: TurnoStatus = TurnoStatus.Confirmed,
        scheduledOnEpochMillis: Long = 1_792_074_600_000L,
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
        // Default seed matches the WorkOrderScreenTest fixture so
        // the formatter shape is pinned (2026-10-15 14:30 UTC).
        scheduledOnEpochMillis = scheduledOnEpochMillis,
    )
}

/**
 * Returns the epoch-millis instant at 00:00:00 UTC of the
 * current day. Used by the badge tests to seed a `Turno` that
 * falls on "today" without hard-coding a date that would drift.
 */
private fun startOfTodayUtcMillis(): Long {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

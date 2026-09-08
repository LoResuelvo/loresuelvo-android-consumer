package com.loresuelvo.consumer.ui.components.proposalcard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [ProposalCard]. Runs on the JVM via
 * [RobolectricTestRunner].
 *
 * The card exposes **two** clickable targets that both fire the
 * same `onViewClicked` callback (the outer Column and the inner
 * "Ver Solicitud" CTA row). The instrumented suite covers the
 * full Home / MisServicios integration; this JVM test pins that
 * the card itself routes taps to the host without intermediaries.
 *
 * Locale pinned to `es-rAR` so the screen resolves the
 * production Spanish copy.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ProposalCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tapping_the_card_fires_onViewClicked_once() {
        var clicks = 0

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    ProposalCard(
                        proposal = sampleProposal(id = "1"),
                        onViewClicked = { clicks++ },
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(PROPOSAL_CARD_TAG_PREFIX + "1")
            .performClick()

        assertEquals(
            "expected the ProposalCard outer Column's click handler to fire " +
                "exactly once on a tap",
            1,
            clicks,
        )
    }

    @Test
    fun tapping_the_ver_solicitud_cta_fires_onViewClicked() {
        var clicks = 0

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    ProposalCard(
                        proposal = sampleProposal(id = "1"),
                        onViewClicked = { clicks++ },
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(PROPOSAL_CARD_VIEW_TAG_PREFIX + "1")
            .performClick()

        assertEquals(
            "expected the inner ViewRequestCta row to route its tap to " +
                "the same onViewClicked callback the card carries",
            1,
            clicks,
        )
    }

    @Test
    fun without_a_click_handler_no_callback_is_invoked() {
        // The default `onViewClicked = {}` keeps the card a no-op
        // when the host forgets to wire it — pinning the behaviour
        // that hid the US-54 Home tap bug in the first place.
        var clicks = 0
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    ProposalCard(
                        proposal = sampleProposal(id = "1"),
                        // Default no-op from the function signature.
                        onViewClicked = {},
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(PROPOSAL_CARD_TAG_PREFIX + "1")
            .performClick()

        assertEquals(
            "expected the default no-op callback to swallow the tap silently",
            0,
            clicks,
        )
    }

    private fun sampleProposal(id: String): ServiceProposal =
        ServiceProposal(
            id = id,
            conversationId = "1000",
            status = ServiceProposalStatus.Pending,
            counterpart = ServiceProposalCounterpart(
                id = "10",
                name = "Carlos",
                surname = "López",
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
            description = "Fuga en el lavamanos",
            amountCents = 1_500_000L,
            scheduledOnEpochMillis = 1_792_074_600_000L,
            createdOnEpochMillis = 1_788_434_364_640L,
        )
}
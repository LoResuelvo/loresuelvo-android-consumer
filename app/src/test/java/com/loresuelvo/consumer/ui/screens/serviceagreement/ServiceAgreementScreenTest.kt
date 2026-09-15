package com.loresuelvo.consumer.ui.screens.serviceagreement

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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalCounterpart
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for the US-21 service-agreement confirmation
 * dialog. The dialog itself is UI-only state inside the
 * composable (a `remember { mutableStateOf(false) }`), so the JVM
 * BDD world cannot reach it. These tests run the screen under
 * Robolectric so they exercise the actual `AlertDialog` dismissal
 * flow (which is the user-visible behaviour the JVM BDD asserts
 * is satisfied by the implementation).
 *
 * The Compose rule here does NOT bring up the production Hilt
 * graph — we render `ServiceAgreementScreen` directly with a
 * `Ready` state. The VM-level transitions are covered by the
 * BDD world; this file is intentionally scoped to the dialog's
 * open/close semantics.
 *
 * Locale pinned to `es-rAR` so the test resolves the production
 * Spanish copy (`Sí, confirmar` / `No, cancelar`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ServiceAgreementScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun confirm_cta_opens_confirmation_dialog() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    ServiceAgreementScreen(
                        state = readyState(),
                        onRetry = {},
                        onConfirm = {},
                        onCancel = {},
                        onReturnHome = {},
                        onPaidAlready = {},
                    )
                }
            }
        }

        // Initially the modal is NOT visible — only the Ready block
        // renders. The dialog's confirm button carries its own
        // testTag so we can pin it precisely.
        composeTestRule
            .onAllNodesWithTag(SERVICE_AGREEMENT_CONFIRM_BUTTON_TAG)
            .assertCountEquals(0)

        // Press the "Confirmar acuerdo" CTA. The screen toggles its
        // local `showConfirmDialog` state to true.
        composeTestRule
            .onNodeWithTag(SERVICE_AGREEMENT_CONFIRM_CTA_TAG)
            .performClick()

        composeTestRule.waitForIdle()

        // The dialog is now visible. The Yes / No buttons + the
        // title render together inside the AlertDialog.
        composeTestRule
            .onNodeWithTag(SERVICE_AGREEMENT_CONFIRM_BUTTON_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(localizedString(R.string.service_agreement_confirm_title))
            .assertIsDisplayed()
    }

    @Test
    fun cancel_button_dismisses_dialog_without_changing_state() {
        var confirmCalls = 0

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    ServiceAgreementScreen(
                        state = readyState(),
                        onRetry = {},
                        onConfirm = { confirmCalls++ },
                        onCancel = {},
                        onReturnHome = {},
                        onPaidAlready = {},
                    )
                }
            }
        }

        // Open the dialog.
        composeTestRule
            .onNodeWithTag(SERVICE_AGREEMENT_CONFIRM_CTA_TAG)
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(SERVICE_AGREEMENT_CONFIRM_BUTTON_TAG)
            .assertIsDisplayed()

        // Tap "No, cancelar". The dialog closes (the Yes button is
        // no longer in the tree) and onConfirm is NOT called.
        composeTestRule
            .onNodeWithText(localizedString(R.string.service_agreement_confirm_no))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithTag(SERVICE_AGREEMENT_CONFIRM_BUTTON_TAG)
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText(localizedString(R.string.service_agreement_confirm_title))
            .assertCountEquals(0)
        org.junit.Assert.assertEquals(
            "dismissing the dialog must not call onConfirm",
            0,
            confirmCalls,
        )
    }

    private fun readyState(): ServiceAgreementUiState.Ready =
        ServiceAgreementUiState.Ready(
            serviceProposalId = 9001,
            proposal = ServiceProposal(
                id = "9001",
                conversationId = "9001-c",
                status = ServiceProposalStatus.Pending,
                counterpart = ServiceProposalCounterpart(
                    id = "100",
                    name = "Carlos",
                    surname = "López",
                    categoryName = "Plomería",
                    profilePhotoUrl = null,
                ),
                description = "Reparación de pérdida en cocina",
                amountCents = 1_500_000L,
                scheduledOnEpochMillis = 1_792_074_600_000L,
                createdOnEpochMillis = 1_788_434_364_640L,
            ),
            providerName = "Carlos López",
            depositCents = null,
            currency = "ARS",
        )

    private fun localizedString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resourceId)
}

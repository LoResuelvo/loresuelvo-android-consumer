package com.loresuelvo.consumer.ui.screens.workorder

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
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalStatus
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.workorder.WorkOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [WorkOrderScreen] (US-54 scenario
 * 16-VSP). Runs on the JVM via [RobolectricTestRunner] so it
 * participates in the fast `./gradlew testDevDebugUnitTest` task
 * without needing an emulator.
 *
 * Pins the four observable branches:
 *  - Loading → spinner + loading copy.
 *  - Ready (with estimated duration) → provider, category,
 *    amount, date, estimated duration, description, status
 *    rows render.
 *  - Ready (no estimated duration) → the estimated-duration
 *    row is hidden so the screen never shows a "0 min" stub.
 *  - NotFound → not-found copy.
 *  - Error (Network / Server) → typed copy + retry CTA.
 *
 * Locale is pinned to `es-rAR` so the localised copy matches
 * the `values/strings.xml` strings the production app ships.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class WorkOrderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun localizedString(resourceId: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(resourceId)

    @Test
    fun ready_state_renders_every_pinned_field_with_the_formatters() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderScreen(
                        state = WorkOrderUiState.Ready(workOrder()),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(WORK_ORDER_SCREEN_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WORK_ORDER_TITLE_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(WORK_ORDER_READY_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_PROVIDER_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_CATEGORY_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_AMOUNT_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_DATE_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_ESTIMATED_DURATION_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_DESCRIPTION_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_STATUS_TAG).assertCountEquals(1)

        composeTestRule.onAllNodesWithText("Carlos López").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Plomería").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("$ 15.000").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("15/10/2026 - 14:30 hs").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("1 h 30 min").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Fuga en el lavamanos").assertCountEquals(1)
        composeTestRule.onAllNodesWithText(localizedString(R.string.work_order_status_pending))
            .assertCountEquals(1)
    }

    @Test
    fun ready_state_without_estimated_duration_hides_the_row() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderScreen(
                        state = WorkOrderUiState.Ready(workOrder(estimatedDurationMinutes = null)),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        // The seven other rows are still present.
        composeTestRule.onAllNodesWithTag(WORK_ORDER_PROVIDER_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_DESCRIPTION_TAG).assertCountEquals(1)

        // The estimated-duration row is absent: a `null` duration
        // must NOT default to "0 min" on the screen.
        composeTestRule.onAllNodesWithTag(WORK_ORDER_ESTIMATED_DURATION_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun loading_state_renders_spinner_and_loading_copy() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderScreen(
                        state = WorkOrderUiState.Loading,
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(WORK_ORDER_SCREEN_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WORK_ORDER_LOADING_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(localizedString(R.string.work_order_loading))
            .assertIsDisplayed()
    }

    @Test
    fun not_found_state_renders_the_not_found_copy() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderScreen(
                        state = WorkOrderUiState.NotFound,
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(WORK_ORDER_NOT_FOUND_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(localizedString(R.string.work_order_not_found))
            .assertIsDisplayed()
    }

    @Test
    fun network_error_renders_network_copy_and_retry() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderScreen(
                        state = WorkOrderUiState.Error(ServiceProposalsOutcome.Failure.Network(java.io.IOException())),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(WORK_ORDER_ERROR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WORK_ORDER_ERROR_RETRY_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(localizedString(R.string.work_order_error_network))
            .assertIsDisplayed()
    }

    @Test
    fun server_error_renders_server_copy_and_retry() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderScreen(
                        state = WorkOrderUiState.Error(
                            ServiceProposalsOutcome.Failure.Server(500, "down for maintenance"),
                        ),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(WORK_ORDER_ERROR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WORK_ORDER_ERROR_RETRY_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(localizedString(R.string.work_order_error_server))
            .assertIsDisplayed()
    }

    private fun workOrder(
        estimatedDurationMinutes: Int? = 90,
    ): WorkOrder = WorkOrder(
        proposalId = "wo-1",
        providerName = "Carlos López",
        categoryName = "Plomería",
        description = "Fuga en el lavamanos",
        amountCents = 1_500_000L,
        scheduledOnEpochMillis = 1_792_074_600_000L,
        estimatedDurationMinutes = estimatedDurationMinutes,
        status = ServiceProposalStatus.Pending,
    )
}
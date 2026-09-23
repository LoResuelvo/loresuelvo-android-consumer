package com.loresuelvo.consumer.ui.screens.workorderdetail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.workorder.CompletionReport
import com.loresuelvo.consumer.domain.workorder.CompletionReportPhoto
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetail
import com.loresuelvo.consumer.domain.workorder.WorkOrderReview
import com.loresuelvo.consumer.ui.components.images.FULLSCREEN_IMAGE_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [WorkOrderDetailScreen] (US-54 scenario
 * 16-VSP, US-27 `visualize-turns-detail`). Runs on the JVM via
 * [RobolectricTestRunner] so it participates in the fast
 * `./gradlew testDevDebugUnitTest` task without needing an
 * emulator.
 *
 * Pins the four observable branches (US-54 16-VSP):
 *  - Loading → spinner + loading copy.
 *  - Ready (with estimated duration) → provider, category,
 *    amount, date, estimated duration, description, status
 *    rows render.
 *  - Ready (no estimated duration) → the estimated-duration
 *    row is hidden so the screen never shows a "0 min" stub.
 *  - NotFound → not-found copy.
 *  - Error (Network / Server) → typed copy + retry CTA.
 *
 * Plus the US-27 render assertions per scenario:
 *  - 03-VTD: scheduled → NO evidence section, NO pay CTA.
 *  - 04-VTD / 05-VTD: completion_report → evidence section + photos row.
 *  - 06-VTD: tapping a photo's testTag → lightbox overlay.
 *  - 07-VTD: paid + review → review section + paid-on row.
 *  - 08-VTD: paid, no review → NO review section.
 *  - 09-VTD: awaiting_payment → pay CTA, positioned ABOVE the
 *    provider row (the "antes de las categorías" invariant).
 *  - 10-VTD: scheduled, no completion_report → NO evidence.
 *
 * The BDD layer (`VisualizeTurnsDetailSteps`) pins the data flow
 * (VM → state); these Compose tests pin the render (state → UI).
 * Both layers are necessary: a regression that drops the
 * evidence section render still passes the BDD (it would only
 * fail if the `completionReport` field flipped to `null`).
 *
 * Locale is pinned to `es-rAR` so the localised copy matches
 * the `values/strings.xml` strings the production app ships.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class WorkOrderDetailScreenTest {

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
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(workOrder()),
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
        composeTestRule.onAllNodesWithText(localizedString(R.string.turno_status_pending))
            .assertCountEquals(1)
    }

    @Test
    fun ready_state_without_estimated_duration_hides_the_row() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(workOrder(estimatedDurationMinutes = null)),
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
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Loading,
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
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.NotFound,
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
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Error(ServiceProposalsOutcome.Failure.Network(java.io.IOException())),
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
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Error(
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
    ): WorkOrderDetail = WorkOrderDetail(
        proposalId = "wo-1",
        provider = com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart(
            id = "100",
            name = "Carlos",
            surname = "López",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        description = "Fuga en el lavamanos",
        amountCents = 1_500_000L,
        scheduledOnEpochMillis = 1_792_074_600_000L,
        acceptedOnEpochMillis = 1_788_434_364_640L,
        paidOnEpochMillis = null,
        completionReport = null,
        review = null,
        estimatedDurationMinutes = estimatedDurationMinutes,
        status = TurnoStatus.Pending,
    )

    /**
     * Builds a work order for the US-27 render assertions. Each
     * scenario sets only the field it cares about — every other
     * field defaults to the same shape `workOrder()` returns.
     */
    private fun workOrderForRender(
        status: TurnoStatus = TurnoStatus.Confirmed,
        paidOnEpochMillis: Long? = null,
        completionReport: CompletionReport? = null,
        review: WorkOrderReview? = null,
    ): WorkOrderDetail = workOrder().copy(
        status = status,
        paidOnEpochMillis = paidOnEpochMillis,
        completionReport = completionReport,
        review = review,
    )

    private fun sampleCompletionReport(): CompletionReport = CompletionReport(
        id = "17",
        description = "Trabajo finalizado y funcionamiento verificado.",
        reportedOnEpochMillis = 1_788_400_000_000L,
        images = listOf(
            CompletionReportPhoto(
                fileId = "uuid-1",
                originalName = "trabajo.jpg",
                url = "https://example.com/trabajo.jpg",
            ),
        ),
    )

    private fun sampleReview(): WorkOrderReview = WorkOrderReview(
        rating = 5,
        description = "Trabajo prolijo y excelente atención.",
    )

    // ---- US-27 render assertions per scenario -----------------

    /**
     * Scenario 03-VTD: scheduled work order does NOT render the
     * "Pagar saldo restante" CTA nor the "Evidencia de
     * finalización" section. The screen stays in its baseline
     * layout (provider / category / amount / date / description
     * / status).
     */
    @Test
    fun ready_state_with_scheduled_status_omits_pay_cta_and_evidence() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            workOrderForRender(status = TurnoStatus.Confirmed),
                        ),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(WORK_ORDER_READY_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(WORK_ORDER_PAY_NOW_TAG).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_EVIDENCE_SECTION_TAG).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_REVIEW_SECTION_TAG).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_PAID_ON_TAG).assertCountEquals(0)
    }

    /**
     * Scenario 04-VTD + 05-VTD: a non-null `completionReport`
     * surfaces the "Evidencia de finalización" section with the
     * description, the reported-on row, and the photos row.
     */
    @Test
    fun ready_state_with_completion_report_renders_evidence_section_and_photos() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            workOrderForRender(
                                status = TurnoStatus.AwaitingPayment,
                                completionReport = sampleCompletionReport(),
                            ),
                        ),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        // The Ready column vertically scrolls; the evidence
        // section + photo grid land below the default Robolectric
        // viewport (1024x768). `assertExists` is enough to pin
        // the render: the BDD layer (`ready_state_with_paid_*`)
        // pins the data-layer wiring separately.
        composeTestRule.onAllNodesWithTag(WORK_ORDER_EVIDENCE_SECTION_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_EVIDENCE_DESCRIPTION_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_EVIDENCE_PHOTOS_ROW_TAG).assertCountEquals(1)
        composeTestRule
            .onAllNodesWithTag(WORK_ORDER_EVIDENCE_PHOTO_TAG_PREFIX + "uuid-1")
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText("Trabajo finalizado y funcionamiento verificado.")
            .assertCountEquals(1)
    }

    /**
     * Scenario 06-VTD: the photo is in the semantics tree with
     * its own testTag so the instrumented suite can target it.
     * The click → lightbox transition is asserted by the
     * instrumented test (the Robolectric `clickable` inside a
     * `horizontalScroll` row does not register the click action
     * reliably for `performClick`, so this JVM-side test pins
     * the render only).
     */
    @Test
    fun ready_state_photo_is_present_in_the_evidence_row() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            workOrderForRender(
                                status = TurnoStatus.AwaitingPayment,
                                completionReport = sampleCompletionReport(),
                            ),
                        ),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        // Photo is in the semantics tree (assertExists works
        // below the viewport where assertIsDisplayed would fail).
        composeTestRule
            .onAllNodesWithTag(WORK_ORDER_EVIDENCE_PHOTO_TAG_PREFIX + "uuid-1")
            .assertCountEquals(1)
        // Lightbox is host-owned state — closed by default.
        composeTestRule.onAllNodesWithTag(FULLSCREEN_IMAGE_TAG).assertCountEquals(0)
    }

    /**
     * Scenario 07-VTD: a paid work order with a consumer review
     * surfaces the "Reseña" section (rating + description) AND
     * the "Fecha en que se saldó el pago" row.
     */
    @Test
    fun ready_state_with_paid_and_review_renders_review_and_paid_on() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            workOrderForRender(
                                status = TurnoStatus.Paid,
                                paidOnEpochMillis = 1_788_500_000_000L,
                                completionReport = sampleCompletionReport(),
                                review = sampleReview(),
                            ),
                        ),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        // The paid-on row sits in the upper part of the Ready
        // column; the review section + rating + description land
        // further down (below the Robolectric viewport). Assert
        // both ranges separately.
        composeTestRule.onAllNodesWithTag(WORK_ORDER_PAID_ON_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_REVIEW_SECTION_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_REVIEW_RATING_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_REVIEW_DESCRIPTION_TAG).assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText("Trabajo prolijo y excelente atención.")
            .assertCountEquals(1)
    }

    /**
     * Scenario 08-VTD: a paid work order with a `null` review
     * does NOT render the "Reseña" section but DOES still
     * render the paid-on row + evidence section.
     */
    @Test
    fun ready_state_with_paid_without_review_omits_review_section() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            workOrderForRender(
                                status = TurnoStatus.Paid,
                                paidOnEpochMillis = 1_788_500_000_000L,
                                completionReport = sampleCompletionReport(),
                            ),
                        ),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule.onAllNodesWithTag(WORK_ORDER_PAID_ON_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_REVIEW_SECTION_TAG).assertCountEquals(0)
    }

    /**
     * Scenario 09-VTD: awaiting_payment surfaces the
     * "Pagar saldo restante" CTA. The CTA renders ABOVE the
     * provider row in the layout (the Gherkin says "antes de
     * las categorías" — the same invariant as "above the
     * counterpart row" since the counterpart row sits in the
     * same category-list surface).
     */
    @Test
    fun ready_state_with_awaiting_payment_renders_pay_cta_above_provider_row() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            workOrderForRender(
                                status = TurnoStatus.AwaitingPayment,
                                completionReport = sampleCompletionReport(),
                            ),
                        ),
                        onRetry = {},
                        onBackClick = {},
                        onPayNow = {},
                    )
                }
            }
        }

        val payCtaNode = composeTestRule.onNodeWithTag(WORK_ORDER_PAY_NOW_TAG)
        payCtaNode.assertIsDisplayed()
        val providerNode = composeTestRule.onNodeWithTag(WORK_ORDER_PROVIDER_TAG)
        providerNode.assertIsDisplayed()

        val payCtaTop = payCtaNode.getBoundsInRoot().top
        val providerTop = providerNode.getBoundsInRoot().top
        assert(payCtaTop < providerTop) {
            "expected pay CTA (top=$payCtaTop) to render ABOVE the provider row (top=$providerTop)"
        }
    }

    /**
     * Scenario 10-VTD: a scheduled work order does NOT render
     * the evidence section even when other rows are populated.
     */
    @Test
    fun ready_state_with_scheduled_status_omits_evidence_section() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            // status = Confirmed (scheduled) with
                            // a populated completionReport that
                            // the screen must still ignore.
                            workOrderForRender(
                                status = TurnoStatus.Confirmed,
                                completionReport = sampleCompletionReport(),
                            ),
                        ),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule.onAllNodesWithTag(WORK_ORDER_EVIDENCE_SECTION_TAG).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_PAY_NOW_TAG).assertCountEquals(0)
    }
}
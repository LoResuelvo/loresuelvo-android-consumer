package com.loresuelvo.consumer.ui.screens.workorderdetail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import org.junit.Assert.assertEquals
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


    @Test
    fun ready_state_with_paid_and_no_review_renders_rate_cta() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            workOrderForRender(
                                status = TurnoStatus.Paid,
                                paidOnEpochMillis = 1_788_500_000_000L,
                                completionReport = sampleCompletionReport(),
                                review = null,
                            ),
                        ),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_CTA_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(WORK_ORDER_RATE_COMPOSER_TAG).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_REVIEW_SECTION_TAG).assertCountEquals(0)
    }

    @Test
    fun ready_state_with_awaiting_payment_hides_rate_cta_and_composer() {
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

        composeTestRule.onAllNodesWithTag(WORK_ORDER_RATE_CTA_TAG).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_RATE_COMPOSER_TAG).assertCountEquals(0)
    }

    @Test
    fun ready_state_with_existing_review_hides_rate_cta_and_composer() {
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

        composeTestRule.onAllNodesWithTag(WORK_ORDER_RATE_CTA_TAG).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(WORK_ORDER_RATE_COMPOSER_TAG).assertCountEquals(0)
        // The read-only review section lands below the visible
        // viewport in the Ready column. `assertCountEquals`
        // pins the render without forcing the test rig to
        // scroll — same pattern as `ready_state_with_paid_*`.
        composeTestRule.onAllNodesWithTag(WORK_ORDER_REVIEW_SECTION_TAG).assertCountEquals(1)
    }

    @Test
    fun ready_state_with_composer_editing_renders_composer_with_submit_disabled() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            workOrderForRender(
                                status = TurnoStatus.Paid,
                                paidOnEpochMillis = 1_788_500_000_000L,
                                completionReport = sampleCompletionReport(),
                                review = null,
                            ),
                            composer = ReviewComposerState.Editing(),
                        ),
                        onRetry = {},
                        onBackClick = {},
                    )
                }
            }
        }

        // CTA hidden now that the composer took over.
        composeTestRule.onAllNodesWithTag(WORK_ORDER_RATE_CTA_TAG).assertCountEquals(0)
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_COMPOSER_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_TITLE_TAG).assertIsDisplayed()
        // 1..5 star buttons render.
        (1..5).forEach { star ->
            composeTestRule
                .onNodeWithTag(WORK_ORDER_RATE_STAR_TAG_PREFIX + star)
                .assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_COMMENT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_CHAR_COUNTER_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0/500").assertCountEquals(1)
        // Submit disabled while no rating is selected.
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_SUBMIT_TAG).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_CANCEL_TAG).assertIsEnabled()
    }

    /**
     * Pin the host-wiring contract: tapping the "Calificar
     * servicio" CTA dispatches the [onOpenReviewForm] callback
     * exactly once. The screen does not mutate its own state.
     */
    @Test
    fun ready_state_rate_cta_invokes_on_open_review_form() {
        var invocations = 0
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    WorkOrderDetailScreen(
                        state = WorkOrderDetailUiState.Ready(
                            workOrderForRender(
                                status = TurnoStatus.Paid,
                                paidOnEpochMillis = 1_788_500_000_000L,
                                completionReport = sampleCompletionReport(),
                                review = null,
                            ),
                        ),
                        onRetry = {},
                        onBackClick = {},
                        onOpenReviewForm = { invocations += 1 },
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_CTA_TAG).performClick()

        assertEquals(1, invocations)
        // Tapping the CTA does not mutate the screen — it
        // still shows the CTA (the state stays Hidden until
        // the VM flips it).
        composeTestRule.onNodeWithTag(WORK_ORDER_RATE_CTA_TAG).assertIsDisplayed()
    }
}

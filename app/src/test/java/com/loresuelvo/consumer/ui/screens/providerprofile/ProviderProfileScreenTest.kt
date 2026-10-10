package com.loresuelvo.consumer.ui.screens.providerprofile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.provider.ProviderCategory
import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderReview
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrder
import com.loresuelvo.consumer.domain.provider.ProviderWorkOrderStatus
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ProviderProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ready_state_renders_reputation_history_report_and_review() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                ProviderProfileScreen(
                    state = ProviderProfileUiState.Ready(sampleProfile()),
                    onRetryClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Juan Gómez").assertIsDisplayed()
        composeTestRule.onNodeWithTag("provider-verification-badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag(PROVIDER_PROFILE_RATING_STARS_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("4,5").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .getString(R.string.provider_profile_review_count, 2),
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag("provider-profile-work-order-84")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Reparación de pérdida").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trabajo finalizado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Excelente atención").assertIsDisplayed()
    }

    @Test
    fun ready_state_with_empty_history_renders_empty_copy() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                ProviderProfileScreen(
                    state = ProviderProfileUiState.Ready(sampleProfile().copy(workOrders = emptyList())),
                    onRetryClick = {},
                    onBackClick = {},
                )
            }
        }

        val emptyCopy = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.provider_profile_history_empty)
        composeTestRule.onNodeWithText(emptyCopy).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PROVIDER_PROFILE_WORK_ORDER_LIST_TAG).assertDoesNotExist()
    }

    @Test
    fun ready_state_with_no_reviews_renders_zero_reputation() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                ProviderProfileScreen(
                    state = ProviderProfileUiState.Ready(
                        sampleProfile().copy(ratingAverage = 0.0, ratingCount = 0),
                    ),
                    onRetryClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("0,0").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .getString(R.string.provider_profile_review_count, 0),
        ).assertIsDisplayed()
    }

    @Test
    fun completed_work_without_review_hides_review_block() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                ProviderProfileScreen(
                    state = ProviderProfileUiState.Ready(
                        sampleProfile().copy(
                            workOrders = listOf(sampleProfile().workOrders.first().copy(review = null)),
                        ),
                    ),
                    onRetryClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("provider-profile-work-order-84")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("provider-profile-review-84").assertDoesNotExist()
    }

    @Test
    fun public_history_has_no_private_data_slots() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                ProviderProfileScreen(
                    state = ProviderProfileUiState.Ready(sampleProfile()),
                    onRetryClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(PROVIDER_PROFILE_AMOUNT_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PROVIDER_PROFILE_CLIENT_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PROVIDER_PROFILE_EVIDENCE_PHOTOS_TAG).assertDoesNotExist()
    }

    @Test
    fun unverified_profile_does_not_show_verification_badge() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                ProviderProfileScreen(
                    state = ProviderProfileUiState.Ready(
                        sampleProfile().copy(identityVerified = false),
                    ),
                    onRetryClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("provider-verification-badge").assertDoesNotExist()
    }

    private fun sampleProfile() = ProviderProfile(
        id = 12,
        name = "Juan",
        surname = "Gómez",
        profilePhotoUrl = null,
        category = ProviderCategory(1, "Plomería"),
        ratingAverage = 4.5,
        ratingCount = 2,
        identityVerified = true,
        workOrders = listOf(
            ProviderWorkOrder(
                id = "84",
                scheduledOnEpochMillis = 1_755_270_000_000,
                description = "Reparación de pérdida",
                status = ProviderWorkOrderStatus.Paid,
                completionReport = com.loresuelvo.consumer.domain.provider.ProviderCompletionReport(
                    description = "Trabajo finalizado",
                    reportedOnEpochMillis = 1_755_273_600_000,
                ),
                review = ProviderReview(5, "Excelente atención"),
            ),
        ),
    )
}

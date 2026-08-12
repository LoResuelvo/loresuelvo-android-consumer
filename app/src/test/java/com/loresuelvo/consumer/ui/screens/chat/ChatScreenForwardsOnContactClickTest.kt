package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisAssessment
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the wire between [ChatScreen] and [DiagnosisSummaryCard]'s
 * `onContactClick` callback. The carousel button is already covered
 * by `DiagnosisSummaryCardCarouselTest.contact_button_invokes_callback_with_the_corresponding_provider`
 * (the carousel → `onContactClick` boundary), and the modal host
 * lives on `ChatRoute`. This test pins the middle layer:
 * `ChatScreen` MUST forward its `onContactClick` parameter straight
 * into `DiagnosisSummaryCard`, otherwise tapping Contactar in the
 * AI chat silently drops the click on the floor.
 *
 * Lives on the JVM via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ChatScreenForwardsOnContactClickTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val firstProvider = Provider(
        id = 1,
        name = "Ana",
        surname = "Pérez",
        categoryId = 1,
        categoryName = "Plomería",
        profilePhotoUrl = null,
    )

    private val secondProvider = Provider(
        id = 2,
        name = "Luis",
        surname = "Gómez",
        categoryId = 1,
        categoryName = "Plomería",
        profilePhotoUrl = null,
    )

    @Test
    fun chat_screen_forwards_carousel_contact_click_to_onContactClick_callback() {
        var contacted: Provider? = null
        composeTestRule.setContent {
            LoresuelvoTheme {
                ChatScreen(
                    promptInput = "",
                    canSend = false,
                    sending = false,
                    messages = emptyList(),
                    assessment = DiagnosisAssessment(
                        outcome = DiagnosisAssessment.OUTCOME_PROFESSIONAL_REQUIRED,
                        problemCategory = Category(1, "Plomería"),
                    ),
                    recommendedProviders = listOf(firstProvider, secondProvider),
                    transientError = null,
                    preliminaryWarningVisible = true,
                    onPromptChange = {},
                    onSendClick = {},
                    onRetryClick = {},
                    onErrorDismiss = {},
                    onContactClick = { contacted = it },
                    onBackClick = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeTestRule
            .onNodeWithTag("$CHAT_DIAGNOSIS_PROVIDER_CONTACT_TAG_PREFIX-1")
            .performClick()
        assertEquals(firstProvider, contacted)
    }
}

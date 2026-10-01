package com.loresuelvo.consumer.ui.screens.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.loresuelvo.consumer.domain.auth.RegisterConsumerAddress
import com.loresuelvo.consumer.domain.auth.User
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ConsumerProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun renders_account_data_and_photo_avatar() {
        setContent(
            ConsumerProfileUiState.Ready(
                User(
                    displayName = "Ana Perez",
                    firstName = "Ana",
                    lastName = "Perez",
                    email = "ana@example.com",
                    profilePhotoUrl = "https://cdn.test/ana.webp",
                    address = RegisterConsumerAddress("Tucuman", "123", "2", "B"),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Mi perfil").assertIsDisplayed()
        composeTestRule.onNodeWithTag("consumer-profile-avatar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("consumer-profile-name").assertIsDisplayed()
        composeTestRule.onNodeWithText("ana@example.com").assertExists()
        composeTestRule.onNodeWithText("Tucuman 123").assertExists()
    }

    @Test
    fun renders_initial_fallback_without_photo() {
        setContent(
            ConsumerProfileUiState.Ready(
                User(
                    displayName = "Ana Perez",
                    firstName = "Ana",
                    lastName = "Perez",
                ),
            ),
        )

        composeTestRule.onNodeWithTag("consumer-profile-avatar").assertIsDisplayed()
        composeTestRule.onNodeWithText("A").assertExists()
    }

    @Test
    fun error_state_exposes_retry_action() {
        var retried = false
        setContent(
            state = ConsumerProfileUiState.Error(
                ConsumerProfileFailure.Unauthorized("Token expired"),
            ),
            onRetryClick = { retried = true },
        )

        composeTestRule.onNodeWithTag("consumer-profile-error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("consumer-profile-retry")
            .assertHasClickAction()
            .performClick()

        assertTrue(retried)
    }

    private fun setContent(
        state: ConsumerProfileUiState,
        onRetryClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ConsumerProfileScreen(
                state = state,
                onRetryClick = onRetryClick,
            )
        }
    }
}

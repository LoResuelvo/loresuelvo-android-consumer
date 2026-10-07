package com.loresuelvo.consumer.ui.screens.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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

    @Test
    fun renders_undecided_notification_card_and_invokes_enable_action() {
        var actionClicked = false
        setContent(
            state = ConsumerProfileUiState.Ready(
                user = User(displayName = "Ana Perez", firstName = "Ana", lastName = "Perez"),
                notificationPermission = com.loresuelvo.consumer.domain.notifications.NotificationPermissionStatus.UNDECIDED,
            ),
            onNotificationActionClick = { actionClicked = true },
        )

        composeTestRule.onNodeWithTag("consumer-profile-content")
            .performScrollToNode(hasTestTag("consumer-profile-notifications"))
        composeTestRule.onNodeWithTag("consumer-profile-notifications").assertIsDisplayed()
        composeTestRule.onNodeWithTag("consumer-profile-notifications-title").assertExists()
        composeTestRule.onNodeWithTag("consumer-profile-notifications-action")
            .assertHasClickAction()
            .performClick()

        assertTrue(actionClicked)
    }

    @Test
    fun renders_granted_notification_card_and_invokes_settings_action() {
        var actionClicked = false
        setContent(
            state = ConsumerProfileUiState.Ready(
                user = User(displayName = "Ana Perez", firstName = "Ana", lastName = "Perez"),
                notificationPermission = com.loresuelvo.consumer.domain.notifications.NotificationPermissionStatus.GRANTED,
            ),
            onNotificationActionClick = { actionClicked = true },
        )

        composeTestRule.onNodeWithTag("consumer-profile-content")
            .performScrollToNode(hasTestTag("consumer-profile-notifications"))
        composeTestRule.onNodeWithTag("consumer-profile-notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Los avisos están habilitados en este teléfono.").assertExists()
        composeTestRule.onNodeWithTag("consumer-profile-notifications-action")
            .assertHasClickAction()
            .performClick()

        assertTrue(actionClicked)
    }

    @Test
    fun renders_denied_notification_card_and_invokes_settings_action() {
        var actionClicked = false
        setContent(
            state = ConsumerProfileUiState.Ready(
                user = User(displayName = "Ana Perez", firstName = "Ana", lastName = "Perez"),
                notificationPermission = com.loresuelvo.consumer.domain.notifications.NotificationPermissionStatus.DENIED,
            ),
            onNotificationActionClick = { actionClicked = true },
        )

        composeTestRule.onNodeWithTag("consumer-profile-content")
            .performScrollToNode(hasTestTag("consumer-profile-notifications"))
        composeTestRule.onNodeWithTag("consumer-profile-notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Los avisos están desactivados. Podés habilitarlos desde los ajustes del sistema.").assertExists()
        composeTestRule.onNodeWithTag("consumer-profile-notifications-action")
            .assertHasClickAction()
            .performClick()

        assertTrue(actionClicked)
    }

    private fun setContent(
        state: ConsumerProfileUiState,
        onRetryClick: () -> Unit = {},
        onNotificationActionClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ConsumerProfileScreen(
                state = state,
                onRetryClick = onRetryClick,
                onNotificationActionClick = onNotificationActionClick,
            )
        }
    }
}

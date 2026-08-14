package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.ui.screens.professional.PROVIDER_AVATAR_TAG
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ConversationTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun header_displays_provider_name_category_and_profile_photo() {
        val counterpart = ConversationCounterpart(
            id = 1L,
            name = "Juan",
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = "https://cdn.loresuelvo.test/jp.jpg",
        )

        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ConversationTopBar(
                        counterpart = counterpart,
                        status = ConversationStatus.Pending,
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TOP_BAR_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Juan Pérez")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Plomería")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(PROVIDER_AVATAR_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("provider-profile-photo-Juan")
            .assertIsDisplayed()
    }

    @Test
    fun header_displays_provider_initial_when_profile_photo_is_missing() {
        val counterpart = ConversationCounterpart(
            id = 1L,
            name = "Juan",
            surname = "Pérez",
            categoryName = "Plomería",
            profilePhotoUrl = null,
        )

        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ConversationTopBar(
                        counterpart = counterpart,
                        status = ConversationStatus.Pending,
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TOP_BAR_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Juan Pérez")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Plomería")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(PROVIDER_AVATAR_TAG)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("J")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("provider-profile-photo-Juan")
            .assertDoesNotExist()
    }
}
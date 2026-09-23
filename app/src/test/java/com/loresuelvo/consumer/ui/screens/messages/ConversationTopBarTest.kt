package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    /**
     * US-27 scenario 02-VTD: when the conversation has an
     * associated work order, the top bar surfaces the
     * "Ver orden" icon button (the host wires the click → nav to
     * `Route.WorkOrderDetail`). The button is the only way the
     * chat screen exposes navigation into the work-order
     * surface, so its render contract is pinned here.
     */
    @Test
    fun view_work_order_cta_renders_when_handler_is_provided() {
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
                        status = ConversationStatus.Other("accepted"),
                        onBackClick = {},
                        onViewWorkOrder = {},
                    )
                }
            }
        }

        val cta = composeTestRule
            .onNodeWithTag(CONVERSATION_TOP_BAR_VIEW_WORK_ORDER_TAG)
        cta.assertIsDisplayed()
        cta.assertHasClickAction()
    }

    /**
     * Pre-acceptance or work-order-less conversations do NOT
     * render the CTA. The host passes `onViewWorkOrder = null`
     * when `conversation.workOrderId == null`; the top bar must
     * not surface the button (otherwise it would 404 on tap).
     */
    @Test
    fun view_work_order_cta_hidden_when_handler_is_null() {
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
                        onViewWorkOrder = null,
                    )
                }
            }
        }

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_TOP_BAR_VIEW_WORK_ORDER_TAG)
            .assertCountEquals(0)
    }

    /**
     * Clicking the CTA fires the host callback. The host owns
     * navigation (the test cannot navigate to a real route), so
     * the assertion is on the click handler firing rather than
     * the resulting route change.
     */
    @Test
    fun view_work_order_cta_click_invokes_handler() {
        var clicked = false
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
                        status = ConversationStatus.Other("accepted"),
                        onBackClick = {},
                        onViewWorkOrder = { clicked = true },
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_TOP_BAR_VIEW_WORK_ORDER_TAG)
            .performClick()
        org.junit.Assert.assertTrue(clicked)
    }
}
package com.loresuelvo.consumer.integration.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.loresuelvo.consumer.ui.screens.auth.components.HowItWorksStep
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Rule
import org.junit.Test

/**
 * Render test for [HowItWorksStep]: the numbered badge, the title
 * and the description are all shown.
 */
class HowItWorksStepTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun renders_number_title_and_description() {

        composeTestRule.setContent {
            LoresuelvoTheme {
                HowItWorksStep(
                    number = 2,
                    title = "Diagnóstico con IA",
                    description = "Identificamos qué necesitás y a quién.",
                )
            }
        }

        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Diagnóstico con IA").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Identificamos qué necesitás y a quién.")
            .assertIsDisplayed()
    }
}

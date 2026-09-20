package com.loresuelvo.consumer.ui.screens.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.loresuelvo.consumer.ui.screens.home.components.EducationalEmptyCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [EducationalEmptyCard], the shared
 * empty-state card used by the Home dashboard for Diagnósticos
 * recientes, Mis Servicios (Home row) and Mis Turnos (andamiaje
 * until the endpoint lands).
 *
 * Pins:
 *  - The card renders the title, body and CTA exactly once
 *    each (a parent that re-uses the same empty-state must not
 *    duplicate the CTA by mistake).
 *  - Tapping the CTA invokes [onCtaClick] exactly once. Empty
 *    states that lose the click would silently swallow the
 *    conversion action, so we pin the wire explicitly.
 *
 * Runs on the JVM via [RobolectricTestRunner] so it participates
 * in `./gradlew testDevDebugUnitTest` without needing an
 * emulator.
 *
 * No locale qualifier: this component receives pre-resolved
 * strings from its caller, so the test is locale-independent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EducationalEmptyCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun renders_title_body_and_cta_exactly_once() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    EducationalEmptyCard(
                        title = "Probá el diagnóstico con IA",
                        body = "Contale qué te pasa y te decimos qué tipo de profesional necesitás. Tarda 30 segundos.",
                        ctaText = "Iniciar diagnóstico",
                        onCtaClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText("Probá el diagnóstico con IA")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "Contale qué te pasa y te decimos qué tipo de profesional necesitás. Tarda 30 segundos.",
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Iniciar diagnóstico")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun tapping_cta_invokes_onCtaClick_exactly_once() {
        var clicks = 0

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    EducationalEmptyCard(
                        title = "Probá el diagnóstico con IA",
                        body = "Contale qué te pasa y te decimos qué tipo de profesional necesitás.",
                        ctaText = "Iniciar diagnóstico",
                        onCtaClick = { clicks += 1 },
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Iniciar diagnóstico").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun modifier_test_tag_is_applied_to_the_root() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    EducationalEmptyCard(
                        title = "Probá el diagnóstico con IA",
                        body = "Contale qué te pasa y te decimos qué tipo de profesional necesitás.",
                        ctaText = "Iniciar diagnóstico",
                        onCtaClick = {},
                        modifier = Modifier.testTag("empty-card"),
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("empty-card").assertIsDisplayed()
    }
}

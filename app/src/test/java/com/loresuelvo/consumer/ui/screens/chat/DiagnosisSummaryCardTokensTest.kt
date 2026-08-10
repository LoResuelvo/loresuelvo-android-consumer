package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import com.loresuelvo.consumer.ui.theme.BrandSecondary
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the theme ↔ card contract for
 * [DiagnosisSummaryCard] so the card's emerald tint (the
 * `secondaryContainer` slot mapped to
 * [BrandSecondary] @ 12% alpha) never silently regresses to
 * Material's default `surfaceVariant` (which paints lilac/purple
 * on most devices).
 *
 * If a future commit flips the card back to `surfaceVariant`, or
 * rebinds `secondaryContainer` in the theme, one of these
 * assertions fails and the change is forced to be a conscious
 * decision in code review.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class DiagnosisSummaryCardTokensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun card_reads_containerColor_from_secondaryContainer_slot() {
        var observed: androidx.compose.ui.graphics.Color? = null
        composeTestRule.setContent {
            LoresuelvoTheme {
                observed = diagnosisSummaryContainerColor
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(
            BrandSecondary.copy(alpha = 0.12f),
            observed,
        )
    }

    @Test
    fun card_reads_contentColor_from_onSecondaryContainer_slot() {
        var observed: androidx.compose.ui.graphics.Color? = null
        composeTestRule.setContent {
            LoresuelvoTheme {
                observed = diagnosisSummaryContentColor
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(BrandSecondary, observed)
    }

    @Test
    fun theme_secondaryContainer_is_distinct_from_surfaceVariant() {
        // Belt-and-braces: confirms the theme's secondaryContainer
        // IS the emerald we expect, and that it differs from
        // surfaceVariant (which would be the lilac default). If a
        // future theme edit normalizes the two slots, both previous
        // tests would coincidentally still pass — this one catches
        // that by failing the moment the slots collide.
        composeTestRule.setContent {
            LoresuelvoTheme {
                val scheme = MaterialTheme.colorScheme
                assertEquals(
                    BrandSecondary.copy(alpha = 0.12f),
                    scheme.secondaryContainer,
                )
                // Material's surfaceVariant default is a tinted
                // lilac; under our theme it falls back to the
                // Material default because we never override it,
                // so it cannot equal the emerald container slot.
                assert(
                    scheme.secondaryContainer != scheme.surfaceVariant,
                ) {
                    "secondaryContainer must not collide with surfaceVariant"
                }
            }
        }
        composeTestRule.waitForIdle()
    }
}

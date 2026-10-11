package com.loresuelvo.consumer.ui.screens.professional

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ProfessionalsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun provider_card_displays_rating_and_review_count() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    ProfessionalsScreen(
                        state = ProfessionalsUiState.Ready(
                            categoryName = "Plomería",
                            providers = listOf(
                                provider(
                                    id = 1,
                                    name = "Juan",
                                    surname = "Pérez",
                                    ratingAverage = 4.8,
                                    ratingCount = 12,
                                ),
                            ),
                        ),
                        contactFormState = ContactProviderUiState.Closed,
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("provider-card-rating-1").assertIsDisplayed()
        composeTestRule.onNodeWithText("4,8 · 12 reseñas").assertIsDisplayed()
    }

    @Test
    fun provider_card_displays_no_rating_label_without_reviews() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    ProfessionalsScreen(
                        state = ProfessionalsUiState.Ready(
                            categoryName = "Plomería",
                            providers = listOf(provider(2, "Pedro", "Díaz", 0.0, 0)),
                        ),
                        contactFormState = ContactProviderUiState.Closed,
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Sin calificaciones").assertIsDisplayed()
    }

    private fun provider(
        id: Int,
        name: String,
        surname: String,
        ratingAverage: Double,
        ratingCount: Int,
    ) = Provider(
        id = id,
        name = name,
        surname = surname,
        categoryId = 1,
        categoryName = "Plomería",
        profilePhotoUrl = null,
        ratingAverage = ratingAverage,
        ratingCount = ratingCount,
    )
}

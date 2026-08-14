package com.loresuelvo.consumer.ui.screens.professional

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [ProviderAvatar]. Run on the JVM via
 * [RobolectricTestRunner] so they participate in the fast
 * `./gradlew testDevDebugUnitTest` task (no emulator needed).
 *
 * Pins the two visible modes:
 *  - **No photo URL** → no Coil node is emitted; only the initial
 *    letter on the brand-coloured circle is rendered.
 *  - **Photo URL present** → the Coil `provider-avatar-image`
 *    slot exists so the image loader kicks off; while the request
 *    is in flight the slot shows the initial fallback (no spinner,
 *    no blank circle).
 *
 * These assertions guard the regression that motivated the refactor:
 * previously `ProviderAvatar` always painted only the initial
 * letter and never attempted to load `profilePhotoUrl`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class ProviderAvatarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun no_photo_url_renders_initial_letter_and_skips_coil() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface(modifier = Modifier.testTag("host")) {
                    ProviderAvatar(
                        name = "Agustina",
                        profilePhotoUrl = null,
                    )
                }
            }
        }

        // The outer slot is always present.
        composeTestRule.onNodeWithTag(PROVIDER_AVATAR_TAG).assertIsDisplayed()
        // Initial letter is painted.
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        // No Coil image was kicked off.
        composeTestRule.onNodeWithTag("provider-profile-photo-Agustina")
            .assertDoesNotExist()
    }

    @Test
    fun blank_photo_url_is_treated_as_no_photo() {
        // Defensive: the wire may produce "" for a missing photo
        // instead of `null`. The avatar should still go straight to
        // the fallback path and not kick off a doomed request.
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ProviderAvatar(
                        name = "Juan",
                        profilePhotoUrl = "   ",
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(PROVIDER_AVATAR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("J").assertIsDisplayed()
        composeTestRule.onNodeWithTag("provider-profile-photo-Juan")
            .assertDoesNotExist()
    }

    @Test
    fun photo_url_kicks_off_coil_image_load() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ProviderAvatar(
                        name = "Agustina",
                        // Robolectric won't actually fetch this URL;
                        // we just need to assert the Coil slot is
                        // emitted so the image loader starts.
                        profilePhotoUrl = "http://example.com/photo.webp",
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(PROVIDER_AVATAR_TAG).assertIsDisplayed()
        // The Coil image slot is present even before the request
        // resolves (it paints the initial fallback underneath).
        composeTestRule.onNodeWithTag("provider-profile-photo-Agustina").assertIsDisplayed()
        // Initial is still painted underneath the image slot so the
        // avatar never looks blank while loading.
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun avatar_is_a_48dp_circle_by_default() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ProviderAvatar(
                        name = "A",
                        profilePhotoUrl = null,
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(PROVIDER_AVATAR_TAG)
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun avatar_respects_custom_size() {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ProviderAvatar(
                        name = "A",
                        profilePhotoUrl = null,
                        size = 96.dp,
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(PROVIDER_AVATAR_TAG)
            .assertWidthIsEqualTo(96.dp)
            .assertHeightIsEqualTo(96.dp)
    }

    @Test
    fun initial_letter_is_centred_inside_the_avatar_circle_when_no_photo_url() {
        // Regression for "letters appear top-left" reported during
        // manual testing on 2026-07-27. The initial must be inside
        // the avatar's bounding box, not flushed to a corner.
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ProviderAvatar(
                        name = "Agustina",
                        profilePhotoUrl = null,
                    )
                }
            }
        }

        val avatar = composeTestRule.onNodeWithTag(PROVIDER_AVATAR_TAG)
            .getBoundsInRoot()
        val letter = composeTestRule.onNodeWithText("A").getBoundsInRoot()

        assertTrue(
            "letter left must be inside avatar, was letter=$letter avatar=$avatar",
            letter.left >= avatar.left,
        )
        assertTrue(
            "letter right must be inside avatar, was letter=$letter avatar=$avatar",
            letter.right <= avatar.right,
        )
        assertTrue(
            "letter top must be inside avatar, was letter=$letter avatar=$avatar",
            letter.top >= avatar.top,
        )
        assertTrue(
            "letter bottom must be inside avatar, was letter=$letter avatar=$avatar",
            letter.bottom <= avatar.bottom,
        )
        // Centred (within Robolectric's pixel rounding tolerance).
        // `Dp` arithmetic on Compose uses `Float` internally so we
        // convert to raw pixels for the comparison.
        val avatarCenterX = (avatar.left.value + avatar.right.value) / 2f
        val avatarCenterY = (avatar.top.value + avatar.bottom.value) / 2f
        val letterCenterX = (letter.left.value + letter.right.value) / 2f
        val letterCenterY = (letter.top.value + letter.bottom.value) / 2f
        assertTrue(
            "letter must be horizontally centred, was " +
                "letterCenter=$letterCenterX avatarCenter=$avatarCenterX",
            kotlin.math.abs(letterCenterX - avatarCenterX) < 2f,
        )
        assertTrue(
            "letter must be vertically centred, was " +
                "letterCenter=$letterCenterY avatarCenter=$avatarCenterY",
            kotlin.math.abs(letterCenterY - avatarCenterY) < 2f,
        )
    }

    @Test
    fun initial_letter_is_centred_inside_the_avatar_circle_when_photo_url_present() {
        // The initial is always painted as the LAST child of the
        // outer Box so the Box's contentAlignment centres it even
        // when Coil is also rendering on top.
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    ProviderAvatar(
                        name = "Agustina",
                        profilePhotoUrl = "http://example.com/photo.webp",
                    )
                }
            }
        }

        val avatar = composeTestRule.onNodeWithTag(PROVIDER_AVATAR_TAG)
            .getBoundsInRoot()
        val letter = composeTestRule.onNodeWithText("A").getBoundsInRoot()

        val avatarCenterX = (avatar.left.value + avatar.right.value) / 2f
        val avatarCenterY = (avatar.top.value + avatar.bottom.value) / 2f
        val letterCenterX = (letter.left.value + letter.right.value) / 2f
        val letterCenterY = (letter.top.value + letter.bottom.value) / 2f
        assertTrue(
            "letter must be horizontally centred, was " +
                "letterCenter=$letterCenterX avatarCenter=$avatarCenterX",
            kotlin.math.abs(letterCenterX - avatarCenterX) < 2f,
        )
        assertTrue(
            "letter must be vertically centred, was " +
                "letterCenter=$letterCenterY avatarCenter=$avatarCenterY",
            kotlin.math.abs(letterCenterY - avatarCenterY) < 2f,
        )
    }
}
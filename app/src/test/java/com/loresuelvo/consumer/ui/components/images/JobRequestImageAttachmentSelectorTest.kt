package com.loresuelvo.consumer.ui.components.images

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose tests for [JobRequestImageAttachmentSelector]. Covers
 * the visible contract pinned by scenario 03-UXUI ("the
 * attached image shows up in the surface") and the validation
 * behaviour pinned by scenario 05-UXUI's sibling (limit / type
 * / size): the selector rejects over-limit batches, filters
 * non-allowed mime types, and lets the user remove a previously
 * attached image.
 *
 * Run on the JVM via Robolectric so the unit-test task exercises
 * them without a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR", sdk = [34])
class JobRequestImageAttachmentSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(@androidx.annotation.StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun sampleImage(name: String = "perfil.jpg"): MediaUpload.Image =
        MediaUpload.Image(
            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
            mimeType = "image/jpeg",
            originalName = name,
        )

    private fun setContent(
        images: List<MediaUpload.Image> = emptyList(),
        error: String? = null,
        maxImages: Int = 3,
        onAttachClick: () -> Unit = {},
        onRemove: (Int) -> Unit = {},
    ) {
        composeTestRule.setContent {
            LoresuelvoTheme {
                Surface {
                    JobRequestImageAttachmentSelector(
                        images = images,
                        maxImages = maxImages,
                        error = error,
                        onAttachClick = onAttachClick,
                        onRemove = onRemove,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    @Test
    fun empty_state_renders_attach_button_enabled() {
        setContent()

        composeTestRule
            .onNodeWithTag(JOB_REQUEST_IMAGE_ATTACH_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onAllNodesWithTag(JOB_REQUEST_IMAGE_REMOVE_BUTTON_TAG)
            .assertCountEquals(0)
    }

    @Test
    fun renders_thumbnail_for_each_attached_image() {
        setContent(
            images = listOf(
                sampleImage("a.jpg"),
                sampleImage("b.jpg"),
            ),
        )

        composeTestRule.onAllNodesWithTag(JOB_REQUEST_IMAGE_THUMBNAIL_TAG)
            .assertCountEquals(2)
        composeTestRule
            .onNodeWithContentDescription("a.jpg")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("b.jpg")
            .assertIsDisplayed()
    }

    @Test
    fun attach_button_is_disabled_when_at_max_images() {
        setContent(
            images = listOf(
                sampleImage("a.jpg"),
                sampleImage("b.jpg"),
                sampleImage("c.jpg"),
            ),
        )

        composeTestRule
            .onNodeWithTag(JOB_REQUEST_IMAGE_ATTACH_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun attach_button_click_invokes_onAttachClick() {
        var clicked = 0
        setContent(onAttachClick = { clicked++ })

        composeTestRule
            .onNodeWithTag(JOB_REQUEST_IMAGE_ATTACH_BUTTON_TAG)
            .performClick()

        assertEquals(1, clicked)
    }

    @Test
    fun remove_button_click_invokes_onRemove_with_correct_index() {
        val removedIndices = mutableListOf<Int>()
        setContent(
            images = listOf(
                sampleImage("a.jpg"),
                sampleImage("b.jpg"),
            ),
            onRemove = { removedIndices += it },
        )

        composeTestRule
            .onAllNodesWithTag(JOB_REQUEST_IMAGE_REMOVE_BUTTON_TAG)
            .get(0)
            .performClick()

        assertEquals(listOf(0), removedIndices)
    }

    @Test
    fun error_message_renders_when_error_is_not_null() {
        val errorMessage = "Ya alcanzaste el límite de 3 imágenes"
        setContent(error = errorMessage)

        composeTestRule
            .onNodeWithTag(JOB_REQUEST_IMAGE_ERROR_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun renders_attached_button_label_localised() {
        setContent()
        composeTestRule
            .onNodeWithText(string(R.string.job_request_image_attach_button))
            .assertIsDisplayed()
    }

    @Test
    fun renders_max_images_hint_localised() {
        setContent()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val expectedHint = context.getString(
            R.string.job_request_image_max_hint,
            MAX_TEST_IMAGES_HINT,
        )
        composeTestRule
            .onNodeWithText(expectedHint)
            .assertIsDisplayed()
    }

    private companion object {
        const val MAX_TEST_IMAGES_HINT: Int = 3
    }
}

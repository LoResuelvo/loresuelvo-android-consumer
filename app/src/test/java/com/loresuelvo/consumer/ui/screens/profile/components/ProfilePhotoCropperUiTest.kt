package com.loresuelvo.consumer.ui.screens.profile.components

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.exifinterface.media.ExifInterface
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import kotlin.math.abs
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es-rAR-w411dp-h891dp", sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfilePhotoCropperUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun camera_orientation_is_applied_to_the_confirmed_photo() {
        var confirmed: MediaUpload.Image? = null
        val photo = profilePhotoFixture(orientation = ExifInterface.ORIENTATION_ROTATE_90)
        compose.setContent {
            LoresuelvoTheme { ProfilePhotoCropper(photo, onConfirm = { confirmed = it }, onCancel = {}) }
        }

        compose.onNodeWithText("Usar esta foto").performScrollTo().assertIsEnabled().performClick()

        assertTopLeftIsBlue(confirmed)
    }

    @Test
    @Config(qualifiers = "es-rAR-w320dp-h480dp", sdk = [34])
    fun photo_controls_remain_accessible_on_a_small_screen() {
        var confirmed: MediaUpload.Image? = null
        val photo = profilePhotoFixture()
        compose.setContent {
            LoresuelvoTheme { ProfilePhotoCropper(photo, onConfirm = { confirmed = it }, onCancel = {}) }
        }

        compose.onNodeWithText("Ajustá tu foto").assertIsDisplayed()
        compose.onNodeWithText("Girar 90°").performScrollTo().performClick()
        compose.onNodeWithText("Usar esta foto").performScrollTo().assertIsEnabled().performClick()

        assertTopLeftIsBlue(confirmed)
    }

    @Test
    fun rotate_button_changes_the_pixels_of_the_confirmed_photo() {
        var confirmed: MediaUpload.Image? = null
        val photo = profilePhotoFixture()
        compose.setContent {
            LoresuelvoTheme { ProfilePhotoCropper(photo, onConfirm = { confirmed = it }, onCancel = {}) }
        }

        compose.onNodeWithText("Girar 90°").performScrollTo().performClick()
        compose.onNodeWithText("Usar esta foto").performScrollTo().assertIsEnabled().performClick()

        assertTopLeftIsBlue(confirmed)
    }

    private fun assertTopLeftIsBlue(photo: MediaUpload.Image?) {
        assertNotNull(photo)
        val bytes = photo!!.bytes
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val color = bitmap.getPixel(bitmap.width / 4, bitmap.height / 4)
        assertTrue("Expected the original blue bottom-left quadrant after rotation", abs(Color.blue(color) - 255) < 25)
        assertTrue(Color.red(color) < 25)
        bitmap.recycle()
    }

}

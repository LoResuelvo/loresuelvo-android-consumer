package com.loresuelvo.consumer.instrumented.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileAction
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileScreen
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileUiState
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePhotoCropperInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var confirmed: MediaUpload.Image? = null

    @Test
    fun camera_orientation_and_manual_rotation_are_preserved_in_the_profile_preview() {
        showEditor(photo(120, 80, ExifInterface.ORIENTATION_ROTATE_90))

        compose.onNodeWithTag("profile-photo-crop-rotate").performScrollTo().performClick()
        confirmPhoto()

        assertQuadrants(listOf(Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED))
        compose.onNodeWithTag("profile-photo-preview").assertIsDisplayed()
    }

    @Test
    fun a_large_camera_photo_can_be_rotated_and_cropped_without_blank_quadrants() {
        showEditor(photo(4096, 3072, ExifInterface.ORIENTATION_NORMAL))

        repeat(4) {
            compose.onNodeWithTag("profile-photo-crop-rotate").performScrollTo().performClick()
        }
        confirmPhoto()

        val bytes = checkNotNull(confirmed).bytes
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertTrue(bitmap.width <= 2048)
        assertEquals(bitmap.width, bitmap.height)
        bitmap.recycle()
        assertQuadrants(listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW))
        compose.onNodeWithTag("profile-photo-preview").assertIsDisplayed()
    }

    private fun showEditor(photo: MediaUpload.Image) {
        var state by mutableStateOf(CompleteProfileUiState(pendingProfilePhoto = photo))
        compose.setContent {
            LoresuelvoTheme {
                CompleteProfileScreen(state = state, onAction = { action ->
                    if (action is CompleteProfileAction.ProfilePhotoCropConfirmed) {
                        confirmed = action.photo
                        state = state.copy(profilePhoto = action.photo, pendingProfilePhoto = null)
                    }
                })
            }
        }
        compose.onNodeWithTag("profile-photo-crop-viewport").assertIsDisplayed()
    }

    private fun confirmPhoto() {
        compose.onNodeWithText(context.getString(R.string.complete_profile_photo_crop_confirm))
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        compose.waitUntil { confirmed != null }
    }

    private fun assertQuadrants(expected: List<Int>) {
        val bytes = checkNotNull(confirmed).bytes
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        expected.forEachIndexed { index, color ->
            val x = if (index % 2 == 0) bitmap.width / 4 else bitmap.width * 3 / 4
            val y = if (index / 2 == 0) bitmap.height / 4 else bitmap.height * 3 / 4
            val actual = bitmap.getPixel(x, y)
            assertTrue("Red quadrant $index", abs(Color.red(actual) - Color.red(color)) < 25)
            assertTrue("Green quadrant $index", abs(Color.green(actual) - Color.green(color)) < 25)
            assertTrue("Blue quadrant $index", abs(Color.blue(actual) - Color.blue(color)) < 25)
        }
        bitmap.recycle()
    }

    private fun photo(width: Int, height: Int, orientation: Int): MediaUpload.Image {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW).forEachIndexed { index, color ->
            paint.color = color
            val left = (index % 2) * width / 2f
            val top = (index / 2) * height / 2f
            canvas.drawRect(left, top, left + width / 2f, top + height / 2f, paint)
        }
        val file = File.createTempFile("profile-camera-", ".jpg", context.cacheDir)
        return try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
            MediaUpload.Image(file.readBytes(), "image/jpeg", "camera.jpg")
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }
}

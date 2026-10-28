package com.loresuelvo.consumer.ui.screens.profile.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfilePhotoBitmapTest {
    @Test
    fun all_eight_camera_orientations_preserve_the_correct_quadrants() {
        val red = Color.RED
        val green = Color.GREEN
        val blue = Color.BLUE
        val yellow = Color.YELLOW
        val expected = mapOf(
            ExifInterface.ORIENTATION_NORMAL to listOf(red, green, blue, yellow),
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to listOf(green, red, yellow, blue),
            ExifInterface.ORIENTATION_ROTATE_180 to listOf(yellow, blue, green, red),
            ExifInterface.ORIENTATION_FLIP_VERTICAL to listOf(blue, yellow, red, green),
            ExifInterface.ORIENTATION_TRANSPOSE to listOf(red, blue, green, yellow),
            ExifInterface.ORIENTATION_ROTATE_90 to listOf(blue, red, yellow, green),
            ExifInterface.ORIENTATION_TRANSVERSE to listOf(yellow, green, blue, red),
            ExifInterface.ORIENTATION_ROTATE_270 to listOf(green, yellow, red, blue),
        )
        expected.forEach { (orientation, colors) ->
            val bitmap = checkNotNull(decodeProfilePhotoBitmap(profilePhotoFixture(orientation = orientation).bytes))
            val rotated = orientation >= ExifInterface.ORIENTATION_TRANSPOSE
            assertEquals("Width for EXIF $orientation", if (rotated) 80 else 120, bitmap.width)
            assertEquals("Height for EXIF $orientation", if (rotated) 120 else 80, bitmap.height)
            assertQuadrants(bitmap, colors)
            bitmap.recycle()
        }
    }

    @Test
    fun a_large_camera_photo_is_sampled_without_losing_any_quadrant() {
        val photo = profilePhotoFixture(width = 4096, height = 3072)

        val bitmap = checkNotNull(decodeProfilePhotoBitmap(photo.bytes))

        assertTrue(bitmap.width <= PROFILE_PHOTO_MAX_DECODED_SIDE)
        assertTrue(bitmap.height <= PROFILE_PHOTO_MAX_DECODED_SIDE)
        assertQuadrants(bitmap, listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW))
        bitmap.recycle()
    }

    @Test
    fun unreadable_bytes_are_rejected_instead_of_showing_a_blank_editor() {
        assertNull(decodeProfilePhotoBitmap(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun manual_rotation_changes_the_pixels_and_a_full_turn_reuses_the_original() {
        val source = checkNotNull(decodeProfilePhotoBitmap(profilePhotoFixture().bytes))
        val rotated = rotateProfilePhotoBitmap(source, 1)

        assertEquals(source.height, rotated.width)
        assertEquals(source.width, rotated.height)
        assertQuadrants(rotated, listOf(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN))
        assertSame(source, rotateProfilePhotoBitmap(source, 4))
        rotated.recycle()
        source.recycle()
    }

    private fun assertQuadrants(bitmap: Bitmap, expected: List<Int>) {
        expected.forEachIndexed { index, color ->
            val x = if (index % 2 == 0) bitmap.width / 4 else bitmap.width * 3 / 4
            val y = if (index / 2 == 0) bitmap.height / 4 else bitmap.height * 3 / 4
            val actual = bitmap.getPixel(x, y)
            assertTrue("Red channel in quadrant $index", abs(Color.red(color) - Color.red(actual)) < 25)
            assertTrue("Green channel in quadrant $index", abs(Color.green(color) - Color.green(actual)) < 25)
            assertTrue("Blue channel in quadrant $index", abs(Color.blue(color) - Color.blue(actual)) < 25)
        }
    }
}

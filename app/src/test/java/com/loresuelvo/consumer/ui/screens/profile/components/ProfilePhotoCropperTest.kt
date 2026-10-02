package com.loresuelvo.consumer.ui.screens.profile.components

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfilePhotoCropperTest {

    @Test
    fun crop_returns_a_square_bitmap_from_a_landscape_photo() {
        val source = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)

        val cropped = cropProfilePhotoBitmap(
            source = source,
            viewportSizePx = 300,
            userScale = 1f,
            offset = Offset.Zero,
        )

        assertEquals(200, cropped.width)
        assertEquals(200, cropped.height)
        cropped.recycle()
        source.recycle()
    }

    @Test
    fun crop_zoom_reduces_the_selected_source_area() {
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)

        val cropped = cropProfilePhotoBitmap(
            source = source,
            viewportSizePx = 300,
            userScale = 2f,
            offset = Offset.Zero,
        )

        assertTrue(cropped.width < source.width)
        assertEquals(cropped.width, cropped.height)
        cropped.recycle()
        source.recycle()
    }
}

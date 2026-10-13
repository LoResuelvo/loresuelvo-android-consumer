package com.loresuelvo.consumer.platform.notifications

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
class NotificationIconAndroidTest {
    @Test fun loresuelvo_notification_mark_is_white_and_has_transparent_background() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val icon = requireNotNull(context.getDrawable(com.loresuelvo.consumer.R.drawable.ic_notification_loresuelvo))
        assertTrue(icon is LayerDrawable)
        val iconSize = (24 * context.resources.displayMetrics.density).toInt()
        val rendered = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, rendered.width, rendered.height)
        icon.draw(Canvas(rendered))

        assertEquals(Color.TRANSPARENT, rendered.getPixel(iconSize - 1, iconSize - 1))
        val paintedPixels = (0 until rendered.width).flatMap { x ->
            (0 until rendered.height).map { y -> rendered.getPixel(x, y) }
        }.filter { Color.alpha(it) != 0 }

        assertTrue("Logo should render visible pixels", paintedPixels.isNotEmpty())
        assertTrue("White tint should keep antialiased edges monochrome", paintedPixels.all {
            Color.red(it) == Color.green(it) && Color.green(it) == Color.blue(it)
        })
        assertTrue("Logo should contain an opaque white interior pixel", paintedPixels.any {
            Color.red(it) == 255 && Color.green(it) == 255 &&
                Color.blue(it) == 255 && Color.alpha(it) >= 250
        })
    }
}

package com.loresuelvo.consumer.ui.screens.profile.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import java.io.File

internal fun profilePhotoFixture(
    width: Int = 120,
    height: Int = 80,
    orientation: Int = ExifInterface.ORIENTATION_NORMAL,
): MediaUpload.Image {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW).forEachIndexed { index, color ->
        paint.color = color
        val left = (index % 2) * width / 2f
        val top = (index / 2) * height / 2f
        canvas.drawRect(left, top, left + width / 2f, top + height / 2f, paint)
    }
    val context = ApplicationProvider.getApplicationContext<Context>()
    val file = File.createTempFile("profile-orientation-", ".jpg", context.cacheDir)
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

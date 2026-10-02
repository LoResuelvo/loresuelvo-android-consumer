package com.loresuelvo.consumer.ui.screens.profile.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val PROFILE_PHOTO_CROP_MAX_SCALE = 4f
private const val PROFILE_PHOTO_JPEG_QUALITY = 90

/**
 * WhatsApp-style circular crop editor for the profile photo.
 *
 * The image is rendered behind a circular viewport. A drag changes the
 * position and a pinch changes the zoom; the resulting square crop is
 * encoded before it is handed back to the profile flow.
 */
@Composable
fun ProfilePhotoCropper(
    photo: MediaUpload.Image,
    onConfirm: (MediaUpload.Image) -> Unit,
    onCancel: () -> Unit,
) {
    val bitmap = remember(photo.bytes) {
        BitmapFactory.decodeByteArray(photo.bytes, 0, photo.bytes.size)
    }

    if (bitmap == null) {
        LaunchedEffect(photo.bytes) { onCancel() }
        return
    }

    var viewportSize by remember(bitmap) { mutableStateOf(IntSize.Zero) }
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.complete_profile_photo_crop_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.complete_profile_photo_crop_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .onSizeChanged { viewportSize = it }
                        .pointerInput(bitmap, viewportSize) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(
                                    1f,
                                    PROFILE_PHOTO_CROP_MAX_SCALE,
                                )
                                val nextOffset = offset + pan
                                scale = nextScale
                                offset = nextOffset.constrainToImage(
                                    source = bitmap,
                                    viewport = viewportSize,
                                    scale = nextScale,
                                )
                            }
                        }
                        .testTag("profile-photo-crop-viewport"),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(
                            R.string.complete_profile_photo_preview,
                        ),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                    )
                    CropOverlay(modifier = Modifier.fillMaxSize())
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.complete_profile_photo_crop_cancel))
                    }
                    Button(
                        onClick = {
                            val cropped = cropProfilePhotoBitmap(
                                source = bitmap,
                                viewportSizePx = minOf(viewportSize.width, viewportSize.height),
                                userScale = scale,
                                offset = offset,
                            )
                            onConfirm(cropped.toProfilePhotoUpload())
                            cropped.recycle()
                        },
                        enabled = minOf(viewportSize.width, viewportSize.height) > 0,
                    ) {
                        Text(stringResource(R.string.complete_profile_photo_crop_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun CropOverlay(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        val radius = size.minDimension / 2f - 2.dp.toPx()
        drawRect(Color.Black.copy(alpha = 0.56f))
        drawCircle(
            color = Color.Transparent,
            radius = radius,
            center = center,
            blendMode = BlendMode.Clear,
        )
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
        )
    }
}

internal fun cropProfilePhotoBitmap(
    source: Bitmap,
    viewportSizePx: Int,
    userScale: Float,
    offset: Offset,
): Bitmap {
    require(viewportSizePx > 0) { "viewportSizePx must be positive" }

    val baseScale = max(
        viewportSizePx.toFloat() / source.width,
        viewportSizePx.toFloat() / source.height,
    )
    val totalScale = baseScale * userScale.coerceAtLeast(1f)
    val cropSize = (viewportSizePx / totalScale)
        .roundToInt()
        .coerceAtLeast(1)
        .coerceAtMost(minOf(source.width, source.height))
    val sourceCenterX = source.width / 2f - offset.x / totalScale
    val sourceCenterY = source.height / 2f - offset.y / totalScale
    val left = (sourceCenterX - cropSize / 2f)
        .roundToInt()
        .coerceIn(0, source.width - cropSize)
    val top = (sourceCenterY - cropSize / 2f)
        .roundToInt()
        .coerceIn(0, source.height - cropSize)

    return Bitmap.createBitmap(source, left, top, cropSize, cropSize)
}

private fun Offset.constrainToImage(
    source: Bitmap,
    viewport: IntSize,
    scale: Float,
): Offset {
    if (minOf(viewport.width, viewport.height) <= 0) return this
    val baseScale = max(
        viewport.width.toFloat() / source.width,
        viewport.height.toFloat() / source.height,
    )
    val renderedWidth = source.width * baseScale * scale
    val renderedHeight = source.height * baseScale * scale
    val maxX = max(0f, (renderedWidth - viewport.width) / 2f)
    val maxY = max(0f, (renderedHeight - viewport.height) / 2f)
    return copy(
        x = x.coerceIn(-maxX, maxX),
        y = y.coerceIn(-maxY, maxY),
    )
}

private fun Bitmap.toProfilePhotoUpload(): MediaUpload.Image {
    val bytes = ByteArrayOutputStream().use { output ->
        check(compress(Bitmap.CompressFormat.JPEG, PROFILE_PHOTO_JPEG_QUALITY, output)) {
            "Could not encode cropped profile photo"
        }
        output.toByteArray()
    }
    return MediaUpload.Image(
        bytes = bytes,
        mimeType = "image/jpeg",
        originalName = "profile-photo.jpg",
    )
}

package com.loresuelvo.consumer.ui.components.images

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.loresuelvo.consumer.R

/**
 * Fullscreen image viewer for the work-order evidence photos
 * (US-27 `visualize-turns-detail` scenario 06-VTD). Reusable
 * viewer for any URL + filename pair — the existing chat
 * viewers in `ui/screens/chat/FullScreenImageViewer.kt`
 * specialise on `MediaReference.Image` / `ChatImage`; this
 * one is the canonical URL+name overload that callers from any
 * feature can import.
 *
 * Stateless: the host owns the visibility flag. When the host
 * renders this composable, the photo fills the screen on a
 * black scrim; tapping the scrim (or the image itself)
 * triggers [onDismiss].
 *
 * Compose testTags mirror the chat viewer (`*-fullscreen-image`)
 * so any instrumented suite can assert the overlay opens +
 * closes consistently across surfaces.
 */
@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    imageName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss,
            )
            .testTag(FULLSCREEN_IMAGE_TAG),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = imageName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .testTag(FULLSCREEN_IMAGE_CONTENT_TAG),
            loading = {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            },
            error = {
                Icon(
                    imageVector = Icons.Filled.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(64.dp),
                )
            },
        )
        Text(
            text = stringResource(R.string.work_order_lightbox_close_hint),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag(FULLSCREEN_IMAGE_HINT_TAG),
        )
    }
}

const val FULLSCREEN_IMAGE_TAG: String = "work-order-fullscreen-image"
const val FULLSCREEN_IMAGE_CONTENT_TAG: String = "work-order-fullscreen-image-content"
const val FULLSCREEN_IMAGE_HINT_TAG: String = "work-order-fullscreen-image-hint"

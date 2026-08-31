package com.loresuelvo.consumer.ui.components.images

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.jobrequest.MAX_JOB_REQUEST_IMAGES

/**
 * Reusable image attachment surface for the "create job
 * request" form (scenario 03-UXUI). Stateless: the parent owns
 * the image list and reacts to [onAttachClick] (typically a
 * gallery / camera picker launcher) and [onRemove].
 *
 * Mirrors the webapp's `ImageAttachmentSelector`:
 *  - Caps the visible list at [maxImages] (default
 *    [MAX_JOB_REQUEST_IMAGES] = 3); the attach button disables
 *    once the limit is reached so the consumer cannot stage
 *    more than the cap.
 *  - Each thumbnail exposes a small `×` chip that calls
 *    [onRemove] with the index of the staged image in [images].
 *  - When [error] is non-null the inline error message renders
 *    below the button so the consumer sees why the previous
 *    attach attempt was rejected.
 *
 * The picker contract lives in the host (the bottom sheet that
 * owns the `MediaReader` / `PickVisualMedia` launcher). Keeping
 * it outside the selector lets the same component be reused
 * for any future "attach images to X" surface without dragging
 * Android launchers into a stateless composable.
 */
@Composable
fun JobRequestImageAttachmentSelector(
    images: List<MediaUpload.Image>,
    onAttachClick: () -> Unit,
    onRemove: (Int) -> Unit,
    maxImages: Int = MAX_JOB_REQUEST_IMAGES,
    error: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (images.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = images, key = { it.originalName }) { image ->
                    ImageThumbnail(
                        image = image,
                        onRemove = { onRemove(images.indexOf(image)) },
                    )
                }
            }
        }

        Button(
            onClick = onAttachClick,
            enabled = images.size < maxImages,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(JOB_REQUEST_IMAGE_ATTACH_BUTTON_TAG),
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.job_request_image_attach_button),
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = stringResource(R.string.job_request_image_max_hint, maxImages),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(JOB_REQUEST_IMAGE_ERROR_TAG),
            )
        }
    }
}

@Composable
private fun ImageThumbnail(
    image: MediaUpload.Image,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag(JOB_REQUEST_IMAGE_THUMBNAIL_TAG),
        contentAlignment = Alignment.Center,
    ) {
        // Project doesn't ship an image loader yet (the
        // AsyncImage swap is a follow-up). The placeholder
        // mirrors the chat's `MediaPreviewCard` shape so the
        // two surfaces feel consistent.
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = image.originalName,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .testTag(JOB_REQUEST_IMAGE_REMOVE_BUTTON_TAG),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(
                    R.string.job_request_image_remove_content_description,
                ),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Compose testTag for the attach button. Disabled when the
 * consumer has reached [JobRequestImageAttachmentSelector.maxImages].
 */
const val JOB_REQUEST_IMAGE_ATTACH_BUTTON_TAG: String =
    "job-request-image-attach-button"

/** Compose testTag applied to every thumbnail in the list. */
const val JOB_REQUEST_IMAGE_THUMBNAIL_TAG: String = "job-request-image-thumbnail"

/** Compose testTag for the per-thumbnail remove (×) chip. */
const val JOB_REQUEST_IMAGE_REMOVE_BUTTON_TAG: String =
    "job-request-image-remove-button"

/** Compose testTag for the inline error message slot. */
const val JOB_REQUEST_IMAGE_ERROR_TAG: String = "job-request-image-error"

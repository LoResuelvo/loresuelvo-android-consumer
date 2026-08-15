package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

/**
 * Card pinned above the input bar when a media attachment is
 * staged (the user picked a file but hasn't tapped "Enviar" yet).
 * Shows a thumbnail placeholder (the project does not pull in a
 * Coil-style image loader yet, so the actual `Uri` is not
 * decoded), the file name + mime type, and two actions:
 *  - **Enviar** ([onSendClick]) — confirms the upload.
 *  - **Descartar** ([onDiscardClick]) — clears the staged media.
 *
 * When [sending] is `true`, the action row is disabled and a
 * small spinner is rendered in place of the icon so the user
 * reads the in-flight state at a glance. The card stays mounted
 * during the upload so a failure can render the `Send / Descartar`
 * row again without a recomposition that would lose the user's
 * preview context.
 */
@Composable
fun MediaPreviewCard(
    pendingMedia: PendingMedia,
    sending: Boolean,
    onSendClick: () -> Unit,
    onDiscardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(MEDIA_PREVIEW_CARD_TAG),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Thumbnail placeholder. The project doesn't ship an
            // image loader yet (avoiding the ~1.5 MB Coil
            // dependency for a 01-MM feature), so the bubble
            // shows an image-icon glyph over a tinted square.
            // A future commit swaps the placeholder for an
            // `AsyncImage(model = pendingMedia.localUri)` once
            // the loader is in place.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .testTag(MEDIA_PREVIEW_THUMBNAIL_TAG),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = stringResource(
                        R.string.conversation_media_preview_image_content_description,
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = pendingMedia.originalName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(MEDIA_PREVIEW_NAME_TAG),
                )
                Text(
                    text = stringResource(
                        R.string.conversation_media_preview_file_name_format,
                        pendingMedia.mimeType,
                        humanReadableSize(pendingMedia.sizeBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            if (sending) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(28.dp)
                        .testTag(MEDIA_PREVIEW_SPINNER_TAG),
                    strokeWidth = 2.dp,
                )
            } else {
                ActionRow(
                    onSendClick = onSendClick,
                    onDiscardClick = onDiscardClick,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    onSendClick: () -> Unit,
    onDiscardClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            onClick = onDiscardClick,
            modifier = Modifier.testTag(MEDIA_PREVIEW_DISCARD_TAG),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Text(
                text = stringResource(R.string.conversation_media_preview_discard),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Surface(
            onClick = onSendClick,
            modifier = Modifier.testTag(MEDIA_PREVIEW_SEND_TAG),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Text(
                text = stringResource(R.string.conversation_media_preview_send),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

private fun humanReadableSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.2f MB".format(mb)
}

const val MEDIA_PREVIEW_CARD_TAG: String = "media-preview-card"
const val MEDIA_PREVIEW_THUMBNAIL_TAG: String = "media-preview-thumbnail"
const val MEDIA_PREVIEW_NAME_TAG: String = "media-preview-name"
const val MEDIA_PREVIEW_SEND_TAG: String = "media-preview-send"
const val MEDIA_PREVIEW_DISCARD_TAG: String = "media-preview-discard"
const val MEDIA_PREVIEW_SPINNER_TAG: String = "media-preview-spinner"
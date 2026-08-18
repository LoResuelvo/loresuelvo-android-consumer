package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

/**
 * Bottom sheet that surfaces the attach sources (gallery, camera,
 * audio recording) when the user taps the `+` button on the
 * conversation input bar. 01-MM only wires the gallery option —
 * camera (02-MM) and audio (03-MM) entries are rendered as
 * disabled affordances so the layout is forward-compatible, but
 * their `onClick` callbacks are deliberately left as `null` so a
 * future commit wires them in instead of leaking an
 * "almost-impl" through the production code path.
 *
 * The host (the conversation route) owns the visibility state
 * (a `remember { mutableStateOf(false) }`); this composable is
 * pure — when [show] flips to `false`, the sheet slides off and
 * the composition is removed.
 *
 * Stateless: every entry routes the user's tap through a
 * nullable callback. When a callback is null, the row is
 * rendered with `onClick = null` (effectively disabled) so the
 * testTags remain in the tree and the BDD can assert the visual
 * contract even before the next scenario lands the impl.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaAttachSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onGalleryClick: (() -> Unit)?,
    onCameraClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (!show) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag(MEDIA_ATTACH_SHEET_TAG),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MediaAttachEntry(
                icon = Icons.Filled.Image,
                label = stringResource(R.string.conversation_attach_gallery),
                onClick = onGalleryClick,
                testTag = MEDIA_ATTACH_GALLERY_ROW_TAG,
            )
            // Camera (02-MM) is wired. Audio (03-MM) is still a
            // placeholder — rendered disabled until the
            // recording flow lands.
            MediaAttachEntry(
                icon = Icons.Filled.PhotoCamera,
                label = stringResource(R.string.conversation_attach_camera),
                onClick = onCameraClick,
                testTag = MEDIA_ATTACH_CAMERA_ROW_TAG,
            )
        }
    }
}

@Composable
private fun MediaAttachEntry(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    testTag: String,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (onClick != null) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.38f)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (onClick != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
    }
}

const val MEDIA_ATTACH_SHEET_TAG: String = "media-attach-sheet"
const val MEDIA_ATTACH_GALLERY_ROW_TAG: String = "media-attach-gallery-row"
const val MEDIA_ATTACH_CAMERA_ROW_TAG: String = "media-attach-camera-row"
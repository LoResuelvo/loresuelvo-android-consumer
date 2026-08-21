package com.loresuelvo.consumer.ui.screens.chat

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
import com.loresuelvo.consumer.domain.conversation.MediaReference

/**
 * Fullscreen image overlay for the conversation screen (06-MM).
 *
 * Stateless — the host (the conversation route) owns the
 * visibility via a `MediaReference.Image?` field on
 * [ConversationUiState.Ready]. When the field flips to non-null,
 * the overlay renders the image over a translucent scrim and
 * intercepts taps on the scrim to dismiss. The image itself is
 * not clickable so a tap on the picture area dismisses too
 * (matches WhatsApp / Telegram behaviour for image previews).
 *
 * Out of scope for this commit: pinch-to-zoom, swipe-down to
 * dismiss, and downloading the image. The 06-MM Gherkin only
 * requires "se abre en pantalla completa".
 */
@Composable
fun FullScreenImageViewer(
    image: MediaReference.Image,
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
            .testTag(CONVERSATION_FULLSCREEN_IMAGE_TAG),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = image.url,
            contentDescription = image.originalName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .testTag(CONVERSATION_FULLSCREEN_IMAGE_CONTENT_TAG),
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
            text = stringResource(
                R.string.conversation_fullscreen_image_close_hint,
            ),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag(CONVERSATION_FULLSCREEN_IMAGE_HINT_TAG),
        )
    }
}

const val CONVERSATION_FULLSCREEN_IMAGE_TAG =
    "conversation-fullscreen-image"

const val CONVERSATION_FULLSCREEN_IMAGE_CONTENT_TAG =
    "conversation-fullscreen-image-content"

const val CONVERSATION_FULLSCREEN_IMAGE_HINT_TAG =
    "conversation-fullscreen-image-hint"

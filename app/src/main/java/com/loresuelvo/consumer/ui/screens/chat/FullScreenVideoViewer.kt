package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.MediaReference

/**
 * Fullscreen conversation video viewer. The native [MediaController] keeps
 * familiar play, pause and seek controls while [VideoView] preserves support
 * for private URLs already used by the conversation media flow.
 */
@Composable
fun FullScreenVideoViewer(
    video: MediaReference.Video,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uri = remember(video.url) { Uri.parse(video.url) }
    var videoView by remember(video.url) { mutableStateOf<VideoView?>(null) }
    var hasError by remember(video.url) { mutableStateOf(false) }
    var isPrepared by remember(video.url) { mutableStateOf(false) }

    DisposableEffect(video.url) {
        onDispose {
            videoView?.stopPlayback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag(CONVERSATION_FULLSCREEN_VIDEO_TAG),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = {
                VideoView(context).also { player ->
                    player.contentDescription = context.getString(
                        R.string.conversation_media_preview_video_content_description,
                    )
                    player.setMediaController(
                        MediaController(context).apply {
                            setAnchorView(player)
                        },
                    )
                    player.setVideoURI(uri)
                    player.setOnPreparedListener {
                        isPrepared = true
                        player.start()
                    }
                    player.setOnErrorListener { _, _, _ ->
                        hasError = true
                        true
                    }
                    videoView = player
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag(CONVERSATION_FULLSCREEN_VIDEO_CONTENT_TAG),
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.62f), CircleShape)
                .testTag(CONVERSATION_FULLSCREEN_VIDEO_CLOSE_TAG),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.conversation_video_close),
                tint = Color.White,
            )
        }

        when {
            hasError -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag(CONVERSATION_FULLSCREEN_VIDEO_ERROR_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.conversation_video_error),
                        color = Color.White,
                    )
                    Button(
                        onClick = {
                            hasError = false
                            isPrepared = false
                            videoView?.setVideoURI(uri)
                        },
                        modifier = Modifier.testTag(CONVERSATION_FULLSCREEN_VIDEO_RETRY_TAG),
                    ) {
                        Text(stringResource(R.string.conversation_video_retry))
                    }
                }
            }

            !isPrepared -> CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

const val CONVERSATION_FULLSCREEN_VIDEO_TAG = "conversation-fullscreen-video"
const val CONVERSATION_FULLSCREEN_VIDEO_CONTENT_TAG = "conversation-fullscreen-video-content"
const val CONVERSATION_FULLSCREEN_VIDEO_CLOSE_TAG = "conversation-fullscreen-video-close"
const val CONVERSATION_FULLSCREEN_VIDEO_ERROR_TAG = "conversation-fullscreen-video-error"
const val CONVERSATION_FULLSCREEN_VIDEO_RETRY_TAG = "conversation-fullscreen-video-retry"

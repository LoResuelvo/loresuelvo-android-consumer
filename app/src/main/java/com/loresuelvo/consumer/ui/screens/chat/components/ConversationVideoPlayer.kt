package com.loresuelvo.consumer.ui.screens.chat.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.MediaReference

/**
 * WhatsApp-style video preview for a conversation bubble.
 *
 * The bubble only prepares the native player so the first frame can be used
 * as a preview. Playback is deliberately moved to [FullScreenVideoViewer]:
 * the centered play affordance is easier to discover and the conversation
 * list does not compete with a video player for gestures or audio focus.
 */
@Composable
fun ConversationVideoPlayer(
    media: MediaReference.Video,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uri = remember(media.url) { Uri.parse(media.url) }
    var videoView by remember(media.url) { mutableStateOf<VideoView?>(null) }
    var isPrepared by remember(media.url) { mutableStateOf(false) }
    var hasError by remember(media.url) { mutableStateOf(false) }

    DisposableEffect(media.url) {
        onDispose {
            videoView?.stopPlayback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .testTag(CONVERSATION_MESSAGE_VIDEO_TAG),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = {
                VideoView(context).also { player ->
                    player.contentDescription = context.getString(
                        R.string.conversation_media_preview_video_content_description,
                    )
                    player.setVideoURI(uri)
                    player.setOnPreparedListener {
                        isPrepared = true
                    }
                    player.setOnErrorListener { _, _, _ ->
                        hasError = true
                        true
                    }
                    videoView = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        when {
            hasError -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag(CONVERSATION_MESSAGE_VIDEO_ERROR_TAG),
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
                        modifier = Modifier.testTag(CONVERSATION_MESSAGE_VIDEO_RETRY_TAG),
                    ) {
                        Text(stringResource(R.string.conversation_video_retry))
                    }
                }
            }

            else -> {
                if (!isPrepared) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }

                IconButton(
                    onClick = onOpenFullscreen,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.68f),
                            shape = CircleShape,
                        )
                        .testTag(CONVERSATION_MESSAGE_VIDEO_PLAY_TAG),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.conversation_video_play),
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .testTag(CONVERSATION_MESSAGE_VIDEO_DURATION_TAG),
                    color = Color.Black.copy(alpha = 0.68f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = formatVideoDuration(media.durationMillis),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun formatVideoDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

const val CONVERSATION_MESSAGE_VIDEO_TAG = "conversation-message-video"
const val CONVERSATION_MESSAGE_VIDEO_PLAY_TAG = "conversation-message-video-play"
const val CONVERSATION_MESSAGE_VIDEO_DURATION_TAG = "conversation-message-video-duration"
const val CONVERSATION_MESSAGE_VIDEO_ERROR_TAG = "conversation-message-video-error"
const val CONVERSATION_MESSAGE_VIDEO_RETRY_TAG = "conversation-message-video-retry"

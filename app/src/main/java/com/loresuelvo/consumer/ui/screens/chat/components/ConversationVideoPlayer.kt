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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.loresuelvo.consumer.domain.conversation.MediaReference
import kotlinx.coroutines.delay

/**
 * Small native Android player for private conversation MP4 URLs.
 * VideoView delegates decoding to the platform and therefore does
 * not assume that a chat URL is public or permanently cacheable.
 * The caller owns which message is selected; this composable owns
 * the player lifecycle and releases it when the bubble leaves the
 * composition.
 */
@Composable
fun ConversationVideoPlayer(
    media: MediaReference.Video,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uri = remember(media.url) { Uri.parse(media.url) }
    var videoView by remember(media.url) { mutableStateOf<VideoView?>(null) }
    var isPrepared by remember(media.url) { mutableStateOf(false) }
    var isPlaying by remember(media.url) { mutableStateOf(false) }
    var hasError by remember(media.url) { mutableStateOf(false) }
    var positionMillis by remember(media.url) { mutableStateOf(0L) }
    var playerDurationMillis by remember(media.url) {
        mutableStateOf(media.durationMillis)
    }

    DisposableEffect(videoView) {
        onDispose {
            videoView?.stopPlayback()
        }
    }

    LaunchedEffect(videoView, isSelected, isPrepared, hasError) {
        val player = videoView ?: return@LaunchedEffect
        if (hasError || !isPrepared) return@LaunchedEffect
        if (isSelected) {
            player.start()
            isPlaying = true
        } else {
            player.pause()
            isPlaying = false
        }
    }

    LaunchedEffect(videoView, isPlaying) {
        while (isPlaying) {
            positionMillis = videoView?.currentPosition?.toLong() ?: positionMillis
            delay(250L)
        }
    }

    val progress = if (playerDurationMillis > 0L) {
        (positionMillis.toFloat() / playerDurationMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CONVERSATION_MESSAGE_VIDEO_TAG),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = {
                    VideoView(context).also { player ->
                        player.contentDescription = media.originalName
                        player.setVideoURI(uri)
                        player.setOnPreparedListener { preparedPlayer ->
                            isPrepared = true
                            playerDurationMillis = preparedPlayer.duration.toLong()
                                .takeIf { it > 0L }
                                ?: media.durationMillis
                        }
                        player.setOnCompletionListener {
                            isPlaying = false
                            positionMillis = playerDurationMillis
                        }
                        player.setOnErrorListener { _, _, _ ->
                            hasError = true
                            isPlaying = false
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
                            text = "No se pudo reproducir el video",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {
                                hasError = false
                                isPrepared = false
                                positionMillis = 0L
                                videoView?.setVideoURI(uri)
                            },
                            modifier = Modifier.testTag(CONVERSATION_MESSAGE_VIDEO_RETRY_TAG),
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                !isPrepared -> CircularProgressIndicator()
            }

            if (isPrepared && !hasError) {
                Button(
                    onClick = {
                        onSelect()
                        if (isPlaying) {
                            videoView?.pause()
                            isPlaying = false
                        } else if (isSelected) {
                            videoView?.start()
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .testTag(CONVERSATION_MESSAGE_VIDEO_PLAY_TAG),
                ) {
                    Text(if (isPlaying) "Pausa" else "Play")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag(CONVERSATION_MESSAGE_VIDEO_PROGRESS_TAG),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CONVERSATION_MESSAGE_VIDEO_DURATION_TAG),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "${formatVideoDuration(positionMillis)} / " +
                    formatVideoDuration(playerDurationMillis),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun formatVideoDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

const val CONVERSATION_MESSAGE_VIDEO_TAG = "conversation-message-video"
const val CONVERSATION_MESSAGE_VIDEO_PLAY_TAG = "conversation-message-video-play"
const val CONVERSATION_MESSAGE_VIDEO_PROGRESS_TAG = "conversation-message-video-progress"
const val CONVERSATION_MESSAGE_VIDEO_DURATION_TAG = "conversation-message-video-duration"
const val CONVERSATION_MESSAGE_VIDEO_ERROR_TAG = "conversation-message-video-error"
const val CONVERSATION_MESSAGE_VIDEO_RETRY_TAG = "conversation-message-video-retry"

package com.loresuelvo.consumer.ui.screens.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.ui.screens.chat.AudioPlaybackState
import com.loresuelvo.consumer.ui.screens.chat.ConversationMessageBubbleActions
import com.loresuelvo.consumer.ui.screens.chat.ConversationMessageBubbleState

@Composable
fun ConversationMessageBubble(
    message: ConversationMessage,
    audioPlayback: AudioPlaybackState = AudioPlaybackState(),
    onPlayAudio: (String) -> Unit = {},
    onPauseAudio: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onVideoClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ConversationMessageBubble(
        state = ConversationMessageBubbleState(
            message = message,
            audioPlayback = audioPlayback,
        ),
        actions = ConversationMessageBubbleActions(
            onPlayAudio = onPlayAudio,
            onPauseAudio = onPauseAudio,
            onImageClick = onImageClick,
            onVideoClick = onVideoClick,
        ),
        modifier = modifier,
    )
}

package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import androidx.compose.foundation.lazy.LazyListState

data class ConversationMessageBubbleState(
    val message: ConversationMessage,
    val audioPlayback: AudioPlaybackState = AudioPlaybackState(),
)

data class ConversationMessageBubbleActions(
    val onPlayAudio: (String) -> Unit = {},
    val onPauseAudio: (String) -> Unit = {},
    val onImageClick: (String) -> Unit = {},
    val onVideoClick: (String) -> Unit = {},
)

data class ConversationMessagesListState(
    val messages: List<ConversationMessage>,
    val listState: LazyListState,
    val audioPlayback: AudioPlaybackState,
)

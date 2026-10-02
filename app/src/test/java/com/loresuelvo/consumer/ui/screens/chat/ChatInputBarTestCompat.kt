package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ChatInputBar(
    promptInput: String,
    canSend: Boolean,
    sending: Boolean,
    recordingAudio: Boolean,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onStartAudioRecording: () -> Unit,
    onStopAudioRecording: () -> Unit,
    onAttachClick: (() -> Unit)? = null,
    audioEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    ChatInputBar(
        state = ChatInputBarState(
            promptInput = promptInput,
            canSend = canSend,
            sending = sending,
            recordingAudio = recordingAudio,
            audioEnabled = audioEnabled,
        ),
        actions = ChatInputBarActions(
            onPromptChange = onPromptChange,
            onSend = onSendClick,
            onStartAudioRecording = onStartAudioRecording,
            onStopAudioRecording = onStopAudioRecording,
            onAttach = onAttachClick,
        ),
        modifier = modifier,
    )
}

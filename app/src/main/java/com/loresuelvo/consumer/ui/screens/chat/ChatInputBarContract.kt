package com.loresuelvo.consumer.ui.screens.chat

/** Immutable rendering configuration for the chat composer. */
data class ChatInputBarState(
    val promptInput: String = "",
    val canSend: Boolean = false,
    val sending: Boolean = false,
    val recordingAudio: Boolean = false,
    val audioEnabled: Boolean = true,
)

/** User interactions emitted by the chat composer. */
data class ChatInputBarActions(
    val onPromptChange: (String) -> Unit = {},
    val onSend: () -> Unit = {},
    val onStartAudioRecording: () -> Unit = {},
    val onStopAudioRecording: () -> Unit = {},
    val onAttach: (() -> Unit)? = null,
)

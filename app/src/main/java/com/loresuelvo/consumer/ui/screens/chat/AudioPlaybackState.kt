package com.loresuelvo.consumer.ui.screens.chat

data class AudioPlaybackState(
    val messageId: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMillis: Long = 0L,
)
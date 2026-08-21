package com.loresuelvo.consumer.data.media

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {

    val isPlaying: StateFlow<Boolean>

    val currentPositionMillis: StateFlow<Long>

    fun play(
        url: String,
        startPositionMillis: Long = 0L,
    )

    fun pause()

    fun stop()
}
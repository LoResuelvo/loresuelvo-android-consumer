package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.data.media.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAudioPlayer : AudioPlayer {

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPositionMillis = MutableStateFlow(0L)
    override val currentPositionMillis: StateFlow<Long> =
        _currentPositionMillis

    var lastPlayedUrl: String? = null
        private set

    var lastStartPositionMillis: Long = 0L
        private set

    override fun play(
        url: String,
        startPositionMillis: Long,
    ) {
        lastPlayedUrl = url
        lastStartPositionMillis = startPositionMillis
        _currentPositionMillis.value = startPositionMillis
        _isPlaying.value = true
    }

    override fun pause() {
        _isPlaying.value = false
    }

    override fun stop() {
        _isPlaying.value = false
        _currentPositionMillis.value = 0L
        lastPlayedUrl = null
        lastStartPositionMillis = 0L
    }

    fun advanceBy(millis: Long) {
        require(millis >= 0L) {
            "millis must be >= 0"
        }

        if (!_isPlaying.value) return

        _currentPositionMillis.value += millis
    }

    fun reset() {
        _isPlaying.value = false
        _currentPositionMillis.value = 0L
        lastPlayedUrl = null
        lastStartPositionMillis = 0L
    }
}
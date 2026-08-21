package com.loresuelvo.consumer.data.media

import android.media.MediaPlayer
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of [AudioPlayer].
 *
 * Uses MediaPlayer for remote audio playback and exposes the
 * current playback state through StateFlow so the UI/ViewModel
 * can render play state and progress.
 */
class AndroidAudioPlayer @Inject constructor() : AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMillis = MutableStateFlow(0L)
    override val currentPositionMillis: StateFlow<Long> =
        _currentPositionMillis.asStateFlow()

    override fun play(
        url: String,
        startPositionMillis: Long,
    ) {
        stop()

        val player = MediaPlayer()

        mediaPlayer = player

        player.setDataSource(url)

        player.setOnPreparedListener {
            if (startPositionMillis > 0L) {
                it.seekTo(startPositionMillis.toInt())
            }

            _currentPositionMillis.value = startPositionMillis
            _isPlaying.value = true

            it.start()
        }

        player.setOnCompletionListener {
            _isPlaying.value = false
            _currentPositionMillis.value = 0L
            releasePlayer()
        }

        player.setOnErrorListener { _, _, _ ->
            _isPlaying.value = false
            releasePlayer()
            true
        }

        player.prepareAsync()
    }

    override fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                _currentPositionMillis.value = player.currentPosition.toLong()
                player.pause()
            }

            _isPlaying.value = false
        }
    }

    override fun stop() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: IllegalStateException) {
                // Player was already stopped/released.
            }
        }

        releasePlayer()

        _isPlaying.value = false
        _currentPositionMillis.value = 0L
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
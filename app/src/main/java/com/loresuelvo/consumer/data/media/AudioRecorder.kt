package com.loresuelvo.consumer.data.media

import android.net.Uri

/**
 * Port for recording audio from the device microphone.
 *
 * The implementation owns the temporary recording file and
 * returns its Uri when recording successfully stops.
 *
 * Permission handling belongs to the UI layer.
 */
interface AudioRecorder {

    /**
     * Starts a new recording.
     *
     * @return [Result.success] when recording starts successfully,
     * or [Result.failure] when the recorder cannot be started.
     */
    fun start(): Result<Unit>

    /**
     * Stops the current recording.
     *
     * @return the Uri of the recorded audio on success.
     */
    fun stop(): Result<Uri>

    /**
     * Cancels the current recording and removes the temporary file.
     */
    fun cancel()
}
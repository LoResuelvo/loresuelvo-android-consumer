package com.loresuelvo.consumer.data.media

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the AudioRecorder abstraction.
 *
 * The real Android implementation depends on the microphone and
 * Android's MediaRecorder, so the JVM test suite verifies the
 * expected state transitions using a deterministic fake.
 */
class AudioRecorderTest {

    private class FakeAudioRecorder : AudioRecorder {

        private val recordedUri =
            Uri.parse("content://test.audio/recording.webm")

        var recording = false
        var startCalls = 0
        var stopCalls = 0
        var cancelCalls = 0

        override fun start(): Result<Unit> {
            startCalls++

            if (recording) {
                return Result.failure(
                    IllegalStateException("Already recording"),
                )
            }

            recording = true
            return Result.success(Unit)
        }

        override fun stop(): Result<Uri> {
            stopCalls++

            if (!recording) {
                return Result.failure(
                    IllegalStateException("Not recording"),
                )
            }

            recording = false
            return Result.success(recordedUri)
        }

        override fun cancel() {
            cancelCalls++
            recording = false
        }
    }

    private fun createRecorder(): FakeAudioRecorder =
        FakeAudioRecorder()

    @Test
    fun start_begins_recording() {
        val recorder = createRecorder()

        val result = recorder.start()

        assertTrue(result.isSuccess)
        assertTrue(recorder.recording)
        assertEquals(1, recorder.startCalls)
    }

    @Test
    fun start_fails_when_already_recording() {
        val recorder = createRecorder()

        recorder.start()

        val result = recorder.start()

        assertTrue(result.isFailure)
        assertTrue(recorder.recording)
        assertEquals(2, recorder.startCalls)
    }

    @Test
    fun stop_returns_recorded_audio_uri() {
        val recorder = createRecorder()

        recorder.start()

        val result = recorder.stop()

        assertTrue(result.isSuccess)
        assertEquals(
            Uri.parse("content://test.audio/recording.webm"),
            result.getOrNull(),
        )
        assertFalse(recorder.recording)
        assertEquals(1, recorder.stopCalls)
    }

    @Test
    fun stop_fails_when_not_recording() {
        val recorder = createRecorder()

        val result = recorder.stop()

        assertTrue(result.isFailure)
        assertFalse(recorder.recording)
    }

    @Test
    fun cancel_stops_recording() {
        val recorder = createRecorder()

        recorder.start()
        recorder.cancel()

        assertFalse(recorder.recording)
        assertEquals(1, recorder.cancelCalls)
    }
}
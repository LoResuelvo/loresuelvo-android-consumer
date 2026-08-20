package com.loresuelvo.consumer.testdi

import android.net.Uri
import com.loresuelvo.consumer.data.media.AudioRecorder
import javax.inject.Inject

class FakeAudioRecorder @Inject constructor() : AudioRecorder {

    var startResult: Result<Unit> = Result.success(Unit)

    var stopResult: Result<Uri> = Result.success(
        Uri.parse("content://test.audio/recording.webm"),
    )

    var startCalls: Int = 0
        private set

    var stopCalls: Int = 0
        private set

    var cancelCalls: Int = 0
        private set

    override fun start(): Result<Unit> {
        startCalls++
        return startResult
    }

    override fun stop(): Result<Uri> {
        stopCalls++
        return stopResult
    }

    override fun cancel() {
        cancelCalls++
    }
}
package com.loresuelvo.consumer.platform.media

import android.net.Uri

interface AudioRecorder {
    fun start(): Result<Unit>

    fun stop(): Result<Uri>

    fun cancel()
}

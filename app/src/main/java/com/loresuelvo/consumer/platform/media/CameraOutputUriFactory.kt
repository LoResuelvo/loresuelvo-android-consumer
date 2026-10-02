package com.loresuelvo.consumer.platform.media

import android.net.Uri

interface CameraOutputUriFactory {
    fun createCameraOutputUri(): Uri
}

package com.loresuelvo.consumer.platform.media

import android.net.Uri
import com.loresuelvo.consumer.domain.conversation.MediaUpload

interface MediaReader {
    suspend fun read(uri: Uri): MediaUpload
}

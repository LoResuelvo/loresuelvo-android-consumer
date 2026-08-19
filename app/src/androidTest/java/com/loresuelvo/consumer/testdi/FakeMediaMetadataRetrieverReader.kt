package com.loresuelvo.consumer.testdi

import android.net.Uri
import com.loresuelvo.consumer.data.media.MediaMetadataRetrieverReader

class FakeMediaMetadataRetrieverReader : MediaMetadataRetrieverReader {

    override suspend fun extractDurationMillis(uri: Uri): Long? = null
}
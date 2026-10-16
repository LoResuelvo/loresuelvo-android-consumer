package com.loresuelvo.consumer.platform.media

/**
 * Extracts normalized peak amplitudes from an audio file.
 *
 * The waveform is presentation data only; it is never sent to the API.
 */
interface AudioWaveformExtractor {
    suspend fun extract(
        bytes: ByteArray,
        mimeType: String,
        durationMillis: Long = 0L,
    ): List<Float>

    suspend fun extract(url: String, durationMillis: Long = 0L): List<Float>
}

object NoOpAudioWaveformExtractor : AudioWaveformExtractor {
    override suspend fun extract(
        bytes: ByteArray,
        mimeType: String,
        durationMillis: Long,
    ): List<Float> =
        emptyList()

    override suspend fun extract(url: String, durationMillis: Long): List<Float> =
        emptyList()
}

const val AUDIO_WAVEFORM_BAR_COUNT: Int = 28

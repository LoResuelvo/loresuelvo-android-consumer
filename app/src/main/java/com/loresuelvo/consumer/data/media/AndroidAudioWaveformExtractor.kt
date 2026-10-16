package com.loresuelvo.consumer.data.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.loresuelvo.consumer.platform.media.AUDIO_WAVEFORM_BAR_COUNT
import com.loresuelvo.consumer.platform.media.AudioWaveformExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Named
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Android PCM decoder used to build a visual waveform from real audio. */
@Singleton
class AndroidAudioWaveformExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("uploadOkHttp")
    private val httpClient: OkHttpClient,
) : AudioWaveformExtractor {

    private val remoteCache = ConcurrentHashMap<String, List<Float>>()

    constructor(context: Context) : this(context, OkHttpClient())

    override suspend fun extract(
        bytes: ByteArray,
        mimeType: String,
        durationMillis: Long,
    ): List<Float> {
        if (bytes.isEmpty()) return emptyList()
        return withTemporaryAudio(bytes, "waveform-local") { file ->
            decode(file, durationMillis)
        }
    }

    override suspend fun extract(url: String, durationMillis: Long): List<Float> {
        if (url.isBlank()) return emptyList()
        remoteCache[url]?.let { return it }

        val waveform = runCatching {
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body ?: return@withContext emptyList()
                    val bytes = body.bytes()
                    if (!response.isSuccessful) return@withContext emptyList()
                    withTemporaryAudio(bytes, "waveform-remote") { file ->
                        decode(file, durationMillis)
                    }
                }
            }
        }.getOrDefault(emptyList())

        if (waveform.isNotEmpty()) remoteCache[url] = waveform
        return waveform
    }

    private suspend fun <T> withTemporaryAudio(
        bytes: ByteArray,
        prefix: String,
        block: suspend (File) -> T,
    ): T {
        val file = File.createTempFile(prefix, ".audio", context.cacheDir)
        return try {
            withContext(Dispatchers.IO) {
                file.outputStream().use { it.write(bytes) }
            }
            block(file)
        } finally {
            file.delete()
        }
    }

    private suspend fun decode(
        file: File,
        expectedDurationMillis: Long,
    ): List<Float> = withContext(Dispatchers.Default) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

        try {
            extractor.setDataSource(file.absolutePath)
            val trackIndex = (0 until extractor.trackCount)
                .firstOrNull { index ->
                    extractor.getTrackFormat(index)
                        .getString(MediaFormat.KEY_MIME)
                        ?.startsWith("audio/") == true
                }
                ?: return@withContext emptyList()

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: return@withContext emptyList()
            val durationUs = format.longOrZero(MediaFormat.KEY_DURATION)
                .takeIf { it > 0L }
                ?: expectedDurationMillis * 1_000L
            val sampleRate = format.integerOrDefault(
                MediaFormat.KEY_SAMPLE_RATE,
                48_000,
            )
            val channelCount = format.integerOrDefault(
                MediaFormat.KEY_CHANNEL_COUNT,
                1,
            )
            if (durationUs <= 0L || sampleRate <= 0 || channelCount <= 0) {
                return@withContext emptyList()
            }

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val peaks = FloatArray(AUDIO_WAVEFORM_BAR_COUNT)
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        val sampleSize = extractor.readSampleData(inputBuffer ?: ByteBuffer.allocate(0), 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputIndex >= 0) {
                    decoder.getOutputBuffer(outputIndex)?.let { outputBuffer ->
                        collectPeaks(
                            outputBuffer = outputBuffer,
                            bufferInfo = bufferInfo,
                            peaks = peaks,
                            durationUs = durationUs,
                            sampleRate = sampleRate,
                            channelCount = channelCount,
                        )
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }

            peaks.map { it.coerceIn(0f, 1f) }
        } catch (_: Throwable) {
            emptyList()
        } finally {
            runCatching { decoder?.stop() }
            decoder?.release()
            extractor.release()
        }
    }

    private fun collectPeaks(
        outputBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
        peaks: FloatArray,
        durationUs: Long,
        sampleRate: Int,
        channelCount: Int,
    ) {
        val start = (bufferInfo.offset).coerceAtLeast(0)
        val end = (bufferInfo.offset + bufferInfo.size).coerceAtMost(outputBuffer.limit())
        if (start >= end) return

        outputBuffer.position(start)
        outputBuffer.limit(end)
        val bytesPerFrame = channelCount * BYTES_PER_SAMPLE
        var frameOffset = 0

        while (outputBuffer.remaining() >= bytesPerFrame) {
            var peak = 0
            repeat(channelCount) {
                peak = maxOf(peak, kotlin.math.abs(outputBuffer.short.toInt()))
            }
            val timestampUs = bufferInfo.presentationTimeUs +
                (frameOffset.toLong() / bytesPerFrame) * 1_000_000L / sampleRate
            val bin = ((timestampUs.toDouble() / durationUs) * peaks.size)
                .toInt()
                .coerceIn(0, peaks.lastIndex)
            peaks[bin] = maxOf(peaks[bin], peak / MAX_PCM_AMPLITUDE)
            frameOffset += bytesPerFrame
        }
    }

    private fun MediaFormat.longOrZero(key: String): Long =
        if (containsKey(key)) getLong(key) else 0L

    private fun MediaFormat.integerOrDefault(key: String, default: Int): Int =
        if (containsKey(key)) getInteger(key) else default

    private companion object {
        const val BYTES_PER_SAMPLE = 2
        const val MAX_PCM_AMPLITUDE = 32_768f
        const val TIMEOUT_US = 10_000L
    }
}

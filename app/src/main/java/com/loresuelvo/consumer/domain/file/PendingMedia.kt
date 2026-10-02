package com.loresuelvo.consumer.domain.file

data class PendingMedia(
    val localUri: String?,
    val mimeType: String,
    val originalName: String,
    val sizeBytes: Long,
    val bytes: ByteArray,
    val kind: PendingMediaKind = PendingMediaKind.IMAGE,
    val durationMillis: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val videoCodec: String = "",
    val audioCodec: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PendingMedia) return false
        return localUri == other.localUri &&
            mimeType == other.mimeType &&
            originalName == other.originalName &&
            sizeBytes == other.sizeBytes &&
            bytes.contentEquals(other.bytes) &&
            kind == other.kind &&
            durationMillis == other.durationMillis &&
            width == other.width &&
            height == other.height &&
            videoCodec == other.videoCodec &&
            audioCodec == other.audioCodec
    }

    override fun hashCode(): Int {
        var result = localUri?.hashCode() ?: 0
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + originalName.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + durationMillis.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + videoCodec.hashCode()
        result = 31 * result + (audioCodec?.hashCode() ?: 0)
        return result
    }
}

enum class PendingMediaKind { IMAGE, AUDIO, VIDEO }

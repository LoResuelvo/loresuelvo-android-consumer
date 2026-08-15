package com.loresuelvo.consumer.domain.conversation

/**
 * Media payload the consumer is about to upload to a conversation.
 * Carries the bytes (read locally from a `content://` URI), the
 * declared mime type, and the original file name. For audio the
 * recording's duration is captured before the upload so the
 * server can echo it back without re-decoding the file.
 *
 * Sealed for the same forward-compatibility reason as
 * [MediaReference]: each variant declares its own extra fields
 * and the UI can render an exhaustive `when`. The data layer
 * maps each variant to its own multipart part (image vs audio
 * use distinct `Content-Disposition` names on the backend).
 *
 * Pure domain. The only non-pure fact is that [bytes] is a
 * `ByteArray` — a JDK type, not an Android type — so the
 * abstraction stays testable in plain JUnit.
 */
sealed interface MediaUpload {

    val bytes: ByteArray

    val mimeType: String

    val originalName: String

    /**
     * Image the consumer picked from the gallery / camera.
     */
    data class Image(
        override val bytes: ByteArray,
        override val mimeType: String,
        override val originalName: String,
    ) : MediaUpload {
        // Generated `equals`/`hashCode` from data class would
        // walk the byte array contents — fine for tests that
        // need bytewise equality, but overkill for the hot
        // paths. Identity is enough at runtime; we override
        // here so future assertions stay explicit about what
        // they compare.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Image) return false
            return bytes.contentEquals(other.bytes) &&
                mimeType == other.mimeType &&
                originalName == other.originalName
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + originalName.hashCode()
            return result
        }
    }

    /**
     * Audio clip the consumer recorded in-app. [durationMillis]
     * is measured at recording time so the server can echo it
     * back without re-decoding the file.
     */
    data class Audio(
        override val bytes: ByteArray,
        override val mimeType: String,
        override val originalName: String,
        val durationMillis: Long,
    ) : MediaUpload {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Audio) return false
            return bytes.contentEquals(other.bytes) &&
                mimeType == other.mimeType &&
                originalName == other.originalName &&
                durationMillis == other.durationMillis
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + originalName.hashCode()
            result = 31 * result + durationMillis.hashCode()
            return result
        }
    }
}
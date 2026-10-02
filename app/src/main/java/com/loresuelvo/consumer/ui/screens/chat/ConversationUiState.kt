package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome

sealed interface ConversationUiState {

    data object Loading : ConversationUiState

    data class Ready(
        val detail: ConversationDetail,
        val promptInput: String,
        val sending: Boolean,
        val transientError: SendMessageOutcome.Failure? = null,
        val lastAttemptedPrompt: String? = null,
        val isAtBottom: Boolean = true,
        val hasUnreadIncoming: Boolean = false,
        val pendingMedia: List<PendingMedia> = emptyList(),
        val attachingMedia: Boolean = false,
        val recordingAudio: Boolean = false,
        val audioPlayback: AudioPlaybackState = AudioPlaybackState(),
        val sendingMedia: Boolean = false,
        val transientMediaError: SendMessageOutcome.Failure? = null,
        val fullscreenImage: MediaReference.Image? = null,
        val fullscreenVideo: MediaReference.Video? = null,
    ) : ConversationUiState

    data class Error(val failure: ConversationDetailOutcome.Failure) : ConversationUiState
}

enum class PendingMediaKind { IMAGE, AUDIO, VIDEO }

data class PendingMedia(
    val localUri: Uri?,
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

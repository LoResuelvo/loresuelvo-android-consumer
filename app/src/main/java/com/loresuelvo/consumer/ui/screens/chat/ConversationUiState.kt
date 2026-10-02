package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.file.PendingMedia as DomainPendingMedia
import com.loresuelvo.consumer.domain.file.PendingMediaKind as DomainPendingMediaKind

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

typealias PendingMedia = DomainPendingMedia
typealias PendingMediaKind = DomainPendingMediaKind

package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.platform.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMediaMessageUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import com.loresuelvo.consumer.platform.media.MediaMetadataRetrieverReader
import com.loresuelvo.consumer.platform.media.AudioRecorder
import com.loresuelvo.consumer.platform.media.AudioPlayer
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.validationError
import com.loresuelvo.consumer.domain.realtime.RealtimeClient
import com.loresuelvo.consumer.domain.realtime.WsEvent
import com.loresuelvo.consumer.domain.notifications.ConversationVisibility
import com.loresuelvo.consumer.domain.notifications.ConversationRefreshRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val getConversationById: GetConversationByIdUseCase,
    private val sendMessage: SendMessageUseCase,
    private val sendMediaMessage: SendMediaMessageUseCase,
    private val mediaReader: MediaReader,
    private val mediaMetadataRetriever: MediaMetadataRetrieverReader,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val webSocketClient: RealtimeClient,
    private val conversationVisibility: ConversationVisibility = ConversationVisibility.None,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationUiState>(
        ConversationUiState.Loading,
    )
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()
    private var refreshJob: kotlinx.coroutines.Job? = null
    private var loadJob: kotlinx.coroutines.Job? = null
    private var activeConversationId: String? = null
    private var pendingRefreshRequest: ConversationRefreshRequest? = null

    init {
        webSocketClient.start()

        viewModelScope.launch {
            webSocketClient.events
                .filterIsInstance<WsEvent.ConversationMessageCreated>()
                .filter { event ->
                    currentConversationIdMatches(event.conversationId)
                }
                .filter { event ->
                    event.message.sender ==
                        com.loresuelvo.consumer.domain.conversation.ConversationSender.Provider
                }
                .collect { event ->
                    appendIncomingMessage(event.message)
                }
        }

        viewModelScope.launch {
            conversationVisibility.refreshRequests.collect { request ->
                if (conversationVisibility.isCurrent(request)) refreshVisibleConversation(request)
            }
        }

        viewModelScope.launch {
            audioPlayer.currentPositionMillis.collect { positionMillis ->
                _uiState.update { current ->
                    if (current !is ConversationUiState.Ready) {
                        return@update current
                    }

                    val playback = current.audioPlayback

                    if (playback.messageId == null) {
                        return@update current
                    }

                    current.copy(
                        audioPlayback = playback.copy(
                            currentPositionMillis = positionMillis,
                        ),
                    )
                }
            }
        }
    }

    private fun currentConversationIdMatches(eventConversationId: Long): Boolean {
        val state = _uiState.value
        return state is ConversationUiState.Ready &&
            state.detail.id == eventConversationId.toString()
    }

    private fun appendIncomingMessage(message: ConversationMessage) {
        _uiState.update { current ->
            if (current !is ConversationUiState.Ready) return@update current
            // De-dupe: if the optimistic bubble with the same
            // server id is already in the list (race between
            // `sendMessage` Success and the WS echo), skip.
            if (current.detail.messages.any { it.id == message.id }) return@update current
            // screen renders the new bubble immediately (auto-
            // scroll) so no "new message" indicator is needed.
            // older messages, surface a "↓ nuevo mensaje" banner
            // by flipping `hasUnreadIncoming` to `true`. The banner
            // CTA (`onUnreadBannerTapped`) clears the flag.
            current.copy(
                detail = current.detail.copy(
                    messages = current.detail.messages + message,
                ),
                hasUnreadIncoming = !current.isAtBottom,
            )
        }
    }

    private fun refreshVisibleConversation(request: ConversationRefreshRequest) {
        val conversationIdText = request.conversationId.toString()
        if (!conversationVisibility.isCurrent(request)) return
        val activeId = activeConversationId
        if (activeId != null && activeId != conversationIdText) return
        if (activeId == null || _uiState.value !is ConversationUiState.Ready) {
            pendingRefreshRequest = request
            return
        }
        if (currentConversationId() != conversationIdText) return
        pendingRefreshRequest = null
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val refreshed = (getConversationById(conversationIdText) as? ConversationDetailOutcome.Success)
                ?.detail ?: return@launch
            _uiState.update { current ->
                if (current !is ConversationUiState.Ready || current.detail.id != conversationIdText) {
                    return@update current
                }
                val currentIds = current.detail.messages.mapTo(mutableSetOf()) { it.id }
                val refreshedIds = refreshed.messages.mapTo(mutableSetOf()) { it.id }
                val remainingLocalMessages = current.detail.messages.filterNot { it.id in refreshedIds }
                val newProviderMessage = refreshed.messages.any {
                    it.sender == com.loresuelvo.consumer.domain.conversation.ConversationSender.Provider &&
                        it.id !in currentIds
                }
                current.copy(
                    detail = refreshed.copy(messages = refreshed.messages + remainingLocalMessages),
                    hasUnreadIncoming = current.hasUnreadIncoming || (!current.isAtBottom && newProviderMessage),
                )
            }
        }
    }

    private fun currentConversationId(): String? =
        (_uiState.value as? ConversationUiState.Ready)?.detail?.id

    /**
     * Loads the conversation detail for [conversationId]. Public
     * so the host can re-trigger on retry (and the host invokes
     * it once on first composition with the nav argument).
     */
    fun load(conversationId: String) {
        activeConversationId = conversationId
        val pendingRefresh = pendingRefreshRequest
        if (
            pendingRefresh == null ||
            pendingRefresh.conversationId.toString() != conversationId ||
            !conversationVisibility.isCurrent(pendingRefresh)
        ) {
            pendingRefreshRequest = null
        }
        loadJob?.cancel()
        refreshJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { ConversationUiState.Loading }
            val next = when (val outcome = getConversationById(conversationId)) {
                is ConversationDetailOutcome.Success ->
                    ConversationUiState.Ready(
                        detail = outcome.detail,
                        promptInput = "",
                        sending = false,
                    )
                is ConversationDetailOutcome.Failure ->
                    ConversationUiState.Error(outcome)
            }
            if (activeConversationId != conversationId) return@launch
            _uiState.update { next }
            val pendingRefresh = pendingRefreshRequest
            if (
                pendingRefresh?.conversationId?.toString() == conversationId &&
                conversationVisibility.isCurrent(pendingRefresh)
            ) {
                refreshVisibleConversation(pendingRefresh)
            } else if (pendingRefresh != null) {
                pendingRefreshRequest = null
            }
        }
    }

    fun onPromptChange(value: String) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(promptInput = value, transientError = null)
            } else {
                state
            }
        }
    }

    fun onSendClick() {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        val prompt = state.promptInput.trim()
        if (prompt.isEmpty() || state.sending) return

        _uiState.update { currentState ->
            if (currentState is ConversationUiState.Ready) {
                currentState.copy(
                    promptInput = "",
                    sending = true,
                    transientError = null,
                    lastAttemptedPrompt = prompt,
                )
            } else {
                currentState
            }
        }
        fireSend(state.detail.id, prompt)
    }

    fun onRetryClick() {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        val prompt = state.lastAttemptedPrompt
        if (prompt.isNullOrBlank() || state.sending) return
        _uiState.update { currentState ->
            if (currentState is ConversationUiState.Ready) {
                currentState.copy(
                    sending = true,
                    transientError = null,
                )
            } else {
                currentState
            }
        }
        fireSend(state.detail.id, prompt)
    }

    fun onErrorDismiss() {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(transientError = null)
            } else {
                state
            }
        }
    }

    /**
     * Reports whether the chat's `LazyColumn` is currently
     * scrolled to its last visible item. The screen wires this
     * to a `derivedStateOf { listState.layoutInfo... }` that
     * re-fires on every scroll. When [atBottom] flips to
     * `true`, the unread-incoming flag clears (the user is now
     * looking at the new bubbles). When it flips to `false`,
     * the flag is preserved so a follow-up incoming message
     * can still surface the "↓ nuevo mensaje" banner.
     */
    fun onScrollPositionChanged(atBottom: Boolean) {
        _uiState.update { state ->
            if (state !is ConversationUiState.Ready) return@update state
            if (state.isAtBottom == atBottom) return@update state
            state.copy(
                isAtBottom = atBottom,
                hasUnreadIncoming = if (atBottom) false else state.hasUnreadIncoming,
            )
        }
    }

    /**
     * Manual "mark as read" hook for the "↓ nuevo mensaje"
     * banner CTA. The user tapped the banner and jumped to the
     * bottom; clear the unread flag manually so the screen
     * collapses back to the normal chat.
     */
    fun onUnreadBannerTapped() {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(hasUnreadIncoming = false)
            } else {
                state
            }
        }
    }

    private fun fireSend(conversationId: String, content: String) {
        viewModelScope.launch {
            when (val outcome = sendMessage(conversationId, content)) {
                is SendMessageOutcome.Success -> applyServerResponse(outcome.message)
                is SendMessageOutcome.Failure.Network ->
                    applySendFailure(outcome)
                is SendMessageOutcome.Failure.Server ->
                    applySendFailure(outcome)
                is SendMessageOutcome.Failure.Unauthorized ->
                    applySendFailure(outcome)
                is SendMessageOutcome.Failure.PayloadTooLarge ->
                    // Text messages can't trigger this branch
                    // (no size limit on text), but the sealed
                    // hierarchy forces the branch — keep parity
                    // with the other Failure subtypes.
                    applySendFailure(outcome)
            }
        }
    }

    private fun applyServerResponse(sentMessage: ConversationMessage) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(
                    sending = false,
                    detail = state.detail.copy(
                        messages = state.detail.messages + sentMessage,
                    ),
                    transientError = null,
                    lastAttemptedPrompt = null,
                )
            } else {
                state
            }
        }
    }

    private fun applySendFailure(failure: SendMessageOutcome.Failure) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(
                    sending = false,
                    transientError = failure,
                    // `lastAttemptedPrompt` is preserved so the
                    // retry CTA can resubmit it.
                )
            } else {
                state
            }
        }
    }

    /**
     * Reads the URI the picker returned (gallery / camera /
     * audio) via [MediaReader], packages the result as a
     * [PendingMedia] (bytes cached for the confirm step), and
     * exposes it on the state. The `attachingMedia` flag flips
     * true for the duration of the read so the screen can show a
     * spinner on the attach card.
     *
     * Read failures are translated to `SendMessageOutcome.Failure.Network`
     * so the existing transient-error card can render the copy
     * without a new error surface — the cause is the same kind
     * of I/O failure the user would see if the backend was
     * unreachable.
     */
    fun onAttachImageFromGallery(uri: Uri) {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        _uiState.update { current ->
            if (current is ConversationUiState.Ready) {
                current.copy(attachingMedia = true, transientMediaError = null)
            } else {
                current
            }
        }
        viewModelScope.launch {
            try {
                val media = mediaReader.read(uri)
                onAttachMedia(media, sourceUri = uri)
            } catch (t: Throwable) {
                applyAttachFailure(t)
            }
        }
    }

    /** Reads and validates a single video selected from the picker. */
    fun onAttachVideoFromPicker(uri: Uri) {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        _uiState.update { current ->
            if (current is ConversationUiState.Ready) {
                current.copy(attachingMedia = true, transientMediaError = null)
            } else {
                current
            }
        }
        viewModelScope.launch {
            try {
                val media = mediaReader.read(uri)
                if (media !is MediaUpload.Video) {
                    applyAttachFailure(IllegalArgumentException("Selected file is not a video"))
                } else {
                    onAttachMedia(media, sourceUri = uri)
                }
            } catch (t: Throwable) {
                applyAttachFailure(t)
            }
        }
    }


    fun onAttachAudioFromUri(uri: Uri) {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        _uiState.update { current ->
            if (current is ConversationUiState.Ready) {
                current.copy(attachingMedia = true, transientMediaError = null)
            } else {
                current
            }
        }
        viewModelScope.launch {
            try {
                val baseMedia = mediaReader.read(uri)
                val withDuration = if (baseMedia is MediaUpload.Audio) {
                    val duration = mediaMetadataRetriever.extractDurationMillis(uri) ?: 0L
                    MediaUpload.Audio(
                        bytes = baseMedia.bytes,
                        mimeType = baseMedia.mimeType,
                        originalName = baseMedia.originalName,
                        durationMillis = duration,
                    )
                } else {
                    // Defensive: the system's voice recorder always
                    // returns an audio mime, but if a future
                    // contract change lets it return an image or
                    // a mime we don't handle, fall through with
                    // what we read instead of crashing.
                    baseMedia
                }
                onAttachMedia(withDuration, sourceUri = uri)
            } catch (t: Throwable) {
                applyAttachFailure(t)
            }
        }
    }

    fun onPlayAudio(messageId: String) {
        val currentState = _uiState.value

        if (currentState !is ConversationUiState.Ready) {
            return
        }

        val message = currentState.detail.messages
            .firstOrNull { it.id == messageId }
            ?: return

        val media = message.media as? MediaReference.Audio
            ?: return

        val playback = currentState.audioPlayback
        val resumePosition = if (
            playback.messageId == messageId &&
            !playback.isPlaying
        ) {
            playback.currentPositionMillis
        } else {
            0L
        }

        audioPlayer.play(
            url = media.url,
            startPositionMillis = resumePosition,
        )

        _uiState.value = currentState.copy(
            audioPlayback = AudioPlaybackState(
                messageId = messageId,
                isPlaying = true,
                currentPositionMillis = resumePosition,
            ),
        )
    }

    /**
     * Pauses the audio playback for [messageId] and flips the
     * playback state to `isPlaying = false` while preserving
     * `messageId` and `currentPositionMillis` so the bubble can
     * still render the play button at the paused position.
     *
     * No-op when:
     *  - the state isn't `Ready` (initial load / error);
     *  - the message id doesn't match the currently playing
     *    message (so tapping pause on a different bubble while
     *    another one is playing doesn't accidentally stop the
     *    active one — that would need a separate
     *    "stop-and-switch" use case);
     *  - the playback is already paused.
     */
    fun onPauseAudio(messageId: String) {
        val currentState = _uiState.value

        if (currentState !is ConversationUiState.Ready) {
            return
        }

        val playback = currentState.audioPlayback

        if (playback.messageId != messageId || !playback.isPlaying) {
            return
        }

        audioPlayer.pause()

        _uiState.value = currentState.copy(
            audioPlayback = playback.copy(
                isPlaying = false,
            ),
        )
    }


    fun onImageClick(messageId: String) {
        val currentState = _uiState.value

        if (currentState !is ConversationUiState.Ready) {
            return
        }

        val media = currentState.detail.messages
            .firstOrNull { it.id == messageId }
            ?.media as? MediaReference.Image
            ?: return

        _uiState.value = currentState.copy(
            fullscreenImage = media,
        )
    }

    fun onFullscreenImageDismiss() {
        val currentState = _uiState.value

        if (currentState !is ConversationUiState.Ready) {
            return
        }

        if (currentState.fullscreenImage == null) {
            return
        }

        _uiState.value = currentState.copy(
            fullscreenImage = null,
        )
    }

    /**
     * Opens the fullscreen video viewer for [messageId]. The inline
     * conversation bubble stays a paused preview; playback starts only in
     * the dedicated viewer so the chat remains lightweight and predictable.
     */
    fun onVideoClick(messageId: String) {
        val currentState = _uiState.value

        if (currentState !is ConversationUiState.Ready) {
            return
        }

        val media = currentState.detail.messages
            .firstOrNull { it.id == messageId }
            ?.media as? MediaReference.Video
            ?: return

        _uiState.value = currentState.copy(
            fullscreenVideo = media,
        )
    }

    fun onFullscreenVideoDismiss() {
        val currentState = _uiState.value

        if (currentState !is ConversationUiState.Ready || currentState.fullscreenVideo == null) {
            return
        }

        _uiState.value = currentState.copy(
            fullscreenVideo = null,
        )
    }


    fun onAttachMedia(media: MediaUpload, sourceUri: Uri? = null) {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        if (media is MediaUpload.Video) {
            val validationError = media.validationError()
            if (validationError != null) {
                _uiState.update { current ->
                    if (current is ConversationUiState.Ready) {
                        current.copy(
                            attachingMedia = false,
                            transientMediaError = SendMessageOutcome.Failure.Server(
                                code = 422,
                                message = validationError.toString(),
                            ),
                        )
                    } else {
                        current
                    }
                }
                return
            }
        }
        val pending = when (media) {
            is MediaUpload.Image -> PendingMedia(
                localUri = sourceUri?.toString(),
                mimeType = media.mimeType,
                originalName = media.originalName,
                sizeBytes = media.bytes.size.toLong(),
                bytes = media.bytes,
                kind = PendingMediaKind.IMAGE,
                durationMillis = 0L,
            )
            is MediaUpload.Audio -> PendingMedia(
                localUri = sourceUri?.toString(),
                mimeType = media.mimeType,
                originalName = media.originalName,
                sizeBytes = media.bytes.size.toLong(),
                bytes = media.bytes,
                kind = PendingMediaKind.AUDIO,
                durationMillis = media.durationMillis,
            )
            is MediaUpload.Video -> PendingMedia(
                localUri = sourceUri?.toString(),
                mimeType = media.mimeType,
                originalName = media.originalName,
                sizeBytes = media.bytes.size.toLong(),
                bytes = media.bytes,
                kind = PendingMediaKind.VIDEO,
                durationMillis = media.durationMillis,
                width = media.width,
                height = media.height,
                videoCodec = media.videoCodec,
                audioCodec = media.audioCodec,
            )
        }
        _uiState.update { current ->
            if (current is ConversationUiState.Ready) {
                // Images append so the consumer can stage several
                // in one session; audio overrides the previous
                // audio (a single recorder per message keeps the
                // wire payload predictable).
                val merged = if (pending.kind != PendingMediaKind.IMAGE) {
                    listOf(pending)
                } else {
                    current.pendingMedia + pending
                }
                current.copy(
                    attachingMedia = false,
                    pendingMedia = merged,
                    transientMediaError = null,
                )
            } else {
                current
            }
        }
    }

    private fun applyAttachFailure(t: Throwable) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(
                    attachingMedia = false,
                    transientMediaError =
                        SendMessageOutcome.Failure.Network(t),
                )
            } else {
                state
            }
        }
    }

    /**
     * Discards the staged [PendingMedia] and any transient media
     * error. No-op outside `Ready`. Bytes are released to the GC
     * alongside the `pendingMedia` field clear.
     */
    fun onDiscardMediaPreview() {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(
                    pendingMedia = emptyList(),
                    attachingMedia = false,
                    sendingMedia = false,
                    transientMediaError = null,
                )
            } else {
                state
            }
        }
    }

    /**
     * Confirms the staged [PendingMedia] and uploads it through
     * the [SendMediaMessageUseCase]. The pending bytes are read
     * directly from the state (cached at attach time) so we
     * don't depend on the picker URI still being readable — that
     * permission can be revoked between attach and confirm.
     *
     * On success, the server-persisted bubble is appended to
     * `detail.messages` and the preview cleared. On failure, the
     * preview is kept (so the user can retry without re-picking)
     * and the typed failure surfaces in `transientMediaError`.
     */
    fun onConfirmMediaSend() {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        val pending = state.pendingMedia
        if (pending.isEmpty()) return
        if (state.sendingMedia) return

        _uiState.update { current ->
            if (current is ConversationUiState.Ready) {
                current.copy(
                    sendingMedia = true,
                    transientMediaError = null,
                )
            } else {
                current
            }
        }

        viewModelScope.launch {
            val upload = pending.map { entry ->
                when (entry.kind) {
                    PendingMediaKind.IMAGE -> MediaUpload.Image(
                        bytes = entry.bytes,
                        mimeType = entry.mimeType,
                        originalName = entry.originalName,
                    )

                    PendingMediaKind.AUDIO -> MediaUpload.Audio(
                        bytes = entry.bytes,
                        mimeType = entry.mimeType,
                        originalName = entry.originalName,
                        durationMillis = entry.durationMillis,
                    )

                    PendingMediaKind.VIDEO -> MediaUpload.Video(
                        bytes = entry.bytes,
                        mimeType = entry.mimeType,
                        originalName = entry.originalName,
                        durationMillis = entry.durationMillis,
                        width = entry.width,
                        height = entry.height,
                        videoCodec = entry.videoCodec,
                        audioCodec = entry.audioCodec,
                    )
                }
            }
            val outcome = if (state.promptInput.isBlank()) {
                sendMediaMessage(state.detail.id, upload)
            } else {
                sendMediaMessage(state.detail.id, upload, state.promptInput)
            }
            when (outcome) {
                is SendMessageOutcome.Success ->
                    applyMediaServerResponse(outcome.message)
                is SendMessageOutcome.Failure.Network ->
                    applyMediaSendFailure(outcome)
                is SendMessageOutcome.Failure.Server ->
                    applyMediaSendFailure(outcome)
                is SendMessageOutcome.Failure.Unauthorized ->
                    applyMediaSendFailure(outcome)
                is SendMessageOutcome.Failure.PayloadTooLarge ->
                    applyMediaSendFailure(outcome)
            }
        }
    }

    private fun applyMediaServerResponse(sentMessage: ConversationMessage) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(
                    sendingMedia = false,
                    promptInput = "",
                    pendingMedia = emptyList(),
                    transientMediaError = null,
                    detail = state.detail.copy(
                        messages = state.detail.messages + sentMessage,
                    ),
                )
            } else {
                state
            }
        }
    }

    private fun applyMediaSendFailure(failure: SendMessageOutcome.Failure) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(
                    sendingMedia = false,
                    transientMediaError = failure,
                    // `pendingMedia` is preserved so the retry CTA
                    // can resubmit without the user re-picking.
                )
            } else {
                state
            }
        }
    }

    fun onStartAudioRecording() {
        val state = _uiState.value

        if (state !is ConversationUiState.Ready) {
            return
        }

        if (state.recordingAudio || state.attachingMedia || state.sendingMedia) {
            return
        }

        val result = audioRecorder.start()

        if (result.isSuccess) {
            _uiState.update { current ->
                if (current is ConversationUiState.Ready) {
                    current.copy(
                        recordingAudio = true,
                        transientMediaError = null,
                    )
                } else {
                    current
                }
            }
        } else {
            applyAttachFailure(
                result.exceptionOrNull()
                    ?: IllegalStateException("Could not start audio recording"),
            )
        }
    }

    fun onStopAudioRecording() {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        if (!state.recordingAudio) return

        val result = audioRecorder.stop()

        if (result.isSuccess) {
            val uri = result.getOrNull()

            if (uri == null) {
                applyAttachFailure(
                    IllegalStateException("Audio recorder returned an empty Uri"),
                )
                return
            }

            _uiState.update { current ->
                if (current is ConversationUiState.Ready) {
                    current.copy(recordingAudio = false)
                } else {
                    current
                }
            }

            onAttachAudioFromUri(uri)
        } else {
            _uiState.update { current ->
                if (current is ConversationUiState.Ready) {
                    current.copy(recordingAudio = false)
                } else {
                    current
                }
            }

            applyAttachFailure(
                result.exceptionOrNull()
                    ?: IllegalStateException("Could not stop audio recording"),
            )
        }
    }

    fun onCancelAudioRecording() {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        if (!state.recordingAudio) return

        audioRecorder.cancel()

        _uiState.update { current ->
            if (current is ConversationUiState.Ready) {
                current.copy(
                    recordingAudio = false,
                    transientMediaError = null,
                )
            } else {
                current
            }
        }
    }
}

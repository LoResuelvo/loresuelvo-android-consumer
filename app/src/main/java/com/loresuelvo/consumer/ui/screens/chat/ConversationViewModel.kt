package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.data.api.WebSocketClient
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationByIdUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMediaMessageUseCase
import com.loresuelvo.consumer.domain.usecase.conversation.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the consumer ↔ provider conversation detail
 * screen (`Route.Conversation`). Drives `GET /conversations/{id}`
 * through [GetConversationByIdUseCase] and `POST
 * /conversations/{id}/messages` through [SendMessageUseCase],
 * mapping the typed outcomes into the sealed
 * [ConversationUiState].
 *
 * Detail loading:
 *  - `Success(detail)` → [ConversationUiState.Ready] with empty
 *    prompt and `sending = false`.
 *  - `Failure.Network / .Server / .Unauthorized` →
 *    [ConversationUiState.Error] carrying the typed failure.
 *
 * Send flow (mirrors the AI diagnostic's [ChatViewModel]):
 *  - [onPromptChange] mirrors the field on the current
 *    [ConversationUiState.Ready] and clears any prior
 *    `transientError`.
 *  - [onSendClick]:
 *      * Trims the prompt, bails on blank or `sending = true`.
 *      * Snapshots the previous state and clears the prompt +
 *        flips `sending = true` + clears the prior
 *        `transientError` synchronously.
 *      * Fires [sendMessage]; on success, appends the
 *        server-persisted message to `detail.messages` and
 *        clears `lastAttemptedPrompt`; on failure, surfaces the
 *        typed failure in `transientError` and preserves the
 *        prompt in `lastAttemptedPrompt` so [onRetryClick] can
 *        resubmit it.
 *  - [onRetryClick] re-fires `sendMessage` with
 *    `lastAttemptedPrompt`. No-op when no previous failure or a
 *    previous send is in flight.
 *  - [onErrorDismiss] clears `transientError` without re-firing
 *    (the user can still hit the retry CTA — `lastAttemptedPrompt`
 *    is kept).
 *
 * The composer is **never** gated on `ConversationStatus.Pending`
 * (scenario 05-IC: "without restrictions"). The "Pendiente" badge
 * in the top bar is informational only.
 *
 * The conversation id is provided by the host
 * ([com.loresuelvo.consumer.ui.navigation.ConversationRoute])
 * via [load] on first composition (and on screen-level retry).
 * Hilt scopes the VM to the route entry so the same instance
 * survives configuration changes.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val getConversationById: GetConversationByIdUseCase,
    private val sendMessage: SendMessageUseCase,
    private val sendMediaMessage: SendMediaMessageUseCase,
    private val mediaReader: MediaReader,
    private val webSocketClient: WebSocketClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationUiState>(
        ConversationUiState.Loading,
    )
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        // Start the WebSocket on first VM construction. The
        // client is `@Singleton` and `start()` is idempotent, so
        // the connection is shared across re-entries (when the
        // user navigates Home and back, a fresh VM is created but
        // the WS keeps streaming). The app-wide subscription below
        // filters events to the currently-loaded conversation id
        // and appends provider bubbles in real-time (scenario 07-IC)
        // while ignoring other conversations' events (08-IC) and
        // echoes of the consumer's own sends.
        webSocketClient.start()
        viewModelScope.launch {
            webSocketClient.events
                .filter { event -> currentConversationIdMatches(event.conversationId) }
                .filter { event -> event.message.sender == com.loresuelvo.consumer.domain.conversation.ConversationSender.Provider }
                .collect { event -> appendIncomingMessage(event.message) }
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
            // Scenario 09-IC: when the user is at the bottom, the
            // screen renders the new bubble immediately (auto-
            // scroll) so no "new message" indicator is needed.
            // Scenario 10-IC: when the user is scrolled up reading
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

    /**
     * Loads the conversation detail for [conversationId]. Public
     * so the host can re-trigger on retry (and the host invokes
     * it once on first composition with the nav argument).
     */
    fun load(conversationId: String) {
        viewModelScope.launch {
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
            _uiState.update { next }
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

    // ---- Media attachment (01-MM onwards) --------------------------

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

    /**
     * Stage a [MediaUpload] for confirmation. The canonical
     * attach surface for non-`Uri` callers (the BDD world, future
     * programmatic attach scenarios, and the audio recorder
     * flow that hands off a pre-built `MediaUpload.Audio` from
     * the route's [MediaMetadataRetriever] pass).
     *
     * Dispatches by [MediaUpload] subtype so the preview card
     * knows how to render the staged media:
     *  - `MediaUpload.Image` → [PendingMediaKind.IMAGE], no
     *    duration (image bubbles don't have a scrubber).
     *  - `MediaUpload.Audio` → [PendingMediaKind.AUDIO],
     *    durationMillis populated from the recorder / metadata
     *    retriever so the player can drive its progress bar
     *    (03-MM).
     *
     * The host's `onAttachImageFromGallery` reads the picker URI
     * via [MediaReader] and forwards the result here with the
     * original `sourceUri` so the preview card can render the
     * real thumbnail.
     */
    fun onAttachMedia(media: MediaUpload, sourceUri: Uri? = null) {
        val state = _uiState.value
        if (state !is ConversationUiState.Ready) return
        val pending = when (media) {
            is MediaUpload.Image -> PendingMedia(
                localUri = sourceUri,
                mimeType = media.mimeType,
                originalName = media.originalName,
                sizeBytes = media.bytes.size.toLong(),
                bytes = media.bytes,
                kind = PendingMediaKind.IMAGE,
                durationMillis = 0L,
            )
            is MediaUpload.Audio -> PendingMedia(
                localUri = sourceUri,
                mimeType = media.mimeType,
                originalName = media.originalName,
                sizeBytes = media.bytes.size.toLong(),
                bytes = media.bytes,
                kind = PendingMediaKind.AUDIO,
                durationMillis = media.durationMillis,
            )
        }
        _uiState.update { current ->
            if (current is ConversationUiState.Ready) {
                current.copy(
                    attachingMedia = false,
                    pendingMedia = pending,
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
                    pendingMedia = null,
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
        val pending = state.pendingMedia ?: return
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
            val upload = MediaUpload.Image(
                bytes = pending.bytes,
                mimeType = pending.mimeType,
                originalName = pending.originalName,
            )
            when (val outcome = sendMediaMessage(state.detail.id, upload)) {
                is SendMessageOutcome.Success ->
                    applyMediaServerResponse(outcome.message)
                is SendMessageOutcome.Failure.Network ->
                    applyMediaSendFailure(outcome)
                is SendMessageOutcome.Failure.Server ->
                    applyMediaSendFailure(outcome)
                is SendMessageOutcome.Failure.Unauthorized ->
                    applyMediaSendFailure(outcome)
            }
        }
    }

    private fun applyMediaServerResponse(sentMessage: ConversationMessage) {
        _uiState.update { state ->
            if (state is ConversationUiState.Ready) {
                state.copy(
                    sendingMedia = false,
                    pendingMedia = null,
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
}
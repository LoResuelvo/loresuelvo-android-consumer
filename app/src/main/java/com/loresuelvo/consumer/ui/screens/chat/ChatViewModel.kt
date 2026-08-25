package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.LoadAiConversationOutcome
import com.loresuelvo.consumer.domain.diagnosis.Sender
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.usecase.LoadAiConversationUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the AI diagnostic chat screen.
 *
 * Commit 04-DIA wires the failure path:
 *
 *  - [onPromptChange] keeps [ChatUiState.promptInput] in sync.
 *  - [onSendClick]:
 *      - Trims the prompt, bails on blank or `sending = true`.
 *      - Captures the prompt in `lastAttemptedPrompt` (used by
 *        [onRetryClick] later).
 *      - Appends the optimistic bubble, clears the input, and
 *        flips `sending = true` + clears any prior `transientError`.
 *      - Delegates to [fireSend] which performs the round-trip
 *        and applies either [applyServerResponse] (Success) or
 *        [applySendFailure] (Failure.Network / .Server /
 *        .Unauthorized → [ChatError]).
 *  - [onRetryClick] resubmits `lastAttemptedPrompt`. No-op when
 *    no error is showing or a previous send is in flight.
 *  - [onErrorDismiss] clears `transientError` without re-firing.
 *  - [loadExisting] (commit 5c) hydrates the chat scroll from a
 *    saved AI session: the Assistant list → tap a row navigates
 *    to `Route.Chat.buildPath(conversationId)` and the route
 *    calls [loadExisting] which fetches the conversation detail
 *    via [loadAiConversation] and populates `state.messages` +
 *    `state.assessment` + `state.recommendedProviders`. A second
 *    call with the same id is a no-op so a recomposition of
 *    the route (e.g. config change) doesn't re-fire the
 *    round-trip. The state machine then transitions cleanly
 *    into the existing send flow — the loaded `state.conversationId`
 *    is what [fireSend] reads so the next prompt appends to the
 *    saved conversation.
 *
 * Idempotency: `lastAttemptedPrompt` is snapshotted from the
 * `state.promptInput.value()` before the optimistic append, so
 * parallel `onSendClick`s can't race on the prompt.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendDiagnosisPrompt: SendDiagnosisPromptUseCase,
    private val loadAiConversation: LoadAiConversationUseCase,
    private val mediaReader: MediaReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(promptInput = value) }
    }

    fun onSendClick() {
        val state = _uiState.value
        val prompt = state.promptInput.trim()
        if (prompt.isEmpty() || state.sending) return

        val snapshot = state
        _uiState.update {
            it.copy(
                messages = snapshot.messages + optimisticMessage(prompt),
                promptInput = "",
                sending = true,
                transientError = null,
                lastAttemptedPrompt = prompt,
            )
        }
        fireSend(prompt, snapshot.conversationId)
    }

    fun onRetryClick() {
        val state = _uiState.value
        val prompt = state.lastAttemptedPrompt
        if (prompt.isNullOrBlank() || state.sending) return
        _uiState.update {
            it.copy(
                sending = true,
                transientError = null,
            )
        }
        fireSend(prompt, state.conversationId)
    }

    fun onErrorDismiss() {
        _uiState.update { it.copy(transientError = null) }
    }

    /**
     * Reads the image URI the system's gallery picker returned
     * via [MediaReader] and appends the staged bytes to
     * `pendingAttachments`. Mirrors
     * [com.loresuelvo.consumer.ui.screens.chat.ConversationViewModel.onAttachImageFromGallery]
     * in the chat-with-provider surface so the BDD layer drives
     * the same wire contract on both VMs.
     *
     * Read failures land in a follow-up commit (08-AIP) that
     * introduces a typed `pendingAttachmentError`; for now any
     * [Throwable] from the picker drops the request silently so
     * the BDD layer can pin the happy path without an extra
     * flag.
     */
    fun onAttachImageFromGallery(uri: Uri) {
        viewModelScope.launch {
            try {
                val media = mediaReader.read(uri)
                onAttachMedia(media, sourceUri = uri)
            } catch (_: Throwable) {
                // See `pendingAttachments` Javadoc for the
                // follow-up commit that introduces a typed
                // attach error.
            }
        }
    }

    /**
     * Canonical non-`Uri` entry point used by the BDD world and
     * any future programmatic attach scenario. Appends an image
     * to `pendingAttachments` preserving the call order (used
     * by 03-AIP for multiple images). Audio is rejected for now
     * — only images travel on the AI chat surface today; a
     * future scenario can extend the `when` dispatch.
     */
    fun onAttachMedia(media: MediaUpload, sourceUri: Uri? = null) {
        require(media is MediaUpload.Image) {
            "AI chat only supports image attachments; got ${media::class.simpleName}"
        }
        val pending = PendingMedia(
            localUri = sourceUri,
            mimeType = media.mimeType,
            originalName = media.originalName,
            sizeBytes = media.bytes.size.toLong(),
            bytes = media.bytes,
            kind = PendingMediaKind.IMAGE,
            durationMillis = 0L,
        )
        _uiState.update { current ->
            current.copy(pendingAttachments = current.pendingAttachments + pending)
        }
    }

    /**
     * Loads a saved AI session. Called by [ChatRoute] when the
     * route was opened with a `conversationId` arg. The
     * conversationId is set to [state.conversationId] on success
     * so the next [onSendClick] uses the right conversation id.
     *
     * No-op when:
     *  - the same id is already loaded (idempotent — recomposition
     *    of the route can re-fire [LaunchedEffect] safely);
     *  - a send is in flight (don't clobber the optimistic
     *    bubble + active round-trip).
     */
    fun loadExisting(conversationId: String) {
        val state = _uiState.value
        if (state.conversationId == conversationId) return
        if (state.sending) return
        _uiState.update { it.copy(sending = true, transientError = null) }
        viewModelScope.launch {
            when (val outcome = loadAiConversation(conversationId)) {
                is LoadAiConversationOutcome.Success -> applyLoadedConversation(outcome)
                is LoadAiConversationOutcome.Failure.Network ->
                    applyLoadFailure(ChatError.Network)
                is LoadAiConversationOutcome.Failure.Server ->
                    applyLoadFailure(ChatError.ServiceUnavailable)
                is LoadAiConversationOutcome.Failure.Unauthorized ->
                    applyLoadFailure(ChatError.Unauthorized(outcome.message))
            }
        }
    }

    private fun fireSend(prompt: String, conversationId: String?) {
        viewModelScope.launch {
            val outcome = sendDiagnosisPrompt(prompt, conversationId)
            when (outcome) {
                is SendDiagnosisPromptOutcome.Success -> applyServerResponse(outcome)
                is SendDiagnosisPromptOutcome.Failure.Network ->
                    applySendFailure(ChatError.Network)
                is SendDiagnosisPromptOutcome.Failure.Server ->
                    applySendFailure(ChatError.ServiceUnavailable)
                is SendDiagnosisPromptOutcome.Failure.Unauthorized ->
                    applySendFailure(ChatError.Unauthorized(outcome.message))
            }
        }
    }

    private fun applyServerResponse(outcome: SendDiagnosisPromptOutcome.Success) {
        val diagnosis = outcome.diagnosis
        _uiState.update {
            it.copy(
                sending = false,
                conversationId = diagnosis.conversationId ?: it.conversationId,
                messages = diagnosis.messages,
                assessment = diagnosis.assessment ?: it.assessment,
                recommendedProviders = diagnosis.recommendedProviders ?: it.recommendedProviders,
                transientError = null,
                lastAttemptedPrompt = null,
            )
        }
    }

    private fun applyLoadedConversation(outcome: LoadAiConversationOutcome.Success) {
        val diagnosis = outcome.diagnosis
        _uiState.update {
            it.copy(
                sending = false,
                conversationId = diagnosis.conversationId ?: it.conversationId,
                messages = diagnosis.messages,
                assessment = diagnosis.assessment ?: it.assessment,
                recommendedProviders = diagnosis.recommendedProviders ?: it.recommendedProviders,
                transientError = null,
                lastAttemptedPrompt = null,
            )
        }
    }

    private fun applySendFailure(error: ChatError) {
        _uiState.update {
            it.copy(
                sending = false,
                transientError = error,
                // messages list is preserved so the user's optimistic
                // bubble stays in place while the error card is shown;
                // the producer can clear it via `onErrorDismiss` or
                // successful `onRetryClick`.
            )
        }
    }

    private fun applyLoadFailure(error: ChatError) {
        _uiState.update {
            it.copy(
                sending = false,
                transientError = error,
                // The chat scroll stays empty on load failure
                // (no messages were hydrated). The error card
                // surfaces the failure; the user can retry by
                // re-entering from the Assistant list.
            )
        }
    }

    private fun optimisticMessage(prompt: String): ChatMessage = ChatMessage(
        id = USER_MESSAGE_ID_PREFIX + UUID.randomUUID(),
        sender = Sender.Consumer,
        content = prompt,
        sentAtEpochMillis = System.currentTimeMillis(),
    )

    private companion object {
        const val USER_MESSAGE_ID_PREFIX = "user-"
    }
}

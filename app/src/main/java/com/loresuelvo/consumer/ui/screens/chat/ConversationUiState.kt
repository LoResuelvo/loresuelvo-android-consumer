package com.loresuelvo.consumer.ui.screens.chat

import android.net.Uri
import com.loresuelvo.consumer.domain.conversation.ConversationDetail
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome

/**
 * UDF state for the consumer ↔ provider conversation detail
 * screen (`Route.Conversation`). Modelled as a sealed hierarchy
 * so the screen renders exactly one of the states without
 * boolean flags — mirrors
 * [com.loresuelvo.consumer.ui.screens.messages.MessagesListUiState]
 * and the AI diagnostic's
 * [com.loresuelvo.consumer.ui.screens.chat.ChatUiState].
 *
 *  - [Loading] — initial fetch of the conversation detail in
 *    flight.
 *  - [Ready] — detail loaded; the composer is live and the user
 *    may send messages. The conversation can be in any
 *    `ConversationStatus` (Pending / Accepted / Other) — the
 *    composer is NOT gated on acceptance (scenario 05-IC
 *    asserts "without restrictions"). The screen renders a
 *    "Pendiente" badge in the top bar when the conversation is
 *    still awaiting the provider's acceptance.
 *  - [Error] — the initial detail fetch failed. The screen
 *    renders the typed failure copy and a retry button. Sending
 *    a message while in this state is a no-op (the composer is
 *    not rendered).
 *
 * Once in [Ready], transient send failures land in
 * [Ready.transientError] (a card pinned above the composer with
 * a retry CTA). The last attempted prompt is snapshotted in
 * [Ready.lastAttemptedPrompt] so the retry CTA can resubmit
 * without the user re-typing.
 *
 * Real-time incoming messages (scenarios 09-IC / 10-IC) are
 * driven by two flags:
 *  - [Ready.isAtBottom] — whether the LazyList is currently
 *    scrolled to the last visible item (true) or the user has
 *    scrolled up reading older messages (false). The screen
 *    reports this back via [ConversationViewModel.onScrollPositionChanged].
 *  - [Ready.hasUnreadIncoming] — `true` when a new provider
 *    message arrived while the user was scrolled up; the screen
 *    renders a "↓ nuevo mensaje" banner the user can tap to jump
 *    to the bottom. Cleared when the user scrolls back to the
 *    bottom OR when a fresh message arrives while they're
 *    already at the bottom.
 *
 * Media attachment state (01-MM onwards):
 *  - [Ready.pendingMedia] — the locally-attached media awaiting
 *    confirmation. `null` when nothing is staged.
 *  - [Ready.attachingMedia] — `true` while the
 *    [com.loresuelvo.consumer.data.media.MediaReader] is reading
 *    the URI from the picker.
 *  - [Ready.sendingMedia] — `true` while the multipart upload is
 *    in flight.
 *  - [Ready.transientMediaError] — typed failure from the read
 *    or the upload. Renders an inline card above the input bar
 *    with retry / dismiss CTAs.
 *  - [Ready.recordingAudio] — `true` while the system voice
 *    recorder is actively recording audio for the 03-MM flow.
 */
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
        val pendingMedia: PendingMedia? = null,
        val attachingMedia: Boolean = false,
        val recordingAudio: Boolean = false,
        val audioPlayback: AudioPlaybackState = AudioPlaybackState(),
        val sendingMedia: Boolean = false,
        val transientMediaError: SendMessageOutcome.Failure? = null,
        val fullscreenImage: MediaReference.Image? = null,
    ) : ConversationUiState

    data class Error(val failure: ConversationDetailOutcome.Failure) : ConversationUiState
}

/**
 * Visual kind discriminator for [PendingMedia]. The sealed
 * hierarchy was deliberately flattened to a string-shaped
 * enum so the existing JSON test data (`PendingMedia(...)`
 * constructor calls in the unit and Compose layers) keeps
 * compiling — adding a new variant only requires updating the
 * mapper inside the preview composable, not the data class
 * shape.
 *
 *  - [IMAGE] — gallery picker (01-MM), camera capture (02-MM),
 *    future inline camera shortcut.
 *  - [AUDIO] — system voice recorder (03-MM). Carries
 *    [PendingMedia.durationMillis] for the player scrubber.
 */
enum class PendingMediaKind { IMAGE, AUDIO }

/**
 * Locally-attached media awaiting the user's confirmation. The
 * data class lives in the UI layer because it carries the
 * Android `Uri` (used to render the preview thumbnail) and the
 * bytes (cached for the confirm step so we don't re-read the
 * file). The screen renders a preview card from this state and
 * clears it on send-success or on explicit discard.
 *
 * [localUri] is nullable because the BDD world (and any future
 * programmatic attach scenario) constructs a [MediaUpload]
 * without going through the Android picker, so there is no URI
 * to surface. The preview card falls back to the placeholder
 * thumbnail when `localUri` is `null`.
 *
 * Bytes are cached (rather than re-reading from the URI on
 * confirm) to keep the upload deterministic — the file system
 * can revoke the temporary URI permission between attach and
 * confirm, which would otherwise crash the upload with a
 * permission-denied `IOException` that's hard to distinguish
 * from a transient network failure.
 *
 * [durationMillis] is `0` for non-audio media. The audio
 * preview uses it to seed the player scrubber (03-MM).
 */
data class PendingMedia(
    val localUri: Uri?,
    val mimeType: String,
    val originalName: String,
    val sizeBytes: Long,
    val bytes: ByteArray,
    val kind: PendingMediaKind = PendingMediaKind.IMAGE,
    val durationMillis: Long = 0L,
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
            durationMillis == other.durationMillis
    }

    override fun hashCode(): Int {
        var result = localUri?.hashCode() ?: 0
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + originalName.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
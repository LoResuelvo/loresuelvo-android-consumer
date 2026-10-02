package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.ui.screens.chat.components.ConversationTopBar
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Consumer ↔ provider conversation detail screen
 * (`Route.Conversation`). Replaces the previous placeholder
 * scaffold. Drives the [ConversationViewModel] via the UDF
 * [ConversationUiState].
 *
 * State rendering:
 *  - [ConversationUiState.Loading] → centred spinner.
 *  - [ConversationUiState.Error] → typed copy + retry button.
 *    The retry calls back to the host, which re-invokes the
 *    VM's [ConversationViewModel.load] with the same id.
 *  - [ConversationUiState.Ready] → top bar + scrolling message
 *    list + composer. The composer is **never** gated on
 *    `ConversationStatus.Pending` (scenario 05-IC: "without
 *    restrictions"). A transient [SendMessageOutcome.Failure]
 *    surfaces as a card pinned above the composer with retry +
 *    dismiss callbacks.
 *
 * Media attach surface (01-MM onwards):
 *  - The [ChatInputBar] receives `onAttachClick = { showAttachSheet = true }`
 *    so the `+` button is rendered to the LEFT of the prompt.
 *  - Tapping the button surfaces [MediaAttachSheet]; tapping
 *    "Galería" calls [onGalleryClick] (the host owns the
 *    `ActivityResultContracts.PickVisualMedia` launcher) which
 *    ultimately drives [ConversationViewModel.onAttachImageFromGallery].
 *  - Once a media is staged, [MediaPreviewCard] renders between
 *    the list and the composer with Send + Discard actions.
 *  - The transient-media-error card lives just above the
 *    composer and uses the same retry / dismiss pattern as the
 *    text transient error card.
 *
 * Auto-scroll: a [LaunchedEffect] keyed on the message count
 * scrolls to the freshly-added bubble so the consumer's just-
 * sent message is always visible. We deliberately do NOT
 * implement the "respect reader position" gate that the AI
 * diagnostic chat has (see `MessagesList.shouldAutoScroll`) —
 * for the provider chat the user is expected to stay at the
 * bottom; scrolling up to re-read history is an edge case the
 * Gherkin does not yet cover.
 *
 * The host (`ConversationRoute` in `LoResuelvoNav`) is the only
 * place that owns the navigation callback and re-invokes the
 * VM's `load(conversationId)` after composition.
 */
@Composable
fun ConversationScreen(
    state: ConversationUiState,
    actions: ConversationScreenActions = ConversationScreenActions(),
    proposalSummaryState: ConversationProposalSummaryUiState =
        ConversationProposalSummaryUiState.Empty,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(CONVERSATION_SCREEN_TAG),
        contentAlignment = Alignment.Center,
    ) {
        // 08-UXUI: the original `Box + Column.windowInsetsPadding`
        // design produced a permanent bottom gap and doubled the
        // padding when the IME opened. Switching to `Scaffold` with
        // a dedicated `topBar` and `bottomBar` mirrors the
        // `ChatScreen` pattern, which the consumer already
        // approved visually: the `Scaffold` consumes the status
        // bar inset for the `topBar` and the nav bar / IME inset
        // for the `bottomBar`, and the `bottomBar` only needs
        // `imePadding()` to lift above the keyboard.
        when (state) {
            is ConversationUiState.Loading -> LoadingState()
            is ConversationUiState.Error -> ErrorState(
                failure = state.failure,
                onRetryClick = actions.errors.onRetry,
            )
            is ConversationUiState.Ready -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        ConversationTopBar(
                            counterpart = state.detail.counterpart,
                            status = state.detail.status,
                            onBackClick = actions.navigation.onBack,
                            // US-27 scenario 02-VTD: when the
                            // conversation has an associated work
                            // order, the top bar surfaces an icon
                            // button that navigates to the work-order
                            // detail screen via the route handler.
                            // `null` for pre-acceptance conversations
                            // hides the button entirely.
                            onViewWorkOrder = state.detail.workOrderId
                                ?.let { workOrderId ->
                                    { actions.navigation.onViewWorkOrder?.invoke(workOrderId) }
                                },
                        )
                    },
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                // 08-UXUI: the `Scaffold` already
                                // consumes the IME inset for the
                                // `bottomBar` (the bar lifts above
                                // the keyboard automatically). The
                                // bottom padding adds breathing room
                                // above the keyboard for a less
                                // cramped feel — the check on
                                // `WindowInsets.ime` keeps the bar
                                // flush against the navigation bar
                                // when the keyboard is closed.
                                .padding(
                                    bottom = if (WindowInsets.ime.asPaddingValues()
                                            .calculateBottomPadding() > 0.dp) 20.dp else 0.dp,
                                ),
                        ) {
                            if (state.transientError != null) {
                                TransientErrorCard(
                                    failure = state.transientError,
                                    onRetryClick = actions.composer.onSend,
                                    onDismiss = actions.errors.onDismiss,
                                )
                            }
                            if (state.pendingMedia.isNotEmpty()) {
                                state.pendingMedia.forEachIndexed { index, attachment ->
                                    MediaPreviewCard(
                                        pendingMedia = attachment,
                                        sending = state.sendingMedia,
                                        onSendClick = actions.media.onConfirmSend,
                                        onDiscardClick = actions.media.onDiscard,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(
                                                "$CONVERSATION_ATTACHMENT_CARD_TAG_PREFIX-$index",
                                            ),
                                    )
                                }
                                if (state.transientMediaError != null) {
                                    MediaTransientErrorCard(
                                        failure = state.transientMediaError,
                                        onRetryClick = actions.media.onConfirmSend,
                                        onDismiss = actions.media.onErrorDismiss,
                                    )
                                }
                            }
                            ChatInputBar(
                                promptInput = state.promptInput,
                                canSend = state.promptInput.isNotBlank() && !state.sending,
                                sending = state.sending,
                                recordingAudio = state.recordingAudio,
                                onPromptChange = actions.composer.onPromptChange,
                                onSendClick = actions.composer.onSend,
                                onStartAudioRecording = actions.composer.onStartAudioRecording,
                                onStopAudioRecording = actions.composer.onStopAudioRecording,
                                onAttachClick = actions.composer.onAttach,
                            )
                        }
                    },
                ) { padding ->
                    ConversationReadyContent(
                        state = state,
                        paddingValues = padding,
                        actions = actions,
                        proposalSummaryState = proposalSummaryState,
                    )
                }
            }
        }
        MediaAttachSheet(
            show = actions.media.showAttachSheet,
            onDismiss = actions.media.onAttachSheetDismiss,
            onGalleryClick = actions.media.onGallery,
            onCameraClick = actions.media.onCamera,
            onVideoClick = actions.media.onVideo,
        )

        (state as? ConversationUiState.Ready)?.fullscreenImage?.let { image ->
            FullScreenImageViewer(
                image = image,
                onDismiss = actions.playback.onFullscreenImageDismiss,
            )
        }

        (state as? ConversationUiState.Ready)?.fullscreenVideo?.let { video ->
            FullScreenVideoViewer(
                video = video,
                onDismiss = actions.playback.onFullscreenVideoDismiss,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(CONVERSATION_LOADING_TAG),
        )
        Text(
            text = stringResource(R.string.conversation_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun ErrorState(
    failure: ConversationDetailOutcome.Failure,
    onRetryClick: () -> Unit,
) {
    val message = when (failure) {
        is ConversationDetailOutcome.Failure.Network ->
            stringResource(R.string.conversation_error_network)
        is ConversationDetailOutcome.Failure.Server ->
            stringResource(R.string.conversation_error_server)
        is ConversationDetailOutcome.Failure.Unauthorized ->
            stringResource(R.string.conversation_error_unauthorized)
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(CONVERSATION_ERROR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onRetryClick,
            modifier = Modifier.testTag(CONVERSATION_ERROR_RETRY_TAG),
        ) {
            Text(
                text = stringResource(R.string.conversation_error_retry),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun TransientErrorCard(
    failure: SendMessageOutcome.Failure,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = when (failure) {
        is SendMessageOutcome.Failure.Network ->
            stringResource(R.string.conversation_transient_error_network)
        is SendMessageOutcome.Failure.Server ->
            stringResource(R.string.conversation_transient_error_server)
        is SendMessageOutcome.Failure.Unauthorized ->
            stringResource(R.string.conversation_transient_error_unauthorized)
        is SendMessageOutcome.Failure.PayloadTooLarge ->
            // The text-message path can't produce a
            // PayloadTooLarge (text has no size limit), but the
            // sealed type forces an explicit branch — fall back
            // to the generic server copy defensively.
            stringResource(R.string.conversation_transient_error_server)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(CONVERSATION_TRANSIENT_ERROR_TAG),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(CONVERSATION_TRANSIENT_ERROR_DISMISS_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.conversation_transient_error_dismiss),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(
                    onClick = onRetryClick,
                    modifier = Modifier.testTag(CONVERSATION_TRANSIENT_ERROR_RETRY_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.conversation_transient_error_retry),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/**
 * Compose testTags for the conversation screen and its inner
 * slots. Pinning the locators (rather than literal Spanish
 * labels) keeps the Compose tests locale-independent.
 */
const val CONVERSATION_SCREEN_TAG: String = "conversation-screen"
const val CONVERSATION_LOADING_TAG: String = "conversation-loading"
const val CONVERSATION_ERROR_TAG: String = "conversation-error"
const val CONVERSATION_ERROR_RETRY_TAG: String = "conversation-error-retry"
const val CONVERSATION_LIST_TAG: String = "conversation-list"
const val CONVERSATION_TRANSIENT_ERROR_TAG: String = "conversation-transient-error"
const val CONVERSATION_TRANSIENT_ERROR_RETRY_TAG: String = "conversation-transient-error-retry"
const val CONVERSATION_TRANSIENT_ERROR_DISMISS_TAG: String = "conversation-transient-error-dismiss"

/**
 * Companion of [TransientErrorCard] for the media upload path
 * (01-MM). Same visual treatment (`errorContainer` surface +
 * dismiss / retry row), but the typed failure is the media-side
 * [SendMessageOutcome.Failure] and the copy uses the
 * `conversation_transient_media_error_*` strings so the wording
 * matches the file-attachment context.
 */
@Composable
private fun MediaTransientErrorCard(
    failure: SendMessageOutcome.Failure,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = when (failure) {
        is SendMessageOutcome.Failure.Network ->
            stringResource(R.string.conversation_transient_media_error_network)
        is SendMessageOutcome.Failure.Server ->
            stringResource(R.string.conversation_transient_media_error_server)
        is SendMessageOutcome.Failure.Unauthorized ->
            stringResource(R.string.conversation_transient_media_error_unauthorized)
        is SendMessageOutcome.Failure.PayloadTooLarge ->
            stringResource(
                R.string.conversation_transient_media_error_payload_too_large,
            )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(CONVERSATION_TRANSIENT_MEDIA_ERROR_TAG),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(CONVERSATION_TRANSIENT_MEDIA_ERROR_DISMISS_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.conversation_transient_error_dismiss),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(
                    onClick = onRetryClick,
                    modifier = Modifier.testTag(CONVERSATION_TRANSIENT_MEDIA_ERROR_RETRY_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.conversation_transient_error_retry),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

const val CONVERSATION_TRANSIENT_MEDIA_ERROR_TAG: String = "conversation-transient-media-error"
const val CONVERSATION_TRANSIENT_MEDIA_ERROR_RETRY_TAG: String = "conversation-transient-media-error-retry"
const val CONVERSATION_TRANSIENT_MEDIA_ERROR_DISMISS_TAG: String = "conversation-transient-media-error-dismiss"

/**
 * Compose testTag prefix for the per-attachment preview card.
 * Indexed (e.g. `conversation-attachment-card-0`) so the BDD step
 * can target the right entry without depending on its display
 * label.
 */
const val CONVERSATION_ATTACHMENT_CARD_TAG_PREFIX: String = "conversation-attachment-card"

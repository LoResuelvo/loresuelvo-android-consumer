package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.ui.screens.chat.components.CONVERSATION_MESSAGE_BUBBLE_TAG
import com.loresuelvo.consumer.ui.screens.chat.components.ConversationMessageBubble
import com.loresuelvo.consumer.ui.screens.chat.components.ConversationTopBar
import com.loresuelvo.consumer.ui.screens.chat.components.NewMessageBanner
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
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onAttachClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onConfirmMediaSend: () -> Unit = {},
    onDiscardMedia: () -> Unit = {},
    onMediaErrorDismiss: () -> Unit = {},
    onAttachSheetDismiss: () -> Unit = {},
    showAttachSheet: Boolean = false,
    onScrollPositionChanged: (Boolean) -> Unit = {},
    onUnreadBannerTapped: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag(CONVERSATION_SCREEN_TAG),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is ConversationUiState.Loading -> LoadingState()
            is ConversationUiState.Ready -> ReadyState(
                state = state,
                onPromptChange = onPromptChange,
                onSendClick = onSendClick,
                onBackClick = onBackClick,
                onErrorDismiss = onErrorDismiss,
                onAttachClick = onAttachClick,
                onGalleryClick = onGalleryClick,
                onConfirmMediaSend = onConfirmMediaSend,
                onDiscardMedia = onDiscardMedia,
                onMediaErrorDismiss = onMediaErrorDismiss,
                onScrollPositionChanged = onScrollPositionChanged,
                onUnreadBannerTapped = onUnreadBannerTapped,
            )
            is ConversationUiState.Error -> ErrorState(
                failure = state.failure,
                onRetryClick = onRetryClick,
            )
        }
        MediaAttachSheet(
            show = showAttachSheet,
            onDismiss = onAttachSheetDismiss,
            onGalleryClick = onGalleryClick,
        )
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
private fun ReadyState(
    state: ConversationUiState.Ready,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onAttachClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onConfirmMediaSend: () -> Unit,
    onDiscardMedia: () -> Unit,
    onMediaErrorDismiss: () -> Unit,
    onScrollPositionChanged: (Boolean) -> Unit,
    onUnreadBannerTapped: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Tracks whether the LazyList's last visible item is the
    // last item of the conversation — i.e. the user is "at the
    // bottom" (scenarios 09-IC / 10-IC). The screen reports
    // this back to the VM so it can decide whether to flag a
    // newly-arrived provider message as "unread" (banner) or
    // let the auto-scroll show it directly.
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount == 0 ||
                info.visibleItemsInfo.lastOrNull()?.index == info.totalItemsCount - 1
        }
    }
    LaunchedEffect(isAtBottom) {
        onScrollPositionChanged(isAtBottom)
    }

    // Auto-scroll on every new message so the consumer's just-sent
    // bubble is always visible. Keyed on the message count so the
    // effect re-fires when a new bubble lands (success path or
    // real-time provider push). Same `derivedStateOf`-style gate
    // as `shouldAutoScroll` in `MessagesList` (AI chat): if the
    // user is scrolled up reading older messages, we skip the
    // forced scroll so they keep their place — the new bubble
    // appears below and the unread banner surfaces instead.
    LaunchedEffect(state.detail.messages.size) {
        if (state.detail.messages.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        if (!isAtBottom || info.totalItemsCount <= info.visibleItemsInfo.size) return@LaunchedEffect
        listState.animateScrollToItem(state.detail.messages.size - 1)
    }

    val coroutineScope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) {
        ConversationTopBar(
            counterpart = state.detail.counterpart,
            status = state.detail.status,
            onBackClick = onBackClick,
        )
        Box(modifier = Modifier.weight(1f)) {
            MessagesList(
                messages = state.detail.messages,
                listState = listState,
            )
            // The unread banner overlays the list at the
            // bottom-edge of the scroll area (anchored to
            // `Alignment.BottomCenter`). Tapping it scrolls the
            // list back to the bottom and clears the unread flag
            // via `onUnreadBannerTapped`.
            if (state.hasUnreadIncoming) {
                NewMessageBanner(
                    onTap = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(
                                state.detail.messages.size - 1,
                            )
                        }
                        onUnreadBannerTapped()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
        }
        if (state.transientError != null) {
            TransientErrorCard(
                failure = state.transientError,
                onRetryClick = onSendClick,
                onDismiss = onErrorDismiss,
            )
        }
        if (state.pendingMedia != null) {
            MediaPreviewCard(
                pendingMedia = state.pendingMedia,
                sending = state.sendingMedia,
                onSendClick = onConfirmMediaSend,
                onDiscardClick = onDiscardMedia,
            )
            if (state.transientMediaError != null) {
                MediaTransientErrorCard(
                    failure = state.transientMediaError,
                    onRetryClick = onConfirmMediaSend,
                    onDismiss = onMediaErrorDismiss,
                )
            }
        }
        ChatInputBar(
            promptInput = state.promptInput,
            canSend = state.promptInput.isNotBlank() && !state.sending,
            sending = state.sending,
            onPromptChange = onPromptChange,
            onSendClick = onSendClick,
            onAttachClick = onAttachClick,
        )
    }
}

@Composable
private fun MessagesList(
    messages: List<com.loresuelvo.consumer.domain.conversation.ConversationMessage>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CONVERSATION_LIST_TAG),
        state = listState,
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(
            items = messages,
            key = { it.id },
        ) { message ->
            ConversationMessageBubble(message = message)
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
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.ui.screens.chat.components.CONVERSATION_MESSAGE_BUBBLE_TAG
import com.loresuelvo.consumer.ui.screens.chat.components.ConversationMessageBubble
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
            )
            is ConversationUiState.Error -> ErrorState(
                failure = state.failure,
                onRetryClick = onRetryClick,
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
private fun ReadyState(
    state: ConversationUiState.Ready,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit,
    onErrorDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    // Auto-scroll on every new message so the consumer's just-sent
    // bubble is always visible. Keyed on the message count so the
    // effect re-fires when a new bubble lands (success path or
    // optimistic append — neither applies today but the code is
    // forward-compatible).
    LaunchedEffect(state.detail.messages.size) {
        if (state.detail.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.detail.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ConversationTopBar(
            counterpart = state.detail.counterpart,
            status = state.detail.status,
            onBackClick = onBackClick,
        )
        MessagesList(
            messages = state.detail.messages,
            listState = listState,
            modifier = Modifier.weight(1f),
        )
        if (state.transientError != null) {
            TransientErrorCard(
                failure = state.transientError,
                onRetryClick = onSendClick,
                onDismiss = onErrorDismiss,
            )
        }
        ChatInputBar(
            promptInput = state.promptInput,
            canSend = state.promptInput.isNotBlank() && !state.sending,
            sending = state.sending,
            onPromptChange = onPromptChange,
            onSendClick = onSendClick,
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
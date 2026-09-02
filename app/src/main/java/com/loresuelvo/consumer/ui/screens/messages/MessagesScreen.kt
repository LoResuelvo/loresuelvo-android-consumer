package com.loresuelvo.consumer.ui.screens.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.ui.screens.messages.components.ConversationRow
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * State-driven surface for the consumer's conversations list
 * (`Route.Messages`). Replaces the previous placeholder scaffold.
 *
 * The composable is intentionally pure with respect to navigation:
 * it only renders the current [MessagesListUiState] and forwards
 * the [onRetryClick] / [onConversationClick] callbacks. The Hilt
 * bridge (`MessagesRoute` in `LoResuelvoNav`) is the only place
 * that instantiates the [MessagesListViewModel] via
 * `hiltViewModel()`.
 *
 * State rendering:
 *  - [MessagesListUiState.Loading] → centred spinner.
 *  - [MessagesListUiState.Ready] with empty list → empty-state card
 *    explaining "you have no conversations yet" (WhatsApp-style
 *    "start a conversation" hint).
 *  - [MessagesListUiState.Ready] with non-empty list → vertical
 *    `LazyColumn` of [ConversationRow]s.
 *  - [MessagesListUiState.Error] → centred card with a typed copy
 *    (network / server / unauthorized) and a "Reintentar" button
 *    that triggers [onRetryClick].
 *
 * Pull-to-refresh and per-row navigation land in follow-up
 * commits (the user said "WhatsApp-style" rows but the scenario
 * 03-IC scope is "see the provider as a contact in my list" —
 * row tap is for 04-IC / 05-IC).
 */
@Composable
fun MessagesScreen(
    state: MessagesListUiState,
    onRetryClick: () -> Unit = {},
    onConversationClick: (conversationId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // 08-UXUI: see HomeScreen — the outer `Scaffold`
            // no longer consumes the top inset, so each
            // bottom-nav screen must apply
            // `statusBarsPadding()` itself.
            .statusBarsPadding()
            .testTag(MESSAGES_SCREEN_TAG),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is MessagesListUiState.Loading -> LoadingState()
            is MessagesListUiState.Ready -> {
                if (state.conversations.isEmpty()) {
                    EmptyState()
                } else {
                    ConversationsList(
                        conversations = state.conversations,
                        onConversationClick = onConversationClick,
                    )
                }
            }
            is MessagesListUiState.Error -> ErrorState(
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
            modifier = Modifier.testTag(MESSAGES_LOADING_TAG),
        )
        Text(
            text = stringResource(R.string.messages_screen_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(MESSAGES_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.messages_screen_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.messages_screen_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConversationsList(
    conversations: List<com.loresuelvo.consumer.domain.conversation.Conversation>,
    onConversationClick: (conversationId: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(MESSAGES_LIST_TAG),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            items = conversations,
            key = { it.id },
        ) { conversation ->
            ConversationRow(
                conversation = conversation,
                onClick = { onConversationClick(conversation.id) },
            )
        }
    }
}

@Composable
private fun ErrorState(
    failure: ConversationsOutcome.Failure,
    onRetryClick: () -> Unit,
) {
    val message = when (failure) {
        is ConversationsOutcome.Failure.Network ->
            stringResource(R.string.messages_screen_error_network)
        is ConversationsOutcome.Failure.Server ->
            stringResource(R.string.messages_screen_error_server)
        is ConversationsOutcome.Failure.Unauthorized ->
            stringResource(R.string.messages_screen_error_unauthorized)
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(MESSAGES_ERROR_TAG),
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
            modifier = Modifier.testTag(MESSAGES_ERROR_RETRY_TAG),
        ) {
            Text(
                text = stringResource(R.string.messages_screen_error_retry),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Compose testTags for the messages screen and its inner slots.
 * Pinning the locators (rather than literal Spanish labels) keeps
 * the Compose tests locale-independent — the test resolves the
 * string from the activity's resources via `getString(R.string.*)`.
 */
const val MESSAGES_SCREEN_TAG: String = "messages-screen"
const val MESSAGES_LOADING_TAG: String = "messages-loading"
const val MESSAGES_EMPTY_TAG: String = "messages-empty"
const val MESSAGES_LIST_TAG: String = "messages-list"
const val MESSAGES_ERROR_TAG: String = "messages-error"
const val MESSAGES_ERROR_RETRY_TAG: String = "messages-error-retry"
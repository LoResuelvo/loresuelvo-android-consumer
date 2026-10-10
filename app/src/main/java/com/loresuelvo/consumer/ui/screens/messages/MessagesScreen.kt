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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
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

@Composable
fun MessagesScreen(
    state: MessagesListUiState,
    onRetryClick: () -> Unit = {},
    onConversationClick: (conversationId: String) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag(MESSAGES_SCREEN_TAG),
    ) {
        Text(
            text = stringResource(R.string.messages_screen_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        MessagesSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is MessagesListUiState.Loading -> LoadingState()
                is MessagesListUiState.Ready -> {
                    if (state.conversations.isEmpty()) {
                        if (state.totalConversations > 0 && state.searchQuery.isNotBlank()) {
                            SearchEmptyState()
                        } else {
                            EmptyState()
                        }
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
}

@Composable
private fun MessagesSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag(MESSAGES_SEARCH_TAG),
        singleLine = true,
        placeholder = {
            Text(text = stringResource(R.string.messages_search_placeholder))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(
                            R.string.messages_search_clear_content_description,
                        ),
                    )
                }
            }
        } else {
            null
        },
    )
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
private fun SearchEmptyState() {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(MESSAGES_SEARCH_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.messages_search_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.messages_search_empty_body),
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
        itemsIndexed(
            items = conversations,
            key = { _, conversation -> conversation.id },
        ) { index, conversation ->
            ConversationRow(
                conversation = conversation,
                onClick = { onConversationClick(conversation.id) },
                showDivider = index < conversations.lastIndex,
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
const val MESSAGES_SEARCH_TAG: String = "messages-search"
const val MESSAGES_SEARCH_EMPTY_TAG: String = "messages-search-empty"
const val MESSAGES_LIST_TAG: String = "messages-list"
const val MESSAGES_ERROR_TAG: String = "messages-error"
const val MESSAGES_ERROR_RETRY_TAG: String = "messages-error-retry"

package com.loresuelvo.consumer.ui.screens.assistant

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary
import com.loresuelvo.consumer.ui.theme.SubtitleGray
import java.text.DateFormat
import java.util.Date

/**
 * State-driven surface for the "Asistente IA" tab
 * (`Route.Assistant`). Replaces the previous placeholder scaffold.
 *
 * The composable is pure with respect to navigation: it only
 * renders the current [AssistantUiState] and forwards the
 * [onRetryClick] / [onConversationClick] callbacks. The Hilt
 * bridge (`AssistantRoute` in `LoResuelvoNav`) is the only place
 * that instantiates the [AssistantViewModel] via `hiltViewModel()`.
 *
 * State rendering:
 *  - [AssistantUiState.Loading] → centred spinner.
 *  - [AssistantUiState.Empty] → empty-state card explaining
 *    "you haven't started a session yet".
 *  - [AssistantUiState.Ready] with non-empty list → vertical
 *    `LazyColumn` of session rows. Each row shows the title
 *    + the formatted `lastMessageAtEpochMillis` timestamp
 *    + an optional `lastMessagePreview`.
 *  - [AssistantUiState.Failure] → centred card with a typed
 *    copy (network / server / unauthorized) and a "Reintentar"
 *    button that triggers [onRetryClick].
 *
 * Row tap navigation (open the saved conversation in the chat
 * thread) is the next US's scope.
 */
@Composable
fun AssistantScreen(
    state: AssistantUiState,
    onRetryClick: () -> Unit,
    onConversationClick: (conversationId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag(ASSISTANT_SCREEN_TAG),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is AssistantUiState.Loading -> LoadingState()
            is AssistantUiState.Empty -> EmptyState()
            is AssistantUiState.Ready -> ConversationsList(
                conversations = state.conversations,
                onConversationClick = onConversationClick,
            )
            is AssistantUiState.Failure -> ErrorState(
                failure = state,
                onRetryClick = onRetryClick,
            )
        }
    }
}

const val ASSISTANT_SCREEN_TAG: String = "assistant-screen"
const val ASSISTANT_LOADING_TAG: String = "assistant-screen-loading"
const val ASSISTANT_EMPTY_TAG: String = "assistant-screen-empty"
const val ASSISTANT_LIST_TAG: String = "assistant-screen-list"
const val ASSISTANT_ERROR_TAG: String = "assistant-screen-error"
const val ASSISTANT_ROW_TAG: String = "assistant-screen-row"
const val ASSISTANT_ROW_TITLE_TAG: String = "assistant-screen-row-title"
const val ASSISTANT_ROW_DATE_TAG: String = "assistant-screen-row-date"
const val ASSISTANT_ROW_PREVIEW_TAG: String = "assistant-screen-row-preview"
const val ASSISTANT_RETRY_TAG: String = "assistant-screen-retry"

@Composable
private fun LoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(ASSISTANT_LOADING_TAG),
        )
        Text(
            text = stringResource(R.string.assistant_screen_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag(ASSISTANT_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.assistant_screen_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.assistant_screen_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConversationsList(
    conversations: List<AiConversationSummary>,
    onConversationClick: (conversationId: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ASSISTANT_LIST_TAG),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = conversations, key = { it.id }) { conversation ->
            ConversationRow(
                conversation = conversation,
                onClick = { onConversationClick(conversation.id) },
            )
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: AiConversationSummary,
    onClick: () -> Unit,
) {
    val timestampLabel = remember(conversation.lastMessageAtEpochMillis) {
        if (conversation.lastMessageAtEpochMillis > 0L) {
            DateFormat
                .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(conversation.lastMessageAtEpochMillis))
        } else {
            null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("$ASSISTANT_ROW_TAG-${conversation.id}")
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("$ASSISTANT_ROW_TITLE_TAG-${conversation.id}"),
            )
            if (timestampLabel != null) {
                Text(
                    text = timestampLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtitleGray,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("$ASSISTANT_ROW_DATE_TAG-${conversation.id}"),
                )
            }
        }
        val preview = conversation.lastMessagePreview
        if (!preview.isNullOrBlank()) {
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = SubtitleGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("$ASSISTANT_ROW_PREVIEW_TAG-${conversation.id}"),
            )
        }
    }
}

@Composable
private fun ErrorState(
    failure: AssistantUiState.Failure,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag(ASSISTANT_ERROR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val message = stringResource(
            when (failure) {
                AssistantUiState.Failure.Network ->
                    R.string.assistant_screen_error_network
                is AssistantUiState.Failure.Server ->
                    R.string.assistant_screen_error_server
                AssistantUiState.Failure.Unauthorized ->
                    R.string.assistant_screen_error_unauthorized
            },
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onRetryClick,
            modifier = Modifier.testTag(ASSISTANT_RETRY_TAG),
        ) {
            Text(text = stringResource(R.string.assistant_screen_error_retry))
        }
    }
}

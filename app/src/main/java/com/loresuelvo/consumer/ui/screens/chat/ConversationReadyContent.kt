package com.loresuelvo.consumer.ui.screens.chat

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.ui.screens.chat.components.ConversationMessageBubble
import com.loresuelvo.consumer.ui.screens.chat.components.NewMessageBanner
import com.loresuelvo.consumer.ui.screens.chat.components.ProposalSummaryCard
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
internal fun ConversationReadyContent(
    state: ConversationUiState.Ready,
    paddingValues: PaddingValues,
    actions: ConversationScreenActions,
    proposalSummaryState: ConversationProposalSummaryUiState,
) {
    val listState = rememberLazyListState()
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount == 0 ||
                info.visibleItemsInfo.lastOrNull()?.index == info.totalItemsCount - 1
        }
    }
    LaunchedEffect(isAtBottom) {
        actions.errors.onScrollPositionChanged(isAtBottom)
    }
    LaunchedEffect(state.detail.messages.size) {
        if (state.detail.messages.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        if (!isAtBottom || info.totalItemsCount <= info.visibleItemsInfo.size) {
            return@LaunchedEffect
        }
        listState.animateScrollToItem(
            conversationLastMessageListIndex(state.detail.messages),
        )
    }

    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        (proposalSummaryState as? ConversationProposalSummaryUiState.Ready)?.let { ready ->
            ProposalSummaryCard(proposal = ready.proposal)
        }
        Box(modifier = Modifier.fillMaxSize()) {
            ConversationMessagesList(
                state = ConversationMessagesListState(
                    messages = state.detail.messages,
                    listState = listState,
                    audioPlayback = state.audioPlayback,
                ),
                actions = ConversationMessageBubbleActions(
                    onPlayAudio = actions.playback.onPlayAudio,
                    onPauseAudio = actions.playback.onPauseAudio,
                    onImageClick = actions.playback.onImageClick,
                    onVideoClick = actions.playback.onVideoClick,
                ),
            )
            if (state.hasUnreadIncoming) {
                NewMessageBanner(
                    onTap = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(
                                conversationLastMessageListIndex(state.detail.messages),
                            )
                        }
                        actions.errors.onUnreadBannerTapped()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ConversationMessagesList(
    state: ConversationMessagesListState,
    actions: ConversationMessageBubbleActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(CONVERSATION_LIST_TAG),
        state = state.listState,
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        state.messages.forEachIndexed { index, message ->
            if (shouldShowConversationDateSeparator(state.messages.getOrNull(index - 1), message)) {
                item(key = conversationDateSeparatorKey(message.id)) {
                    ConversationDateSeparator(epochMillis = message.createdOnEpochMillis)
                }
            }
            item(key = message.id) {
                ConversationMessageBubble(
                    state = ConversationMessageBubbleState(
                        message = message,
                        audioPlayback = state.audioPlayback,
                    ),
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun ConversationDateSeparator(epochMillis: Long) {
    val label = when {
        DateUtils.isToday(epochMillis) -> stringResource(R.string.conversation_date_today)
        isConversationDateYesterday(epochMillis) ->
            stringResource(R.string.conversation_date_yesterday)
        else -> DateFormat
            .getDateInstance(DateFormat.LONG, Locale.getDefault())
            .format(Date(epochMillis))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag(CONVERSATION_DATE_SEPARATOR_TAG),
        )
    }
}

internal fun shouldShowConversationDateSeparator(
    previousMessage: ConversationMessage?,
    message: ConversationMessage,
): Boolean {
    if (message.createdOnEpochMillis <= 0L) return false
    val previousTimestamp = previousMessage?.createdOnEpochMillis ?: return true
    if (previousTimestamp <= 0L) return true
    return !isSameConversationLocalDay(previousTimestamp, message.createdOnEpochMillis)
}

internal fun conversationLastMessageListIndex(messages: List<ConversationMessage>): Int {
    if (messages.isEmpty()) return 0
    var dateSeparatorCount = 0
    messages.forEachIndexed { index, message ->
        if (shouldShowConversationDateSeparator(messages.getOrNull(index - 1), message)) {
            dateSeparatorCount++
        }
    }
    return messages.lastIndex + dateSeparatorCount
}

private fun isConversationDateYesterday(epochMillis: Long): Boolean {
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val messageDate = Calendar.getInstance().apply {
        timeInMillis = epochMillis
    }
    return yesterday.get(Calendar.ERA) == messageDate.get(Calendar.ERA) &&
        yesterday.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR)
}

private fun isSameConversationLocalDay(firstEpochMillis: Long, secondEpochMillis: Long): Boolean {
    val firstDate = Calendar.getInstance().apply { timeInMillis = firstEpochMillis }
    val secondDate = Calendar.getInstance().apply { timeInMillis = secondEpochMillis }
    return firstDate.get(Calendar.ERA) == secondDate.get(Calendar.ERA) &&
        firstDate.get(Calendar.YEAR) == secondDate.get(Calendar.YEAR) &&
        firstDate.get(Calendar.DAY_OF_YEAR) == secondDate.get(Calendar.DAY_OF_YEAR)
}

private fun conversationDateSeparatorKey(messageId: String): String =
    "conversation-date-separator-$messageId"

const val CONVERSATION_DATE_SEPARATOR_TAG = "conversation-date-separator"

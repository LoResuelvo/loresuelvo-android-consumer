package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.domain.conversation.ConversationMessage
import com.loresuelvo.consumer.ui.screens.chat.components.ConversationMessageBubble
import com.loresuelvo.consumer.ui.screens.chat.components.NewMessageBanner
import com.loresuelvo.consumer.ui.screens.chat.components.ProposalSummaryCard
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
        listState.animateScrollToItem(state.detail.messages.size - 1)
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
                messages = state.detail.messages,
                listState = listState,
                audioPlayback = state.audioPlayback,
                onPlayAudio = actions.playback.onPlayAudio,
                onPauseAudio = actions.playback.onPauseAudio,
                onImageClick = actions.playback.onImageClick,
            )
            if (state.hasUnreadIncoming) {
                NewMessageBanner(
                    onTap = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(state.detail.messages.size - 1)
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
    messages: List<ConversationMessage>,
    listState: LazyListState,
    audioPlayback: AudioPlaybackState,
    onPlayAudio: (String) -> Unit,
    onPauseAudio: (String) -> Unit,
    onImageClick: (String) -> Unit,
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
            ConversationMessageBubble(
                message = message,
                audioPlayback = audioPlayback,
                onPlayAudio = onPlayAudio,
                onPauseAudio = onPauseAudio,
                onImageClick = onImageClick,
            )
        }
    }
}

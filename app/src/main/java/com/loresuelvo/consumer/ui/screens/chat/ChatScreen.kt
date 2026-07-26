package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Sender
import com.loresuelvo.consumer.ui.screens.chat.CHAT_INPUT_DIVIDER_TAG

/**
 * Stateless Composable for the AI diagnostic chat screen.
 *
 * Layout (top-down):
 *  - [ChatTopBar] with the back arrow and the "Chat con IA" title.
 *  - The conversation list, which always starts with the assistant's
 *    initial message ("¡Hola! Soy el asistente…") and is followed by
 *    the user / assistant messages the VM has accumulated. The
 *    "welcome" feeling comes from the initial bubble being the only
 *    item when the user has not sent anything yet; once the user
 *    sends the first message, the initial bubble stays at the top
 *    as the chronological first message of the conversation.
 *  - [ChatInputBar] pinned at the bottom, with `imePadding` and
 *    `navigationBarsPadding` so the keyboard never covers the
 *    field. The send icon is disabled when `!canSend`, which
 *    includes `state.sending == true`.
 */
@Composable
fun ChatScreen(
    promptInput: String,
    canSend: Boolean,
    sending: Boolean,
    messages: List<ChatMessage>,
    transientError: ChatError?,
    preliminaryWarningVisible: Boolean,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onRetryClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialMessage = ChatMessage(
        id = INITIAL_MESSAGE_ID,
        sender = Sender.Assistant,
        content = stringResource(R.string.chat_initial_message_body),
        sentAtEpochMillis = 0L,
    )
    val conversation = remember(messages) { listOf(initialMessage) + messages }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ChatTopBar(onBackClick = onBackClick) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (preliminaryWarningVisible) {
                    PreliminaryBanner()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    MessagesList(
                        messages = conversation,
                        typingIndicatorVisible = sending,
                        transientError = transientError,
                        onRetryClick = onRetryClick,
                        onErrorDismissClick = onErrorDismiss,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.testTag(CHAT_INPUT_DIVIDER_TAG),
                )
                ChatInputBar(
                    promptInput = promptInput,
                    canSend = canSend,
                    sending = sending,
                    onPromptChange = onPromptChange,
                    onSendClick = onSendClick,
                )
            }
        }
    }
}

/**
 * Stable id for the initial message so [androidx.compose.foundation.lazy.LazyColumn]
 * keys don't churn between recompositions when the user
 * sends / receives a new message.
 */
private const val INITIAL_MESSAGE_ID: String = "initial-assistant-welcome"

package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage

/**
 * Stateless Composable for the AI diagnostic chat screen.
 *
 * Layout (top-down):
 *  - [ChatTopBar] with the back arrow and the "Chat con IA" title.
 *  - [PreliminaryBanner] above the messages when
 *    `preliminaryWarningVisible = true` (scenario 05-DIA).
 *  - [MessagesList] when the conversation has at least one entry,
 *    OR the localisable placeholder body when the list is empty
 *    AND we are not mid-round-trip. When `sending = true` the
 *    [MessagesList] also renders [TypingIndicatorBubble]; when
 *    `transientError != null` it appends [ChatErrorCard] with the
 *    retry / dismiss callbacks (scenarios 03-DIA / 04-DIA).
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
                    contentAlignment = Alignment.Center,
                ) {
                    if (messages.isEmpty() && !sending && transientError == null) {
                        Text(
                            text = stringResource(R.string.chat_placeholder_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 32.dp,
                            ),
                        )
                    } else {
                        MessagesList(
                            messages = messages,
                            typingIndicatorVisible = sending,
                            transientError = transientError,
                            onRetryClick = onRetryClick,
                            onErrorDismissClick = onErrorDismiss,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                ChatInputBar(
                    promptInput = promptInput,
                    canSend = canSend,
                    onPromptChange = onPromptChange,
                    onSendClick = onSendClick,
                )
            }
        }
    }
}

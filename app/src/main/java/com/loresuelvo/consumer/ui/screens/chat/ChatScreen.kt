package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Layout:
 *  - [ChatTopBar] with the back arrow and the "Chat con IA" title.
 *  - [MessagesList] (when the conversation has at least one entry),
 *    or the localisable placeholder body (when the list is empty).
 *  - [ChatInputBar] pinned at the bottom, with `imePadding` and
 *    `navigationBarsPadding` so the keyboard never covers the field.
 *
 * Scenario 01-DIA exercises the consumer side: once the user sends
 * their prompt, the messages list renders a single bubble on the
 * right. Successive commits add server responses, typing indicators,
 * error cards and recommendations without growing this Composable
 * — each concern will get its own child file under `ui/screens/chat/`.
 */
@Composable
fun ChatScreen(
    promptInput: String,
    canSend: Boolean,
    messages: List<ChatMessage>,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (messages.isEmpty()) {
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
                    MessagesList(messages = messages)
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

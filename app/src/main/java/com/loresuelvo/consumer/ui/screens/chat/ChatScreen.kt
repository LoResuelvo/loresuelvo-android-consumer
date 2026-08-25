package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisAssessment
import com.loresuelvo.consumer.domain.diagnosis.Sender
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.screens.chat.CHAT_INPUT_DIVIDER_TAG
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing

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
    assessment: DiagnosisAssessment?,
    recommendedProviders: List<Provider>?,
    transientError: ChatError?,
    preliminaryWarningVisible: Boolean,
    pendingAttachments: List<PendingMedia> = emptyList(),
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onRetryClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onContactClick: (Provider) -> Unit,
    onBackClick: () -> Unit,
    onAttachImageFromGallery: () -> Unit = {},
    onRemoveAttachment: (Int) -> Unit = {},
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
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ChatTopBar(onBackClick = onBackClick) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding(),
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.testTag(CHAT_INPUT_DIVIDER_TAG)
                                .padding(horizontal = 16.dp),
                )
                if (pendingAttachments.isNotEmpty()) {
                    PendingAttachmentStrip(
                        attachments = pendingAttachments,
                        onRemove = onRemoveAttachment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(CHAT_ATTACHMENT_STRIP_TAG),
                    )
                }
                ChatInputBar(
                    promptInput = promptInput,
                    canSend = canSend,
                    sending = sending,
                    recordingAudio = false,
                    onPromptChange = onPromptChange,
                    onSendClick = onSendClick,
                    onStartAudioRecording = {},
                    onStopAudioRecording = {},
                    onAttachClick = onAttachImageFromGallery,
                )
            }
        },
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
                MessagesList(
                    messages = conversation,
                    typingIndicatorVisible = sending,
                    transientError = transientError,
                    onRetryClick = onRetryClick,
                    onErrorDismissClick = onErrorDismiss,
                    modifier = Modifier.weight(1f),
                )
                if (assessment != null && assessment.isProfessionalRequired) {
                    DiagnosisSummaryCard(
                        categoryName = assessment.problemCategory?.name,
                        providers = recommendedProviders.orEmpty(),
                        onContactClick = onContactClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
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

/**
 * Compose testTag for the strip that renders above the input bar
 * once the consumer has staged one or more images. The visual
 * preview itself is provided by [PendingAttachmentStrip] below;
 * each row carries the original filename + a discard callback.
 */
const val CHAT_ATTACHMENT_STRIP_TAG: String = "chat-attachment-strip"

/**
 * Compose testTag for the per-attachment discard icon. Indexed
 * (e.g. `chat-attachment-discard-0`) so the BDD step can target
 * the right entry without depending on its display label.
 */
const val CHAT_ATTACHMENT_DISCARD_TAG_PREFIX: String = "chat-attachment-discard"

/**
 * Minimal preview strip rendered above the [ChatInputBar] while
 * the consumer has staged images. Each row carries the file's
 * original name + a close icon that fires [onRemove] with the
 * row's index. A richer [com.loresuelvo.consumer.ui.screens.chat.MediaPreviewCard]
 * lives next to the chat-with-provider surface and replaces this
 * once we ship the AIP send flow (06-AIP).
 *
 * The strip is purely presentational: the underlying state lives
 * on the VM (see [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel.uiState]),
 * and the BDD layer drives it through
 * [com.loresuelvo.consumer.ui.screens.chat.ChatViewModel.onAttachMedia]
 * for tests. The accept / discard UI mirrors the chat-with-provider
 * semantics so future 04-AIP / 05-AIP steps can build on it.
 */
@Composable
private fun PendingAttachmentStrip(
    attachments: List<PendingMedia>,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        attachments.forEachIndexed { index, attachment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .testTag("$CHAT_ATTACHMENT_DISCARD_TAG_PREFIX-$index"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = attachment.originalName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.chat_attachment_discard),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onRemove(index) }
                        .padding(start = 8.dp)
                        .testTag("$CHAT_ATTACHMENT_DISCARD_TAG_PREFIX-$index-button"),
                )
            }
        }
    }
}

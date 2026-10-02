package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.ChatImage
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
    state: ChatUiState,
    actions: ChatScreenActions = ChatScreenActions(),
    modifier: Modifier = Modifier,
) {
    var fullscreenImage by remember { mutableStateOf<ChatImage?>(null) }
    val initialMessage = ChatMessage(
        id = INITIAL_MESSAGE_ID,
        sender = Sender.Assistant,
        content = stringResource(R.string.chat_initial_message_body),
        sentAtEpochMillis = 0L,
    )
    val conversation = remember(state.messages) { listOf(initialMessage) + state.messages }

    Scaffold(

        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ChatTopBar(onBackClick = actions.navigation.onBack) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)

                    .padding(
                                bottom = if (WindowInsets.ime.asPaddingValues()
                                        .calculateBottomPadding() > 0.dp) 20.dp else 0.dp,
                    ),
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.testTag(CHAT_INPUT_DIVIDER_TAG)
                                .padding(horizontal = 16.dp),
                )
                if (state.pendingAttachments.isNotEmpty()) {
                    state.pendingAttachments.forEachIndexed { index, attachment ->
                        MediaPreviewCard(
                            pendingMedia = attachment,
                            sending = false,
                            onSendClick = { actions.media.onConfirmSend(index) },
                            onDiscardClick = { actions.media.onDiscard(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("$CHAT_ATTACHMENT_CARD_TAG_PREFIX-$index"),
                        )
                    }
                }
                ChatInputBar(
                    state = ChatInputBarState(
                        promptInput = state.promptInput,
                        canSend = state.canSend,
                        sending = state.sending,
                        audioEnabled = actions.media.audioEnabled,
                    ),
                    actions = ChatInputBarActions(
                        onPromptChange = actions.composer.onPromptChange,
                        onSend = actions.composer.onSend,
                        onAttach = actions.media.onAttach,
                    ),
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
                if (state.preliminaryWarningVisible) {
                    PreliminaryBanner()
                }
                MessagesList(
                    messages = conversation,
                    typingIndicatorVisible = state.sending,
                    transientError = state.transientError,
                    onRetryClick = actions.errors.onRetry,
                    onErrorDismissClick = actions.errors.onDismiss,
                    onImageClick = { fullscreenImage = it },
                    modifier = Modifier.weight(1f),
                )
                if (state.assessment != null && state.assessment.isProfessionalRequired) {
                    DiagnosisSummaryCard(
                        categoryName = state.assessment.problemCategory?.name,
                        providers = state.recommendedProviders.orEmpty(),
                        onContactClick = actions.diagnosis.onContact,
                        onViewProfileClick = actions.diagnosis.onViewProfile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    MediaAttachSheet(
        show = actions.media.showAttachSheet,
        onDismiss = actions.media.onAttachSheetDismiss,
        onGalleryClick = actions.media.onGallery,
        onCameraClick = actions.media.onCamera,
    )

    fullscreenImage?.let { image ->
        FullScreenImageViewer(
            image = image,
            onDismiss = { fullscreenImage = null },
        )
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
const val CHAT_ATTACHMENT_CARD_TAG_PREFIX: String = "chat-attachment-card"

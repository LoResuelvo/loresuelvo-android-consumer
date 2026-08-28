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
import androidx.compose.foundation.layout.statusBarsPadding
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
    onAttachClick: () -> Unit = {},
    onAttachImageFromGallery: () -> Unit = {},
    onAttachImageFromCamera: () -> Unit = {},
    onConfirmAttachmentSend: (Int) -> Unit = {},
    onDiscardAttachment: (Int) -> Unit = {},
    showAttachSheet: Boolean = false,
    onAttachSheetDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var fullscreenImage by remember { mutableStateOf<ChatImage?>(null) }
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
                    pendingAttachments.forEachIndexed { index, attachment ->
                        MediaPreviewCard(
                            pendingMedia = attachment,
                            sending = false,
                            onSendClick = { onConfirmAttachmentSend(index) },
                            onDiscardClick = { onDiscardAttachment(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("$CHAT_ATTACHMENT_CARD_TAG_PREFIX-$index"),
                        )
                    }
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
                    onAttachClick = onAttachClick,
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
                    onImageClick = { fullscreenImage = it },
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

    MediaAttachSheet(
        show = showAttachSheet,
        onDismiss = onAttachSheetDismiss,
        onGalleryClick = onAttachImageFromGallery,
        onCameraClick = onAttachImageFromCamera,
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

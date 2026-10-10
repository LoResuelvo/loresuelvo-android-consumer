package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.conversation.ConversationDetailOutcome
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import com.loresuelvo.consumer.ui.screens.chat.components.ConversationTopBar
import com.loresuelvo.consumer.ui.theme.SubtitleGray

@Composable
fun ConversationScreen(
    state: ConversationUiState,
    actions: ConversationScreenActions = ConversationScreenActions(),
    proposalSummaryState: ConversationProposalSummaryUiState =
        ConversationProposalSummaryUiState.Empty,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(CONVERSATION_SCREEN_TAG),
        contentAlignment = Alignment.Center,
    ) {

        when (state) {
            is ConversationUiState.Loading -> LoadingState()
            is ConversationUiState.Error -> ErrorState(
                failure = state.failure,
                onRetryClick = actions.errors.onRetry,
            )
            is ConversationUiState.Ready -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        ConversationTopBar(
                            counterpart = state.detail.counterpart,
                            status = state.detail.status,
                            onBackClick = actions.navigation.onBack,

                            onViewWorkOrder = state.detail.workOrderId
                                ?.let { workOrderId ->
                                    { actions.navigation.onViewWorkOrder?.invoke(workOrderId) }
                                },
                            onViewProviderProfile = actions.navigation.onViewProviderProfile?.let {
                                { it(state.detail.counterpart.id) }
                            },
                        )
                    },
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
                            if (state.transientError != null) {
                                TransientErrorCard(
                                    failure = state.transientError,
                                    onRetryClick = actions.composer.onSend,
                                    onDismiss = actions.errors.onDismiss,
                                )
                            }
                            if (state.pendingMedia.isNotEmpty()) {
                                state.pendingMedia.forEachIndexed { index, attachment ->
                                    MediaPreviewCard(
                                        pendingMedia = attachment,
                                        sending = state.sendingMedia,
                                        onSendClick = actions.media.onConfirmSend,
                                        onDiscardClick = actions.media.onDiscard,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(
                                                "$CONVERSATION_ATTACHMENT_CARD_TAG_PREFIX-$index",
                                            ),
                                    )
                                }
                                if (state.transientMediaError != null) {
                                    MediaTransientErrorCard(
                                        failure = state.transientMediaError,
                                        onRetryClick = actions.media.onConfirmSend,
                                        onDismiss = actions.media.onErrorDismiss,
                                    )
                                }
                            }
                            ChatInputBar(
                                state = ChatInputBarState(
                                    promptInput = state.promptInput,
                                    canSend = state.promptInput.isNotBlank() && !state.sending,
                                    sending = state.sending,
                                    recordingAudio = state.recordingAudio,
                                ),
                                actions = ChatInputBarActions(
                                    onPromptChange = actions.composer.onPromptChange,
                                    onSend = actions.composer.onSend,
                                    onStartAudioRecording = actions.composer.onStartAudioRecording,
                                    onStopAudioRecording = actions.composer.onStopAudioRecording,
                                    onAttach = actions.composer.onAttach,
                                ),
                            )
                        }
                    },
                ) { padding ->
                    ConversationReadyContent(
                        state = state,
                        paddingValues = padding,
                        actions = actions,
                        proposalSummaryState = proposalSummaryState,
                    )
                }
            }
        }
        MediaAttachSheet(
            show = actions.media.showAttachSheet,
            onDismiss = actions.media.onAttachSheetDismiss,
            onGalleryClick = actions.media.onGallery,
            onCameraClick = actions.media.onCamera,
            onVideoClick = actions.media.onVideo,
        )

        (state as? ConversationUiState.Ready)?.fullscreenImage?.let { image ->
            FullScreenImageViewer(
                image = image,
                onDismiss = actions.playback.onFullscreenImageDismiss,
            )
        }

        (state as? ConversationUiState.Ready)?.fullscreenVideo?.let { video ->
            FullScreenVideoViewer(
                video = video,
                onDismiss = actions.playback.onFullscreenVideoDismiss,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(CONVERSATION_LOADING_TAG),
        )
        Text(
            text = stringResource(R.string.conversation_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun ErrorState(
    failure: ConversationDetailOutcome.Failure,
    onRetryClick: () -> Unit,
) {
    val message = when (failure) {
        is ConversationDetailOutcome.Failure.Network ->
            stringResource(R.string.conversation_error_network)
        is ConversationDetailOutcome.Failure.Server ->
            stringResource(R.string.conversation_error_server)
        is ConversationDetailOutcome.Failure.Unauthorized ->
            stringResource(R.string.conversation_error_unauthorized)
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(CONVERSATION_ERROR_TAG),
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
            modifier = Modifier.testTag(CONVERSATION_ERROR_RETRY_TAG),
        ) {
            Text(
                text = stringResource(R.string.conversation_error_retry),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun TransientErrorCard(
    failure: SendMessageOutcome.Failure,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = when (failure) {
        is SendMessageOutcome.Failure.Network ->
            stringResource(R.string.conversation_transient_error_network)
        is SendMessageOutcome.Failure.Server ->
            stringResource(R.string.conversation_transient_error_server)
        is SendMessageOutcome.Failure.Unauthorized ->
            stringResource(R.string.conversation_transient_error_unauthorized)
        is SendMessageOutcome.Failure.PayloadTooLarge ->

            stringResource(R.string.conversation_transient_error_server)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(CONVERSATION_TRANSIENT_ERROR_TAG),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(CONVERSATION_TRANSIENT_ERROR_DISMISS_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.conversation_transient_error_dismiss),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(
                    onClick = onRetryClick,
                    modifier = Modifier.testTag(CONVERSATION_TRANSIENT_ERROR_RETRY_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.conversation_transient_error_retry),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/**
 * Compose testTags for the conversation screen and its inner
 * slots. Pinning the locators (rather than literal Spanish
 * labels) keeps the Compose tests locale-independent.
 */
const val CONVERSATION_SCREEN_TAG: String = "conversation-screen"
const val CONVERSATION_LOADING_TAG: String = "conversation-loading"
const val CONVERSATION_ERROR_TAG: String = "conversation-error"
const val CONVERSATION_ERROR_RETRY_TAG: String = "conversation-error-retry"
const val CONVERSATION_LIST_TAG: String = "conversation-list"
const val CONVERSATION_TRANSIENT_ERROR_TAG: String = "conversation-transient-error"
const val CONVERSATION_TRANSIENT_ERROR_RETRY_TAG: String = "conversation-transient-error-retry"
const val CONVERSATION_TRANSIENT_ERROR_DISMISS_TAG: String = "conversation-transient-error-dismiss"

@Composable
private fun MediaTransientErrorCard(
    failure: SendMessageOutcome.Failure,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = when (failure) {
        is SendMessageOutcome.Failure.Network ->
            stringResource(R.string.conversation_transient_media_error_network)
        is SendMessageOutcome.Failure.Server ->
            stringResource(R.string.conversation_transient_media_error_server)
        is SendMessageOutcome.Failure.Unauthorized ->
            stringResource(R.string.conversation_transient_media_error_unauthorized)
        is SendMessageOutcome.Failure.PayloadTooLarge ->
            stringResource(
                R.string.conversation_transient_media_error_payload_too_large,
            )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(CONVERSATION_TRANSIENT_MEDIA_ERROR_TAG),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(CONVERSATION_TRANSIENT_MEDIA_ERROR_DISMISS_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.conversation_transient_error_dismiss),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(
                    onClick = onRetryClick,
                    modifier = Modifier.testTag(CONVERSATION_TRANSIENT_MEDIA_ERROR_RETRY_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.conversation_transient_error_retry),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

const val CONVERSATION_TRANSIENT_MEDIA_ERROR_TAG: String = "conversation-transient-media-error"
const val CONVERSATION_TRANSIENT_MEDIA_ERROR_RETRY_TAG: String = "conversation-transient-media-error-retry"
const val CONVERSATION_TRANSIENT_MEDIA_ERROR_DISMISS_TAG: String = "conversation-transient-media-error-dismiss"

/**
 * Compose testTag prefix for the per-attachment preview card.
 * Entries are indexed so tests can target a specific attachment
 * without depending on its display label.
 */
const val CONVERSATION_ATTACHMENT_CARD_TAG_PREFIX: String = "conversation-attachment-card"

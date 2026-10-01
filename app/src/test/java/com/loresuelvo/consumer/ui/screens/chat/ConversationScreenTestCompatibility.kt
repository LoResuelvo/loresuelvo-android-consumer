package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Keeps the existing focused UI fixtures readable while the production
 * surface moves to grouped contracts. Production callers use the
 * state-plus-actions API directly; this test-only adapter makes the
 * callback assertions explicit without duplicating screen behavior.
 */
@Composable
fun ConversationScreen(
    state: ConversationUiState,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit,
    onViewWorkOrder: ((String) -> Unit)? = null,
    onRetryClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
    onPauseAudio: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onFullscreenImageDismiss: () -> Unit = {},
    onAttachClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onStartAudioRecording: () -> Unit = {},
    onStopAudioRecording: () -> Unit = {},
    onConfirmMediaSend: () -> Unit = {},
    onDiscardMedia: () -> Unit = {},
    onMediaErrorDismiss: () -> Unit = {},
    onAttachSheetDismiss: () -> Unit = {},
    showAttachSheet: Boolean = false,
    onScrollPositionChanged: (Boolean) -> Unit = {},
    onUnreadBannerTapped: () -> Unit = {},
    proposalSummaryState: ConversationProposalSummaryUiState =
        ConversationProposalSummaryUiState.Empty,
    modifier: Modifier = Modifier,
) {
    ConversationScreen(
        state = state,
        actions = ConversationScreenActions(
            navigation = ConversationScreenActions.Navigation(
                onBack = onBackClick,
                onViewWorkOrder = onViewWorkOrder,
            ),
            composer = ConversationScreenActions.Composer(
                onPromptChange = onPromptChange,
                onSend = onSendClick,
                onAttach = onAttachClick,
                onStartAudioRecording = onStartAudioRecording,
                onStopAudioRecording = onStopAudioRecording,
            ),
            media = ConversationScreenActions.Media(
                showAttachSheet = showAttachSheet,
                onGallery = onGalleryClick,
                onCamera = onCameraClick,
                onConfirmSend = onConfirmMediaSend,
                onDiscard = onDiscardMedia,
                onErrorDismiss = onMediaErrorDismiss,
                onAttachSheetDismiss = onAttachSheetDismiss,
            ),
            playback = ConversationScreenActions.Playback(
                onPlayAudio = onPlayAudio,
                onPauseAudio = onPauseAudio,
                onImageClick = onImageClick,
                onFullscreenImageDismiss = onFullscreenImageDismiss,
            ),
            errors = ConversationScreenActions.Errors(
                onRetry = onRetryClick,
                onDismiss = onErrorDismiss,
                onScrollPositionChanged = onScrollPositionChanged,
                onUnreadBannerTapped = onUnreadBannerTapped,
            ),
        ),
        proposalSummaryState = proposalSummaryState,
        modifier = modifier,
    )
}

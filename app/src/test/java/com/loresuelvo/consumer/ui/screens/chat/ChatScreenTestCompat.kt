package com.loresuelvo.consumer.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisAssessment
import com.loresuelvo.consumer.domain.provider.Provider

/** Test-only adapter that keeps legacy fixtures focused on rendered state. */
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
    onViewProfileClick: (Provider) -> Unit = {},
    onBackClick: () -> Unit,
    onAttachClick: () -> Unit = {},
    onAttachImageFromGallery: () -> Unit = {},
    onAttachImageFromCamera: () -> Unit = {},
    onConfirmAttachmentSend: (Int) -> Unit = {},
    onDiscardAttachment: (Int) -> Unit = {},
    showAttachSheet: Boolean = false,
    onAttachSheetDismiss: () -> Unit = {},
    audioEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // `canSend` was derived by ChatUiState before the contract was grouped.
    // Keep the parameter read so old fixtures remain source-compatible while
    // production uses the single source of truth in ChatUiState.
    @Suppress("UNUSED_VARIABLE")
    val ignoredCanSend = canSend

    ChatScreen(
        state = ChatUiState(
            promptInput = promptInput,
            sending = sending,
            messages = messages,
            assessment = assessment,
            recommendedProviders = recommendedProviders,
            transientError = transientError,
            preliminaryWarningVisible = preliminaryWarningVisible,
            pendingAttachments = pendingAttachments,
        ),
        actions = ChatScreenActions(
            composer = ChatScreenActions.Composer(
                onPromptChange = onPromptChange,
                onSend = onSendClick,
            ),
            diagnosis = ChatScreenActions.Diagnosis(
                onContact = onContactClick,
                onViewProfile = onViewProfileClick,
            ),
            navigation = ChatScreenActions.Navigation(onBack = onBackClick),
            media = ChatScreenActions.Media(
                onAttach = onAttachClick,
                onGallery = onAttachImageFromGallery,
                onCamera = onAttachImageFromCamera,
                onConfirmSend = onConfirmAttachmentSend,
                onDiscard = onDiscardAttachment,
                showAttachSheet = showAttachSheet,
                onAttachSheetDismiss = onAttachSheetDismiss,
                audioEnabled = audioEnabled,
            ),
            errors = ChatScreenActions.Errors(
                onRetry = onRetryClick,
                onDismiss = onErrorDismiss,
            ),
        ),
        modifier = modifier,
    )
}

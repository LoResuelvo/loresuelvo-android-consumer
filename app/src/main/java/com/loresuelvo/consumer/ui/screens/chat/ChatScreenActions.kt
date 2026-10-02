package com.loresuelvo.consumer.ui.screens.chat

import com.loresuelvo.consumer.domain.provider.Provider

/**
 * Stateless callback contract for [ChatScreen]. Grouping callbacks by
 * responsibility keeps the screen boundary stable as the AI chat gains
 * more media and diagnosis actions.
 */
data class ChatScreenActions(
    val composer: Composer = Composer(),
    val diagnosis: Diagnosis = Diagnosis(),
    val navigation: Navigation = Navigation(),
    val media: Media = Media(),
    val errors: Errors = Errors(),
) {
    data class Composer(
        val onPromptChange: (String) -> Unit = {},
        val onSend: () -> Unit = {},
    )

    data class Diagnosis(
        val onContact: (Provider) -> Unit = {},
        val onViewProfile: (Provider) -> Unit = {},
    )

    data class Navigation(
        val onBack: () -> Unit = {},
    )

    data class Media(
        val onAttach: () -> Unit = {},
        val onGallery: () -> Unit = {},
        val onCamera: () -> Unit = {},
        val onConfirmSend: (Int) -> Unit = {},
        val onDiscard: (Int) -> Unit = {},
        val showAttachSheet: Boolean = false,
        val onAttachSheetDismiss: () -> Unit = {},
        val audioEnabled: Boolean = false,
    )

    data class Errors(
        val onRetry: () -> Unit = {},
        val onDismiss: () -> Unit = {},
    )
}

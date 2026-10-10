package com.loresuelvo.consumer.ui.screens.chat

/**
 * Stateless contracts exposed by [ConversationScreen]. Keeping actions
 * grouped by responsibility prevents the screen signature from growing as
 * new media types, such as video, are added to the composer.
 */
data class ConversationScreenActions(
    val navigation: Navigation = Navigation(),
    val composer: Composer = Composer(),
    val media: Media = Media(),
    val playback: Playback = Playback(),
    val errors: Errors = Errors(),
) {
    data class Navigation(
        val onBack: () -> Unit = {},
        val onViewWorkOrder: ((String) -> Unit)? = null,
        val onViewProviderProfile: ((Long) -> Unit)? = null,
    )

    data class Composer(
        val onPromptChange: (String) -> Unit = {},
        val onSend: () -> Unit = {},
        val onAttach: () -> Unit = {},
        val onStartAudioRecording: () -> Unit = {},
        val onStopAudioRecording: () -> Unit = {},
    )

    data class Media(
        val showAttachSheet: Boolean = false,
        /** Extension point for future video selection without changing the screen API. */
        val onVideo: () -> Unit = {},
        val onGallery: () -> Unit = {},
        val onCamera: () -> Unit = {},
        val onConfirmSend: () -> Unit = {},
        val onDiscard: () -> Unit = {},
        val onErrorDismiss: () -> Unit = {},
        val onAttachSheetDismiss: () -> Unit = {},
    )

    data class Playback(
        val onPlayAudio: (String) -> Unit = {},
        val onPauseAudio: (String) -> Unit = {},
        val onImageClick: (String) -> Unit = {},
        val onFullscreenImageDismiss: () -> Unit = {},
        val onVideoClick: (String) -> Unit = {},
        val onFullscreenVideoDismiss: () -> Unit = {},
    )

    data class Errors(
        val onRetry: () -> Unit = {},
        val onDismiss: () -> Unit = {},
        val onScrollPositionChanged: (Boolean) -> Unit = {},
        val onUnreadBannerTapped: () -> Unit = {},
    )
}

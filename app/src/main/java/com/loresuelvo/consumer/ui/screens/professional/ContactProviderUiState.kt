package com.loresuelvo.consumer.ui.screens.professional

import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.provider.Provider

sealed interface ContactProviderUiState {

    data object Closed : ContactProviderUiState

    data class Open(
        val provider: Provider,
        val title: String = "",
        val description: String = "",
        val isSubmitting: Boolean = false,
        val error: ContactProviderError? = null,
        val attachedImages: List<MediaUpload.Image> = emptyList(),
        val attachmentError: String? = null,
    ) : ContactProviderUiState {
        val canSubmit: Boolean
            get() = title.isNotBlank() && description.isNotBlank() && !isSubmitting
    }
}

/**
 * UI-facing error for the contact-provider flow. Mirrors the
 * `WelcomeError` pattern: typed at the screen boundary, resolved
 * to a localised string by the Composable via `stringResource`.
 * `Server` carries the backend's raw message so the consumer can
 * see exactly what the validation / 5xx said.
 */
sealed interface ContactProviderError {
    data object Network : ContactProviderError
    data object Unauthorized : ContactProviderError
    data class Server(val code: Int, val message: String) : ContactProviderError
}

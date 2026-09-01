package com.loresuelvo.consumer.ui.screens.professional

import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.provider.Provider

/**
 * UDF state for the contact-provider bottom sheet.
 *
 *  - [Closed]: the modal is not visible. The screen renders the
 *    providers list as usual.
 *  - [Open]: the modal is visible. The provider being contacted
 *    is fixed for the lifetime of the open state (the form does
 *    not support switching providers mid-flight). [title] and
 *    [description] track the user's input; [isSubmitting] flips
 *    during the `POST /job-requests` round-trip; [error] is the
 *    typed failure surfaced by the use case (network, server,
 *    unauthorized) — null while the form is idle or submitting.
 *
 *  - [attachedImages]: images the consumer has picked from the
 *    device but has not yet submitted (scenario 03-UXUI).
 *    Validated against the
 *    [com.loresuelvo.consumer.domain.jobrequest.MAX_JOB_REQUEST_IMAGES]
 *    cap; rejected batches surface via [attachmentError].
 *
 *  - [attachmentError]: localised message describing why the
 *    most recent `onAttachImages` call was rejected (limit
 *    reached). Null while the form is idle or the consumer is
 *    typing; cleared by [ContactProviderViewModel.onRemoveImage]
 *    so the consumer can retry the attach.
 *
 * [canSubmit] is a derived property: the form is submittable only
 * when both fields are non-blank AND the round-trip is not in
 * flight. The submit button reads this to gate its `enabled`
 * state, and the [ContactProviderViewModel] uses it as a guard
 * inside `onSubmit`.
 */
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

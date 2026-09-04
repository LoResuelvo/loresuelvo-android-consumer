package com.loresuelvo.consumer.ui.screens.professional

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.jobrequest.ALLOWED_IMAGE_MIME_TYPES
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestData
import com.loresuelvo.consumer.domain.jobrequest.CreateJobRequestOutcome
import com.loresuelvo.consumer.domain.jobrequest.MAX_IMAGE_BYTES
import com.loresuelvo.consumer.domain.jobrequest.MAX_JOB_REQUEST_IMAGES
import com.loresuelvo.consumer.domain.jobrequest.UploadJobRequestImagesOutcome
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.domain.usecase.jobrequest.CreateJobRequestUseCase
import com.loresuelvo.consumer.domain.usecase.jobrequest.UploadJobRequestImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UDF ViewModel for the contact-provider bottom sheet.
 *
 * State machine:
 *  - `Closed` → user taps "Contactar" on a provider card →
 *    `Open(provider, "", "", false, null)`.
 *  - `Open` → user types in the fields → state mutates with
 *    the new value AND clears any prior error.
 *  - `Open` → user attaches one or more images from the
 *    gallery / camera (scenario 03-UXUI) → staged into
 *    [ContactProviderUiState.Open.attachedImages] after passing
 *    the mime / size / cap validation; rejected batches
 *    surface via `attachmentError`.
 *  - `Open` → user taps "Enviar solicitud" (gated by `canSubmit`)
 *    → `Open(... isSubmitting = true, error = null)` →
 *    `CreateJobRequestUseCase` → on success, `Closed` +
 *    [ContactProviderEvent.NavigateToConversation]; on failure,
 *    `Open(... isSubmitting = false, error = <typed>)`.
 *  - `Open` → user taps "Cancelar" or swipes the modal down →
 *    `Closed`.
 *
 * The events are exposed via a buffered `Channel` so a slow
 * collector cannot drop the navigation event. The host in
 * `LoResuelvoNav` collects them inside a `LaunchedEffect` scoped
 * to the navigation entry, which survives configuration changes.
 */
@HiltViewModel
class ContactProviderViewModel @Inject constructor(
    private val createJobRequest: CreateJobRequestUseCase,
    private val mediaReader: MediaReader,
    private val uploadJobRequestImages: UploadJobRequestImagesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactProviderUiState>(
        ContactProviderUiState.Closed,
    )
    val uiState: StateFlow<ContactProviderUiState> = _uiState.asStateFlow()

    private val _events = Channel<ContactProviderEvent>(Channel.BUFFERED)
    val events: Flow<ContactProviderEvent> = _events.receiveAsFlow()

    fun onOpenContact(provider: Provider) {
        _uiState.update { ContactProviderUiState.Open(provider) }
    }

    fun onCancel() {
        _uiState.update { ContactProviderUiState.Closed }
    }

    fun onDismissError() {
        _uiState.update { state ->
            if (state is ContactProviderUiState.Open) state.copy(error = null) else state
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { state ->
            if (state is ContactProviderUiState.Open) {
                state.copy(title = title, error = null)
            } else {
                state
            }
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { state ->
            if (state is ContactProviderUiState.Open) {
                state.copy(description = description, error = null)
            } else {
                state
            }
        }
    }

    /**
     * Stages the [images] the consumer just picked from the
     * device. Mirrors the webapp's `ImageAttachmentSelector`:
     *  - Drops files outside [ALLOWED_IMAGE_MIME_TYPES] (the
     *    picker filters the chooser, but a programmatic attach
     *    could bypass that — the VM is the last line of
     *    defence).
     *  - Drops files larger than [MAX_IMAGE_BYTES].
     *  - Rejects the batch entirely (without mutating the
     *    existing list) when it would push the total over
     *    [MAX_JOB_REQUEST_IMAGES], and surfaces the rejection
     *    via `attachmentError` so the UI can render the
     *    "limit reached" copy.
     *
     * The caller supplies already-decoded [MediaUpload.Image]
     * instances so the VM doesn't need to know about Android
     * `Uri`s or `MediaReader` — that contract lives in the
     * route's gallery / camera launchers.
     */
    fun onAttachImages(images: List<MediaUpload.Image>) {
        _uiState.update { state ->
            if (state !is ContactProviderUiState.Open) return@update state
            val eligible = images.filter(::isEligibleImage)
            val next = state.attachedImages + eligible
            if (next.size > MAX_JOB_REQUEST_IMAGES) {
                state.copy(
                    attachmentError = LIMIT_REACHED_SENTINEL,
                )
            } else {
                state.copy(
                    attachedImages = next,
                    attachmentError = null,
                )
            }
        }
    }

    /**
     * Removes the staged image at [index]. Out-of-range indexes
     * are a no-op (the UI may race with recomposition). Also
     * clears the limit-reached error so the consumer can retry
     * the attach immediately after removing one image.
     */
    fun onRemoveImage(index: Int) {
        _uiState.update { state ->
            if (state !is ContactProviderUiState.Open) return@update state
            if (index !in state.attachedImages.indices) return@update state
            state.copy(
                attachedImages = state.attachedImages.toMutableList().apply {
                    removeAt(index)
                },
                attachmentError = null,
            )
        }
    }

    private fun isEligibleImage(image: MediaUpload.Image): Boolean =
        image.mimeType in ALLOWED_IMAGE_MIME_TYPES &&
            image.bytes.size <= MAX_IMAGE_BYTES

    /**
     * Decodes [uri] via [MediaReader] (the same port the chat
     * surfaces use) and appends the resulting image to the
     * staged list. Mirrors the `01-AIP` flow on the AI
     * diagnostic chat. `null` and unreadable URIs are dropped
     * silently — the picker contract already filters URIs at
     * the OS level. Audio / video media are also dropped
     * (only [MediaUpload.Image] is accepted); the picker
     * surfaces only image files today.
     */
    fun onAttachImageFromUri(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val decoded = try {
                mediaReader.read(uri)
            } catch (e: java.io.IOException) {
                return@launch
            } as? MediaUpload.Image ?: return@launch
            onAttachImages(listOf(decoded))
        }
    }

    private companion object {
        /**
         * Sentinel that flips the [ContactProviderUiState.Open.attachmentError]
         * slot to a non-null value. The route translates it into a
         * localised string via `stringResource` so the VM stays
         * locale-agnostic.
         */
        const val LIMIT_REACHED_SENTINEL: String = "limit_reached"
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state !is ContactProviderUiState.Open) return
        if (!state.canSubmit) return
        // Flip the loading flag synchronously so the UI sees the
        // spinner immediately. The HTTP round-trip happens in the
        // launch below; the loading state is captured in the
        // observed history before the launch completes (mirrors
        // ChatViewModel.onSendClick's pattern).
        _uiState.update { state.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            // Upload every staged image first so the create call
            // carries the backend-issued file UUIDs in
            // `image_file_ids[]`. Without this step the backend
            // would persist the JobRequest without any images and
            // the provider would see no attachments. The empty
            // case short-circuits inside the use case.
            val uploadOutcome = uploadJobRequestImages(state.attachedImages)
            if (uploadOutcome is UploadJobRequestImagesOutcome.Failure) {
                _uiState.update {
                    state.copy(
                        isSubmitting = false,
                        error = uploadOutcome.toContactProviderError(),
                    )
                }
                return@launch
            }
            val fileIds = (uploadOutcome as UploadJobRequestImagesOutcome.Success).fileIds
            when (val outcome = createJobRequest(
                CreateJobRequestData(
                    providerId = state.provider.id,
                    title = state.title,
                    description = state.description,
                    imageFileIds = fileIds,
                ),
            )) {
                is CreateJobRequestOutcome.Success -> {
                    val conversationId = outcome.jobRequest.conversationId
                    if (conversationId != null) {
                        _uiState.update { ContactProviderUiState.Closed }
                        _events.send(
                            ContactProviderEvent.NavigateToConversation(conversationId),
                        )
                    } else {
                        // The backend's contract guarantees a
                        // conversation_id on success. If it ever
                        // changes, surface the disagreement instead
                        // of silently dropping the user.
                        _uiState.update {
                            state.copy(
                                isSubmitting = false,
                                error = ContactProviderError.Server(
                                    code = 0,
                                    message = "Conversation ID missing in response",
                                ),
                            )
                        }
                    }
                }
                is CreateJobRequestOutcome.Failure -> {
                    _uiState.update {
                        state.copy(
                            isSubmitting = false,
                            error = outcome.toContactProviderError(),
                        )
                    }
                }
            }
        }
    }

    private fun CreateJobRequestOutcome.Failure.toContactProviderError():
        ContactProviderError = when (this) {
        is CreateJobRequestOutcome.Failure.Network -> ContactProviderError.Network
        is CreateJobRequestOutcome.Failure.Unauthorized ->
            ContactProviderError.Unauthorized
        is CreateJobRequestOutcome.Failure.Server ->
            ContactProviderError.Server(code, message)
    }

    private fun UploadJobRequestImagesOutcome.Failure.toContactProviderError():
        ContactProviderError = when (this) {
        is UploadJobRequestImagesOutcome.Failure.Network ->
            ContactProviderError.Network
        is UploadJobRequestImagesOutcome.Failure.Unauthorized ->
            ContactProviderError.Unauthorized
        is UploadJobRequestImagesOutcome.Failure.Server ->
            ContactProviderError.Server(code, message)
    }
}

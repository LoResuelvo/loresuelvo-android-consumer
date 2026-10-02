package com.loresuelvo.consumer.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.usecase.auth.RegisterConsumerCommand
import com.loresuelvo.consumer.domain.usecase.auth.RegisterConsumerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UDF ViewModel for the `CompleteProfile` screen. Owns the form
 * state, drives the registration use case, and emits one-shot
 * events for navigation.
 *
 * State vs event separation follows AGENTS.md: persistent state
 * lives in [CompleteProfileUiState]; one-shot signals (navigate,
 * session cleared, …) live in [CompleteProfileEvent] and flow
 * through a buffered Channel.
 *
 * Side effects: on [UserRegistrationOutcome.Failure.Unauthorized]
 * the VM calls `AuthSessionStore.clearSession()` so the navigation
 * graph can fall back to Welcome. No other side effects.
 */
@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val registerConsumerUseCase: RegisterConsumerUseCase,
    private val sessionStore: AuthSessionStore,
    private val mediaReader: MediaReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CompleteProfileUiState(
            firstName = sessionStore.sessionFlow.value?.user?.firstName.orEmpty(),
            lastName = sessionStore.sessionFlow.value?.user?.lastName.orEmpty(),
        )
    )
    val uiState: StateFlow<CompleteProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<CompleteProfileEvent>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<CompleteProfileEvent> = _events.receiveAsFlow()

    fun onAction(action: CompleteProfileAction) {
        when (action) {
            is CompleteProfileAction.FirstNameChanged -> onFirstNameChange(action.value)
            is CompleteProfileAction.LastNameChanged -> onLastNameChange(action.value)
            is CompleteProfileAction.StreetChanged -> onStreetChange(action.value)
            is CompleteProfileAction.StreetNumberChanged -> onStreetNumberChange(action.value)
            is CompleteProfileAction.FloorChanged -> onFloorChange(action.value)
            is CompleteProfileAction.UnitChanged -> onUnitChange(action.value)
            CompleteProfileAction.PickPhotoClicked -> Unit
            is CompleteProfileAction.ProfilePhotoCropConfirmed ->
                onProfilePhotoCropConfirmed(action.photo)
            CompleteProfileAction.CancelProfilePhotoCropClicked -> cancelProfilePhotoCrop()
            CompleteProfileAction.RemovePhotoClicked -> removeProfilePhoto()
            CompleteProfileAction.ContinueClicked -> onContinueClick()
        }
    }

    /** Reads and validates the picker result while keeping Android I/O out of the composables. */
    fun onProfilePhotoSelected(uri: Uri) {
        _uiState.update { it.copy(photoLoading = true, error = null) }
        viewModelScope.launch {
            try {
                when (val media = mediaReader.read(uri)) {
                    is MediaUpload.Image -> validateProfilePhoto(media)
                    is MediaUpload.Audio -> _uiState.update {
                        it.copy(
                            photoLoading = false,
                            error = CompleteProfileError.ProfilePhotoUnsupportedFormat,
                        )
                    }
                    is MediaUpload.Video -> _uiState.update {
                        it.copy(
                            photoLoading = false,
                            error = CompleteProfileError.ProfilePhotoUnsupportedFormat,
                        )
                    }
                }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(photoLoading = false, error = CompleteProfileError.ProfilePhotoUnreadable)
                }
            }
        }
    }

    private fun validateProfilePhoto(photo: MediaUpload.Image) {
        val error = profilePhotoValidationError(photo)
        _uiState.update {
            it.copy(
                pendingProfilePhoto = if (error == null) photo else it.pendingProfilePhoto,
                photoLoading = false,
                error = error,
            )
        }
    }

    /** Commits only the image produced by the circular crop editor. */
    fun onProfilePhotoCropConfirmed(photo: MediaUpload.Image) {
        val error = profilePhotoValidationError(photo)
        _uiState.update {
            it.copy(
                profilePhoto = if (error == null) photo else it.profilePhoto,
                pendingProfilePhoto = if (error == null) null else it.pendingProfilePhoto,
                error = error,
            )
        }
    }

    private fun profilePhotoValidationError(photo: MediaUpload.Image): CompleteProfileError? {
        return when {
            photo.bytes.isEmpty() -> CompleteProfileError.ProfilePhotoEmpty
            photo.bytes.size > MAX_PROFILE_PHOTO_BYTES -> CompleteProfileError.ProfilePhotoTooLarge
            photo.mimeType.lowercase() !in ALLOWED_PROFILE_PHOTO_MIME_TYPES ->
                CompleteProfileError.ProfilePhotoUnsupportedFormat
            else -> null
        }
    }

    private fun cancelProfilePhotoCrop() {
        _uiState.update { it.copy(pendingProfilePhoto = null, error = null) }
    }

    private fun removeProfilePhoto() {
        _uiState.update { it.copy(profilePhoto = null, pendingProfilePhoto = null, error = null) }
    }

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, error = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, error = null) }
    }

    fun onStreetChange(value: String) {
        _uiState.update { it.copy(street = value, error = null) }
    }

    fun onStreetNumberChange(value: String) {
        _uiState.update { it.copy(streetNumber = value, error = null) }
    }

    fun onFloorChange(value: String) {
        _uiState.update { it.copy(floor = value, error = null) }
    }

    fun onUnitChange(value: String) {
        _uiState.update { it.copy(unit = value, error = null) }
    }

    fun onContinueClick() {
        val state = _uiState.value
        when {
            state.firstName.isBlank() -> {
                _uiState.update { it.copy(error = CompleteProfileError.MissingFirstName) }
                return
            }
            state.lastName.isBlank() -> {
                _uiState.update { it.copy(error = CompleteProfileError.MissingLastName) }
                return
            }
            state.street.isBlank() -> {
                _uiState.update { it.copy(error = CompleteProfileError.MissingStreet) }
                return
            }
            state.streetNumber.isBlank() -> {
                _uiState.update { it.copy(error = CompleteProfileError.MissingStreetNumber) }
                return
            }
        }
        // Double-tap protection: a second click while the first
        // registration is in flight is a no-op. Locking on
        // state.loading rather than a separate flag means the
        // protection is automatically released on any completion
        // path (Success, Failure, or an unexpected throw).
        if (state.loading || state.photoLoading) return
        if (state.error is CompleteProfileError.ProfilePhotoEmpty ||
            state.error is CompleteProfileError.ProfilePhotoUnsupportedFormat ||
            state.error is CompleteProfileError.ProfilePhotoTooLarge ||
            state.error is CompleteProfileError.ProfilePhotoUnreadable
        ) {
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            val outcome = registerConsumerUseCase(
                RegisterConsumerCommand(
                    firstName = state.firstName,
                    lastName = state.lastName,
                    street = state.street,
                    streetNumber = state.streetNumber,
                    floor = state.floor,
                    unit = state.unit,
                    profilePhoto = state.profilePhoto,
                )
            )
            when (outcome) {
                is UserRegistrationOutcome.Success -> {
                    _uiState.update { it.copy(loading = false, error = null) }
                    _events.trySend(CompleteProfileEvent.NavigateToHome)
                }
                is UserRegistrationOutcome.Failure.Network -> _uiState.update {
                    it.copy(
                        loading = false,
                        error = CompleteProfileError.Network(
                            outcome.cause.message ?: "Network error"
                        ),
                    )
                }
                is UserRegistrationOutcome.Failure.Server -> _uiState.update {
                    it.copy(
                        loading = false,
                        error = CompleteProfileError.Server(
                            code = outcome.code,
                            message = outcome.message,
                        ),
                    )
                }
                is UserRegistrationOutcome.Failure.Unauthorized -> {
                    sessionStore.clearSession()
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = CompleteProfileError.Unauthorized(outcome.message),
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_PROFILE_PHOTO_BYTES: Int = 5 * 1024 * 1024
        val ALLOWED_PROFILE_PHOTO_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
        )
    }
}

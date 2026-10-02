package com.loresuelvo.consumer.ui.screens.profile

import com.loresuelvo.consumer.domain.conversation.MediaUpload

/**
 * UDF state for the `CompleteProfile` screen.
 *
 * [loading] is `true` while the [CompleteProfileViewModel] is
 * waiting for the [com.loresuelvo.consumer.domain.usecase.auth.RegisterConsumerUseCase].
 * The button is disabled and a spinner is shown.
 *
 * [error] is non-null only when the most recent `onContinueClick`
 * produced a user-visible error. Local validation (blank fields)
 * and remote failures (network / server / unauthorized) both flow
 * through this field as typed [CompleteProfileError] instances; the
 * screen maps each to a localized message.
 */
data class CompleteProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val street: String = "",
    val streetNumber: String = "",
    val floor: String = "",
    val unit: String = "",
    val profilePhoto: MediaUpload.Image? = null,
    val pendingProfilePhoto: MediaUpload.Image? = null,
    val photoLoading: Boolean = false,
    val loading: Boolean = false,
    val error: CompleteProfileError? = null,
)

/**
 * User intent dispatched by the complete-profile UI.
 *
 * Keeping the screen contract event-based prevents every new form field
 * from adding another value/callback pair to each composable boundary.
 */
sealed interface CompleteProfileAction {
    data class FirstNameChanged(val value: String) : CompleteProfileAction
    data class LastNameChanged(val value: String) : CompleteProfileAction
    data class StreetChanged(val value: String) : CompleteProfileAction
    data class StreetNumberChanged(val value: String) : CompleteProfileAction
    data class FloorChanged(val value: String) : CompleteProfileAction
    data class UnitChanged(val value: String) : CompleteProfileAction
    data object PickPhotoClicked : CompleteProfileAction
    data class ProfilePhotoCropConfirmed(val photo: MediaUpload.Image) : CompleteProfileAction
    data object CancelProfilePhotoCropClicked : CompleteProfileAction
    data object RemovePhotoClicked : CompleteProfileAction
    data object ContinueClicked : CompleteProfileAction
}

/**
 * Typed error state for `CompleteProfile`. Sealed so the screen
 * exhaustively matches the variants when mapping to localized
 * messages. Mirrors the failures documented in
 * `loresuelvo-api/openapi/paths/consumers.yaml`.
 */
sealed interface CompleteProfileError {
    data object MissingFirstName : CompleteProfileError
    data object MissingLastName : CompleteProfileError
    data object MissingStreet : CompleteProfileError
    data object MissingStreetNumber : CompleteProfileError
    data object ProfilePhotoEmpty : CompleteProfileError
    data object ProfilePhotoUnsupportedFormat : CompleteProfileError
    data object ProfilePhotoTooLarge : CompleteProfileError
    data object ProfilePhotoUnreadable : CompleteProfileError
    data class Network(val message: String) : CompleteProfileError
    data class Server(val code: Int, val message: String) : CompleteProfileError
    data class Unauthorized(val message: String) : CompleteProfileError
}
/**
 * One-shot events emitted by [CompleteProfileViewModel]. Distinct
 * from the [CompleteProfileUiState] so the screen can react to
 * them exactly once (e.g. navigate on `Success`) without leaving
 * "should navigate" flags in the persistent state.
 */
sealed interface CompleteProfileEvent {
    data object NavigateToHome : CompleteProfileEvent
}

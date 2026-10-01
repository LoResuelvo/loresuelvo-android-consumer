package com.loresuelvo.consumer.ui.screens.profile

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

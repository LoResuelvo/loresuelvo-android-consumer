package com.loresuelvo.consumer.ui.screens.profile

import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.User

import com.loresuelvo.consumer.domain.notifications.NotificationPermissionStatus

sealed interface ConsumerProfileUiState {
    data object Loading : ConsumerProfileUiState
    data class Ready(
        val user: User,
        val calendarConnection: CalendarConnectionUiState = CalendarConnectionUiState.Idle,
        val notificationPermission: NotificationPermissionStatus = NotificationPermissionStatus.UNDECIDED,
    ) : ConsumerProfileUiState
    data class Error(val failure: ConsumerProfileFailure) : ConsumerProfileUiState
}

sealed interface CalendarConnectionUiState {
    data object Idle : CalendarConnectionUiState
    data object Connecting : CalendarConnectionUiState
    data object Cancelled : CalendarConnectionUiState
    data object ConfigurationError : CalendarConnectionUiState
    data class Failed(val failure: CalendarConnectionFailure) : CalendarConnectionUiState
}

sealed interface CalendarConnectionFailure {
    data class Network(val cause: Throwable) : CalendarConnectionFailure
    data class Unauthorized(val message: String) : CalendarConnectionFailure
    data class Server(val code: Int, val message: String) : CalendarConnectionFailure
}

sealed interface ConsumerProfileFailure {
    data object NotFound : ConsumerProfileFailure
    data class Network(val cause: Throwable) : ConsumerProfileFailure
    data class Server(val code: Int, val message: String) : ConsumerProfileFailure
    data class Unauthorized(val message: String) : ConsumerProfileFailure
}

internal fun CurrentUserOutcome.toConsumerProfileFailure(): ConsumerProfileFailure? = when (this) {
    is CurrentUserOutcome.Success -> null
    CurrentUserOutcome.NotFound -> ConsumerProfileFailure.NotFound
    is CurrentUserOutcome.Failure.Network -> ConsumerProfileFailure.Network(cause)
    is CurrentUserOutcome.Failure.Server -> ConsumerProfileFailure.Server(code, message)
    is CurrentUserOutcome.Failure.Unauthorized -> ConsumerProfileFailure.Unauthorized(message)
}

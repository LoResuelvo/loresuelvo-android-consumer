package com.loresuelvo.consumer.domain.calendar

sealed interface CalendarConnectionOutcome {
    data object Success : CalendarConnectionOutcome

    sealed interface Failure : CalendarConnectionOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Unauthorized(val message: String) : Failure
        data class Server(val code: Int, val message: String) : Failure
    }
}

interface CalendarConnectionRepository {
    suspend fun connect(serverAuthCode: String): CalendarConnectionOutcome
}

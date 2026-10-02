package com.loresuelvo.consumer.domain.turno

sealed interface TurnosOutcome {
    data class Success(val turnos: List<Turno>) : TurnosOutcome
    sealed interface Failure : TurnosOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
    }
}

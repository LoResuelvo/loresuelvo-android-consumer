package com.loresuelvo.consumer.domain.turno

interface TurnosRepository {
    suspend fun getTurnos(): TurnosOutcome
}

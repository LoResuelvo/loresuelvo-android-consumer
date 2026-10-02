package com.loresuelvo.consumer.domain.usecase.turno

import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTurnosUseCase @Inject constructor(
    private val turnosRepository: TurnosRepository,
) {
    suspend operator fun invoke(): TurnosOutcome = turnosRepository.getTurnos()
}

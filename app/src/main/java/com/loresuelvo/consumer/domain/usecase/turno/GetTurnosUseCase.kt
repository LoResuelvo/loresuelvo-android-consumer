package com.loresuelvo.consumer.domain.usecase.turno

import com.loresuelvo.consumer.domain.turno.TurnosOutcome
import com.loresuelvo.consumer.domain.turno.TurnosRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Returns the consumer's full list of scheduled appointments
 * (visualize-turns.feature scenario 02-VT). Pure pass-through to
 * [TurnosRepository.getTurnos] — exists to keep the
 * [com.loresuelvo.consumer.ui.screens.turnos.TurnosViewModel]
 * free of any repository import and to match the
 * one-use-case-per-action convention.
 *
 * Errors are not swallowed: [TurnosOutcome.Failure.Network] and
 * [TurnosOutcome.Failure.Server] propagate verbatim so the
 * screen can render the typed retry CTA (scenarios 13-VT /
 * 14-VT).
 */
@Singleton
class GetTurnosUseCase @Inject constructor(
    private val turnosRepository: TurnosRepository,
) {
    suspend operator fun invoke(): TurnosOutcome = turnosRepository.getTurnos()
}

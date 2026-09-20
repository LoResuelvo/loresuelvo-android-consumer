package com.loresuelvo.consumer.domain.turno

/**
 * Denormalised counterpart attached to a [Turno].
 *
 * Landed minimally for scenario 02-VT: the type carries the
 * fields needed to render a row in the "Mis Turnos" list
 * (id, full name, category, profile photo URL).
 */
data class TurnoCounterpart(
    val id: String,
    val name: String,
    val surname: String,
    val categoryName: String,
    val profilePhotoUrl: String?,
)

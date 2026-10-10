package com.loresuelvo.consumer.domain.turno

data class TurnoCounterpart(
    val id: String,
    val name: String,
    val surname: String,
    val categoryName: String,
    val profilePhotoUrl: String?,
    val identityVerified: Boolean = false,
)

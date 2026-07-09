package com.loresuelvo.consumer.domain.auth

/**
 * Pure-domain payload for the consumer registration use case. This is
 * the data the [RegisterConsumerUseCase] passes to the [UserRepository]
 * port; the data layer maps it to a snake_case DTO before hitting the
 * HTTP endpoint.
 *
 * All three fields are non-null. Empty [firstName] / [lastName] are
 * allowed at this level; UI-side validation enforces a non-blank
 * form. The API currently trims surrounding whitespace on its side
 * and does not enforce non-empty validation, so blank values reach
 * the backend and are persisted as empty strings.
 */
data class RegisterConsumerData(
    val email: String,
    val firstName: String,
    val lastName: String,
)

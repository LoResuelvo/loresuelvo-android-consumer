package com.loresuelvo.consumer.domain.usecase.auth

/**
 * Pure-domain command for [RegisterConsumerUseCase]. Carries the
 * user-entered first and last name; the use case trims whitespace
 * and merges them with the email from the cached session before
 * calling the repository. The command does not hold a User or a
 * session — that is the use case's job to assemble.
 */
data class RegisterConsumerCommand(
    val firstName: String,
    val lastName: String,
)

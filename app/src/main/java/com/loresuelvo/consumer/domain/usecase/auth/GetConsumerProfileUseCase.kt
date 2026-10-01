package com.loresuelvo.consumer.domain.usecase.auth

import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the authenticated consumer profile from the existing /me contract. */
@Singleton
class GetConsumerProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(): CurrentUserOutcome = userRepository.getCurrentUser()
}

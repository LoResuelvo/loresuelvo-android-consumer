package com.loresuelvo.consumer.domain.usecase.auth

import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the consumer-registration flow: takes the form input
 * as a [RegisterConsumerCommand], combines it with the email from
 * the cached Auth0 session, calls [UserRepository.registerConsumer],
 * and persists the updated profile on success.
 *
 * The use case is the only place where the session email and the
 * form input are joined. It does NOT know about the UI, about
 * Auth0 directly, or about navigation. It returns typed outcomes
 * and never throws on HTTP / network errors.
 *
 * Mirrors the webapp's `application/onboarding/register-user.ts`
 * in shape, adapted to Kotlin and to the Android session store.
 */
@Singleton
class RegisterConsumerUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authSessionStore: AuthSessionStore,
) {
    suspend operator fun invoke(
        command: RegisterConsumerCommand,
    ): UserRegistrationOutcome {
        val session = authSessionStore.getSession()
            ?: return UserRegistrationOutcome.Failure.Unauthorized("Sin sesión activa")

        val email = session.user.email
            ?: return UserRegistrationOutcome.Failure.Server(
                code = 0,
                message = "Email ausente en la sesión",
            )

        val data = RegisterConsumerData(
            email = email,
            firstName = command.firstName.trim(),
            lastName = command.lastName.trim(),
        )

        val outcome = userRepository.registerConsumer(data)
        if (outcome is UserRegistrationOutcome.Success) {
            // Persist the updated profile so isProfileComplete() returns
            // true on the next read and the navigation graph can move
            // past CompleteProfile without waiting for a session reload.
            authSessionStore.saveSession(
                session.copy(user = outcome.user),
            )
        }
        return outcome
    }
}

package com.loresuelvo.consumer.domain.usecase.auth

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.SessionSynchronizationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncAuthenticatedSessionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionStore: AuthSessionStore,
) {
    suspend operator fun invoke(
        authenticatedSession: AuthSession,
    ): SessionSynchronizationOutcome {
        // The interceptor needs the new token before it can resolve /me.
        sessionStore.saveSession(authenticatedSession)

        return when (val outcome = userRepository.getCurrentUser()) {
            is CurrentUserOutcome.Success -> {
                val synchronized = authenticatedSession.copy(user = outcome.user)
                sessionStore.saveSession(synchronized)
                SessionSynchronizationOutcome.Success(synchronized)
            }
            CurrentUserOutcome.NotFound ->
                SessionSynchronizationOutcome.Success(authenticatedSession)
            is CurrentUserOutcome.Failure.Network -> {
                sessionStore.clearSession()
                SessionSynchronizationOutcome.Failure.Network(outcome.cause)
            }
            is CurrentUserOutcome.Failure.Server -> {
                sessionStore.clearSession()
                SessionSynchronizationOutcome.Failure.Server(outcome.code, outcome.message)
            }
            is CurrentUserOutcome.Failure.Unauthorized -> {
                sessionStore.clearSession()
                SessionSynchronizationOutcome.Failure.Unauthorized(outcome.message)
            }
        }
    }
}

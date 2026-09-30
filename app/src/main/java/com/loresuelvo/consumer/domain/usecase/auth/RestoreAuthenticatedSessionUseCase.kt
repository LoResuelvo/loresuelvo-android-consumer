package com.loresuelvo.consumer.domain.usecase.auth

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import javax.inject.Inject

class RestoreAuthenticatedSessionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionStore: AuthSessionStore,
) {
    suspend operator fun invoke(session: AuthSession): CurrentUserOutcome {
        val outcome = userRepository.getCurrentUser()
        // No restaurar una cuenta cerrada o reemplazada durante la petición.
        if (sessionStore.sessionFlow.value != session) return outcome
        when (outcome) {
            is CurrentUserOutcome.Success -> sessionStore.saveSession(session.copy(user = outcome.user))
            CurrentUserOutcome.NotFound -> sessionStore.saveSession(
                session.copy(user = session.user.copy(address = null)),
            )
            is CurrentUserOutcome.Failure.Unauthorized -> sessionStore.clearSession()
            is CurrentUserOutcome.Failure -> Unit
        }
        return outcome
    }
}

package com.loresuelvo.consumer.platform.notifications

import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.usecase.installation.RegisterInstallationUseCase
import com.loresuelvo.consumer.domain.installation.RegistrationOutcome
import com.loresuelvo.consumer.domain.installation.InstallationRegistrationRequests
import com.loresuelvo.consumer.domain.installation.RegistrationCandidate
import com.loresuelvo.consumer.domain.auth.AuthSession
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Singleton
class InstallationRegistrationCoordinator @Inject constructor(
    private val requests: InstallationRegistrationRequests,
    private val sessions: AuthSessionStore,
    private val register: RegisterInstallationUseCase,
    private val locale: RegistrationLocaleProvider,
    @Named("registrationScope") private val scope: CoroutineScope,
) {
    private val mutableOutcome = MutableStateFlow<RegistrationOutcome?>(null)
    val outcome = mutableOutcome.asStateFlow()
    private var job: Job? = null

    @Synchronized
    fun start() {
        if (job != null) return
        job = scope.launch {
            requests.pending.collect { candidate ->
                if (candidate == null) return@collect
                registerCandidate(candidate)
            }
        }
    }

    private suspend fun registerCandidate(candidate: RegistrationCandidate) {
        val session = sessions.getSession() ?: return
        val result = registerCurrentSession(candidate, session)
        publishIfCurrent(candidate, session, result)
    }

    private suspend fun registerCurrentSession(
        candidate: RegistrationCandidate,
        session: AuthSession,
    ): RegistrationOutcome = try {
        register(
            session = session,
            locale = locale.locale(),
            attemptId = UUID.randomUUID().toString(),
            isCurrent = { commitIfCurrent(candidate, session) {} },
            commitIfCurrent = { action -> commitIfCurrent(candidate, session, action) },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        RegistrationOutcome.UnexpectedFailure(error)
    }

    private fun commitIfCurrent(
        candidate: RegistrationCandidate,
        session: AuthSession,
        action: () -> Unit,
    ): Boolean = requests.ifCurrent(candidate.generation) {
        if (!matchesCurrentSession(session)) false else {
            action()
            true
        }
    }

    private fun matchesCurrentSession(session: AuthSession): Boolean =
        sessions.getSession() == session && sessions.sessionFlow.value == session

    private fun publishIfCurrent(
        candidate: RegistrationCandidate,
        session: AuthSession,
        result: RegistrationOutcome,
    ) {
        commitIfCurrent(candidate, session) {
            mutableOutcome.value = result
            if (result is RegistrationOutcome.Success) requests.acknowledge(candidate.generation)
        }
    }

}

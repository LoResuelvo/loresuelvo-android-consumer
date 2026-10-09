package com.loresuelvo.consumer.domain.usecase.installation

import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.installation.InstallationConfirmation
import com.loresuelvo.consumer.domain.installation.InstallationRegistrationResult
import com.loresuelvo.consumer.domain.installation.InstallationRepository
import com.loresuelvo.consumer.domain.installation.InstallationStateStore
import com.loresuelvo.consumer.domain.installation.PushRegistrationTokenProvider
import com.loresuelvo.consumer.domain.installation.PushTokenOutcome
import com.loresuelvo.consumer.domain.installation.RegistrationOutcome
import com.loresuelvo.consumer.domain.auth.AuthSession
import java.io.IOException

class RegisterInstallationUseCase(
    private val tokenProvider: PushRegistrationTokenProvider,
    private val store: InstallationStateStore,
    private val repository: InstallationRepository,
) {
    suspend operator fun invoke(
        session: AuthSession,
        locale: String,
        attemptId: String,
        isCurrent: () -> Boolean,
        commitIfCurrent: (() -> Unit) -> Boolean,
    ): RegistrationOutcome = invoke(
        session = session,
        locale = locale,
        attemptId = attemptId,
        isNewAuthentication = false,
        isCurrent = isCurrent,
        commitIfCurrent = commitIfCurrent,
    )

    suspend operator fun invoke(
        session: AuthSession,
        locale: String,
        attemptId: String,
        isNewAuthentication: Boolean,
        isCurrent: () -> Boolean,
        commitIfCurrent: (() -> Unit) -> Boolean,
    ): RegistrationOutcome {
        val userId = session.user.backendUserId?.takeIf { it > 0 }
            ?: return RegistrationOutcome.UnverifiedAccount
        if (!isCurrent()) return RegistrationOutcome.Superseded
        return when (val outcome = tokenProvider.token()) {
            is PushTokenOutcome.Available -> registerWithToken(
                session,
                userId,
                outcome.token,
                locale,
                attemptId,
                isCurrent,
                commitIfCurrent,
                isNewAuthentication,
            )
            PushTokenOutcome.ConfigurationUnavailable -> RegistrationOutcome.ConfigurationUnavailable
            is PushTokenOutcome.Failure -> RegistrationOutcome.TokenFailure(outcome.cause)
        }
    }

    private suspend fun registerWithToken(
        session: AuthSession,
        userId: Int,
        token: String,
        locale: String,
        attemptId: String,
        isCurrent: () -> Boolean,
        commitIfCurrent: (() -> Unit) -> Boolean,
        isNewAuthentication: Boolean,
    ): RegistrationOutcome {
        if (token.isBlank()) {
            return RegistrationOutcome.TokenFailure(IllegalStateException("Empty registration token"))
        }
        if (!isCurrent()) return RegistrationOutcome.Superseded
        return try {
            val binding = store.prepare(userId, attemptId, isNewAuthentication)
            if (!isCurrent()) return RegistrationOutcome.Superseded
            val response = repository.register(binding, token, locale, session.accessToken)
            if (!isCurrent()) return RegistrationOutcome.Superseded
            when (response) {
                is InstallationRegistrationResult.Failed -> response.outcome
                is InstallationRegistrationResult.Confirmed -> confirmRegistration(
                    binding, response.confirmation, locale, commitIfCurrent,
                )
            }
        } catch (error: IOException) {
            RegistrationOutcome.StorageFailure(error)
        }
    }

    private fun confirmRegistration(
        binding: InstallationBinding,
        confirmation: InstallationConfirmation,
        locale: String,
        commitIfCurrent: (() -> Unit) -> Boolean,
    ): RegistrationOutcome {
        if (!matchesRegistration(confirmation, binding, locale)) return RegistrationOutcome.InvalidConfirmation
        if (!commitIfCurrent { store.confirm(binding) }) return RegistrationOutcome.Superseded
        return RegistrationOutcome.Success(binding.identity.id, binding.id, binding.userId)
    }

    private fun matchesRegistration(
        confirmation: InstallationConfirmation,
        binding: InstallationBinding,
        locale: String,
    ): Boolean = confirmation.enabled &&
        confirmation.installationId == binding.identity.id &&
        confirmation.bindingId == binding.id &&
        confirmation.app == "consumer" &&
        confirmation.locale == locale
}

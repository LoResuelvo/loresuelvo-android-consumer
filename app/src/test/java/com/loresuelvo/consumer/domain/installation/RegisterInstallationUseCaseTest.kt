package com.loresuelvo.consumer.domain.installation

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.usecase.installation.RegisterInstallationUseCase
import com.loresuelvo.consumer.domain.auth.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterInstallationUseCaseTest {
    private val session = AuthSession(User("Consumer", backendUserId = 17), "dummy-jwt")
    private val binding = InstallationBinding(InstallationIdentity("installation", "secret"), "binding", null, 17, "attempt")

    @Test
    fun unverified_account_does_not_request_a_token_or_register() = runTest {
        var tokenCalled = false
        val useCase = useCase(token = { tokenCalled = true; PushTokenOutcome.Available("token") })
        assertEquals(RegistrationOutcome.UnverifiedAccount, useCase(session.copy(user = User("Claims")), "es", "attempt", { true }) { it(); true })
        assertFalse(tokenCalled)
    }

    @Test
    fun missing_configuration_and_token_failure_are_typed() = runTest {
        val cause = IllegalStateException("token unavailable")
        for ((token, expected) in listOf(
            PushTokenOutcome.ConfigurationUnavailable to RegistrationOutcome.ConfigurationUnavailable,
            PushTokenOutcome.Failure(cause) to RegistrationOutcome.TokenFailure(cause),
        )) {
            assertEquals(expected, useCase(token = { token })(session, "es", "attempt", { true }) { it(); true })
        }
    }

    @Test
    fun mismatched_or_disabled_confirmation_never_activates_a_binding() = runTest {
        val confirmation = InstallationConfirmation("installation", "binding", "consumer", "es", true)
        for (invalid in listOf(
            confirmation.copy(installationId = "other"), confirmation.copy(bindingId = "other"),
            confirmation.copy(app = "provider"), confirmation.copy(locale = "en"), confirmation.copy(enabled = false),
        )) {
            var confirmed = false
            val useCase = useCase(confirmation = invalid, confirm = { confirmed = true })
            assertEquals(RegistrationOutcome.InvalidConfirmation, useCase(session, "es", "attempt", { true }) { it(); true })
            assertFalse(confirmed)
        }
    }

    @Test
    fun logout_while_token_is_resolving_prevents_the_http_request() = runTest {
        var current = true
        var sent = false
        val useCase = useCase(token = { current = false; PushTokenOutcome.Available("token") }, sent = { sent = true })
        assertEquals(RegistrationOutcome.Superseded, useCase(session, "es", "attempt", { current }) { it(); true })
        assertFalse(sent)
    }

    @Test
    fun logout_during_http_cannot_confirm_old_binding() = runTest {
        var current = true
        var confirmed = false
        val useCase = useCase(sent = { current = false }, confirm = { confirmed = true })
        assertEquals(RegistrationOutcome.Superseded, useCase(session, "es", "attempt", { current }) { block ->
            if (current) { block(); true } else false
        })
        assertFalse(confirmed)
    }

    @Test
    fun matching_confirmation_activates_after_api_success() = runTest {
        var confirmed = false
        val outcome = useCase(confirm = { confirmed = true })(session, "es", "attempt", { true }) { it(); true }
        assertTrue(confirmed)
        assertEquals(RegistrationOutcome.Success(binding.identity.id, binding.id, binding.userId), outcome)
    }

    @Test
    fun failed_local_confirmation_is_not_reported_as_success() = runTest {
        val error = java.io.IOException("commit failed")
        val useCase = useCase(confirm = { throw error })
        assertEquals(RegistrationOutcome.StorageFailure(error), useCase(session, "es", "attempt", { true }) { it(); true })
    }

    private fun useCase(
        token: suspend () -> PushTokenOutcome = { PushTokenOutcome.Available("token") },
        confirmation: InstallationConfirmation = InstallationConfirmation("installation", "binding", "consumer", "es", true),
        sent: () -> Unit = {},
        confirm: () -> Unit = {},
    ) = RegisterInstallationUseCase(
        object : PushRegistrationTokenProvider { override suspend fun token() = token.invoke() },
        object : InstallationStateStore {
            override fun prepare(userId: Int, attemptId: String) = binding
            override fun confirm(binding: InstallationBinding) = confirm.invoke()
        },
        object : InstallationRepository {
            override suspend fun register(binding: InstallationBinding, token: String, locale: String, accessToken: String): InstallationRegistrationResult {
                assertEquals("dummy-jwt", accessToken)
                sent()
                return InstallationRegistrationResult.Confirmed(confirmation)
            }
        },
    )
}

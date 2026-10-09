package com.loresuelvo.consumer.platform.notifications

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.installation.*
import com.loresuelvo.consumer.domain.usecase.installation.RegisterInstallationUseCase
import com.loresuelvo.consumer.ui.notifications.PushRegistrationRequests
import java.io.IOException
import javax.inject.Provider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InstallationRegistrationCoordinatorTest {
    private val first = AuthSession(User("First", backendUserId = 17), "first-jwt")
    private val second = AuthSession(User("Second", backendUserId = 29), "second-jwt")
    private class Sessions : AuthSessionStore {
        override val sessionFlow = MutableStateFlow<AuthSession?>(null)
        override fun getSession() = sessionFlow.value
        override fun saveSession(session: AuthSession) { sessionFlow.value = session }
        override fun clearSession() { sessionFlow.value = null }
    }
    private class Bindings : InstallationStateStore {
        val confirmedUsers = mutableListOf<Int>()
        private var current: InstallationBinding? = null
        private val pending = mutableListOf<PendingInstallationRemoval>()
        private var nextId = 0

        override fun prepare(userId: Int, attemptId: String) = prepare(userId, attemptId, false)
        override fun prepare(userId: Int, attemptId: String, newAuthentication: Boolean): InstallationBinding {
            val existing = current
            if (existing?.userId == userId && !newAuthentication) return existing
            return InstallationBinding(
                InstallationIdentity("installation", "secret"),
                "binding-${userId}-${nextId++}", existing?.id, userId, attemptId,
            ).also { current = it }
        }
        override fun confirm(binding: InstallationBinding) {
            confirmedUsers += binding.userId
            current = binding.copy(confirmed = true)
            pending.removeAll { it.bindingId == binding.previousId }
        }
        override fun beginRemoval(userId: Int): PendingInstallationRemoval? {
            val binding = current?.takeIf { it.userId == userId } ?: return null
            current = binding.copy(confirmed = false)
            return PendingInstallationRemoval(binding.identity, binding.id, userId).also(pending::add)
        }
        override fun pendingRemoval() = pending.firstOrNull()
        override fun pendingRemovals() = pending.toList()
        override fun completeRemoval(bindingId: String): Boolean {
            if (!pending.removeAll { it.bindingId == bindingId }) return false
            if (current?.id == bindingId) current = null
            return true
        }
        fun activeBinding() = current
    }
    private fun confirmation(binding: InstallationBinding) = InstallationRegistrationResult.Confirmed(
        InstallationConfirmation(binding.identity.id, binding.id, "consumer", "es", true),
    )

    @Test fun account_change_while_token_is_pending_registers_only_the_latest_account() = runTest {
        val sessions = Sessions()
        val requests = PushRegistrationRequests()
        val bindings = Bindings()
        val token = CompletableDeferred<PushTokenOutcome>()
        var tokenCalls = 0
        val credentials = mutableListOf<String>()
        val coordinator = InstallationRegistrationCoordinator(requests, sessions,
            RegisterInstallationUseCase(
                object : PushRegistrationTokenProvider {
                    override suspend fun token() = if (tokenCalls++ == 0) token.await() else PushTokenOutcome.Available("new-token")
                }, bindings,
                object : InstallationRepository {
                    override suspend fun register(binding: InstallationBinding, token: String, locale: String, accessToken: String): InstallationRegistrationResult {
                        credentials += accessToken
                        return confirmation(binding)
                    }
                },
            ), bindings,
            com.loresuelvo.consumer.domain.installation.InstallationRemovalRepository { _, _ ->
                com.loresuelvo.consumer.domain.installation.InstallationRemovalResult.Removed
            }, RegistrationLocaleProvider { "es" }, backgroundScope)
        coordinator.start()
        sessions.saveSession(first)
        requests.request()
        runCurrent()
        requests.invalidate()
        sessions.clearSession()
        sessions.saveSession(second)
        requests.request()
        token.complete(PushTokenOutcome.Available("old-token"))
        runCurrent()

        assertEquals(listOf("second-jwt"), credentials)
        assertEquals(listOf(29), bindings.confirmedUsers)
        assertTrue(coordinator.outcome.value is RegistrationOutcome.Success)
        assertNull(requests.pending.value)
    }

    @Test fun in_flight_request_keeps_its_captured_jwt_but_cannot_activate_after_account_change() = runTest {
        val sessions = Sessions()
        val requests = PushRegistrationRequests()
        val bindings = Bindings()
        val pendingResponse = CompletableDeferred<Unit>()
        val credentials = mutableListOf<String>()
        val coordinator = InstallationRegistrationCoordinator(requests, sessions,
            RegisterInstallationUseCase(object : PushRegistrationTokenProvider {
                override suspend fun token() = PushTokenOutcome.Available("token")
            }, bindings, object : InstallationRepository {
                override suspend fun register(binding: InstallationBinding, token: String, locale: String, accessToken: String): InstallationRegistrationResult {
                    credentials += accessToken
                    if (credentials.size == 1) pendingResponse.await()
                    return confirmation(binding)
                }
            }), bindings,
            com.loresuelvo.consumer.domain.installation.InstallationRemovalRepository { _, _ ->
                com.loresuelvo.consumer.domain.installation.InstallationRemovalResult.Removed
            }, RegistrationLocaleProvider { "es" }, backgroundScope)
        coordinator.start()
        sessions.saveSession(first)
        requests.request()
        runCurrent()
        assertEquals(listOf("first-jwt"), credentials)
        sessions.saveSession(second)
        requests.request()
        runCurrent()
        assertEquals(1, credentials.size)
        pendingResponse.complete(Unit)
        runCurrent()

        assertEquals(listOf("first-jwt", "second-jwt"), credentials)
        assertEquals(listOf(29), bindings.confirmedUsers)
    }

    @Test fun offline_logout_cleanup_retries_with_fresh_bearer_without_clearing_new_binding() = runTest {
        val sessions = Sessions()
        val requests = PushRegistrationRequests()
        val bindings = Bindings()
        val removalCredentials = mutableListOf<String>()
        val removedIds = mutableListOf<String>()
        val registrationCredentials = mutableListOf<String>()
        var removalCalls = 0
        val coordinator = InstallationRegistrationCoordinator(
            requests,
            sessions,
            RegisterInstallationUseCase(
                object : PushRegistrationTokenProvider {
                    override suspend fun token() = PushTokenOutcome.Available("token")
                },
                bindings,
                object : InstallationRepository {
                    override suspend fun register(
                        binding: InstallationBinding,
                        token: String,
                        locale: String,
                        accessToken: String,
                    ): InstallationRegistrationResult {
                        registrationCredentials += accessToken
                        return confirmation(binding)
                    }
                },
            ),
            bindings,
            InstallationRemovalRepository { removal, accessToken ->
                removedIds += removal.bindingId
                removalCredentials += accessToken
                if (removalCalls++ == 0) InstallationRemovalResult.NetworkFailure(IOException())
                else InstallationRemovalResult.Superseded
            },
            RegistrationLocaleProvider { "es" },
            backgroundScope,
        )
        coordinator.start()
        sessions.saveSession(first)
        requests.requestNewAuthentication()
        runCurrent()
        val oldBinding = bindings.activeBinding()!!
        coordinator.onLogout(first)
        sessions.clearSession()
        runCurrent()

        assertEquals(listOf(oldBinding.id), removedIds)
        assertEquals(listOf("first-jwt"), removalCredentials)
        assertNotNull(bindings.pendingRemoval())
        assertEquals(false, bindings.activeBinding()?.confirmed)

        sessions.saveSession(AuthSession(first.user, "fresh-jwt"))
        requests.requestNewAuthentication()
        runCurrent()

        val newBinding = bindings.activeBinding()!!
        assertEquals(listOf(oldBinding.id, oldBinding.id), removedIds)
        assertEquals(listOf("first-jwt", "fresh-jwt"), removalCredentials)
        assertEquals(listOf("first-jwt", "fresh-jwt"), registrationCredentials)
        assertNull(bindings.pendingRemoval())
        assertTrue(newBinding.confirmed)
        assertFalse(oldBinding.id == newBinding.id)
        assertFalse(bindings.completeRemoval(oldBinding.id))
        assertEquals(newBinding.id, bindings.activeBinding()?.id)
    }

    @Test fun lazy_storage_construction_failure_is_observable_without_losing_the_candidate() = runTest {
        val requests = PushRegistrationRequests()
        val error = IOException("storage unavailable")
        val runtime = RegistrationRuntime(requests, Provider { throw error }, backgroundScope)
        runtime.start()
        requests.request()
        runCurrent()

        assertEquals(RegistrationOutcome.StorageFailure(error), runtime.startupFailure.value)
        assertNotNull(requests.pending.value)
    }

    @Test fun encrypted_preferences_security_failure_does_not_escape_the_runtime() = runTest {
        val requests = PushRegistrationRequests()
        val error = SecurityException("encrypted preferences unavailable")
        val runtime = RegistrationRuntime(requests, Provider { throw error }, backgroundScope)
        runtime.start()
        requests.request()
        runCurrent()
        assertEquals(RegistrationOutcome.StorageFailure(error), runtime.startupFailure.value)
        assertNotNull(requests.pending.value)
    }
}

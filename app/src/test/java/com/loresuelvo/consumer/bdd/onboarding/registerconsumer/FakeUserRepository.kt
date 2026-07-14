package com.loresuelvo.consumer.bdd.onboarding.registerconsumer

import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-memory `UserRepository` used by the BDD specs. Plays back a
 * configured [nextOutcome] every time `registerConsumer` is called and
 * records what reached it so the specs can assert the exact payload
 * the use case passed to the port. Tests never have to mock the network
 * layer to assert that the right body — and only one — was sent.
 */
class FakeUserRepository : UserRepository {

    override suspend fun getCurrentUser(): CurrentUserOutcome = CurrentUserOutcome.NotFound

    val invocations: AtomicInteger = AtomicInteger(0)
    private val _captured: MutableList<RegisterConsumerData> = mutableListOf()
    val captured: List<RegisterConsumerData> get() = _captured.toList()

    /**
     * One-shot: the next call returns this outcome and clears
     * `nextOutcome`. Tests that want the same outcome for multiple calls
     * (e.g. double-tap) set the field again before each call, but most
     * specs only need a single POST so this is the simpler default.
     */
    var nextOutcome: UserRegistrationOutcome = defaultSuccess

    override suspend fun registerConsumer(
        data: RegisterConsumerData,
    ): UserRegistrationOutcome {
        invocations.incrementAndGet()
        _captured += data
        val outcome = nextOutcome
        nextOutcome = defaultSuccess
        return outcome
    }

    companion object {
        private val defaultSuccess = UserRegistrationOutcome.Success(
            User(
                displayName = "Test User",
                firstName = "Test",
                lastName = "User",
                email = "test@example.com",
            ),
        )
    }
}

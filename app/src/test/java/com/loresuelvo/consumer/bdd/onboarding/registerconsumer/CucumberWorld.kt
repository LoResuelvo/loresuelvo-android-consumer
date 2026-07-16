package com.loresuelvo.consumer.bdd.onboarding.registerconsumer

import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.usecase.auth.RegisterConsumerUseCase
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileEvent
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileUiState
import com.loresuelvo.consumer.ui.screens.profile.CompleteProfileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Per-scenario world for the register-consumer BDD specs. Owns a
 * [StandardTestDispatcher] that ALL of the following share, so the
 * scenario can manipulate the [CompleteProfileViewModel] and observe
 * its state / events deterministically:
 *
 *  - `Dispatchers.Main` (set to the test dispatcher before the VM is
 *    built; cleared in [close]). The VM's `viewModelScope` resolves to
 *    the test dispatcher and is advanced by [scheduler.advanceUntilIdle].
 *  - A `CoroutineScope` hosted on the same dispatcher, used to observe
 *    `uiState` and `events` for the duration of the scenario.
 *  - A [FakeAuthSessionStore] and [FakeUserRepository] so step
 *    definitions can drive and inspect the registration flow without
 *    Android, Hilt, or `MockWebServer`.
 *
 * The world is reconstructed per scenario by Cucumber JVM (one step
 * def instance per scenario) so no state leaks across scenarios. The
 * scheduler is a fresh one each scenario, so a scenario cannot affect
 * a later one's timing.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CucumberWorld : AutoCloseable {

    private val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private lateinit var sessionStore: FakeAuthSessionStore
    private lateinit var userRepo: FakeUserRepository
    private lateinit var viewModel: CompleteProfileViewModel

    private val observedStates: MutableList<CompleteProfileUiState> = mutableListOf()
    private val observedEvents: MutableList<CompleteProfileEvent> = mutableListOf()

    private var started: Boolean = false

    /**
     * (Re)initialise the world for the current scenario. Idempotent —
     * safe to call from a `Background` step or from a `@Before` hook.
     */
    fun startScenario(authenticated: Boolean) {
        if (started) return
        started = true

        // Pin `Dispatchers.Main` to our test dispatcher BEFORE the VM
        // is built. `viewModelScope` lazily reads Main.immediate the
        // first time it's touched, so we must set it here.
        Dispatchers.setMain(dispatcher)

        val seedSession = if (authenticated) {
            AuthSession(
                user = User(
                    displayName = "Ana",
                    firstName = null,
                    lastName = null,
                    email = "ana@example.com",
                ),
                accessToken = "test-token",
            )
        } else null

        sessionStore = FakeAuthSessionStore(seedSession)
        userRepo = FakeUserRepository()

        viewModel = CompleteProfileViewModel(
            RegisterConsumerUseCase(userRepo, sessionStore),
            sessionStore,
        )

        // UNDISPATCHED so the collector receives the initial state
        // synchronously on `startScenario`'s call stack.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedStates += it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.events.collect { observedEvents += it }
        }

        // Drain any notifications scheduled by VM construction.
        scheduler.advanceUntilIdle()
    }

    fun configureOutcome(outcome: UserRegistrationOutcome) {
        require(started) { "startScenario() must be called before configureOutcome()" }
        userRepo.nextOutcome = outcome
    }

    fun setFirstName(value: String) {
        require(started) { "startScenario() must be called before setFirstName()" }
        viewModel.onFirstNameChange(value)
        scheduler.advanceUntilIdle()
    }

    fun setLastName(value: String) {
        require(started) { "startScenario() must be called before setLastName()" }
        viewModel.onLastNameChange(value)
        scheduler.advanceUntilIdle()
    }

    fun tapContinue(times: Int = 1) {
        require(started) { "startScenario() must be called before tapContinue()" }
        require(times >= 1) { "times must be >= 1" }
        repeat(times) { viewModel.onContinueClick() }
        scheduler.advanceUntilIdle()
    }

    fun lastState(): CompleteProfileUiState = observedStates.last()

    fun sessionValue(): AuthSession? = sessionStore.sessionFlow.value

    fun emittedEvents(): List<CompleteProfileEvent> = observedEvents.toList()

    fun capturedPosts(): List<RegisterConsumerData> = userRepo.captured

    fun postInvocations(): Int = userRepo.invocations.get()

    override fun close() {
        supervisorJob.cancel()
        // `Dispatchers.resetMain` is safe even when the test has not
        // called setMain (it's a no-op then).
        Dispatchers.resetMain()
    }
}

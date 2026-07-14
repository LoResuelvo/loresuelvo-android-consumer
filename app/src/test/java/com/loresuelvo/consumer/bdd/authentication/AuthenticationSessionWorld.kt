package com.loresuelvo.consumer.bdd.authentication

import android.content.Context
import com.loresuelvo.consumer.domain.auth.AuthProvider
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.AuthenticationOutcome
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.LogoutOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.loresuelvo.consumer.domain.auth.UserRepository
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.usecase.auth.SyncAuthenticatedSessionUseCase
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.ui.auth.WelcomeViewModel
import com.loresuelvo.consumer.ui.session.SessionViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthenticationSessionWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val context: Context = mockk(relaxed = true)
    private val sessionStore = FakeSessionStore()
    private val userRepository = FakeUserRepository()
    private val authProvider = FakeAuthProvider()

    private lateinit var welcomeViewModel: WelcomeViewModel
    private lateinit var sessionViewModel: SessionViewModel

    init {
        Dispatchers.setMain(dispatcher)
    }

    fun seedNoSession() {
        sessionStore.clearSession()
        buildWelcomeViewModel()
    }

    fun seedAuthenticatedSession() {
        sessionStore.saveSession(auth0Session())
        sessionViewModel = SessionViewModel(sessionStore, authProvider)
    }

    fun configureExistingAccount() {
        authProvider.loginOutcome = AuthenticationOutcome.Success(auth0Session())
    }

    fun configureNewAccount() {
        authProvider.loginOutcome = AuthenticationOutcome.Success(auth0Session())
    }

    fun configureCompleteBackendProfile() {
        userRepository.currentUserOutcome = CurrentUserOutcome.Success(
            User("Ana Perez", "Ana", "Perez", "ana@example.com"),
        )
    }

    fun configureMissingBackendProfile() {
        userRepository.currentUserOutcome = CurrentUserOutcome.NotFound
    }

    fun chooseLogin() {
        if (!::welcomeViewModel.isInitialized) buildWelcomeViewModel()
        welcomeViewModel.login(context)
        scheduler.advanceUntilIdle()
    }

    fun finishAuthentication() = chooseLogin()

    fun logoutSuccessfully() {
        authProvider.logoutOutcome = LogoutOutcome.Success
        sessionViewModel.signOut(context)
        scheduler.advanceUntilIdle()
    }

    fun loginCalls(): Int = authProvider.loginCalls
    fun signupCalls(): Int = authProvider.signupCalls
    fun session(): AuthSession? = sessionStore.sessionFlow.value

    private fun buildWelcomeViewModel() {
        welcomeViewModel = WelcomeViewModel(
            authProvider,
            SyncAuthenticatedSessionUseCase(userRepository, sessionStore),
            GetCategoriesUseCase(FakeCategoryRepository),
        )
    }

    private fun auth0Session(): AuthSession = AuthSession(
        User(displayName = "Ana", email = "ana@example.com"),
        "access-token",
    )

    override fun close() {
        Dispatchers.resetMain()
    }

    private class FakeAuthProvider : AuthProvider {
        var loginCalls = 0
        var signupCalls = 0
        var loginOutcome: AuthenticationOutcome = AuthenticationOutcome.Cancelled
        var logoutOutcome: LogoutOutcome = LogoutOutcome.Cancelled

        override suspend fun login(context: Context): AuthenticationOutcome {
            loginCalls += 1
            return loginOutcome
        }

        override suspend fun signup(context: Context): AuthenticationOutcome {
            signupCalls += 1
            return AuthenticationOutcome.Cancelled
        }

        override suspend fun loginWithGoogle(context: Context): AuthenticationOutcome =
            loginOutcome

        override suspend fun logout(context: Context): LogoutOutcome = logoutOutcome
    }

    private class FakeSessionStore : AuthSessionStore {
        private val flow = MutableStateFlow<AuthSession?>(null)
        override val sessionFlow: StateFlow<AuthSession?> = flow
        override fun getSession(): AuthSession? = flow.value
        override fun saveSession(session: AuthSession) { flow.value = session }
        override fun clearSession() { flow.value = null }
    }

    private class FakeUserRepository : UserRepository {
        var currentUserOutcome: CurrentUserOutcome = CurrentUserOutcome.NotFound
        override suspend fun getCurrentUser(): CurrentUserOutcome = currentUserOutcome
        override suspend fun registerConsumer(data: RegisterConsumerData): UserRegistrationOutcome =
            error("Registration is outside this feature")
    }

    private object FakeCategoryRepository : CategoryRepository {
        override suspend fun getCategories(): CategoriesOutcome =
            CategoriesOutcome.Success(emptyList())
    }
}

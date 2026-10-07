package com.loresuelvo.consumer.bdd.notifications

import android.content.Context
import androidx.lifecycle.ViewModelStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.data.api.ApiUserRepository
import com.loresuelvo.consumer.data.api.AuthInterceptor
import com.loresuelvo.consumer.data.api.BackendApi
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.AuthenticationOutcome
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.CategoryRepository
import com.loresuelvo.consumer.domain.usecase.auth.SyncAuthenticatedSessionUseCase
import com.loresuelvo.consumer.domain.usecase.category.GetCategoriesUseCase
import com.loresuelvo.consumer.platform.auth.AuthProvider
import com.loresuelvo.consumer.ui.auth.WelcomeViewModel
import com.loresuelvo.consumer.ui.notifications.PushRegistrationRequests
import io.mockk.coEvery
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import retrofit2.Retrofit

/**
 * La aceptación observa el efecto HTTP real del login, no un registrador fake
 * desconectado. Auth0 y el almacenamiento se sustituyen; el flujo de sesión,
 * el ViewModel y el adaptador de perfil son los de producción.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConsumerPushNotificationsWorld {
    private val dispatcher = StandardTestDispatcher()
    private val sessionStore = InMemorySessionStore()
    private val context = mockk<Context>()
    private val authProvider = mockk<AuthProvider>()
    private val server = MockWebServer()
    private val viewModelStore = ViewModelStore()
    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionStore))
        .build()
    private lateinit var viewModel: WelcomeViewModel

    fun start() {
        Dispatchers.setMain(dispatcher)
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(
                Json { ignoreUnknownKeys = true }
                    .asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(BackendApi::class.java)
        val categories = object : CategoryRepository {
            override suspend fun getCategories() = CategoriesOutcome.Success(emptyList())
        }
        coEvery { authProvider.login(context) } returns AuthenticationOutcome.Success(
            AuthSession(User(displayName = "Unverified identity"), "dummy-access-token"),
        )
        viewModel = WelcomeViewModel(
            authProvider,
            SyncAuthenticatedSessionUseCase(ApiUserRepository(api, sessionStore), sessionStore),
            GetCategoriesUseCase(categories),
            PushRegistrationRequests(),
        )
        viewModelStore.put("welcome", viewModel)
        dispatcher.scheduler.advanceUntilIdle()
    }

    fun session(): AuthSession? = sessionStore.getSession()

    fun login() = runTest(dispatcher) {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"id":17,"name":"Verified","surname":"Consumer",
                    "email":"consumer@example.test","role":"consumer",
                    "address":{"street":"Test street","street_number":"1"}}""",
            ),
        )
        // El futuro adapter debe poder completar su llamada sin esperar una
        // respuesta inexistente. Ajustar la identidad al seam al implementarlo.
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"installation_id":"00000000-0000-4000-8000-000000000001",
                        "binding_id":"00000000-0000-4000-8000-000000000002",
                        "app":"consumer","locale":"es","enabled":true}""",
                ),
        )
        viewModel.login(context)
        runCurrent()
        viewModel.uiState.first { !it.loading }
        advanceUntilIdle()
        assertNull("Login must succeed before checking push registration", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.loading)
        assertEquals("Verified", session()?.user?.firstName)
        assertTrue("The API-confirmed consumer profile must be complete", session()!!.user.isProfileComplete())
    }

    fun assertPhoneRegistered() {
        val profileRequest = server.takeRequest(0, TimeUnit.MILLISECONDS)
        assertNotNull("Login must verify the account with the API", profileRequest)
        assertEquals("GET", profileRequest!!.method)
        assertEquals("/me", profileRequest.path)

        val registration = server.takeRequest(0, TimeUnit.MILLISECONDS)
        assertNotNull(
            "Verified login must register this phone for push notifications after /me",
            registration,
        )
        assertEquals("PUT", registration!!.method)
        assertTrue("Registration must target this installation", registration.path!!.startsWith("/installations/"))
        assertEquals("Bearer dummy-access-token", registration.getHeader("Authorization"))
    }

    fun close() {
        viewModelStore.clear()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        server.shutdown()
        Dispatchers.resetMain()
    }

    private class InMemorySessionStore : AuthSessionStore {
        override val sessionFlow = MutableStateFlow<AuthSession?>(null)
        private var persisted: AuthSession? = null

        override fun getSession() = persisted
        override fun persistSession(session: AuthSession) {
            persisted = session
        }
        override fun saveSession(session: AuthSession) {
            persistSession(session)
            sessionFlow.value = session
        }
        override fun clearSession() {
            persisted = null
            sessionFlow.value = null
        }
    }
}

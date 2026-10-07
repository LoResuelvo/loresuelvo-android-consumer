package com.loresuelvo.consumer.bdd.notifications

import android.content.Context
import android.content.SharedPreferences
import com.loresuelvo.consumer.platform.notifications.InstallationRegistrationCoordinator
import com.loresuelvo.consumer.platform.notifications.RegistrationRuntime
import javax.inject.Provider
import com.loresuelvo.consumer.platform.notifications.RegistrationLocaleProvider
import com.loresuelvo.consumer.domain.usecase.installation.RegisterInstallationUseCase
import com.loresuelvo.consumer.domain.installation.PushRegistrationTokenProvider
import com.loresuelvo.consumer.domain.installation.PushTokenOutcome
import com.loresuelvo.consumer.domain.installation.RegistrationOutcome
import com.loresuelvo.consumer.data.api.ApiInstallationRepository
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import io.mockk.every
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
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
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val requests = PushRegistrationRequests()
    private val applicationScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val installationClient = OkHttpClient.Builder().build()
    private lateinit var installationPreferences: SharedPreferences
    private lateinit var coordinator: InstallationRegistrationCoordinator
    private val viewModelStore = ViewModelStore()
    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionStore))
        .build()
    private lateinit var viewModel: WelcomeViewModel

    fun start() {
        Dispatchers.setMain(dispatcher)
        server.start()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/me") return MockResponse().setHeader("Content-Type", "application/json")
                    .setBody("""{"id":17,"name":"Verified","surname":"Consumer",
                        "email":"consumer@example.test","role":"consumer",
                        "address":{"street":"Test street","street_number":"1"}}""")
                if (request.path?.startsWith("/installations/") == true) {
                    val body = json.parseToJsonElement(request.body.clone().readUtf8()).jsonObject
                    return MockResponse().setResponseCode(201).setHeader("Content-Type", "application/json")
                        .setBody(buildJsonObject {
                            put("installation_id", request.path!!.substringAfterLast('/'))
                            put("binding_id", body["binding_id"]!!.jsonPrimitive.content)
                            put("app", body["app"]!!.jsonPrimitive.content)
                            put("locale", body["locale"]!!.jsonPrimitive.content)
                            put("enabled", true)
                        }.toString())
                }
                return MockResponse().setResponseCode(404)
            }
        }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
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
            requests,
        )
        val installationApi = Retrofit.Builder().baseUrl(server.url("/"))
            .client(installationClient).addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(BackendApi::class.java)
        installationPreferences = fakePreferences()
        coordinator = InstallationRegistrationCoordinator(
            requests, sessionStore,
            RegisterInstallationUseCase(
                object : PushRegistrationTokenProvider {
                    override suspend fun token() = PushTokenOutcome.Available("dummy-fcm-token")
                },
                EncryptedInstallationStateStore(installationPreferences), ApiInstallationRepository(installationApi),
            ),
            RegistrationLocaleProvider { "es" }, applicationScope,
        )
        RegistrationRuntime(requests, Provider { coordinator }, applicationScope).start()
        viewModelStore.put("welcome", viewModel)
        dispatcher.scheduler.advanceUntilIdle()
    }

    fun session(): AuthSession? = sessionStore.getSession()

    fun login() = runTest(dispatcher) {
        viewModel.login(context)
        runCurrent()
        viewModel.uiState.first { !it.loading }
        advanceUntilIdle()
        assertNull("Login must succeed before checking push registration", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.loading)
        assertEquals("Verified", session()?.user?.firstName)
        assertEquals(17, session()?.user?.backendUserId)
        assertTrue("The API-confirmed consumer profile must be complete", session()!!.user.isProfileComplete())
    }

    fun assertPhoneRegistered() = runTest(dispatcher) {
        val outcome = coordinator.outcome.first { it != null }
        assertTrue("Registration must finish successfully, not merely send a PUT", outcome is RegistrationOutcome.Success)
        assertTrue(installationPreferences.getBoolean("confirmed", false))
        outcome as RegistrationOutcome.Success
        assertNull(requests.pending.value)
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
        val body = json.parseToJsonElement(registration.body.readUtf8()).jsonObject
        assertEquals(setOf("installation_secret", "app", "fcm_token", "locale", "binding_id"), body.keys)
        assertEquals("consumer", body["app"]!!.jsonPrimitive.content)
        assertEquals("es", body["locale"]!!.jsonPrimitive.content)
        assertEquals("dummy-fcm-token", body["fcm_token"]!!.jsonPrimitive.content)
        for (id in listOf(registration.path!!.substringAfterLast('/'),
            body["installation_secret"]!!.jsonPrimitive.content, body["binding_id"]!!.jsonPrimitive.content)) {
            assertEquals(4, UUID.fromString(id).version())
            assertEquals(id, UUID.fromString(id).toString())
        }
        assertEquals(outcome.installationId, registration.path!!.substringAfterLast('/'))
        assertEquals(outcome.bindingId, body["binding_id"]!!.jsonPrimitive.content)
    }

    fun close() {
        applicationScope.cancel()
        viewModelStore.clear()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        installationClient.dispatcher.executorService.shutdown()
        installationClient.connectionPool.evictAll()
        server.shutdown()
        Dispatchers.resetMain()
    }

    private fun fakePreferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { prefs.getString(any(), any()) } answers { values[firstArg<String>()] as? String ?: secondArg() }
        every { prefs.getInt(any(), any()) } answers { values[firstArg<String>()] as? Int ?: secondArg() }
        every { prefs.getBoolean(any(), any()) } answers { values[firstArg<String>()] as? Boolean ?: secondArg() }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers { values[firstArg<String>()] = secondArg<String?>(); editor }
        every { editor.putInt(any(), any()) } answers { values[firstArg<String>()] = secondArg<Int>(); editor }
        every { editor.putBoolean(any(), any()) } answers { values[firstArg<String>()] = secondArg<Boolean>(); editor }
        every { editor.commit() } returns true
        return prefs
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

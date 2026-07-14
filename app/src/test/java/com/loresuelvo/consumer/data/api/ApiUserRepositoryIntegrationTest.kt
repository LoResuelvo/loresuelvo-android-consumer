package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.RegisterConsumerRequestDto
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.auth.RegisterConsumerData
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of [ApiUserRepository] against a [MockWebServer]
 * emulating the loresuelvo-api backend. Every documented status code
 * in `openapi/paths/consumers.yaml` is exercised:
 *   201: success
 *   400: bad request / invalid email
 *   401: invalid_token / missing claims
 *   409: email already registered
 *   transport failure: socket closed by peer (network drop)
 *
 * Tests verify both the outcome and the wire-level details: HTTP
 * method, path, request body shape, and the presence of the
 * `Authorization` header.
 */
class ApiUserRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private lateinit var repository: ApiUserRepository
    private lateinit var sessionStore: AuthSessionStore
    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val session = AuthSession(
            user = User(
                displayName = "Ana",
                firstName = "Ana",
                lastName = "Perez",
                email = "ana@example.com",
            ),
            accessToken = "test-token",
        )
        sessionStore = TestAuthSessionStore(session)

        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionStore))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val backendApi = retrofit.create(BackendApi::class.java)
        repository = ApiUserRepository(
            backendApi = backendApi,
            sessionStore = sessionStore,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun get_current_user_200_returns_persisted_backend_profile() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"AuthID":"auth0|123","Name":"Ana","Surname":"Perez","Email":"ana@example.com","Role":"consumer"}""",
                ),
        )

        val outcome = repository.getCurrentUser()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/me", recorded.path)
        assertEquals("Bearer test-token", recorded.getHeader("Authorization"))
        assertTrue(outcome is CurrentUserOutcome.Success)
        val user = (outcome as CurrentUserOutcome.Success).user
        assertEquals("Ana Perez", user.displayName)
        assertEquals("Ana", user.firstName)
        assertEquals("Perez", user.lastName)
        assertEquals("ana@example.com", user.email)
    }

    @Test
    fun get_current_user_404_returns_NotFound_for_new_account() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"user not found"}"""),
        )

        val outcome = repository.getCurrentUser()

        assertEquals(CurrentUserOutcome.NotFound, outcome)
    }

    @Test
    fun get_current_user_401_returns_Unauthorized() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"invalid_token","message":"Failed to validate JWT."}"""),
        )

        val outcome = repository.getCurrentUser()

        assertTrue(outcome is CurrentUserOutcome.Failure.Unauthorized)
        assertEquals(
            "Failed to validate JWT.",
            (outcome as CurrentUserOutcome.Failure.Unauthorized).message,
        )
    }

    @Test
    fun register_consumer_201_returns_Success_with_session_user() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"message":"cuenta registrada exitosamente"}"""),
        )
        val data = RegisterConsumerData(
            email = "ana@example.com",
            firstName = "Ana",
            lastName = "Perez",
        )

        val outcome = repository.registerConsumer(data)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/consumers", recorded.path)
        assertTrue(
            "Authorization header should be present",
            recorded.getHeader("Authorization") == "Bearer test-token",
        )
        val sent = json.decodeFromString(RegisterConsumerRequestDto.serializer(), recorded.body.readUtf8())
        assertEquals("ana@example.com", sent.email)
        assertEquals("Ana", sent.firstName)
        assertEquals("Perez", sent.surname)

        assertTrue("outcome must be Success", outcome is UserRegistrationOutcome.Success)
        val success = outcome as UserRegistrationOutcome.Success
        assertEquals("Ana", success.user.firstName)
        assertEquals("Perez", success.user.lastName)
        assertEquals("ana@example.com", success.user.email)
    }

    @Test
    fun register_consumer_400_returns_Server_failure_with_code_and_message() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"Invalid email format"}"""),
        )

        val outcome = repository.registerConsumer(
            RegisterConsumerData("a@x.com", "A", "B")
        )

        assertTrue(outcome is UserRegistrationOutcome.Failure)
        val failure = outcome as UserRegistrationOutcome.Failure.Server
        assertEquals(400, failure.code)
        assertEquals("Invalid email format", failure.message)
    }

    @Test
    fun register_consumer_409_returns_Server_failure_with_conflict_message() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"Email is already registered"}"""),
        )

        val outcome = repository.registerConsumer(
            RegisterConsumerData("dup@x.com", "X", "Y")
        )

        assertTrue(outcome is UserRegistrationOutcome.Failure)
        val failure = outcome as UserRegistrationOutcome.Failure.Server
        assertEquals(409, failure.code)
        assertEquals("Email is already registered", failure.message)
    }

    @Test
    fun register_consumer_401_with_message_returns_Unauthorized() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"invalid_token","message":"Failed to validate JWT."}"""),
        )

        val outcome = repository.registerConsumer(
            RegisterConsumerData("a@x.com", "A", "B")
        )

        assertTrue(outcome is UserRegistrationOutcome.Failure)
        val failure = outcome as UserRegistrationOutcome.Failure.Unauthorized
        assertEquals("Failed to validate JWT.", failure.message)
    }

    @Test
    fun register_consumer_401_without_message_falls_back_to_error_field() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"missing claims"}"""),
        )

        val outcome = repository.registerConsumer(
            RegisterConsumerData("a@x.com", "A", "B")
        )

        assertTrue(outcome is UserRegistrationOutcome.Failure)
        val failure = outcome as UserRegistrationOutcome.Failure.Unauthorized
        assertEquals("missing claims", failure.message)
    }

    @Test
    fun register_consumer_500_with_no_body_returns_Server_failure() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(""),
        )

        val outcome = repository.registerConsumer(
            RegisterConsumerData("a@x.com", "A", "B")
        )

        assertTrue(outcome is UserRegistrationOutcome.Failure)
        val failure = outcome as UserRegistrationOutcome.Failure.Server
        assertEquals(500, failure.code)
        // Body was empty; the mapping falls back to the HTTP status text.
        assertNotNull(failure.message)
    }

    @Test
    fun register_consumer_network_failure_returns_Network_failure_with_cause() = runTest {
        // DISCONNECT_AT_START makes the server close the socket before any
        // bytes are read; OkHttp surfaces this as an IOException.
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val outcome = repository.registerConsumer(
            RegisterConsumerData("a@x.com", "A", "B")
        )

        assertTrue("outcome must be Failure.Network", outcome is UserRegistrationOutcome.Failure.Network)
        val failure = outcome as UserRegistrationOutcome.Failure.Network
        assertTrue("cause must be IOException", failure.cause is IOException)
    }

    /**
     * Minimal in-memory [AuthSessionStore] for tests. The real
     * implementation reads from encrypted preferences; here we just
     * keep the session in a field.
     */
    private class TestAuthSessionStore(
        initial: AuthSession,
    ) : AuthSessionStore {
        private var current: AuthSession? = initial
        override val sessionFlow: kotlinx.coroutines.flow.StateFlow<AuthSession?>
            get() = kotlinx.coroutines.flow.MutableStateFlow(current)

        override fun getSession(): AuthSession? = current
        override fun saveSession(session: AuthSession) { current = session }
        override fun clearSession() { current = null }
    }
}

package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.workorder.WorkOrderDetailCounterpart
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiWorkOrderDetailLookupTest {
    private lateinit var server: MockWebServer
    private lateinit var useCase: GetWorkOrderDetailUseCase
    private lateinit var sessionStore: TestAuthSessionStore
    @Volatile private var sessionBeforeAuth: AuthSession? = null
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        sessionStore = TestAuthSessionStore()

        val backendApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(Interceptor { chain ->
                        sessionBeforeAuth?.let { replacement ->
                            sessionStore.saveSession(replacement)
                            sessionBeforeAuth = null
                        }
                        chain.proceed(chain.request())
                    })
                    .addInterceptor(AuthInterceptor(sessionStore))
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(2, TimeUnit.SECONDS)
                    .writeTimeout(2, TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApi::class.java)
        useCase = GetWorkOrderDetailUseCase(
            ApiWorkOrderDetailRepository(backendApi, sessionStore),
            GetTurnosUseCase(ApiTurnosRepository(backendApi, sessionStore)),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun loads_exact_current_work_order_with_provider_from_current_list() = runBlocking {
        enqueueCurrentList()
        enqueueCurrentDetail()

        val outcome = useCase("88")

        val listRequest = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("GET /work-orders must resolve the current counterpart", listRequest)
        assertEquals("GET", listRequest?.method)
        assertEquals("/work-orders", listRequest?.path)

        val detailRequest = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("the exact work order must be loaded after resolving its provider", detailRequest)
        assertEquals("GET", detailRequest?.method)
        assertEquals("/work-orders/88", detailRequest?.path)
        assertTrue("expected Found, was $outcome", outcome is GetWorkOrderOutcome.Found)

        val detail = (outcome as GetWorkOrderOutcome.Found).workOrder
        assertEquals("101", detail.proposalId)
        assertEquals("Actualizada", detail.provider.name)
        assertEquals("Prestadora", detail.provider.surname)
        assertEquals("Plomería", detail.provider.categoryName)
        assertEquals("Detalle actual del servidor", detail.description)
        assertEquals(TurnoStatus.AwaitingPayment, detail.status)
        assertEquals("Reporte actual", detail.completionReport?.description)
    }

    @Test
    fun preserves_forbidden_work_order_as_a_distinct_failure() = runBlocking {
        enqueueCurrentList()
        server.enqueue(MockResponse().setResponseCode(403))

        val outcome = useCase("88")

        val failure = (outcome as? GetWorkOrderOutcome.Failure)?.failure
        assertNotNull("A forbidden work order must return a typed failure", failure)
        assertTrue(
            failure is com.loresuelvo.consumer.domain.serviceproposal.ServiceProposalsOutcome.Failure.AccessDenied,
        )
    }

    @Test
    fun work_order_lookup_401_clears_the_current_session_after_the_protected_request() = runBlocking {
        sessionStore.saveSession(
            AuthSession(
                user = User(displayName = "Consumidora"),
                accessToken = "session-token",
            ),
        )
        server.enqueue(MockResponse().setResponseCode(401))

        val outcome = useCase(
            "88",
            WorkOrderDetailCounterpart(
                id = "21",
                name = "Carlos",
                surname = "López",
                categoryName = "Plomería",
                profilePhotoUrl = null,
            ),
        )

        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("GET", request?.method)
        assertEquals("/work-orders/88", request?.path)
        assertEquals("Bearer session-token", request?.getHeader("Authorization"))
        assertTrue(outcome is GetWorkOrderOutcome.Failure)
        assertEquals(null, sessionStore.sessionFlow.value)
    }

    @Test
    fun work_order_lookup_uses_its_captured_session_if_account_changes_before_auth_interceptor() = runBlocking {
        sessionStore.saveSession(
            AuthSession(User(displayName = "Cuenta A"), accessToken = "token-a"),
        )
        sessionBeforeAuth = AuthSession(
            User(displayName = "Cuenta B"),
            accessToken = "token-b",
        )
        server.enqueue(MockResponse().setResponseCode(401))

        val outcome = useCase(
            "88",
            WorkOrderDetailCounterpart("21", "Carlos", "López", "Plomería", null),
        )

        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertEquals("Bearer token-a", request?.getHeader("Authorization"))
        assertTrue(outcome is GetWorkOrderOutcome.Failure)
        assertEquals("token-b", sessionStore.getSession()?.accessToken)
    }

    @Test
    fun notification_work_order_lookup_without_provider_clears_session_on_list_401() = runBlocking {
        sessionStore.saveSession(
            AuthSession(User(displayName = "Consumidora"), accessToken = "session-token"),
        )
        server.enqueue(MockResponse().setResponseCode(401))

        val outcome = useCase("88")

        val request = server.takeRequest(1, TimeUnit.SECONDS)
        assertEquals("GET", request?.method)
        assertEquals("/work-orders", request?.path)
        assertEquals("Bearer session-token", request?.getHeader("Authorization"))
        assertTrue(outcome is GetWorkOrderOutcome.Failure)
        assertEquals(null, sessionStore.sessionFlow.value)
    }

    private fun enqueueCurrentList() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {
                        "id": 99,
                        "service_proposal_id": 900,
                        "amount_cents": 900000,
                        "scheduled_on": "2026-10-08T15:00:00Z",
                        "description": "Servicio señuelo",
                        "status": "scheduled",
                        "counterpart": {
                          "id": 21,
                          "role": "provider",
                          "name": "Otra",
                          "surname": "Persona",
                          "category_name": "Electricidad"
                        }
                      },
                      {
                        "id": 88,
                        "service_proposal_id": 101,
                        "amount_cents": 1800000,
                        "scheduled_on": "2026-10-09T15:00:00Z",
                        "description": "Servicio actual",
                        "status": "scheduled",
                        "counterpart": {
                          "id": 20,
                          "role": "provider",
                          "name": "Actualizada",
                          "surname": "Prestadora",
                          "category_name": "Plomería"
                        }
                      }
                    ]
                    """.trimIndent(),
                ),
        )
    }

    private fun enqueueCurrentDetail() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 88,
                      "service_proposal_id": 101,
                      "consumer_id": 17,
                      "provider_id": 20,
                      "amount_cents": 2000000,
                      "scheduled_on": "2026-10-09T15:00:00Z",
                      "description": "Detalle actual del servidor",
                      "status": "awaiting_payment",
                      "accepted_on": "2026-10-01T13:00:00Z",
                      "completion_report": {
                        "id": 45,
                        "description": "Reporte actual",
                        "reported_on": "2026-10-07T12:00:00Z",
                        "images": []
                      }
                    }
                    """.trimIndent(),
                ),
        )
    }

    private class TestAuthSessionStore : AuthSessionStore {
        private val current = MutableStateFlow<AuthSession?>(null)
        override val sessionFlow: StateFlow<AuthSession?> = current
        override fun getSession(): AuthSession? = current.value
        override fun saveSession(session: AuthSession) { current.value = session }
        override fun clearSession() { current.value = null }
    }
}

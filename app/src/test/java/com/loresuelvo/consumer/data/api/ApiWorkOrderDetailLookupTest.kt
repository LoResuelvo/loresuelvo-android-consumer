package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.turno.TurnoStatus
import com.loresuelvo.consumer.domain.usecase.turno.GetTurnosUseCase
import com.loresuelvo.consumer.domain.usecase.workorder.GetWorkOrderDetailUseCase
import com.loresuelvo.consumer.domain.workorder.GetWorkOrderOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiWorkOrderDetailLookupTest {
    private lateinit var server: MockWebServer
    private lateinit var useCase: GetWorkOrderDetailUseCase
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val backendApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.SECONDS)
                    .readTimeout(2, TimeUnit.SECONDS)
                    .writeTimeout(2, TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApi::class.java)
        useCase = GetWorkOrderDetailUseCase(
            ApiWorkOrderDetailRepository(backendApi),
            GetTurnosUseCase(ApiTurnosRepository(backendApi)),
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
}

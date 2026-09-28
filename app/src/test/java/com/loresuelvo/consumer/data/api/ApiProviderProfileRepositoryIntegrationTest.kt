package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiProviderProfileRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ApiProviderProfileRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }.asConverterFactory("application/json".toMediaType()),
            )
            .build()
        repository = ApiProviderProfileRepository(retrofit.create(BackendApi::class.java))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun get_provider_profile_200_uses_provider_id_path_and_maps_response() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 12,
                      "name": "Juan",
                      "surname": "Gómez",
                      "profile_photo": {"original_name": "foto.jpg", "url": "https://cdn.example/foto.jpg"},
                      "category": {"id": 1, "name": "Plomería"},
                      "rating_average": 4.5,
                      "rating_count": 2,
                      "identity_verified": true,
                      "work_orders": [{
                        "id": 84,
                        "scheduled_on": "2026-08-15T15:00:00Z",
                        "description": "Reparación de pérdida",
                        "status": "paid",
                        "completion_report": {"description": "Finalizado", "reported_on": "2026-08-15T16:00:00Z"},
                        "review": {"rating": 5, "description": "Excelente"}
                      }]
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.getProviderProfile(12)
        val request = server.takeRequest()

        assertEquals("GET", request.method)
        assertEquals("/providers/12", request.path)
        assertTrue(outcome is ProviderProfileOutcome.Success)
        assertEquals(12, (outcome as ProviderProfileOutcome.Success).profile.id)
        assertEquals("Juan", outcome.profile.name)
    }

    @Test
    fun get_provider_profile_404_returns_server_failure() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("{" +
            "\"message\":\"provider not found\"}"))

        val outcome = repository.getProviderProfile(99)

        assertTrue(outcome is ProviderProfileOutcome.Failure.Server)
        assertEquals(404, (outcome as ProviderProfileOutcome.Failure.Server).code)
    }

    @Test
    fun get_provider_profile_network_drop_returns_network_failure() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val outcome = repository.getProviderProfile(12)

        assertTrue(outcome is ProviderProfileOutcome.Failure.Network)
    }
}

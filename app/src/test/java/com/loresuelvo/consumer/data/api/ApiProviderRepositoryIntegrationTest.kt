package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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

/**
 * End-to-end coverage of [ApiProviderRepository] against a
 * [MockWebServer] emulating the loresuelvo-api backend. Exercised
 * scenarios:
 *   200 + multi body -> Success(2 providers)
 *   200 + empty body  -> Success(empty)
 *   500               -> Failure.Server
 *   network drop      -> Failure.Network
 *
 * Wire-level inspection verifies the path and `category_id` query
 * param on every request.
 */
class ApiProviderRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ApiProviderRepository

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

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
                json.asConverterFactory("application/json".toMediaType()),
            )
            .build()
        val backendApi = retrofit.create(BackendApi::class.java)
        repository = ApiProviderRepository(backendApi = backendApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun get_providers_200_returns_Success_with_mapped_list() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {
                        "id": 92,
                        "name": "Agustina",
                        "surname": "Molina",
                        "category_id": 2,
                        "category_name": "Electricidad",
                        "profile_photo_url": "http://x/p1.webp"
                      },
                      {
                        "id": 32,
                        "name": "Agustina",
                        "surname": "Ruiz",
                        "category_id": 2,
                        "category_name": "Electricidad",
                        "profile_photo_url": "http://x/p2.webp"
                      }
                    ]
                    """.trimIndent(),
                ),
        )

        val outcome = repository.getProvidersByCategory(2)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        // `path` includes the query string in MockWebServer 4.x.
        assertEquals("/providers?category_id=2", recorded.path)
        assertEquals("2", recorded.requestUrl!!.queryParameter("category_id"))

        assertTrue(outcome is com.loresuelvo.consumer.domain.provider.ProvidersOutcome.Success)
        val success = outcome as com.loresuelvo.consumer.domain.provider.ProvidersOutcome.Success
        assertEquals(2, success.providers.size)
        assertEquals(92, success.providers[0].id)
        assertEquals("Agustina", success.providers[0].name)
        assertEquals("Molina", success.providers[0].surname)
        assertEquals(2, success.providers[0].categoryId)
        assertEquals("Electricidad", success.providers[0].categoryName)
        assertEquals("http://x/p1.webp", success.providers[0].profilePhotoUrl)
    }

    @Test
    fun get_providers_200_empty_returns_Success_empty() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        val outcome = repository.getProvidersByCategory(99)

        assertTrue(outcome is com.loresuelvo.consumer.domain.provider.ProvidersOutcome.Success)
        assertTrue(
            (outcome as com.loresuelvo.consumer.domain.provider.ProvidersOutcome.Success).providers.isEmpty(),
        )
    }

    @Test
    fun get_providers_500_returns_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("internal error"),
        )

        val outcome = repository.getProvidersByCategory(1)

        assertTrue(outcome is com.loresuelvo.consumer.domain.provider.ProvidersOutcome.Failure.Server)
        assertEquals(500, (outcome as com.loresuelvo.consumer.domain.provider.ProvidersOutcome.Failure.Server).code)
    }

    @Test
    fun get_providers_network_drop_returns_Network_failure() = runBlocking {
        server.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val outcome = repository.getProvidersByCategory(1)

        assertTrue(
            "outcome must be Failure.Network, was $outcome",
            outcome is com.loresuelvo.consumer.domain.provider.ProvidersOutcome.Failure.Network,
        )
    }
}

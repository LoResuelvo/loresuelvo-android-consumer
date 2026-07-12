package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import kotlinx.coroutines.test.runTest
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
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of [ApiCategoryRepository] against a
 * [MockWebServer] emulating the backend's `GET /categories`:
 *   200: JSON array -> Success(list)
 *   200: empty array -> Success(empty)
 *   500: -> Failure.Server
 *   transport drop -> Failure.Network
 *
 * Verifies both the outcome and the wire-level request (method,
 * path).
 */
class ApiCategoryRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ApiCategoryRepository
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
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val backendApi = retrofit.create(BackendApi::class.java)
        repository = ApiCategoryRepository(backendApi = backendApi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun get_categories_200_returns_Success_with_mapped_list() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {"id":4,"name":"Carpintería"},
                      {"id":2,"name":"Electricidad"},
                      {"id":531,"name":"Streamer"}
                    ]
                    """.trimIndent(),
                ),
        )

        val outcome = repository.getCategories()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/categories", recorded.path)

        assertTrue("outcome must be Success", outcome is CategoriesOutcome.Success)
        val success = outcome as CategoriesOutcome.Success
        assertEquals(3, success.categories.size)
        assertEquals(listOf(4, 2, 531), success.categories.map { it.id })
        assertEquals("Carpintería", success.categories.first().name)
    }

    @Test
    fun get_categories_200_empty_array_returns_Success_empty() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"),
        )

        val outcome = repository.getCategories()

        assertTrue(outcome is CategoriesOutcome.Success)
        assertTrue((outcome as CategoriesOutcome.Success).categories.isEmpty())
    }

    @Test
    fun get_categories_500_returns_Server_failure() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(""),
        )

        val outcome = repository.getCategories()

        assertTrue(outcome is CategoriesOutcome.Failure.Server)
        assertEquals(500, (outcome as CategoriesOutcome.Failure.Server).code)
    }

    @Test
    fun get_categories_network_drop_returns_Network_failure() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val outcome = repository.getCategories()

        assertTrue("outcome must be Failure.Network", outcome is CategoriesOutcome.Failure.Network)
        assertTrue((outcome as CategoriesOutcome.Failure.Network).cause is IOException)
    }
}

package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.domain.calendar.CalendarConnectionOutcome
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiCalendarConnectionRepositoryIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ApiCalendarConnectionRepository

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val api = Retrofit.Builder()
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
        repository = ApiCalendarConnectionRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun connect_sends_server_auth_code_to_calendar_endpoint() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))

        val outcome = repository.connect("android-calendar-code")
        val request = server.takeRequest()

        assertEquals(CalendarConnectionOutcome.Success, outcome)
        assertEquals("POST", request.method)
        assertEquals("/me/calendar-connection", request.path)
        assertEquals(
            "{\"server_auth_code\":\"android-calendar-code\"}",
            request.body.readUtf8(),
        )
    }

    @Test
    fun connect_maps_unauthorized_response() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))

        val outcome = repository.connect("expired-code")

        assertTrue(outcome is CalendarConnectionOutcome.Failure.Unauthorized)
    }

    @Test
    fun connect_rejects_blank_code_without_network_call() = runBlocking {
        val outcome = repository.connect(" ")

        assertTrue(outcome is CalendarConnectionOutcome.Failure.Server)
        assertEquals(400, (outcome as CalendarConnectionOutcome.Failure.Server).code)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun connect_maps_network_failure() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val outcome = repository.connect("network-code")

        assertTrue(outcome is CalendarConnectionOutcome.Failure.Network)
    }
}

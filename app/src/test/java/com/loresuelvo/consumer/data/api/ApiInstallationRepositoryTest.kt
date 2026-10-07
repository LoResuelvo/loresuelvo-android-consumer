package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.installation.InstallationIdentity
import com.loresuelvo.consumer.domain.installation.InstallationRegistrationResult
import com.loresuelvo.consumer.domain.installation.RegistrationOutcome
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class ApiInstallationRepositoryTest {
    private val server = MockWebServer()
    private val json = Json { explicitNulls = false }
    private val binding = InstallationBinding(
        InstallationIdentity("c80869b4-6a8a-4092-bd7b-3c6993437ab0", "4372a7ae-54a5-4f05-b1b0-774f4c66d96a"),
        "7c6dbaca-9042-4b1e-9111-41fd1af965b7", null, 17, "attempt",
    )
    private val client = OkHttpClient.Builder().readTimeout(100, TimeUnit.MILLISECONDS).build()
    private lateinit var repository: ApiInstallationRepository

    @Before fun start() {
        server.start()
        repository = ApiInstallationRepository(Retrofit.Builder().baseUrl(server.url("/"))
            .client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(BackendApi::class.java))
    }

    @After fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        server.shutdown()
    }

    @Test fun registers_with_captured_jwt_and_omits_previous_binding_on_first_registration() = runBlocking {
        for (status in listOf(201, 200)) {
            server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
                .setBody("""{"installation_id":"${binding.identity.id}","binding_id":"${binding.id}","app":"consumer","locale":"en","enabled":true}"""))
            val result = repository.register(binding, "dummy-fcm", "en", "captured-account-jwt")
            assertTrue(result is InstallationRegistrationResult.Confirmed)
            assertTrue((result as InstallationRegistrationResult.Confirmed).confirmation.enabled)
            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/installations/${binding.identity.id}", request.path)
            assertEquals("Bearer captured-account-jwt", request.getHeader("Authorization"))
            val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals(setOf("installation_secret", "app", "fcm_token", "locale", "binding_id"), body.keys)
            assertEquals(binding.identity.secret, body["installation_secret"]!!.jsonPrimitive.content)
            assertEquals(binding.id, body["binding_id"]!!.jsonPrimitive.content)
            assertEquals("consumer", body["app"]!!.jsonPrimitive.content)
            assertEquals("dummy-fcm", body["fcm_token"]!!.jsonPrimitive.content)
            assertEquals("en", body["locale"]!!.jsonPrimitive.content)
            assertFalse(body.containsKey("previous_binding_id"))
        }
    }

    @Test fun returns_typed_http_errors_without_retries() = runBlocking {
        for ((status, expected) in listOf(
            400 to RegistrationOutcome.BadRequest, 401 to RegistrationOutcome.Unauthorized,
            403 to RegistrationOutcome.Forbidden, 409 to RegistrationOutcome.Conflict,
            500 to RegistrationOutcome.ServerFailure(500), 503 to RegistrationOutcome.ServerFailure(503),
        )) {
            server.enqueue(MockResponse().setResponseCode(status))
            assertEquals(InstallationRegistrationResult.Failed(expected), repository.register(binding, "token", "es", "jwt"))
            assertEquals("Bearer jwt", server.takeRequest().getHeader("Authorization"))
        }
        assertEquals(6, server.requestCount)
    }

    @Test fun incomplete_confirmation_is_invalid_instead_of_success() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"enabled":true}"""))
        assertEquals(InstallationRegistrationResult.Failed(RegistrationOutcome.InvalidConfirmation), repository.register(binding, "token", "es", "jwt"))
    }

    @Test fun response_timeout_is_a_network_failure() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val result = repository.register(binding, "token", "es", "jwt")
        assertTrue(result is InstallationRegistrationResult.Failed)
        assertTrue((result as InstallationRegistrationResult.Failed).outcome is RegistrationOutcome.NetworkFailure)
    }

    @Test fun unrecognized_success_status_does_not_confirm_registration() = runBlocking {
        for (status in listOf(202, 204)) {
            val response = MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
            if (status == 202) response.setBody("""{"installation_id":"${binding.identity.id}","binding_id":"${binding.id}","app":"consumer","locale":"es","enabled":true}""")
            server.enqueue(response)
            assertEquals(InstallationRegistrationResult.Failed(RegistrationOutcome.InvalidConfirmation), repository.register(binding, "token", "es", "jwt"))
        }
    }
}

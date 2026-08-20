package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.data.api.upload.FileUploader
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import kotlinx.coroutines.runBlocking
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
 * End-to-end coverage of [ApiFileRepository] against a
 * [MockWebServer] emulating the backend's presign / confirm
 * endpoints plus a stub [FileUploader] for the storage PUT.
 *
 * Pins:
 *  - `POST /files/presign` body shape (`original_name`,
 *    `mime_type`, `size_bytes`, `purpose` snake_case) — the
 *    backend validates `purpose` against its `UploadPolicy`
 *    table, so the wire constant matters.
 *  - Response mapping to `PresignUploadOutcome.Success` /
 *    `Failure.Network / Server / Unauthorized`.
 *  - `POST /files/{fileID}/confirm` body echoes the presign
 *    response (`key`, `mime_type`, `size_bytes`).
 *  - Response mapping including the nested `audio.{codec,
 *    duration_seconds}` block (scenario 03-MM).
 */
class ApiFileRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ApiFileRepository
    private val stubUploader = RecordingFileUploader()
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
        repository = ApiFileRepository(
            backendApi = backendApi,
            fileUploader = stubUploader,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun presign_posts_snake_case_payload_and_maps_response() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "file_id": "4af47f1b-97b6-4b32-baa0-b95d6077f919",
                      "key": "files/2026/08/conversation_message_audio/4af47f1b.webm",
                      "upload_url": "https://storage.example/upload-url",
                      "headers": {
                        "Content-Type": "audio/webm"
                      }
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.presign(
            PresignUploadRequest(
                originalName = "nota-voz.webm",
                mimeType = "audio/webm",
                sizeBytes = 5242880,
                purpose = FilePurpose.CONVERSATION_MESSAGE_AUDIO,
            ),
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/files/presign", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(
            "body must carry snake_case keys, was '$body'",
            body.contains("\"original_name\":\"nota-voz.webm\"") &&
                body.contains("\"mime_type\":\"audio/webm\"") &&
                body.contains("\"size_bytes\":5242880") &&
                body.contains("\"purpose\":\"conversation_message_audio\""),
        )

        val success = outcome as? PresignUploadOutcome.Success
        assertNotNull("expected Success, was $outcome", success)
        assertEquals(
            "4af47f1b-97b6-4b32-baa0-b95d6077f919",
            success!!.result.fileId,
        )
        assertEquals(
            "files/2026/08/conversation_message_audio/4af47f1b.webm",
            success.result.key,
        )
        assertEquals("https://storage.example/upload-url", success.result.uploadUrl)
        assertEquals("audio/webm", success.result.headers["Content-Type"])
    }

    @Test
    fun presign_400_maps_to_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"Unsupported conversation audio","message":"audio > 5MB"}""",
                ),
        )

        val outcome = repository.presign(
            PresignUploadRequest(
                originalName = "huge.webm",
                mimeType = "audio/webm",
                sizeBytes = 10_000_000,
                purpose = FilePurpose.CONVERSATION_MESSAGE_AUDIO,
            ),
        )

        val failure = outcome as? PresignUploadOutcome.Failure.Server
        assertNotNull("expected Server failure, was $outcome", failure)
        assertEquals(400, failure!!.code)
        assertEquals("audio > 5MB", failure.message)
    }

    @Test
    fun presign_401_maps_to_Unauthorized_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"invalid_token"}"""),
        )

        val outcome = repository.presign(
            PresignUploadRequest(
                originalName = "x.webm",
                mimeType = "audio/webm",
                sizeBytes = 1024,
                purpose = FilePurpose.CONVERSATION_MESSAGE_AUDIO,
            ),
        )

        val failure = outcome as? PresignUploadOutcome.Failure.Unauthorized
        assertNotNull("expected Unauthorized failure, was $outcome", failure)
    }

    @Test
    fun presign_transport_drop_maps_to_Network_failure() = runBlocking {
        server.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val outcome = repository.presign(
            PresignUploadRequest(
                originalName = "x.webm",
                mimeType = "audio/webm",
                sizeBytes = 1024,
                purpose = FilePurpose.CONVERSATION_MESSAGE_AUDIO,
            ),
        )

        val failure = outcome as? PresignUploadOutcome.Failure.Network
        assertNotNull("expected Network failure, was $outcome", failure)
    }

    @Test
    fun confirm_posts_snake_case_payload_and_maps_audio_metadata() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": "4af47f1b-97b6-4b32-baa0-b95d6077f919",
                      "original_name": "nota-voz.webm",
                      "mime_type": "audio/webm",
                      "type": "audio",
                      "audio": {
                        "codec": "opus",
                        "duration_seconds": 6
                      }
                    }
                    """.trimIndent(),
                ),
        )

        val outcome = repository.confirm(
            fileId = "4af47f1b-97b6-4b32-baa0-b95d6077f919",
            request = ConfirmUploadRequest(
                key = "files/2026/08/conversation_message_audio/4af47f1b.webm",
                mimeType = "audio/webm",
                sizeBytes = 5_242_880,
            ),
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals(
            "/files/4af47f1b-97b6-4b32-baa0-b95d6077f919/confirm",
            recorded.path,
        )
        val body = recorded.body.readUtf8()
        assertTrue(
            "body must carry key/mime_type/size_bytes, was '$body'",
            body.contains(
                "\"key\":\"files/2026/08/conversation_message_audio/4af47f1b.webm\"",
            ) &&
                body.contains("\"mime_type\":\"audio/webm\"") &&
                body.contains("\"size_bytes\":5242880"),
        )

        val success = outcome as? ConfirmUploadOutcome.Success
        assertNotNull("expected Success, was $outcome", success)
        assertEquals(
            "4af47f1b-97b6-4b32-baa0-b95d6077f919",
            success!!.file.id,
        )
        assertEquals("audio/webm", success.file.mimeType)
        assertEquals("nota-voz.webm", success.file.originalName)
        assertEquals("opus", success.file.codec)
        assertEquals(6, success.file.durationSeconds)
    }

    @Test
    fun confirm_400_maps_to_Server_failure() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"Unsupported conversation audio","message":"codec mismatch"}""",
                ),
        )

        val outcome = repository.confirm(
            fileId = "4af47f1b",
            request = ConfirmUploadRequest(
                key = "files/2026/08/conversation_message_audio/4af47f1b.webm",
                mimeType = "audio/webm",
                sizeBytes = 1024,
            ),
        )

        val failure = outcome as? ConfirmUploadOutcome.Failure.Server
        assertNotNull("expected Server failure, was $outcome", failure)
        assertEquals(400, failure!!.code)
        assertEquals("codec mismatch", failure.message)
    }

    @Test
    fun uploadBytes_delegates_to_FileUploader_with_url_headers_and_bytes() = runBlocking {
        stubUploader.nextOutcome = UploadBytesOutcome.Success

        val outcome = repository.uploadBytes(
            uploadUrl = "https://storage.example/upload",
            headers = mapOf("Content-Type" to "audio/webm"),
            bytes = byteArrayOf(0x01, 0x02, 0x03),
        )

        assertEquals(UploadBytesOutcome.Success, outcome)
        assertEquals(1, stubUploader.calls.size)
        val call = stubUploader.calls.single()
        assertEquals("https://storage.example/upload", call.uploadUrl)
        assertEquals("audio/webm", call.headers["Content-Type"])
        assertTrue(call.bytes.contentEquals(byteArrayOf(0x01, 0x02, 0x03)))
    }

    @Test
    fun uploadBytes_propagates_FileUploader_failure() = runBlocking {
        val cause = IOException("connection reset")
        stubUploader.nextOutcome = UploadBytesOutcome.Failure.Network(cause)

        val outcome = repository.uploadBytes(
            uploadUrl = "https://storage.example/upload",
            headers = mapOf("Content-Type" to "audio/webm"),
            bytes = byteArrayOf(0x01),
        )

        val failure = outcome as? UploadBytesOutcome.Failure.Network
        assertNotNull("expected Network failure, was $outcome", failure)
        assertEquals(cause, failure!!.cause)
    }

    private class RecordingFileUploader : FileUploader {
        data class Call(
            val uploadUrl: String,
            val headers: Map<String, String>,
            val bytes: ByteArray,
        )
        val calls = mutableListOf<Call>()
        var nextOutcome: UploadBytesOutcome = UploadBytesOutcome.Success

        override suspend fun upload(
            uploadUrl: String,
            headers: Map<String, String>,
            bytes: ByteArray,
        ): UploadBytesOutcome {
            calls.add(Call(uploadUrl, headers, bytes))
            return nextOutcome
        }
    }
}
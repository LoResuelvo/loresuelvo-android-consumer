package com.loresuelvo.consumer.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.loresuelvo.consumer.data.api.upload.FileUploader
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.SendMessageOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
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

/**
 * End-to-end coverage of the consumer ↔ provider chat audio
 * upload (scenario 03-MM). Drives `ApiConversationRepository
 * .sendMediaMessage(audio)` against two `MockWebServer`s:
 *
 *  - the **backend** server (presign / confirm /
 *    `POST /conversations/{id}/messages`)
 *  - the **storage** server (the pre-signed `PUT` the
 *    presign response points at)
 *
 * Pins the wire contract documented in
 * `openapi/paths/files-presign.yaml`,
 * `openapi/paths/file-confirm.yaml` and
 * `openapi/paths/conversation-messages.yaml`, plus the Go
 * backend's reference flow in
 * `features/steps/send_audio_test.go` `consumerSentAudioInActiveChat`.
 *
 * Critical invariants:
 *  - presign body carries `purpose = "conversation_message_audio"`.
 *  - The upload URL + headers from the presign response go
 *    verbatim to the storage `PUT` (no Auth0 token).
 *  - The confirmed file id round-trips through the JSON
 *    `audio_file_id` of the final `POST /messages`.
 *  - `content` is empty on the final message (audio is
 *    exclusive — backend's
 *    `internal/domain/conversation/service.go:113-115`).
 */
class AudioMessageIntegrationTest {

    private lateinit var backend: MockWebServer
    private lateinit var storage: MockWebServer
    private lateinit var repository: ApiConversationRepository
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        backend = MockWebServer()
        storage = MockWebServer()
        backend.start()
        storage.start()

        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(backend.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val backendApi = retrofit.create(BackendApi::class.java)
        val fileRepository = ApiFileRepository(
            backendApi = backendApi,
            fileUploader = StorageBackedFileUploader(
                client = client,
                storageUrl = storage.url("/"),
            ),
        )
        repository = ApiConversationRepository(
            backendApi = backendApi,
            fileRepository = fileRepository,
        )
    }

    @After
    fun tearDown() {
        backend.shutdown()
        storage.shutdown()
    }

    @Test
    fun audio_happy_path_runs_presign_upload_confirm_postMessage_in_order() = runBlocking {
        // 1) presign returns a pre-signed URL pointing at the
        //    storage server.
        backend.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "file_id": "4af47f1b-97b6-4b32-baa0-b95d6077f919",
                      "key": "files/2026/08/conversation_message_audio/4af47f1b.webm",
                      "upload_url": "${storage.url("/upload/bucket/key")}",
                      "headers": {
                        "Content-Type": "audio/webm"
                      }
                    }
                    """.trimIndent(),
                ),
        )
        // 2) storage accepts the bytes.
        storage.enqueue(MockResponse().setResponseCode(200))
        // 3) confirm returns the validated audio metadata.
        backend.enqueue(
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
        // 4) postMessage returns the persisted bubble with the
        //    private audio URL.
        backend.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 99,
                      "sender_role": "consumer",
                      "content": "",
                      "created_on": "2026-08-20T10:00:00Z",
                      "audio": {
                        "id": "4af47f1b-97b6-4b32-baa0-b95d6077f919",
                        "url": "https://storage.example/private/audio.webm?signature=...",
                        "original_name": "nota-voz.webm",
                        "mime_type": "audio/webm",
                        "codec": "opus",
                        "duration_seconds": 6
                      }
                    }
                    """.trimIndent(),
                ),
        )

        val audioBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val outcome = repository.sendMediaMessage(
            conversationId = "1",
            media = MediaUpload.Audio(
                bytes = audioBytes,
                mimeType = "audio/webm",
                originalName = "nota-voz.webm",
                durationMillis = 5_420L,
            ),
        )

        val success = outcome as? SendMessageOutcome.Success
        assertNotNull("expected Success, was $outcome", success)
        val message = success!!.message
        assertEquals("99", message.id)
        assertEquals("", message.content)
        val media = message.media
        assertTrue(
            "expected MediaReference.Audio, was $media",
            media is MediaReference.Audio,
        )
        val audio = media as MediaReference.Audio
        assertEquals(
            "4af47f1b-97b6-4b32-baa0-b95d6077f919",
            audio.id,
        )
        assertEquals("audio/webm", audio.mimeType)
        assertEquals("nota-voz.webm", audio.originalName)
        assertEquals(
            "duration must come from the backend's validated seconds, " +
                "not the local MediaUpload.Audio.durationMillis (server is " +
                "source of truth — the recorder already round-trips for " +
                "the preview). 6 s × 1000 = 6000 ms.",
            6_000L,
            audio.durationMillis,
        )
        assertEquals(
            "https://storage.example/private/audio.webm?signature=...",
            audio.url,
        )

        // presign request
        val presignRecorded = backend.takeRequest()
        assertEquals("POST", presignRecorded.method)
        assertEquals("/files/presign", presignRecorded.path)
        val presignBody = presignRecorded.body.readUtf8()
        assertTrue(
            "presign must carry conversation_message_audio, was '$presignBody'",
            presignBody.contains("\"purpose\":\"conversation_message_audio\"") &&
                presignBody.contains("\"original_name\":\"nota-voz.webm\"") &&
                presignBody.contains("\"mime_type\":\"audio/webm\"") &&
                presignBody.contains("\"size_bytes\":4"),
        )

        // storage upload PUT
        val uploadRecorded = storage.takeRequest()
        assertEquals("PUT", uploadRecorded.method)
        assertEquals("/upload/bucket/key", uploadRecorded.path)
        assertEquals("audio/webm", uploadRecorded.getHeader("Content-Type"))
        assertEquals(audioBytes.size.toLong(), uploadRecorded.bodySize)

        // confirm request
        val confirmRecorded = backend.takeRequest()
        assertEquals("POST", confirmRecorded.method)
        assertEquals(
            "/files/4af47f1b-97b6-4b32-baa0-b95d6077f919/confirm",
            confirmRecorded.path,
        )
        val confirmBody = confirmRecorded.body.readUtf8()
        assertTrue(
            "confirm body must echo key/mime/size, was '$confirmBody'",
            confirmBody.contains(
                "\"key\":\"files/2026/08/conversation_message_audio/4af47f1b.webm\"",
            ) &&
                confirmBody.contains("\"mime_type\":\"audio/webm\"") &&
                confirmBody.contains("\"size_bytes\":4"),
        )

        // postMessage request (audio_file_id, empty content)
        val postRecorded = backend.takeRequest()
        assertEquals("POST", postRecorded.method)
        assertEquals("/conversations/1/messages", postRecorded.path)
        val postBody = postRecorded.body.readUtf8()
        assertTrue(
            "postMessage must carry audio_file_id with empty content, was '$postBody'",
            postBody.contains(
                "\"audio_file_id\":\"4af47f1b-97b6-4b32-baa0-b95d6077f919\"",
            ) &&
                postBody.contains("\"content\":\"\""),
        )
        // No multipart on the wire.
        val contentType = postRecorded.getHeader("Content-Type") ?: ""
        assertTrue(
            "postMessage must NOT be multipart, was '$contentType'",
            !contentType.startsWith("multipart/form-data"),
        )
    }

    @Test
    fun audio_storage_5xx_surfaces_Server_failure_with_no_postMessage_call() = runBlocking {
        backend.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "file_id": "4af47f1b",
                      "key": "files/2026/08/conversation_message_audio/4af47f1b.webm",
                      "upload_url": "${storage.url("/upload/bucket/key")}",
                      "headers": { "Content-Type": "audio/webm" }
                    }
                    """.trimIndent(),
                ),
        )
        storage.enqueue(
            MockResponse().setResponseCode(500).setBody("Storage unavailable"),
        )

        val outcome = repository.sendMediaMessage(
            conversationId = "1",
            media = MediaUpload.Audio(
                bytes = byteArrayOf(0x01),
                mimeType = "audio/webm",
                originalName = "nota-voz.webm",
                durationMillis = 0L,
            ),
        )

        val failure = outcome as? SendMessageOutcome.Failure.Server
        assertNotNull("expected Server failure, was $outcome", failure)
        assertEquals(500, failure!!.code)
        // Only the presign round-trip happened; confirm and
        // postMessage must not be called.
        assertEquals(1, backend.requestCount)
        assertEquals(1, storage.requestCount)
    }

    @Test
    fun audio_presign_failure_skips_upload_and_postMessage() = runBlocking {
        backend.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"Unsupported conversation audio","message":"size > 5MB"}""",
                ),
        )

        val outcome = repository.sendMediaMessage(
            conversationId = "1",
            media = MediaUpload.Audio(
                bytes = byteArrayOf(0x01),
                mimeType = "audio/webm",
                originalName = "huge.webm",
                durationMillis = 0L,
            ),
        )

        val failure = outcome as? SendMessageOutcome.Failure.Server
        assertNotNull("expected Server failure, was $outcome", failure)
        assertEquals(400, failure!!.code)
        assertEquals("size > 5MB", failure.message)
        assertEquals(0, storage.requestCount)
        assertEquals(1, backend.requestCount)
    }

    @Test
    fun audio_confirm_failure_skips_postMessage() = runBlocking {
        backend.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "file_id": "4af47f1b",
                      "key": "files/2026/08/conversation_message_audio/4af47f1b.webm",
                      "upload_url": "${storage.url("/upload/bucket/key")}",
                      "headers": { "Content-Type": "audio/webm" }
                    }
                    """.trimIndent(),
                ),
        )
        storage.enqueue(MockResponse().setResponseCode(200))
        backend.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"error":"Unsupported conversation audio","message":"codec mismatch"}""",
                ),
        )

        val outcome = repository.sendMediaMessage(
            conversationId = "1",
            media = MediaUpload.Audio(
                bytes = byteArrayOf(0x01),
                mimeType = "audio/webm",
                originalName = "bad-codec.webm",
                durationMillis = 0L,
            ),
        )

        val failure = outcome as? SendMessageOutcome.Failure.Server
        assertNotNull("expected Server failure, was $outcome", failure)
        assertEquals(400, failure!!.code)
        // presign + storage upload + confirm all happened;
        // postMessage must not.
        assertEquals(2, backend.requestCount)
        assertEquals(1, storage.requestCount)
    }

    /**
     * Production-shape uploader that points at the storage
     * MockWebServer (mirrors what
     * `NetworkModule.provideUploadOkHttpClient` produces in
     * production).
     */
    private class StorageBackedFileUploader(
        private val client: OkHttpClient,
        private val storageUrl: okhttp3.HttpUrl,
    ) : FileUploader {
        override suspend fun upload(
            uploadUrl: String,
            headers: Map<String, String>,
            bytes: ByteArray,
        ): com.loresuelvo.consumer.domain.file.UploadBytesOutcome {
            // Guard: the upload URL must point at the storage
            // server, not the backend. This is the production
            // invariant that protects against a regression
            // where the presign response is mistakenly pointed
            // back at the backend.
            require(uploadUrl.startsWith(storageUrl.toString())) {
                "uploadUrl must be on the storage host, was '$uploadUrl'"
            }
            return com.loresuelvo.consumer.data.api.upload.OkHttpFileUploader(client)
                .upload(uploadUrl, headers, bytes)
        }
    }
}
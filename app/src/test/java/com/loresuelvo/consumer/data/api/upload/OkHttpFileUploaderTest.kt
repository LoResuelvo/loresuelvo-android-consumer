package com.loresuelvo.consumer.data.api.upload

import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins the contract of [OkHttpFileUploader] — the data-layer
 * adapter that `PUT`s the pre-signed storage URL the backend
 * returned from `POST /files/presign`.
 *
 * Critical invariants this test pins:
 *  - The `Content-Type` the caller passes in `headers` ends up
 *    on the outbound `PUT` as the request body media type (the
 *    storage signature includes it; stripping it fails the
 *    upload with `403 SignatureDoesNotMatch`).
 *  - Every entry in `headers` is applied verbatim, not
 *    filtered or rewritten. Storage signatures bind the
 *    header set, so adding or removing a header invalidates
 *    them.
 *  - A 2xx response maps to `UploadBytesOutcome.Success`.
 *  - A non-2xx response maps to `Server(code, message)`.
 *  - A transport drop maps to `Network(cause)`.
 */
class OkHttpFileUploaderTest {

    private lateinit var server: MockWebServer
    private lateinit var uploader: OkHttpFileUploader

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        // Dedicated OkHttpClient without any interceptors —
        // mirrors the production `NetworkModule.provideUploadOkHttpClient`
        // shape (no `AuthInterceptor`, no retry).
        val client = OkHttpClient.Builder()
            .build()
        uploader = OkHttpFileUploader(client)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun upload_2xx_returns_Success() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200),
        )

        val outcome = uploader.upload(
            uploadUrl = server.url("/upload/bucket/key").toString(),
            headers = mapOf("Content-Type" to "audio/webm"),
            bytes = byteArrayOf(0x01, 0x02, 0x03),
        )

        assertEquals(UploadBytesOutcome.Success, outcome)
        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("/upload/bucket/key", recorded.path)
        assertEquals("audio/webm", recorded.getHeader("Content-Type"))
        assertEquals(3, recorded.bodySize)
    }

    @Test
    fun upload_403_returns_Server_with_status_code() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("SignatureDoesNotMatch"),
        )

        val outcome = uploader.upload(
            uploadUrl = server.url("/upload/bucket/key").toString(),
            headers = mapOf("Content-Type" to "audio/webm"),
            bytes = byteArrayOf(0x01),
        )

        val failure = outcome as? UploadBytesOutcome.Failure.Server
        assertTrue("expected Server failure, was $outcome", failure != null)
        assertEquals(403, failure!!.code)
        assertTrue(
            "message must surface the storage error, was '${failure.message}'",
            failure.message.isNotBlank(),
        )
    }

    @Test
    fun upload_transport_drop_returns_Network_failure() = runBlocking {
        server.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
        )

        val outcome = uploader.upload(
            uploadUrl = server.url("/upload/bucket/key").toString(),
            headers = mapOf("Content-Type" to "audio/webm"),
            bytes = byteArrayOf(0x01),
        )

        val failure = outcome as? UploadBytesOutcome.Failure.Network
        assertTrue("expected Network failure, was $outcome", failure != null)
    }

    @Test
    fun upload_applies_every_header_verbatim() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        uploader.upload(
            uploadUrl = server.url("/upload/bucket/key").toString(),
            headers = mapOf(
                "Content-Type" to "audio/webm",
                "x-amz-meta-uploader" to "android-consumer",
            ),
            bytes = byteArrayOf(0x01),
        )

        val recorded = server.takeRequest()
        assertEquals("audio/webm", recorded.getHeader("Content-Type"))
        assertEquals("android-consumer", recorded.getHeader("x-amz-meta-uploader"))
    }

    @Test
    fun upload_omits_Content_Type_when_header_map_is_empty() = runBlocking {
        // The storage signature may include Content-Type itself;
        // if the caller doesn't supply one we send the OkHttp
        // default (no `Content-Type` header). This test pins that
        // behaviour so a future "always default to webm" change
        // surfaces here rather than as a 403 in production.
        server.enqueue(MockResponse().setResponseCode(200))

        uploader.upload(
            uploadUrl = server.url("/upload/bucket/key").toString(),
            headers = emptyMap(),
            bytes = byteArrayOf(0x01),
        )

        val recorded = server.takeRequest()
        assertEquals(null, recorded.getHeader("Content-Type"))
    }
}
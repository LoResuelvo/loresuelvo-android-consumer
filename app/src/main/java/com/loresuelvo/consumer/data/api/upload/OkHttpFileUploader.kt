package com.loresuelvo.consumer.data.api.upload

import android.util.Log
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OkHttp-backed [FileUploader]. Uses a dedicated `OkHttpClient`
 * provided by `NetworkModule` that does NOT carry the
 * `AuthInterceptor` — pre-signed storage URLs are authenticated
 * by the storage signature in the URL itself, and any
 * `Authorization: Bearer …` header would be rejected (or, worse,
 * would invalidate the signature when the storage adapter
 * hashes the request headers).
 *
 * Threading: every blocking OkHttp call runs on
 * [Dispatchers.IO] via [withContext], mirroring the dispatch
 * pattern used by `AndroidMediaReader.read`. The caller (the
 * `FileRepository` adapter) sits on the caller's dispatcher
 * and never blocks the main thread.
 *
 * The request body is built with the raw bytes via
 * `ByteArray.toRequestBody(...)` using the `Content-Type` from
 * the [headers] map when present (storage adapters require the
 * exact type the presign signature was computed for). If
 * `Content-Type` is absent the OkHttp default
 * `application/octet-stream` is used; the backend's storage
 * adapter is configured to accept that fallback.
 */
@Singleton
class OkHttpFileUploader @Inject constructor(
    private val client: OkHttpClient,
) : FileUploader {

    override suspend fun upload(
        uploadUrl: String,
        headers: Map<String, String>,
        bytes: ByteArray,
    ): UploadBytesOutcome = withContext(Dispatchers.IO) {
        try {
            val mediaType = headers[HEADER_CONTENT_TYPE]
                ?.toMediaTypeOrNull()
            val requestBody = bytes.toRequestBody(
                contentType = mediaType,
                offset = 0,
                byteCount = bytes.size,
            )
            val requestBuilder = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
            // Apply every header verbatim. Iterating a Map
            // preserves insertion order; OkHttp iterates the
            // builder the same way, so the headers hit the wire
            // in the order the backend issued them — order
            // doesn't matter for storage signatures but it
            // keeps the test traces predictable.
            headers.forEach { (name, value) ->
                if (name.isNotBlank()) {
                    requestBuilder.header(name, value)
                }
            }
            val response = client.newCall(requestBuilder.build()).execute()
            response.use {
                if (response.isSuccessful) {
                    UploadBytesOutcome.Success
                } else {
                    Log.w(
                        TAG,
                        "storage PUT failed: code=${response.code} " +
                            "message='${response.message}' url=$uploadUrl",
                    )
                    UploadBytesOutcome.Failure.Server(
                        code = response.code,
                        message = response.message.ifBlank { "Upload failed" },
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "storage PUT transport error: ${t::class.simpleName}: ${t.message}", t)
            UploadBytesOutcome.Failure.Network(t)
        }
    }

    private companion object {
        const val HEADER_CONTENT_TYPE: String = "Content-Type"
        const val TAG: String = "OkHttpFileUploader"
    }
}
package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.data.api.mapper.toDto
import com.loresuelvo.consumer.data.api.upload.FileUploader
import com.loresuelvo.consumer.domain.api.ApiError
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [FileRepository] against the
 * backend's presign / confirm endpoints, plus the
 * [FileUploader] for the in-between storage PUT.
 *
 *  - [presign] calls `POST /files/presign` via Retrofit
 *    (carries the Auth0 bearer through `AuthInterceptor`).
 *  - [uploadBytes] delegates to [FileUploader] with a raw
 *    `OkHttpClient` that does NOT add the bearer — the
 *    storage signature is the auth.
 *  - [confirm] calls `POST /files/{fileID}/confirm` via
 *    Retrofit (back on the authenticated client).
 *
 * Like every other adapter in this package, none of the three
 * methods throws on HTTP / network failures: every exception
 * is mapped to a typed `*Outcome.Failure` via [toApiError] so
 * the orchestrating use case (and the ViewModel) handles each
 * branch explicitly. The 401 branch maps to each outcome's
 * dedicated `Unauthorized` subtype so the auth flow can clear
 * the local session when the JWT expires.
 */
@Singleton
class ApiFileRepository @Inject constructor(
    private val backendApi: BackendApi,
    private val fileUploader: FileUploader,
) : FileRepository {

    override suspend fun presign(
        request: PresignUploadRequest,
    ): PresignUploadOutcome = try {
        val dto = backendApi.presignFile(request.toDto())
        PresignUploadOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapPresignFailure(t)
    }

    override suspend fun uploadBytes(
        uploadUrl: String,
        headers: Map<String, String>,
        bytes: ByteArray,
    ): UploadBytesOutcome = fileUploader.upload(
        uploadUrl = uploadUrl,
        headers = headers,
        bytes = bytes,
    )

    override suspend fun confirm(
        fileId: String,
        request: ConfirmUploadRequest,
    ): ConfirmUploadOutcome = try {
        val dto = backendApi.confirmFile(fileId, request.toDto())
        ConfirmUploadOutcome.Success(dto.toDomain())
    } catch (t: Throwable) {
        mapConfirmFailure(t)
    }

    private fun mapPresignFailure(
        t: Throwable,
    ): PresignUploadOutcome.Failure = when (val error = t.toApiError()) {
        is ApiError.Network ->
            PresignUploadOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            PresignUploadOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            PresignUploadOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            PresignUploadOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }

    private fun mapConfirmFailure(
        t: Throwable,
    ): ConfirmUploadOutcome.Failure = when (val error = t.toApiError()) {
        is ApiError.Network ->
            ConfirmUploadOutcome.Failure.Network(error.networkCause)
        is ApiError.Unauthorized ->
            ConfirmUploadOutcome.Failure.Unauthorized(error.errorMessage)
        is ApiError.Server ->
            ConfirmUploadOutcome.Failure.Server(error.code, error.errorMessage)
        is ApiError.Unknown ->
            ConfirmUploadOutcome.Failure.Server(0, error.message ?: "Unknown error")
    }
}
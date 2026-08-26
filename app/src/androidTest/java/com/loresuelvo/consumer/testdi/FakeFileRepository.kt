package com.loresuelvo.consumer.testdi

import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeFileRepository @Inject constructor() : FileRepository {

    override suspend fun presign(
        request: PresignUploadRequest,
    ): PresignUploadOutcome =
        PresignUploadOutcome.Failure.Server(
            code = 500,
            message = "FakeFileRepository: not implemented for acceptance tests",
        )

    override suspend fun uploadBytes(
        uploadUrl: String,
        headers: Map<String, String>,
        bytes: ByteArray,
    ): UploadBytesOutcome =
        UploadBytesOutcome.Failure.Server(
            code = 500,
            message = "FakeFileRepository: not implemented for acceptance tests",
        )

    override suspend fun confirm(
        fileId: String,
        request: ConfirmUploadRequest,
    ): ConfirmUploadOutcome =
        ConfirmUploadOutcome.Failure.Server(
            code = 500,
            message = "FakeFileRepository: not implemented for acceptance tests",
        )
}
package com.loresuelvo.consumer.domain.usecase.auth

import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.auth.UploadProfilePhotoOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmedFile
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadResult
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadProfilePhotoUseCaseTest {

    private val fileRepository = mockk<FileRepository>()
    private val useCase = UploadProfilePhotoUseCase(fileRepository)
    private val photo = MediaUpload.Image(
        bytes = byteArrayOf(1, 2, 3),
        mimeType = "image/webp",
        originalName = "avatar.webp",
    )

    @Test
    fun uploads_profile_photo_and_returns_confirmed_id() = runTest {
        val target = PresignUploadResult(
            fileId = "pending-1",
            key = "profile/avatar.webp",
            uploadUrl = "https://storage.test/avatar.webp",
            headers = mapOf("Content-Type" to "image/webp"),
        )
        coEvery { fileRepository.presign(any()) } returns PresignUploadOutcome.Success(target)
        coEvery { fileRepository.uploadBytes(target.uploadUrl, target.headers, photo.bytes) } returns
            UploadBytesOutcome.Success
        coEvery {
            fileRepository.confirm(
                "pending-1",
                any(),
            )
        } returns ConfirmUploadOutcome.Success(
            ConfirmedFile("confirmed-1", "image/webp", "avatar.webp", "", 0),
        )

        val outcome = useCase(photo)

        assertEquals(
            UploadProfilePhotoOutcome.Success("confirmed-1"),
            outcome,
        )
        coVerifyOrder {
            fileRepository.presign(match { it.purpose == FilePurpose.PROFILE_PHOTO })
            fileRepository.uploadBytes(target.uploadUrl, target.headers, photo.bytes)
            fileRepository.confirm("pending-1", any())
        }
    }

    @Test
    fun stops_before_storage_upload_when_presign_fails() = runTest {
        coEvery { fileRepository.presign(any()) } returns
            PresignUploadOutcome.Failure.Server(413, "too large")

        val outcome = useCase(photo)

        assertTrue(outcome is UploadProfilePhotoOutcome.Failure.Server)
        assertEquals(413, (outcome as UploadProfilePhotoOutcome.Failure.Server).code)
    }
}

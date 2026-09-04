package com.loresuelvo.consumer.domain.usecase.jobrequest

import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.ConfirmedFile
import com.loresuelvo.consumer.domain.file.FilePurpose
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.PresignUploadResult
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import com.loresuelvo.consumer.domain.jobrequest.UploadJobRequestImagesOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins the orchestration contract for [UploadJobRequestImagesUseCase]:
 *  - Runs the presign → upload → confirm pipeline for each staged
 *    image using `FilePurpose.JOB_REQUEST_IMAGE` so the backend's
 *    `job_request_image` policy is enforced.
 *  - Returns the confirmed file IDs in the same order as the input
 *    so `CreateJobRequestData.imageFileIds` is stable.
 *  - Short-circuits on the first failure (no partial uploads leak
 *    to the next image).
 *  - Typed failures propagate unchanged: Network / Server /
 *    Unauthorized map to the same sealed surface the rest of the
 *    contact-provider flow already handles.
 *
 * Companion file: [CreateJobRequestUseCaseTest].
 */
class UploadJobRequestImagesUseCaseTest {

    private val fileRepository = mockk<FileRepository>()
    private lateinit var useCase: UploadJobRequestImagesUseCase

    private val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    private fun image(name: String) = MediaUpload.Image(
        bytes = jpegBytes,
        mimeType = "image/jpeg",
        originalName = name,
    )

    private fun presignResult(name: String, fileId: String) = PresignUploadOutcome.Success(
        PresignUploadResult(
            fileId = fileId,
            key = "key-$name",
            uploadUrl = "https://upload.test/$name",
            headers = mapOf("Content-Type" to "image/jpeg"),
        ),
    )

    @Before
    fun setUp() {
        useCase = UploadJobRequestImagesUseCase(fileRepository)
        coEvery { fileRepository.uploadBytes(any(), any(), any()) } returns
            UploadBytesOutcome.Success
    }

    @Test
    fun `empty list returns Success with empty ids and never calls the repository`() = runTest {
        val outcome = useCase(emptyList())

        assertTrue(
            "expected Success for empty input, was $outcome",
            outcome is UploadJobRequestImagesOutcome.Success,
        )
        assertEquals(
            emptyList<String>(),
            (outcome as UploadJobRequestImagesOutcome.Success).fileIds,
        )
        coVerify(exactly = 0) { fileRepository.presign(any()) }
        coVerify(exactly = 0) { fileRepository.uploadBytes(any(), any(), any()) }
        coVerify(exactly = 0) { fileRepository.confirm(any(), any()) }
    }

    @Test
    fun `uploads each image with JOB_REQUEST_IMAGE purpose and returns ids in order`() = runTest {
        coEvery { fileRepository.presign(any()) } returnsMany listOf(
            presignResult("a.jpg", fileId = "file-1"),
            presignResult("b.jpg", fileId = "file-2"),
        )
        coEvery { fileRepository.confirm("file-1", any()) } returns
            ConfirmUploadOutcome.Success(
                ConfirmedFile(
                    id = "file-1",
                    mimeType = "image/jpeg",
                    originalName = "a.jpg",
                    codec = "",
                    durationSeconds = 0,
                ),
            )
        coEvery { fileRepository.confirm("file-2", any()) } returns
            ConfirmUploadOutcome.Success(
                ConfirmedFile(
                    id = "file-2",
                    mimeType = "image/jpeg",
                    originalName = "b.jpg",
                    codec = "",
                    durationSeconds = 0,
                ),
            )

        val outcome = useCase(listOf(image("a.jpg"), image("b.jpg")))

        assertTrue(outcome is UploadJobRequestImagesOutcome.Success)
        assertEquals(
            listOf("file-1", "file-2"),
            (outcome as UploadJobRequestImagesOutcome.Success).fileIds,
        )
        coVerify(exactly = 2) { fileRepository.presign(any()) }
        coVerify(exactly = 2) { fileRepository.uploadBytes(any(), any(), jpegBytes) }
        coVerify(exactly = 2) { fileRepository.confirm(any(), any()) }
    }

    @Test
    fun `presign request carries job_request_image purpose and original metadata`() = runTest {
        val captured = slot<PresignUploadRequest>()
        coEvery { fileRepository.presign(capture(captured)) } returns presignResult(
            name = "a.jpg",
            fileId = "file-1",
        )
        coEvery { fileRepository.confirm(any(), any()) } returns
            ConfirmUploadOutcome.Success(
                ConfirmedFile(
                    id = "file-1",
                    mimeType = "image/jpeg",
                    originalName = "a.jpg",
                    codec = "",
                    durationSeconds = 0,
                ),
            )

        useCase(listOf(image("a.jpg")))

        assertEquals("a.jpg", captured.captured.originalName)
        assertEquals("image/jpeg", captured.captured.mimeType)
        assertEquals(jpegBytes.size, captured.captured.sizeBytes)
        assertEquals(FilePurpose.JOB_REQUEST_IMAGE, captured.captured.purpose)
    }

    @Test
    fun `presign Network failure short-circuits the pipeline`() = runTest {
        coEvery { fileRepository.presign(any()) } returns
            PresignUploadOutcome.Failure.Network(java.io.IOException("dns"))

        val outcome = useCase(listOf(image("a.jpg"), image("b.jpg")))

        assertTrue(outcome is UploadJobRequestImagesOutcome.Failure.Network)
        coVerify(exactly = 0) { fileRepository.uploadBytes(any(), any(), any()) }
        coVerify(exactly = 0) { fileRepository.confirm(any(), any()) }
    }

    @Test
    fun `presign Server failure short-circuits and surfaces typed Server`() = runTest {
        coEvery { fileRepository.presign(any()) } returns
            PresignUploadOutcome.Failure.Server(code = 500, message = "boom")

        val outcome = useCase(listOf(image("a.jpg")))

        assertTrue(outcome is UploadJobRequestImagesOutcome.Failure.Server)
        outcome as UploadJobRequestImagesOutcome.Failure.Server
        assertEquals(500, outcome.code)
        assertTrue(
            "message should mention the offending file, got ${outcome.message}",
            outcome.message.contains("a.jpg"),
        )
    }

    @Test
    fun `presign Unauthorized maps to Unauthorized with the wire message`() = runTest {
        coEvery { fileRepository.presign(any()) } returns
            PresignUploadOutcome.Failure.Unauthorized("expired")

        val outcome = useCase(listOf(image("a.jpg")))

        assertTrue(outcome is UploadJobRequestImagesOutcome.Failure.Unauthorized)
        assertEquals(
            "expired",
            (outcome as UploadJobRequestImagesOutcome.Failure.Unauthorized).message,
        )
    }

    @Test
    fun `uploadBytes Network failure short-circuits and surfaces Network`() = runTest {
        coEvery { fileRepository.presign(any()) } returns presignResult("a.jpg", fileId = "file-1")
        coEvery { fileRepository.uploadBytes(any(), any(), any()) } returns
            UploadBytesOutcome.Failure.Network(java.io.IOException("timeout"))

        val outcome = useCase(listOf(image("a.jpg")))

        assertTrue(outcome is UploadJobRequestImagesOutcome.Failure.Network)
        coVerify(exactly = 0) { fileRepository.confirm(any(), any()) }
    }

    @Test
    fun `confirm Server failure short-circuits and surfaces typed Server`() = runTest {
        coEvery { fileRepository.presign(any()) } returns presignResult("a.jpg", fileId = "file-1")
        coEvery { fileRepository.confirm(any(), any()) } returns
            ConfirmUploadOutcome.Failure.Server(code = 502, message = "boom")

        val outcome = useCase(listOf(image("a.jpg")))

        assertTrue(outcome is UploadJobRequestImagesOutcome.Failure.Server)
        outcome as UploadJobRequestImagesOutcome.Failure.Server
        assertEquals(502, outcome.code)
        assertTrue(outcome.message.contains("a.jpg"))
    }

    @Test
    fun `second image failure short-circuits before the second presign`() = runTest {
        // First image succeeds, second image's presign fails.
        // The use case must NOT keep calling presign for the third
        // image in the list — partial uploads must be deterministic.
        coEvery { fileRepository.presign(any()) } returnsMany listOf(
            presignResult("a.jpg", fileId = "file-1"),
            PresignUploadOutcome.Failure.Server(code = 400, message = "bad mime"),
        )
        coEvery { fileRepository.confirm("file-1", any()) } returns
            ConfirmUploadOutcome.Success(
                ConfirmedFile(
                    id = "file-1",
                    mimeType = "image/jpeg",
                    originalName = "a.jpg",
                    codec = "",
                    durationSeconds = 0,
                ),
            )

        val outcome = useCase(listOf(image("a.jpg"), image("b.jpg"), image("c.jpg")))

        assertTrue(outcome is UploadJobRequestImagesOutcome.Failure.Server)
        // Two presigns (a succeeded, b failed); never reached c.
        coVerify(exactly = 2) { fileRepository.presign(any()) }
    }
}
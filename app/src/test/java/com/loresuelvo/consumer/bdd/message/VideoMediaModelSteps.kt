package com.loresuelvo.consumer.bdd.message

import com.loresuelvo.consumer.data.api.dto.ConversationMessageDto
import com.loresuelvo.consumer.data.api.dto.MessageVideoDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.domain.conversation.MAX_CONVERSATION_VIDEO_BYTES
import com.loresuelvo.consumer.domain.conversation.MediaReference
import com.loresuelvo.consumer.domain.conversation.MediaUpload
import com.loresuelvo.consumer.domain.conversation.VideoValidationError
import com.loresuelvo.consumer.domain.conversation.validationError
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class VideoMediaModelSteps {

    private lateinit var upload: MediaUpload.Video
    private var validationError: VideoValidationError? = null
    private var reference: MediaReference.Video? = null

    @Given("que tengo un archivo de video MP4 H.264 de 20 segundos")
    fun haveValidVideo() {
        upload = MediaUpload.Video(
            bytes = ByteArray(16),
            mimeType = "video/mp4",
            originalName = "evidence.mp4",
            durationMillis = 20_000L,
            width = 1280,
            height = 720,
            videoCodec = "h264",
        )
    }

    @When("Android lee el archivo para el chat")
    fun androidReadsVideo() {
        validationError = upload.validationError()
    }

    @Then("el dominio conserva bytes, MIME, nombre, duración y dimensiones")
    fun domainKeepsVideoMetadata() {
        assertEquals("video/mp4", upload.mimeType)
        assertEquals("evidence.mp4", upload.originalName)
        assertEquals(20_000L, upload.durationMillis)
        assertEquals(1280, upload.width)
        assertEquals(720, upload.height)
        assertEquals(16, upload.bytes.size)
    }

    @Then("el video pasa las validaciones de tamaño y formato")
    fun videoPassesValidation() {
        assertEquals(null, validationError)
    }

    @Given("que el backend devuelve un mensaje con metadata de video")
    fun backendReturnsVideoMessage() {
        val message = ConversationMessageDto(
            id = 1,
            senderRole = "provider",
            content = "video",
            video = MessageVideoDto(
                id = "video-1",
                url = "https://cdn.example/video.mp4",
                originalName = "evidence.mp4",
                durationSeconds = 20,
                width = 1280,
                height = 720,
            ),
        ).toDomain()
        reference = message.media as MediaReference.Video
    }

    @When("Android mapea el mensaje REST o WebSocket")
    fun androidMapsMessage() {
        check(reference != null) { "video reference must be mapped" }
    }

    @Then("el mensaje conserva la referencia de video sin convertirla en imagen")
    fun messageKeepsVideoReference() {
        assertEquals("video-1", reference?.id)
        assertEquals(1280, reference?.width)
        assertEquals(720, reference?.height)
    }

    @Given("que tengo un video que supera uno de los límites del chat")
    fun haveOversizedVideo() {
        upload = MediaUpload.Video(
            bytes = ByteArray((MAX_CONVERSATION_VIDEO_BYTES + 1).toInt()),
            mimeType = "video/mp4",
            originalName = "too-large.mp4",
            durationMillis = 20_000L,
            width = 1280,
            height = 720,
            videoCodec = "h264",
        )
    }

    @When("Android valida el archivo antes del upload")
    fun androidValidatesBeforeUpload() {
        validationError = upload.validationError()
    }

    @Then("obtiene un error tipado de video y no inicia la subida")
    fun getsTypedVideoError() {
        assertTrue(validationError is VideoValidationError.TooLarge)
    }
}

package com.loresuelvo.consumer.instrumented.diagnosis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.data.media.AndroidMediaReader
import com.loresuelvo.consumer.domain.diagnosis.ChatImage
import com.loresuelvo.consumer.domain.diagnosis.ChatMessage
import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.LoadAiConversationOutcome
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.Sender
import com.loresuelvo.consumer.domain.diagnosis.usecase.LoadAiConversationUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.UploadAttachmentsAndSendUseCase
import com.loresuelvo.consumer.domain.file.ConfirmUploadOutcome
import com.loresuelvo.consumer.domain.file.ConfirmUploadRequest
import com.loresuelvo.consumer.domain.file.ConfirmedFile
import com.loresuelvo.consumer.domain.file.FileRepository
import com.loresuelvo.consumer.domain.file.PresignUploadOutcome
import com.loresuelvo.consumer.domain.file.PresignUploadRequest
import com.loresuelvo.consumer.domain.file.PresignUploadResult
import com.loresuelvo.consumer.domain.file.UploadBytesOutcome
import com.loresuelvo.consumer.ui.screens.chat.CHAT_ATTACHMENT_CARD_TAG_PREFIX
import com.loresuelvo.consumer.ui.screens.chat.CONVERSATION_FULLSCREEN_IMAGE_TAG
import com.loresuelvo.consumer.ui.screens.chat.ChatScreen
import com.loresuelvo.consumer.ui.screens.chat.ChatScreenActions
import com.loresuelvo.consumer.ui.screens.chat.ChatViewModel
import com.loresuelvo.consumer.ui.screens.chat.MEDIA_PREVIEW_SEND_TAG
import com.loresuelvo.consumer.ui.screens.chat.chatMessageImageTag
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatImageVisibilityInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = ImageDiagnosisRepository()
    private val files = ImageFileRepository()
    private val store = ViewModelStore()
    private lateinit var photo: File
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        photo = File.createTempFile("ai-photo-", ".png", context.cacheDir)
        Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(10, 150, 130))
            photo.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        repository.saved = Diagnosis(
            conversationId = "42",
            messages = listOf(
                ChatMessage(
                    id = "server-photo",
                    sender = Sender.Consumer,
                    content = "",
                    sentAtEpochMillis = 1L,
                    images = listOf(ChatImage("file-1", Uri.fromFile(photo).toString(), photo.name, "image/png")),
                ),
                ChatMessage("assistant", Sender.Assistant, "Image received", 2L),
            ),
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val send = SendDiagnosisPromptUseCase(repository)
            viewModel = ChatViewModel(
                send,
                LoadAiConversationUseCase(repository),
                AndroidMediaReader(context),
                UploadAttachmentsAndSendUseCase(files, send),
            )
            store.put("chat", viewModel)
        }
        compose.setContent {
            val state by viewModel.uiState.collectAsState()
            LoresuelvoTheme {
                ChatScreen(
                    state = state,
                    actions = ChatScreenActions(
                        composer = ChatScreenActions.Composer(onSend = viewModel::onSendClick),
                        media = ChatScreenActions.Media(onConfirmSend = { viewModel.onSendClick() }),
                        errors = ChatScreenActions.Errors(onRetry = viewModel::onRetryClick),
                    ),
                )
            }
        }
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        photo.delete()
    }

    @Test
    fun photo_is_visible_during_processing_after_failure_and_after_retry_without_text() {
        compose.runOnIdle { viewModel.onAttachImageFromGallery(Uri.fromFile(photo)) }
        compose.waitUntil { viewModel.uiState.value.pendingAttachments.isNotEmpty() }
        compose.onNodeWithTag(MEDIA_PREVIEW_SEND_TAG).performClick()
        compose.waitUntil { repository.sendCount == 1 }

        val localMessageId = viewModel.uiState.value.messages.single().id
        assertPhotoDisplayed(localMessageId)
        compose.onNodeWithTag("$CHAT_ATTACHMENT_CARD_TAG_PREFIX-0").assertDoesNotExist()
        compose.onNodeWithTag(chatMessageImageTag(localMessageId, 0)).performClick()
        compose.onNodeWithTag(CONVERSATION_FULLSCREEN_IMAGE_TAG).assertIsDisplayed().performClick()

        compose.runOnIdle {
            repository.pending.complete(SendDiagnosisPromptOutcome.Failure.Server(503, "Unavailable"))
        }
        compose.waitUntil { !viewModel.uiState.value.sending }
        assertPhotoDisplayed(localMessageId)

        compose.runOnIdle { repository.pending = CompletableDeferred() }
        compose.onNodeWithText(context.getString(R.string.chat_error_retry)).performClick()
        compose.waitUntil { repository.sendCount == 2 }
        assertPhotoDisplayed(localMessageId)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(1, files.uploadCount)
        assertNull(repository.existingConversationId)

        compose.runOnIdle {
            repository.pending.complete(SendDiagnosisPromptOutcome.Success(repository.saved))
        }
        compose.waitUntil { viewModel.uiState.value.conversationId == "42" }
        assertPhotoDisplayed("server-photo")
    }

    @Test
    fun reopening_a_conversation_displays_the_saved_photo() {
        compose.runOnIdle { viewModel.loadExisting("42") }
        compose.waitUntil { viewModel.uiState.value.conversationId == "42" }
        assertPhotoDisplayed("server-photo")
        compose.onNodeWithTag(chatMessageImageTag("server-photo", 0)).performClick()
        compose.onNodeWithTag(CONVERSATION_FULLSCREEN_IMAGE_TAG).assertIsDisplayed()
    }

    private fun assertPhotoDisplayed(messageId: String) {
        compose.onNodeWithTag(chatMessageImageTag(messageId, 0)).assertIsDisplayed()
        compose.waitUntil {
            compose.onAllNodesWithContentDescription(photo.name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private class ImageDiagnosisRepository : DiagnosisRepository {
        var pending = CompletableDeferred<SendDiagnosisPromptOutcome>()
        lateinit var saved: Diagnosis
        var sendCount = 0
        var existingConversationId: String? = null

        override suspend fun sendPrompt(
            content: String,
            existingConversationId: String?,
            imageFileIds: List<String>,
        ): SendDiagnosisPromptOutcome {
            check(content.isEmpty())
            check(imageFileIds == listOf("file-1"))
            this.existingConversationId = existingConversationId
            sendCount++
            return pending.await()
        }

        override suspend fun getAiConversation(conversationId: String): LoadAiConversationOutcome =
            LoadAiConversationOutcome.Success(saved)
    }

    private class ImageFileRepository : FileRepository {
        var uploadCount = 0

        override suspend fun presign(request: PresignUploadRequest): PresignUploadOutcome =
            PresignUploadOutcome.Success(PresignUploadResult("file-1", "photo", "https://uploads.test/photo", emptyMap()))

        override suspend fun uploadBytes(
            uploadUrl: String,
            headers: Map<String, String>,
            bytes: ByteArray,
        ): UploadBytesOutcome {
            check(bytes.isNotEmpty())
            uploadCount++
            return UploadBytesOutcome.Success
        }

        override suspend fun confirm(fileId: String, request: ConfirmUploadRequest): ConfirmUploadOutcome =
            ConfirmUploadOutcome.Success(ConfirmedFile(fileId, request.mimeType, "photo.png", "", 0))
    }
}

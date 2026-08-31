package com.loresuelvo.consumer.bdd.fixes

import com.loresuelvo.consumer.data.media.MediaReader
import com.loresuelvo.consumer.domain.diagnosis.usecase.LoadAiConversationUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.SendDiagnosisPromptUseCase
import com.loresuelvo.consumer.domain.diagnosis.usecase.UploadAttachmentsAndSendUseCase
import com.loresuelvo.consumer.ui.screens.chat.ChatUiState
import com.loresuelvo.consumer.ui.screens.chat.ChatViewModel
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Per-scenario world for `features/fixes/ux_ui_fixes.feature`.
 * Owns a [StandardTestDispatcher] so step defs can inspect the
 * UDF state of [ChatViewModel] deterministically without Hilt,
 * Compose, or a backend. Relaxed mocks cover the scenarios that
 * only assert default state values (01-UXUI); later scenarios
 * can extend this scaffolding without breaking the existing one.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UxUiFixesWorld : AutoCloseable {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + supervisorJob)

    private val sendDiagnosisPrompt = mockk<SendDiagnosisPromptUseCase>(relaxed = true)
    private val loadAiConversation = mockk<LoadAiConversationUseCase>(relaxed = true)
    private val mediaReader = mockk<MediaReader>(relaxed = true)
    private val uploadAttachmentsAndSend =
        mockk<UploadAttachmentsAndSendUseCase>(relaxed = true)

    private lateinit var viewModel: ChatViewModel
    private val observedUiStates = mutableListOf<ChatUiState>()
    private var started = false

    fun startScenario() {
        if (started) return
        started = true
        Dispatchers.setMain(dispatcher)
        viewModel = ChatViewModel(
            sendDiagnosisPrompt = sendDiagnosisPrompt,
            loadAiConversation = loadAiConversation,
            mediaReader = mediaReader,
            uploadAttachmentsAndSend = uploadAttachmentsAndSend,
        )
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { observedUiStates += it }
        }
        scheduler.advanceUntilIdle()
    }

    fun lastUiState(): ChatUiState = observedUiStates.last()

    override fun close() {
        supervisorJob.cancel()
        Dispatchers.resetMain()
    }
}

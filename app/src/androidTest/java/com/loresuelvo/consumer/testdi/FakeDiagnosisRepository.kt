package com.loresuelvo.consumer.instrumented.diagnosis

import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import com.loresuelvo.consumer.domain.diagnosis.LoadAiConversationOutcome
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDiagnosisRepository @Inject constructor() : DiagnosisRepository {

    override suspend fun sendPrompt(
        content: String,
        existingConversationId: String?,
        imageFileIds: List<String>,
    ): SendDiagnosisPromptOutcome =
        SendDiagnosisPromptOutcome.Failure.Server(
            code = 0,
            message = "FakeDiagnosisRepository: acceptance tests do not exercise the chat",
        )

    override suspend fun getAiConversation(
        conversationId: String,
    ): LoadAiConversationOutcome =
        LoadAiConversationOutcome.Failure.Server(
            code = 0,
            message = "FakeDiagnosisRepository: acceptance tests do not exercise loading AI conversations",
        )
}

package com.loresuelvo.consumer.acceptance.diagnosis

import com.loresuelvo.consumer.domain.diagnosis.Diagnosis
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisRepository
import com.loresuelvo.consumer.domain.diagnosis.SendDiagnosisPromptOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt-friendly fake for the AI diagnostic chat repository. Used
 * by every `@HiltAndroidTest` that uninstalls
 * [com.loresuelvo.consumer.di.RepositoryModule] so the new
 * `DiagnosisRepository` binding is satisfied without dragging in
 * the production Retrofit-typed [com.loresuelvo.consumer.data.api.ApiDiagnosisRepository].
 *
 * The fake never returns a successful response: the test classes
 * that wire it (e.g. `CompleteProfileScreenAcceptanceTest`,
 * `WelcomeCategoriesAcceptanceTest`, `ProfessionalsAcceptanceTest`,
 * `RealBackendFlowE2ETest`, `ChatNavigationAcceptanceTest`) never
 * navigate to the chat screen, so the `sendPrompt(...)` call
 * never runs. The failure-shaped default is the defensive
 * equivalent of [com.loresuelvo.consumer.bdd.diagnosis.FakeDiagnosisRepository]:
 * if the test ever exercises it by mistake, the chat's `ChatErrorCard`
 * shows up rather than crashing the process.
 *
 * If a future acceptance test needs the full conversation flow,
 * extend this fake with a configurable response — mirroring the
 * pattern in the JVM BDD layer.
 */
@Singleton
class FakeDiagnosisRepository @Inject constructor() : DiagnosisRepository {

    override suspend fun sendPrompt(
        content: String,
        existingConversationId: String?,
    ): SendDiagnosisPromptOutcome =
        SendDiagnosisPromptOutcome.Failure.Server(
            code = 0,
            message = "FakeDiagnosisRepository: acceptance tests do not exercise the chat",
        )
}

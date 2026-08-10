package com.loresuelvo.consumer.domain.diagnosis

import com.loresuelvo.consumer.domain.category.Category

/**
 * Diagnosis outcome attached to the conversation when the AI
 * settles into a terminal state. `null` while the AI is still
 * collecting information.
 *
 * `outcome` is the wire-level sentinel the backend uses
 * (verified against `loresuelvo-api` on 2026-08-10):
 *  - `COLLECTING_INFORMATION`: AI asked a follow-up question.
 *  - `PROFESSIONAL_REQUIRED`: diagnosis concluded; the user
 *    should be shown the matched providers.
 *  - any other string: a future backend addition. The UI gates
 *    rendering on [isProfessionalRequired] today; new outcomes
 *    won't crash, they'll just stay invisible until a future
 *    commit adds the matching UI surface.
 *
 * `problemCategory` is only populated alongside
 * `PROFESSIONAL_REQUIRED`. The category id is threaded into every
 * mapped recommended provider by the mapper (see
 * `DiagnosisDtoMapper`), so the consumer app never has to invent
 * a category id it didn't receive.
 */
data class DiagnosisAssessment(
    val outcome: String,
    val problemCategory: Category? = null,
) {

    val isProfessionalRequired: Boolean
        get() = outcome == OUTCOME_PROFESSIONAL_REQUIRED

    companion object {
        const val OUTCOME_COLLECTING_INFORMATION: String = "collecting_information"
        const val OUTCOME_PROFESSIONAL_REQUIRED: String = "professional_required"
    }
}

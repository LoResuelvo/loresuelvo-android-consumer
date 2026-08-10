package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.AssessmentDto
import com.loresuelvo.consumer.domain.diagnosis.DiagnosisAssessment

/**
 * DTO → domain translation for the AI diagnostic chat
 * [AssessmentDto]. The wire's `outcome` is a free-form string; it
 * is kept on the domain as-is so a future backend addition
 * ("self_service", "in_progress", …) does not crash the consumer.
 * UI gates render only on known sentinels via
 * [DiagnosisAssessment.isProfessionalRequired].
 */
internal fun AssessmentDto.toDomain(): DiagnosisAssessment =
    DiagnosisAssessment(
        outcome = outcome,
        problemCategory = problemCategory?.toDomain(),
    )

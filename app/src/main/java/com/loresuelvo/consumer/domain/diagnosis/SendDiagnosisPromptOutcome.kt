package com.loresuelvo.consumer.domain.diagnosis

/**
 * Outcome of [DiagnosisRepository.sendPrompt]. Sealed so callers
 * handle every branch explicitly (mirrors
 * [com.loresuelvo.consumer.domain.category.CategoriesOutcome] and
 * [com.loresuelvo.consumer.domain.auth.UserRegistrationOutcome]).
 */
sealed interface SendDiagnosisPromptOutcome {

    data class Success(val diagnosis: Diagnosis) : SendDiagnosisPromptOutcome

    sealed interface Failure : SendDiagnosisPromptOutcome {

        /** Transport-level failure: timeouts, DNS, connection refused. */
        data class Network(
            val cause: Throwable,
            /**
             * Attachments the upload pipeline confirmed BEFORE the
             * message endpoint rejected. Empty for upload-pipeline
             * failures (08-AIP); non-empty when the prompt endpoint
             * rejected AFTER the bytes were already on the storage
             * backend (10-AIP). The VM keeps these in
             * `state.sentAttachments` so the user can retry the
             * prompt without re-uploading.
             */
            val partiallyUploadedAttachments: List<com.loresuelvo.consumer.ui.screens.chat.PendingMedia> = emptyList(),
        ) : Failure

        /**
         * Any non-2xx response. [code] is the HTTP status, [message]
         * the human-readable text extracted from the error body.
         */
        data class Server(
            val code: Int,
            val message: String,
            val partiallyUploadedAttachments: List<com.loresuelvo.consumer.ui.screens.chat.PendingMedia> = emptyList(),
        ) : Failure

        /** 401: Auth0 session expired or invalid. */
        data class Unauthorized(
            val message: String,
            val partiallyUploadedAttachments: List<com.loresuelvo.consumer.ui.screens.chat.PendingMedia> = emptyList(),
        ) : Failure
    }
}

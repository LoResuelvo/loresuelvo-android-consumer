package com.loresuelvo.consumer.domain.jobrequest

/**
 * Outcome of [com.loresuelvo.consumer.domain.usecase.jobrequest.UploadJobRequestImagesUseCase].
 * Mirrors the typed-failure tree used by every other flow in the
 * app so the contact-provider ViewModel can collapse the upload
 * step into the existing [CreateJobRequestOutcome.Failure] mapping
 * without bespoke handling per branch.
 *
 *  - [Success] carries the list of confirmed file IDs in the same
 *    order as the input so `CreateJobRequestData.imageFileIds`
 *    stays deterministic and the provider sees the same image
 *    ordering the consumer picked.
 *  - [Failure.Network] / [Failure.Server] / [Failure.Unauthorized]
 *    propagate the wire contract the `FileRepository` already
 *    surfaces for the chat's media uploads.
 */
sealed interface UploadJobRequestImagesOutcome {

    data class Success(val fileIds: List<String>) : UploadJobRequestImagesOutcome

    sealed interface Failure : UploadJobRequestImagesOutcome {

        /** Transport-level failure: timeouts, DNS, connection refused. */
        data class Network(val cause: Throwable) : Failure

        /** Any non-2xx response from any step of the pipeline. The
         *  [message] carries the offending file name so the user
         *  knows which image failed when several are staged. */
        data class Server(val code: Int, val message: String) : Failure

        /** 401: Auth0 session expired or invalid. */
        data class Unauthorized(val message: String) : Failure
    }
}
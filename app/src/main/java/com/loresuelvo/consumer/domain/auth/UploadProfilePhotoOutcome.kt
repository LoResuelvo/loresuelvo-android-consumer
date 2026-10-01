package com.loresuelvo.consumer.domain.auth

/** Outcome of the profile-photo presign → upload → confirm pipeline. */
sealed interface UploadProfilePhotoOutcome {

    data class Success(val fileId: String) : UploadProfilePhotoOutcome

    sealed interface Failure : UploadProfilePhotoOutcome {
        data class Network(val cause: Throwable) : Failure
        data class Server(val code: Int, val message: String) : Failure
        data class Unauthorized(val message: String) : Failure
    }
}

package com.loresuelvo.consumer.domain.installation

data class InstallationIdentity(val id: String, val secret: String)

data class InstallationBinding(
    val identity: InstallationIdentity,
    val id: String,
    val previousId: String?,
    val userId: Int,
    val attemptId: String,
    val confirmed: Boolean = false,
)

data class InstallationConfirmation(
    val installationId: String,
    val bindingId: String,
    val app: String,
    val locale: String,
    val enabled: Boolean,
)

sealed interface RegistrationOutcome {
    data class Success(val installationId: String, val bindingId: String, val userId: Int) : RegistrationOutcome
    data object UnverifiedAccount : RegistrationOutcome
    data object Superseded : RegistrationOutcome
    data object ConfigurationUnavailable : RegistrationOutcome
    data class TokenFailure(val cause: Throwable) : RegistrationOutcome
    data class StorageFailure(val cause: Throwable) : RegistrationOutcome
    data object InvalidConfirmation : RegistrationOutcome
    data object BadRequest : RegistrationOutcome
    data object Unauthorized : RegistrationOutcome
    data object Forbidden : RegistrationOutcome
    data object Conflict : RegistrationOutcome
    data class ServerFailure(val code: Int) : RegistrationOutcome
    data class NetworkFailure(val cause: Throwable) : RegistrationOutcome
    data class UnexpectedFailure(val cause: Throwable) : RegistrationOutcome
}

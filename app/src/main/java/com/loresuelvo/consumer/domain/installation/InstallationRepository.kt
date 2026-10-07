package com.loresuelvo.consumer.domain.installation

sealed interface InstallationRegistrationResult {
    data class Confirmed(val confirmation: InstallationConfirmation) : InstallationRegistrationResult
    data class Failed(val outcome: RegistrationOutcome) : InstallationRegistrationResult
}

interface InstallationRepository {
    suspend fun register(
        binding: InstallationBinding,
        token: String,
        locale: String,
        accessToken: String,
    ): InstallationRegistrationResult
}

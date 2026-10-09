package com.loresuelvo.consumer.domain.installation

import com.loresuelvo.consumer.domain.auth.AuthSession

data class PendingInstallationRemoval(
    val identity: InstallationIdentity,
    val bindingId: String,
    val userId: Int,
)

sealed interface InstallationRemovalResult {
    data object Removed : InstallationRemovalResult
    data object Superseded : InstallationRemovalResult
    data object Unauthorized : InstallationRemovalResult
    data object Forbidden : InstallationRemovalResult
    data class ServerFailure(val code: Int) : InstallationRemovalResult
    data class NetworkFailure(val cause: Throwable) : InstallationRemovalResult
    data class UnexpectedFailure(val cause: Throwable) : InstallationRemovalResult
}

fun interface InstallationRemovalRepository {
    suspend fun remove(
        removal: PendingInstallationRemoval,
        accessToken: String,
    ): InstallationRemovalResult
}

fun interface InstallationLogoutHandler {
    fun onLogout(session: AuthSession)
}

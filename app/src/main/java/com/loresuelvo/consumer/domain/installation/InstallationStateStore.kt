package com.loresuelvo.consumer.domain.installation

interface InstallationStateStore {
    fun prepare(userId: Int, attemptId: String): InstallationBinding
    fun prepare(userId: Int, attemptId: String, newAuthentication: Boolean): InstallationBinding =
        prepare(userId, attemptId)
    fun confirm(binding: InstallationBinding)
    fun beginRemoval(userId: Int): PendingInstallationRemoval? = null
    fun pendingRemoval(): PendingInstallationRemoval? = null
    fun pendingRemovals(): List<PendingInstallationRemoval> = pendingRemoval()?.let(::listOf).orEmpty()
    fun completeRemoval(bindingId: String): Boolean = false
}

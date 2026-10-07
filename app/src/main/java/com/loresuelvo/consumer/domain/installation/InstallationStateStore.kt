package com.loresuelvo.consumer.domain.installation

interface InstallationStateStore {
    fun prepare(userId: Int, attemptId: String): InstallationBinding
    fun confirm(binding: InstallationBinding)
}

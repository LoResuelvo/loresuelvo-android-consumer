package com.loresuelvo.consumer.domain.installation

import kotlinx.coroutines.flow.StateFlow

data class RegistrationCandidate(
    val generation: Long,
    val isNewAuthentication: Boolean = false,
)

interface InstallationRegistrationRequests {
    val pending: StateFlow<RegistrationCandidate?>
    fun request()
    fun requestNewAuthentication() = request()
    fun invalidate()
    fun acknowledge(generation: Long): Boolean
    fun ifCurrent(generation: Long, action: () -> Boolean): Boolean
}

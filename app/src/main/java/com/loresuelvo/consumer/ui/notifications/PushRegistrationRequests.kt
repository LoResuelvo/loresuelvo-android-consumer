package com.loresuelvo.consumer.ui.notifications

import com.loresuelvo.consumer.domain.installation.InstallationRegistrationRequests
import com.loresuelvo.consumer.domain.installation.RegistrationCandidate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PushRegistrationRequests @Inject constructor() : InstallationRegistrationRequests {
    private var generation = 0L
    private val mutablePending = MutableStateFlow<RegistrationCandidate?>(null)
    override val pending: StateFlow<RegistrationCandidate?> = mutablePending.asStateFlow()

    @Synchronized
    override fun request() {
        generation += 1
        mutablePending.value = RegistrationCandidate(generation)
    }

    @Synchronized
    override fun acknowledge(generation: Long): Boolean {
        if (mutablePending.value?.generation != generation) return false
        mutablePending.value = null
        return true
    }

    @Synchronized
    override fun invalidate() {
        generation += 1
        mutablePending.value = null
    }

    @Synchronized
    override fun ifCurrent(generation: Long, action: () -> Boolean): Boolean =
        mutablePending.value?.generation == generation && action()
}

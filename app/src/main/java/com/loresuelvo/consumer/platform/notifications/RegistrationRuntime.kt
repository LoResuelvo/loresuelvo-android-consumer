package com.loresuelvo.consumer.platform.notifications

import com.loresuelvo.consumer.domain.installation.RegistrationOutcome
import com.loresuelvo.consumer.domain.installation.InstallationRegistrationRequests
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Singleton
class RegistrationRuntime @Inject constructor(
    private val requests: InstallationRegistrationRequests,
    private val coordinator: Provider<InstallationRegistrationCoordinator>,
    @Named("registrationScope") private val scope: CoroutineScope,
) {
    private val mutableFailure = MutableStateFlow<RegistrationOutcome?>(null)
    val startupFailure = mutableFailure.asStateFlow()
    private var job: Job? = null

    @Synchronized
    fun start() {
        if (job != null) return
        job = scope.launch {
            requests.pending.collect { candidate ->
                if (candidate == null) return@collect
                startCoordinator()
            }
        }
    }

    private fun startCoordinator() {
        try {
            coordinator.get().start()
            mutableFailure.value = null
        } catch (error: IOException) {
            mutableFailure.value = RegistrationOutcome.StorageFailure(error)
        } catch (error: GeneralSecurityException) {
            mutableFailure.value = RegistrationOutcome.StorageFailure(error)
        } catch (error: SecurityException) {
            mutableFailure.value = RegistrationOutcome.StorageFailure(error)
        } catch (error: IllegalArgumentException) {
            mutableFailure.value = RegistrationOutcome.ConfigurationUnavailable
        }
    }
}

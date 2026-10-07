package com.loresuelvo.consumer.ui.notifications

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Señal de sincronización terminada, no de registro ni cuenta verificada.
 * No retiene sesión o credenciales. El consumidor obtiene la sesión actual
 * y valida su identidad antes de registrar. Sobrevive a navegación, no a
 * reinicios de proceso: la recuperación persistente pertenece a aplicación.
 */
@Singleton
class PushRegistrationRequests @Inject constructor() {
    data class Candidate(val generation: Long)

    private var generation = 0L
    private val mutablePending = MutableStateFlow<Candidate?>(null)
    val pending: StateFlow<Candidate?> = mutablePending.asStateFlow()

    @Synchronized
    fun request() {
        generation += 1
        mutablePending.value = Candidate(generation)
    }

    /** Una confirmación anterior no puede borrar una solicitud más reciente. */
    @Synchronized
    fun acknowledge(generation: Long): Boolean {
        if (mutablePending.value?.generation != generation) return false
        mutablePending.value = null
        return true
    }
}

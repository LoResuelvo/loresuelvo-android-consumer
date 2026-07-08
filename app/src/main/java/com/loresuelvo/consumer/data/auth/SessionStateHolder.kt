package com.loresuelvo.consumer.data.auth

import com.loresuelvo.consumer.domain.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object SessionStateHolder {
    private val _state = MutableStateFlow<AuthSession?>(null)
    val state: StateFlow<AuthSession?> = _state.asStateFlow()

    fun set(session: AuthSession?) {
        _state.value = session
    }
}
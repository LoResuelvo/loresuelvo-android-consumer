package com.loresuelvo.consumer.domain.realtime

import kotlinx.coroutines.flow.SharedFlow

interface RealtimeClient {
    val events: SharedFlow<WsEvent>

    fun start()

    fun stop()
}

package com.loresuelvo.consumer.domain.realtime

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

interface RealtimeClient {
    val events: SharedFlow<WsEvent>

    fun start()

    fun stop()

    companion object {
        val None: RealtimeClient = object : RealtimeClient {
            override val events: SharedFlow<WsEvent> = MutableSharedFlow()
            override fun start() = Unit
            override fun stop() = Unit
        }
    }
}

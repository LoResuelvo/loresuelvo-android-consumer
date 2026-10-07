package com.loresuelvo.consumer.platform.notifications

import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@Singleton
class NavigationIntentDispatcher @Inject constructor() {
    private val channel = Channel<Intent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    fun dispatch(intent: Intent) { channel.trySend(intent) }
}

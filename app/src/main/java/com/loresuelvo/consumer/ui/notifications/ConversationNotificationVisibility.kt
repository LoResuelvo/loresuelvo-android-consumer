package com.loresuelvo.consumer.ui.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.loresuelvo.consumer.domain.notifications.ConversationVisibility
import com.loresuelvo.consumer.ui.navigation.Route

@Composable
fun ConversationNotificationVisibility(
    visibility: ConversationVisibility?,
    route: String?,
    conversationId: String?,
    authenticated: Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val visibleId = conversationId?.toIntOrNull()?.takeIf { authenticated && route == Route.Conversation("").path }
    DisposableEffect(lifecycleOwner, visibleId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) visibility?.show(visibleId)
            if (event == Lifecycle.Event.ON_PAUSE) visibility?.show(null)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) visibility?.show(visibleId)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            visibility?.show(null)
        }
    }
}

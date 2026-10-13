package com.loresuelvo.consumer.ui.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
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
    onConversationEntered: (Int) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnConversationEntered = rememberUpdatedState(onConversationEntered)
    val visibleId = conversationId?.toIntOrNull()?.takeIf { authenticated && route == Route.Conversation("").path }
    DisposableEffect(lifecycleOwner, visibleId) {
        var enteredCurrentConversation = false
        fun showCurrentConversation() {
            visibility?.show(visibleId)
            if (visibleId != null && !enteredCurrentConversation) {
                enteredCurrentConversation = true
                currentOnConversationEntered.value(visibleId)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) showCurrentConversation()
            if (event == Lifecycle.Event.ON_PAUSE) {
                visibility?.show(null)
                enteredCurrentConversation = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) showCurrentConversation()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            visibility?.show(null)
        }
    }
}

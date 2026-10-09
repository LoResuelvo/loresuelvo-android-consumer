package com.loresuelvo.consumer.platform.notifications

import com.loresuelvo.consumer.domain.notifications.ConversationVisibility
import com.loresuelvo.consumer.domain.notifications.ConversationRefreshRequest
import com.loresuelvo.consumer.domain.auth.AuthSessionStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class VisibleConversationStore @Inject constructor(
    private val sessions: AuthSessionStore,
) : ConversationVisibility {
    private val refreshFlow = MutableSharedFlow<ConversationRefreshRequest>(
        replay = 1,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    @Volatile private var conversationId: Int? = null
    override val refreshRequests: SharedFlow<ConversationRefreshRequest> = refreshFlow.asSharedFlow()

    @Synchronized
    override fun visibleConversation() = conversationId

    @Synchronized
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun show(conversationId: Int?) {
        if (conversationId == null) refreshFlow.resetReplayCache()
        this.conversationId = conversationId
    }

    @Synchronized
    override fun requestRefreshIfVisible(
        conversationId: Int,
        eventId: String,
        recipientUserId: Int,
    ): Boolean {
        if (this.conversationId != conversationId) return false
        if (sessions.getSession()?.user?.backendUserId != recipientUserId) return false
        return refreshFlow.tryEmit(
            ConversationRefreshRequest(conversationId, eventId, recipientUserId),
        )
    }

    @Synchronized
    override fun isCurrent(request: ConversationRefreshRequest): Boolean =
        conversationId == request.conversationId &&
            sessions.getSession()?.user?.backendUserId == request.recipientUserId

}

package com.loresuelvo.consumer.platform.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class ConsumerFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var receiver: Provider<ConsumerMessageReceiver>
    @Inject lateinit var serviceReceiver: Provider<ConsumerServiceNotificationReceiver>

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val type = message.data["type"].orEmpty()
            if (type == "conversation.message.created") {
                receiver.get().receive(message.data)
            } else {
                serviceReceiver.get().receive(message.data)
            }
        }
        catch (_: java.io.IOException) { }
        catch (_: java.security.GeneralSecurityException) { }
        catch (_: SecurityException) { }
    }
}

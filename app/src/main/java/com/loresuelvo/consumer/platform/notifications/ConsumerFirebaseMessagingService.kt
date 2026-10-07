package com.loresuelvo.consumer.platform.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class ConsumerFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var receiver: Provider<ConsumerMessageReceiver>

    override fun onMessageReceived(message: RemoteMessage) {
        try { receiver.get().receive(message.data) }
        catch (_: java.io.IOException) { }
        catch (_: java.security.GeneralSecurityException) { }
        catch (_: SecurityException) { }
    }
}

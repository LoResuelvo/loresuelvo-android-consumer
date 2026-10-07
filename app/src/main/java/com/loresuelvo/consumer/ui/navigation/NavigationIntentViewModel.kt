package com.loresuelvo.consumer.ui.navigation

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.loresuelvo.consumer.domain.notifications.ConversationVisibility
import com.loresuelvo.consumer.platform.notifications.MessageNotificationIntent
import com.loresuelvo.consumer.platform.notifications.MessageNotificationNavigation
import com.loresuelvo.consumer.platform.notifications.NavigationIntentDispatcher
import com.loresuelvo.consumer.platform.notifications.ServiceNotificationIntent
import com.loresuelvo.consumer.platform.notifications.ServiceNotificationNavigation
import com.loresuelvo.consumer.platform.notifications.ServiceTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class NavigationIntentViewModel @Inject constructor(
    dispatcher: NavigationIntentDispatcher,
    val conversationVisibility: ConversationVisibility,
    private val notifications: Provider<MessageNotificationNavigation>,
    private val serviceNotifications: Provider<ServiceNotificationNavigation>,
) : ViewModel() {
    val events = dispatcher.events
    fun isMessageNotification(intent: Intent) = intent.action == MessageNotificationIntent.ACTION
    fun conversationId(intent: Intent): Int? = try { notifications.get().conversationId(intent) }
    catch (_: IOException) { null }
    catch (_: GeneralSecurityException) { null }
    catch (_: SecurityException) { null }

    fun isServiceNotification(intent: Intent) = intent.action == ServiceNotificationIntent.ACTION
    fun serviceDestination(intent: Intent): ServiceTarget? = try { serviceNotifications.get().target(intent) }
    catch (_: IOException) { null }
    catch (_: GeneralSecurityException) { null }
    catch (_: SecurityException) { null }
}

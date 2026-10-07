package com.loresuelvo.consumer.platform.notifications

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loresuelvo.consumer.data.auth.EncryptedAuthSessionStore
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.data.installation.EncryptedInstallationStateStore
import com.loresuelvo.consumer.data.notifications.MessageNotificationDecoder
import com.loresuelvo.consumer.data.notifications.StoredNotificationEvents
import com.loresuelvo.consumer.domain.auth.AuthSession
import com.loresuelvo.consumer.domain.auth.User
import com.loresuelvo.consumer.domain.notifications.*
import com.loresuelvo.consumer.domain.usecase.notifications.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [24, 34])
class MessageNotificationsAndroidTest {
    private lateinit var context: Context
    private lateinit var sessions: EncryptedAuthSessionStore
    private lateinit var installations: EncryptedInstallationStateStore
    private lateinit var receiver: ConsumerMessageReceiver
    private lateinit var authorize: AuthorizeMessageNotificationUseCase
    private lateinit var visibility: VisibleConversationStore
    private lateinit var payload: Map<String, String>
    private lateinit var manager: NotificationManager

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val sessionPrefs = context.getSharedPreferences("message_sessions", 0)
        val installationPrefs = context.getSharedPreferences("message_installations", 0)
        sessionPrefs.edit().clear().commit()
        installationPrefs.edit().clear().commit()
        sessions = EncryptedAuthSessionStore(sessionPrefs)
        sessions.saveSession(AuthSession(User("Private name", backendUserId = 17), "private-jwt"))
        installations = EncryptedInstallationStateStore(installationPrefs)
        val binding = installations.prepare(17, "login")
        installations.confirm(binding)
        visibility = VisibleConversationStore()
        authorize = AuthorizeMessageNotificationUseCase(sessions, installations, NotificationClock { 1000 })
        val publisher = AndroidMessageNotificationPublisher(context, authorize)
        receiver = ConsumerMessageReceiver(MessageNotificationDecoder(), ReceiveMessageNotificationUseCase(
            authorize, visibility, publisher, StoredNotificationEvents(installationPrefs), publisher,
        ))
        payload = mapOf(
            "version" to "1", "event_id" to "message:51:17", "type" to "conversation.message.created",
            "resource_type" to "conversation", "destination" to "conversation", "resource_id" to "42",
            "recipient_user_id" to "17", "recipient_app" to "consumer", "installation_id" to binding.identity.id,
            "binding_id" to binding.id, "title" to "Nuevo mensaje", "body" to "Tenés un nuevo mensaje en LoResuelvo.",
            "expires_at" to "2099-01-01T00:00:00Z",
        )
        manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(true)
    }

    @Test fun publishes_private_notification_and_immutable_explicit_conversation_intent() {
        assertEquals(MessageNotificationOutcome.Published, receiver.receive(payload))
        val notification = shadowOf(manager).allNotifications.single()
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertNotNull(notification.publicVersion)
        assertNotEquals(0, notification.smallIcon.resId)
        val pending = shadowOf(notification.contentIntent)
        val intent = pending.savedIntent
        assertTrue(pending.flags and android.app.PendingIntent.FLAG_IMMUTABLE != 0)
        assertEquals("com.loresuelvo.consumer.MainActivity", intent.component?.className)
        assertEquals(42, MessageNotificationNavigation(authorize).conversationId(intent))
        assertEquals(setOf("version", "event_id", "type", "resource_type", "destination", "resource_id",
            "recipient_user_id", "recipient_app", "installation_id", "binding_id", "expires_at"), intent.extras!!.keySet())
        assertFalse(intent.extras.toString().contains("private-jwt"))
        sessions.clearSession()
        assertNull(MessageNotificationNavigation(authorize).conversationId(intent))
    }

    @Test fun invalid_expired_foreign_or_private_payload_never_publishes() {
        for ((field, value) in listOf("version" to "2", "resource_id" to "0", "recipient_user_id" to "29",
            "recipient_app" to "provider", "binding_id" to "e805f061-0f6d-4e16-8f41-605c4707bf89",
            "expires_at" to "1970-01-01T00:00:00Z", "body" to "Private message and address")) {
            assertEquals(MessageNotificationOutcome.Invalid, receiver.receive(payload + (field to value)))
        }
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test fun same_conversation_suppressed_and_event_deduplicated_after_receiver_recreation() {
        visibility.show(42)
        assertEquals(MessageNotificationOutcome.VisibleConversation, receiver.receive(payload))
        visibility.show(99)
        assertEquals(MessageNotificationOutcome.Published, receiver.receive(payload))
        val events = StoredNotificationEvents(context.getSharedPreferences("message_installations", 0))
        val publisher = AndroidMessageNotificationPublisher(context, authorize)
        val recreated = ConsumerMessageReceiver(MessageNotificationDecoder(), ReceiveMessageNotificationUseCase(
            authorize, VisibleConversationStore(), publisher, events, publisher,
        ))
        assertEquals(MessageNotificationOutcome.Duplicate, recreated.receive(payload))
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test fun rejected_publication_does_not_consume_redelivery() {
        var publicationAllowed = false
        val preferences = context.getSharedPreferences("message_installations", 0)
        val useCase = ReceiveMessageNotificationUseCase(authorize, visibility, NotificationAvailability { true },
            StoredNotificationEvents(preferences), MessageNotificationPublisher { publicationAllowed })
        val notification = MessageNotificationDecoder().decode(payload)!!.toDomain()
        assertEquals(MessageNotificationOutcome.Unavailable, useCase(notification))
        publicationAllowed = true
        assertEquals(MessageNotificationOutcome.Published, useCase(notification))
    }

    @Test fun extended_year_does_not_escape_decoder() {
        assertEquals(MessageNotificationOutcome.Invalid, receiver.receive(payload + ("expires_at" to "+1000000000-12-31T23:59:59Z")))
    }

    @Test fun malformed_notification_intent_is_rejected_before_navigation() {
        val notification = MessageNotificationDecoder().decode(payload)!!.toDomain()
        val wrongType = MessageNotificationIntent().create(context, notification).putExtra("resource_id", "42")
        assertNull(MessageNotificationNavigation(authorize).conversationId(wrongType))
        val unreadable = io.mockk.mockk<android.content.Intent>()
        io.mockk.every { unreadable.action } returns MessageNotificationIntent.ACTION
        io.mockk.every { unreadable.getStringExtra(any()) } throws android.os.BadParcelableException("Invalid parcel")
        assertNull(MessageNotificationIntent().read(unreadable))
    }

    @Test fun disabled_app_notifications_are_respected() {
        shadowOf(manager).setNotificationsEnabled(false)
        assertEquals(MessageNotificationOutcome.Unavailable, receiver.receive(payload))
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test fun disabled_message_channel_returns_unavailable() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = android.app.NotificationChannel(
                AndroidMessageNotificationPublisher.CHANNEL,
                "Mensajes",
                NotificationManager.IMPORTANCE_NONE,
            )
            manager.createNotificationChannel(channel)
            assertEquals(MessageNotificationOutcome.Unavailable, receiver.receive(payload))
            assertTrue(shadowOf(manager).allNotifications.isEmpty())
        }
    }
}

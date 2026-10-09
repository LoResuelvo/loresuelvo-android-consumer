package com.loresuelvo.consumer.platform.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.loresuelvo.consumer.domain.installation.PushRegistrationTokenProvider
import com.loresuelvo.consumer.domain.installation.PushTokenOutcome
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

@Singleton
class FirebaseRegistrationTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : PushRegistrationTokenProvider {
    override suspend fun token(): PushTokenOutcome {
        return try {
            if (getOrInitializeFirebaseApp() == null) return PushTokenOutcome.ConfigurationUnavailable
            withTimeout(15_000) { awaitToken() }
        } catch (error: TimeoutCancellationException) {
            PushTokenOutcome.Failure(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            PushTokenOutcome.Failure(error)
        }
    }

    fun initializeForReception(): Boolean {
        return try { getOrInitializeFirebaseApp() != null }
        catch (_: IllegalArgumentException) { false }
        catch (_: IllegalStateException) { false }
    }

    private fun getOrInitializeFirebaseApp(): FirebaseApp? =
        FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(context)

    private suspend fun awaitToken(): PushTokenOutcome = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (continuation.isActive) continuation.resume(PushTokenOutcome.Available(token))
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resume(PushTokenOutcome.Failure(error))
            }
    }

}

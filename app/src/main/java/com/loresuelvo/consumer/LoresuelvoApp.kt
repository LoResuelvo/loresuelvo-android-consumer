package com.loresuelvo.consumer

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp
import com.loresuelvo.consumer.platform.notifications.RegistrationRuntime
import javax.inject.Inject
import com.loresuelvo.consumer.platform.notifications.FirebaseRegistrationTokenProvider
import com.loresuelvo.consumer.platform.notifications.NavigationIntentDispatcher

/**
 * Application class for the consumer app.
 *
 * Annotated with `@HiltAndroidApp` to trigger Hilt's code generation,
 * including a base class for the application that serves as the
 * application-level dependency container. Without this annotation:
 *
 * - `@AndroidEntryPoint` Activities would not work.
 * - `@HiltViewModel` ViewModels would not work.
 * - `@HiltAndroidTest` instrumented tests would fail to bootstrap.
 *
 * Registered in `AndroidManifest.xml` via `android:name=".LoresuelvoApp"`.
 *
 * The [navControllerSink] is a tiny bridge for the App Link /
 * deep-link entry point: when [MainActivity.onNewIntent] is invoked
 * (warm start from a Custom Tab redirect), it emits the new
 * `Intent` into the sink so the Compose [androidx.navigation.NavHostController]
 * inside [com.loresuelvo.consumer.ui.navigation.LoResuelvoNav] can
 * re-issue `handleDeepLink(...)`. Using a process-scoped sink avoids
 * `MainActivity` needing a back-reference to a `NavController` that
 * is created inside the composable tree.
 */
@HiltAndroidApp
class LoresuelvoApp : Application() {
    @Inject lateinit var registrationRuntime: RegistrationRuntime

    @Inject lateinit var firebaseTokens: FirebaseRegistrationTokenProvider
    @Inject lateinit var navigationIntents: NavigationIntentDispatcher

    override fun onCreate() {
        super.onCreate()
        firebaseTokens.initializeForReception()
        registrationRuntime.start()
    }

    val navControllerEvents get() = navigationIntents.events

    fun dispatchNavigationIntent(intent: Intent) { navigationIntents.dispatch(intent) }
}

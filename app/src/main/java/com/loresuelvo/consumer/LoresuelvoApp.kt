package com.loresuelvo.consumer

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

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

    private val navControllerChannel = Channel<Intent>(Channel.BUFFERED)

    val navControllerEvents = navControllerChannel.receiveAsFlow()

    fun dispatchNavigationIntent(intent: Intent) {
        navControllerChannel.trySend(intent)
    }
}

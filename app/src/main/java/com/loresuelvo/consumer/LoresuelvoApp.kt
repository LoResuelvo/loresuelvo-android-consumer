package com.loresuelvo.consumer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

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
 */
@HiltAndroidApp
class LoresuelvoApp : Application()

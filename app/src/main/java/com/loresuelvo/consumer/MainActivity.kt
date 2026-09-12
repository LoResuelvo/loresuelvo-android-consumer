package com.loresuelvo.consumer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.loresuelvo.consumer.ui.navigation.LoResuelvoNav
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host for the consumer app.
 *
 * `WindowCompat.setDecorFitsSystemWindows(window, false)` opts into
 * the modern Android edge-to-edge layout model: the OS does NOT
 * resize the activity when the IME comes up. Instead, Compose
 * receives the keyboard height via `WindowInsets.ime`, and the
 * `imePadding()` modifier on the chat's input column pushes the
 * composer above the keyboard. Without this, the default
 * `adjustResize` model makes `WindowInsets.ime` report 0 (the OS
 * already consumed the inset), so `imePadding()` is a no-op and
 * the keyboard visually overlaps the input bar.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            LoresuelvoTheme {
                LoResuelvoNav()
            }
        }

        // Cold start: the app was opened directly by an external
        // App Link / deep link. The intent is dispatched to the
        // application-level channel and will be consumed by
        // LoResuelvoNav once its collector is ready.
        val app = applicationContext as? LoresuelvoApp

        intent?.let {
            app?.dispatchNavigationIntent(it)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        val app = applicationContext as? LoresuelvoApp
        app?.dispatchNavigationIntent(intent)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
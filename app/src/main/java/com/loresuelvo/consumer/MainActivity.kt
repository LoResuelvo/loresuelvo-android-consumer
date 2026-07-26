package com.loresuelvo.consumer

import android.os.Bundle
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
        setContent { LoresuelvoTheme { LoResuelvoNav() } }
    }
}

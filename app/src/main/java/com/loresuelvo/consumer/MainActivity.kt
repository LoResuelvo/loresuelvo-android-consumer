package com.loresuelvo.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.loresuelvo.consumer.ui.navigation.LoResuelvoNav
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LoresuelvoTheme { LoResuelvoNav() } }
    }
}

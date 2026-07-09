package com.loresuelvo.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.loresuelvo.consumer.ui.navigation.LoResuelvoNav

/**
 * Single Activity. Holds the composition root and delegates the
 * entire UI tree to [LoResuelvoNav]. Per AGENTS.md's "Topología"
 * rule, this class stays at ≤ 12 lines: it must only call
 * `setContent { LoResuelvoNav() }`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LoResuelvoNav() }
    }
}

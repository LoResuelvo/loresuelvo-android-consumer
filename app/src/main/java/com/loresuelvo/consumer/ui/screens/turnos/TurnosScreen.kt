package com.loresuelvo.consumer.ui.screens.turnos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R

/**
 * State-driven surface for the "Mis Turnos" screen
 * (`Route.Turnos`).
 *
 * Landed minimally for scenario 01-VT: the screen renders only
 * the top app bar with the "Mis Turnos" title plus a centred
 * placeholder body (no list, no error surface yet). The full
 * Loading spinner, empty / Ready list and Error branches arrive
 * with scenarios 02-VT..14-VT.
 *
 * Compose testTags are exported as `TURNOS_*` constants so the
 * instrumented suite can target each branch without depending
 * on the localised copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnosScreen(
    state: TurnosUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(TURNOS_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.turnos_title),
                        modifier = Modifier.testTag(TURNOS_TITLE_TAG),
                    )
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Landed minimally: the only branch today is
            // Loading. Scenarios 02-VT..14-VT extend this `when`
            // with Ready(items), Ready(empty) and Error
            // surfaces.
            when (state) {
                is TurnosUiState.Loading -> Unit
            }
        }
    }
}

const val TURNOS_SCREEN_TAG: String = "turnos-screen"
const val TURNOS_TITLE_TAG: String = "turnos-title"

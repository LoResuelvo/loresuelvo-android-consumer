package com.loresuelvo.consumer.ui.screens.turnos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.turno.Turno

/**
 * State-driven surface for the "Mis Turnos" screen
 * (`Route.Turnos`).
 *
 * Landed minimally for scenario 02-VT: the screen renders the
 * top app bar plus the Loading + Ready(non-empty) branches. The
 * Ready(empty) branch (scenario 03-VT) and the Error branch
 * (scenarios 13-VT / 14-VT) arrive in their own commits.
 *
 * The Ready branch renders a `LazyColumn` of plain `Text`
 * rows. The rich `TurnoCard` (avatar + status badge +
 * formatted date) lands with scenario 04-VT.
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
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is TurnosUiState.Loading -> LoadingState()
                is TurnosUiState.Ready -> ReadyList(
                    turnos = state.turnos,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(TURNOS_LOADING_TAG))
        Text(
            text = stringResource(R.string.turnos_loading),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ReadyList(turnos: List<Turno>) {
    // Landed minimally for scenario 02-VT: a plain LazyColumn
    // of full-name `Text` rows. The rich `TurnoCard` (avatar,
    // status badge, formatted date) lands with scenario 04-VT.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TURNOS_LIST_TAG),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = turnos, key = { it.id }) { turno ->
            Text(
                text = "${turno.counterpart.name} ${turno.counterpart.surname}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag(TURNOS_ROW_TAG_PREFIX + turno.id),
            )
        }
    }
}

const val TURNOS_SCREEN_TAG: String = "turnos-screen"
const val TURNOS_TITLE_TAG: String = "turnos-title"
const val TURNOS_LOADING_TAG: String = "turnos-loading"
const val TURNOS_LIST_TAG: String = "turnos-list"
const val TURNOS_ROW_TAG_PREFIX: String = "turnos-row-"

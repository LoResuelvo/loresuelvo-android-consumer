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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.turno.Turno
import com.loresuelvo.consumer.ui.components.turnocard.TurnoCard

/**
 * State-driven surface for the "Mis Turnos" screen
 * (`Route.Turnos`).
 *
 * Landed incrementally per scenario:
 *  - 01-VT → Loading branch + top app bar.
 *  - 02-VT → Ready(non-empty) branch with a list of [TurnoCard]s.
 *  - 03-VT → Ready(empty) branch with the empty-state copy.
 *
 * The Error branch (scenarios 13-VT / 14-VT) lands in its own
 * commit.
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
                is TurnosUiState.Ready ->
                    if (state.turnos.isEmpty()) EmptyState() else ReadyList(state.turnos)
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
private fun EmptyState() {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .testTag(TURNOS_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.turnos_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.turnos_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReadyList(turnos: List<Turno>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TURNOS_LIST_TAG),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = turnos, key = { it.id }) { turno ->
            TurnoCard(
                turno = turno,
                onCardClicked = { /* post-MVP detail (12-VT) */ },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

const val TURNOS_SCREEN_TAG: String = "turnos-screen"
const val TURNOS_TITLE_TAG: String = "turnos-title"
const val TURNOS_LOADING_TAG: String = "turnos-loading"
const val TURNOS_LIST_TAG: String = "turnos-list"
const val TURNOS_EMPTY_TAG: String = "turnos-empty"

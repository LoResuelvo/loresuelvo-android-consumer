package com.loresuelvo.consumer.ui.screens.professional

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.domain.provider.Provider
import com.loresuelvo.consumer.ui.professional.ProfessionalsUiState
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Stateless screen for the single-category provider list. The shape
 * is dictated entirely by [ProfessionalsUiState]; no business logic
 * lives here.
 *
 *   Loading -> circular progress
 *   Ready   -> LazyColumn of [ProviderCard]
 *   Empty   -> explanatory copy + back action
 *   Error   -> retry copy + [onRetryClick] callback
 *
 * Navigation args (categoryId, categoryName) are owned by
 * `LoResuelvoNav.ProfessionalsRoute`; this composable receives only
 * the [state] slice + retry callback.
 */
@Composable
fun ProfessionalsScreen(
    state: ProfessionalsUiState,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = state.categoryName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(16.dp))

        when (state) {
            is ProfessionalsUiState.Loading -> LoadingView()
            is ProfessionalsUiState.Ready -> ReadyList(state.providers)
            is ProfessionalsUiState.Empty -> EmptyView(state.categoryName)
            is ProfessionalsUiState.Error -> ErrorView(onRetryClick)
        }
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ReadyList(providers: List<Provider>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items = providers, key = { it.id }) { provider ->
            ProviderCard(provider = provider)
        }
    }
}

@Composable
private fun EmptyView(categoryName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.professionals_empty_title, categoryName),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.professionals_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun ErrorView(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.professionals_error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.professionals_error_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = SubtitleGray,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRetryClick) {
            Text(text = stringResource(R.string.professionals_retry))
        }
    }
}

@Composable
private fun ProviderCard(provider: Provider) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderAvatar(provider)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${provider.name} ${provider.surname}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = provider.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SubtitleGray,
                )
            }
        }
    }
}

@Composable
private fun ProviderAvatar(provider: Provider) {
    val size = 48.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = provider.name.firstOrNull()?.uppercase() ?: "?",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true, name = "Professionals · Ready")
@Composable
private fun ProfessionalsReadyPreview() {
    LoresuelvoTheme {
        ProfessionalsScreen(
            state = ProfessionalsUiState.Ready(
                categoryName = "Electricidad",
                providers = listOf(
                    Provider(
                        id = 92,
                        name = "Agustina",
                        surname = "Molina",
                        categoryId = 2,
                        categoryName = "Electricidad",
                        profilePhotoUrl = null,
                    ),
                    Provider(
                        id = 32,
                        name = "Agustina",
                        surname = "Ruiz",
                        categoryId = 2,
                        categoryName = "Electricidad",
                        profilePhotoUrl = null,
                    ),
                ),
            ),
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Professionals · Empty")
@Composable
private fun ProfessionalsEmptyPreview() {
    LoresuelvoTheme {
        ProfessionalsScreen(
            state = ProfessionalsUiState.Empty(categoryName = "Gas"),
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Professionals · Error")
@Composable
private fun ProfessionalsErrorPreview() {
    LoresuelvoTheme {
        ProfessionalsScreen(
            state = ProfessionalsUiState.Error(categoryName = "Electricidad"),
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Professionals · Loading")
@Composable
private fun ProfessionalsLoadingPreview() {
    LoresuelvoTheme {
        ProfessionalsScreen(
            state = ProfessionalsUiState.Loading(categoryName = "Electricidad"),
            onRetryClick = {},
        )
    }
}

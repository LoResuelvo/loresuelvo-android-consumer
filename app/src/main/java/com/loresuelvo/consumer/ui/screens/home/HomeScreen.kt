package com.loresuelvo.consumer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.screens.home.components.ActiveRequest
import com.loresuelvo.consumer.ui.screens.home.components.ActiveRequestsSection
import com.loresuelvo.consumer.ui.screens.home.components.AiSearchBar
import com.loresuelvo.consumer.ui.screens.home.components.CategoryGrid
import com.loresuelvo.consumer.ui.screens.home.components.HomeHeader
import com.loresuelvo.consumer.ui.screens.home.components.RecentDiagnosesEmpty
import com.loresuelvo.consumer.ui.screens.home.components.SectionTitle
import com.loresuelvo.consumer.ui.theme.LoresuelvoTheme

/**
 * Home screen for the authenticated consumer. The layout follows
 * the "value-first" principle: greeting + AI-driven entry point on
 * top, the category grid (the primary conversion action) in the
 * middle, and below-fold secondary information (active requests,
 * recent diagnoses).
 *
 * Stateless: every visible value is sourced from [HomeUiState] and
 * every user action is delegated to a callback. The hosting
 * `LoResuelvoNav.HomeRoute` wires navigation and the
 * `HomeViewModel`'s state.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    displayName: String?,
    activeRequests: List<ActiveRequest> = emptyList(),
    onCategoryClick: (categoryId: Int, categoryName: String) -> Unit,
    onNotificationsClick: () -> Unit,
    onAiSendClick: () -> Unit,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    // Active requests list comes from the caller (HomeViewModel once
    // `/requests` exists). Empty by default today, which surfaces the
    // empty-state copy "No tienes ninguna solicitud en curso."

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        HomeHeader(
            displayName = displayName,
            onNotificationsClick = onNotificationsClick,
        )

        AiSearchBar(onSendClick = onAiSendClick)

        SectionTitle(
            text = stringResource(R.string.home_section_categories),
            link = stringResource(R.string.home_section_categories_link),
        )

        CategorySection(
            state = state.categories,
            onCategoryClick = onCategoryClick,
            onRetryClick = onRetryClick,
        )

        SectionTitle(
            text = stringResource(R.string.home_section_requests),
            link = stringResource(R.string.home_section_requests_link),
        )
        ActiveRequestsSection(requests = activeRequests)

        Text(
            text = stringResource(R.string.home_section_diagnoses),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        RecentDiagnosesEmpty()

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_logout),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun CategorySection(
    state: CategoriesState,
    onCategoryClick: (categoryId: Int, categoryName: String) -> Unit,
    onRetryClick: () -> Unit,
) {
    when (state) {
        CategoriesState.Loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        is CategoriesState.Ready -> CategoryGrid(
            categories = state.items,
            onCategoryClick = onCategoryClick,
            modifier = Modifier
                .height(280.dp)
                .fillMaxWidth(),
        )

        CategoriesState.Error -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.welcome_categories_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = onRetryClick) {
                Text(text = stringResource(R.string.professionals_retry))
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenReadyPreview() {
    LoresuelvoTheme {
        HomeScreen(
            state = HomeUiState.Ready(
                categories = CategoriesState.Ready(
                    listOf(
                        com.loresuelvo.consumer.domain.category.Category(1, "Plomería"),
                        com.loresuelvo.consumer.domain.category.Category(2, "Gasista"),
                        com.loresuelvo.consumer.domain.category.Category(3, "Electricista"),
                        com.loresuelvo.consumer.domain.category.Category(4, "Climatización"),
                        com.loresuelvo.consumer.domain.category.Category(5, "Pintura"),
                        com.loresuelvo.consumer.domain.category.Category(6, "Albañilería"),
                    ),
                ),
            ),
            displayName = "Matias",
            activeRequests = listOf(
                ActiveRequest(
                    title = "Fuga en lavamanos",
                    time = "Hoy 14:30",
                    status = "En camino",
                    proName = "Carlos M.",
                    proInitial = "C",
                    rating = 4.9,
                    reviewCount = 120,
                ),
            ),
            onCategoryClick = { _, _ -> },
            onNotificationsClick = {},
            onAiSendClick = {},
            onRetryClick = {},
            onLogoutClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenLoadingPreview() {
    LoresuelvoTheme {
        HomeScreen(
            state = HomeUiState.Loading(),
            displayName = "Matias",
            onCategoryClick = { _, _ -> },
            onNotificationsClick = {},
            onAiSendClick = {},
            onRetryClick = {},
            onLogoutClick = {},
        )
    }
}

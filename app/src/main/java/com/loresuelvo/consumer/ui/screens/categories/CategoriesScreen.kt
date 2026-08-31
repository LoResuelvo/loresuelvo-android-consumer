package com.loresuelvo.consumer.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.R
import com.loresuelvo.consumer.ui.screens.home.CategoriesState
import com.loresuelvo.consumer.ui.screens.home.components.CategoryGrid
import com.loresuelvo.consumer.ui.theme.SubtitleGray

/**
 * Stateless "all categories" screen reached from the Home
 * "Ver todas" link (scenario 02-UXUI). Reuses the Home
 * [CategoryGrid] without the height cap and exposes a search
 * bar above the grid. Tapping a category forwards
 * `(id, name)` to [onCategoryClick].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onCategoryClick: (categoryId: Int, categoryName: String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CategoriesTopBar(onBackClick = onBackClick) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            CategoriesSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
            )
            Box(modifier = Modifier.fillMaxSize()) {
                when (val categories = state.categories) {
                    CategoriesState.Loading -> LoadingState()
                    is CategoriesState.Ready -> if (categories.items.isEmpty()) {
                        EmptyState()
                    } else {
                        CategoryGrid(
                            categories = categories.items,
                            onCategoryClick = onCategoryClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag(CATEGORIES_GRID_TAG),
                        )
                    }
                    CategoriesState.Error -> ErrorState(
                        messageResId = (state as? CategoriesUiState.Error)?.messageResId
                            ?: R.string.categories_error_body,
                        onRetryClick = onRetryClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.categories_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(
                        R.string.categories_back_content_description,
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun CategoriesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(CATEGORIES_SEARCH_FIELD_TAG),
        placeholder = { Text(text = stringResource(R.string.categories_search_hint)) },
        leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.testTag(CATEGORIES_SEARCH_CLEAR_TAG),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(
                            R.string.categories_search_clear_content_description,
                        ),
                    )
                }
            }
        },
        singleLine = true,
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(CATEGORIES_LOADING_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.categories_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = SubtitleGray,
            )
        }
    }
}

/**
 * Shared "non-Ready" surface used both for "no categories
 * published" and "search returned no matches". The two share the
 * same visual treatment today; splitting them is a follow-up if
 * the copy diverges.
 */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag(CATEGORIES_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.categories_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.categories_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )
    }
}

@Composable
private fun ErrorState(
    messageResId: Int,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag(CATEGORIES_ERROR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.categories_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleGray,
        )
        Button(
            onClick = onRetryClick,
            modifier = Modifier
                .padding(top = 12.dp)
                .testTag(CATEGORIES_RETRY_TAG),
        ) {
            Text(text = stringResource(R.string.categories_retry))
        }
    }
}

const val CATEGORIES_GRID_TAG: String = "categories-grid"
const val CATEGORIES_LOADING_TAG: String = "categories-loading"
const val CATEGORIES_EMPTY_TAG: String = "categories-empty"
const val CATEGORIES_ERROR_TAG: String = "categories-error"
const val CATEGORIES_RETRY_TAG: String = "categories-retry"
const val CATEGORIES_SEARCH_FIELD_TAG: String = "categories-search-field"
const val CATEGORIES_SEARCH_CLEAR_TAG: String = "categories-search-clear"

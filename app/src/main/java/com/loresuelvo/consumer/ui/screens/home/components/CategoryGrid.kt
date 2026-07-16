package com.loresuelvo.consumer.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loresuelvo.consumer.domain.category.Category

/**
 * 3-column grid of [CategoryGridItem]s. Each item carries its
 * categoryId + categoryName to the parent's click handler; the grid
 * itself is layout-only (stateless).
 */
@Composable
fun CategoryGrid(
    categories: List<Category>,
    onCategoryClick: (categoryId: Int, categoryName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(items = categories, key = { it.id }) { category ->
            CategoryGridItem(
                name = category.name,
                icon = categoryIconRes(category),
                onClick = { onCategoryClick(category.id, category.name) },
            )
        }
    }
}

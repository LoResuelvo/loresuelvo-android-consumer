package com.loresuelvo.consumer.ui.screens.categories

data class CategoriesScreenActions(
    val onCategoryClick: (categoryId: Int, categoryName: String) -> Unit = { _, _ -> },
    val onSearchQueryChange: (String) -> Unit = {},
    val onBackClick: () -> Unit = {},
    val onRetryClick: () -> Unit = {},
)

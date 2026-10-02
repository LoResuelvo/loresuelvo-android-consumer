package com.loresuelvo.consumer.bdd.fixes

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository

class FakeCategoryRepository(
    initial: CategoriesOutcome = CategoriesOutcome.Success(DEFAULT_CATEGORIES),
) : CategoryRepository {

    private var nextOutcome: CategoriesOutcome = initial

    fun enqueue(outcome: CategoriesOutcome) {
        nextOutcome = outcome
    }

    override suspend fun getCategories(): CategoriesOutcome = nextOutcome

    companion object {
        val DEFAULT_CATEGORIES: List<Category> = listOf(
            Category(id = 1, name = "Albañilería"),
            Category(id = 2, name = "Carpintería"),
            Category(id = 3, name = "Climatización"),
            Category(id = 4, name = "Electricidad"),
            Category(id = 5, name = "Gas"),
            Category(id = 6, name = "Herrería"),
            Category(id = 7, name = "Jardinería"),
            Category(id = 8, name = "Pintura"),
            Category(id = 9, name = "Plomería"),
        )
    }
}

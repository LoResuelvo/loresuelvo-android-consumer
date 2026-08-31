package com.loresuelvo.consumer.bdd.fixes

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository

/**
 * Test-only [CategoryRepository] for the `ux_ui_fixes` BDD spec.
 * Lets the world seed a deterministic category list (or a
 * failure) per scenario. Defaults to a representative 9-item
 * set so scenarios that forget to seed still land on a
 * non-empty Ready state.
 */
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

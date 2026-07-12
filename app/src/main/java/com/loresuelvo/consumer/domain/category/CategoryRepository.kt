package com.loresuelvo.consumer.domain.category

/**
 * Port for reading the platform's service categories. Implemented in
 * `data/api/ApiCategoryRepository.kt` against the backend's public
 * `GET /categories` endpoint. No Android, no Retrofit, no JSON —
 * just the contract the use cases depend on. Implementations must
 * not throw on HTTP / network failures; they translate to a
 * [CategoriesOutcome].
 */
interface CategoryRepository {

    suspend fun getCategories(): CategoriesOutcome
}

package com.loresuelvo.consumer.domain.usecase.category

import com.loresuelvo.consumer.domain.category.CategoriesOutcome
import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.category.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [GetCategoriesUseCase]. The use case is a thin
 * orchestrator: it must delegate to the port and propagate the typed
 * outcome verbatim, without swallowing or reshaping failures.
 */
class GetCategoriesUseCaseTest {

    private val repository = mockk<CategoryRepository>()
    private val useCase = GetCategoriesUseCase(repository)

    @Test
    fun delegates_to_repository_and_returns_success() = runTest {
        val categories = listOf(Category(1, "Plomería"), Category(2, "Electricidad"))
        coEvery { repository.getCategories() } returns CategoriesOutcome.Success(categories)

        val outcome = useCase()

        assertTrue(outcome is CategoriesOutcome.Success)
        assertEquals(categories, (outcome as CategoriesOutcome.Success).categories)
        coVerify(exactly = 1) { repository.getCategories() }
    }

    @Test
    fun propagates_network_failure() = runTest {
        coEvery { repository.getCategories() } returns
            CategoriesOutcome.Failure.Network(IOException("dns"))

        val outcome = useCase()

        assertTrue(outcome is CategoriesOutcome.Failure.Network)
    }

    @Test
    fun propagates_server_failure() = runTest {
        coEvery { repository.getCategories() } returns
            CategoriesOutcome.Failure.Server(503, "unavailable")

        val outcome = useCase()

        assertTrue(outcome is CategoriesOutcome.Failure.Server)
        assertEquals(503, (outcome as CategoriesOutcome.Failure.Server).code)
    }
}

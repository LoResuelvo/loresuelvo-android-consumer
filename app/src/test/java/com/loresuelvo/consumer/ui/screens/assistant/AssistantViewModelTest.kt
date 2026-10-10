package com.loresuelvo.consumer.ui.screens.assistant

import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary
import com.loresuelvo.consumer.domain.usecase.assistant.GetAiConversationsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val getConversations = mockk<GetAiConversationsUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun conversations_are_sorted_by_latest_activity_and_can_be_filtered() = runTest {
        coEvery { getConversations() } returns AiConversationListOutcome.Success(
            listOf(
                AiConversationSummary("old", "Pintura", 10L, "Pared"),
                AiConversationSummary("new", "Electricidad", 30L, "Cortocircuito"),
                AiConversationSummary("middle", "Plomería", 20L, "Canilla"),
            ),
        )

        val viewModel = AssistantViewModel(getConversations)
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as AssistantUiState.Ready
        assertEquals(listOf("new", "middle", "old"), loaded.conversations.map { it.id })

        viewModel.onSearchQueryChange("plom")

        val filtered = viewModel.uiState.value as AssistantUiState.Ready
        assertEquals(listOf("middle"), filtered.conversations.map { it.id })
        assertEquals(3, filtered.totalConversations)
        assertEquals("plom", filtered.searchQuery)
    }

    @Test
    fun empty_search_result_keeps_ready_state_with_total_count() = runTest {
        coEvery { getConversations() } returns AiConversationListOutcome.Success(
            listOf(AiConversationSummary("1", "Pintura", 10L)),
        )

        val viewModel = AssistantViewModel(getConversations)
        advanceUntilIdle()
        viewModel.onSearchQueryChange("electricidad")

        val filtered = viewModel.uiState.value as AssistantUiState.Ready
        assertTrue(filtered.conversations.isEmpty())
        assertEquals(1, filtered.totalConversations)
    }
}

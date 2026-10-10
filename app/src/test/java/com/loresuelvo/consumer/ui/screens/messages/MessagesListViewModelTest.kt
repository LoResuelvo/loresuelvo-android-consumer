package com.loresuelvo.consumer.ui.screens.messages

import com.loresuelvo.consumer.domain.conversation.Conversation
import com.loresuelvo.consumer.domain.conversation.ConversationCounterpart
import com.loresuelvo.consumer.domain.conversation.ConversationStatus
import com.loresuelvo.consumer.domain.conversation.ConversationsOutcome
import com.loresuelvo.consumer.domain.usecase.conversation.GetConversationsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
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
class MessagesListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCase = mockk<GetConversationsUseCase>()
    private lateinit var viewModel: MessagesListViewModel

    private fun sampleConversation(
        id: String = "1",
        providerName: String = "Juan",
        providerSurname: String = "Gómez",
        updatedOn: Long = 0L,
    ) = Conversation(
        id = id,
        status = ConversationStatus.Pending,
        counterpart = ConversationCounterpart(
            id = 20L,
            name = providerName,
            surname = providerSurname,
            categoryName = "Plomería",
            profilePhotoUrl = null,
        ),
        lastMessage = null,
        updatedOnEpochMillis = updatedOn,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_is_Loading_then_transitions_to_Ready_with_the_list() = runTest {
        val conversations = listOf(
            sampleConversation(id = "1", providerName = "Juan", providerSurname = "Gómez"),
            sampleConversation(id = "2", providerName = "Pedro", providerSurname = "Dib"),
        )
        coEvery { useCase() } returns ConversationsOutcome.Success(conversations)

        viewModel = MessagesListViewModel(useCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "expected Ready, was $state",
            state is MessagesListUiState.Ready,
        )
        assertEquals(
            conversations,
            (state as MessagesListUiState.Ready).conversations,
        )
        coVerify(exactly = 1) { useCase() }
    }

    @Test
    fun success_with_empty_list_transitions_to_Ready_with_emptyList() = runTest {
        coEvery { useCase() } returns ConversationsOutcome.Success(emptyList())

        viewModel = MessagesListViewModel(useCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MessagesListUiState.Ready)
        assertEquals(
            emptyList<Conversation>(),
            (state as MessagesListUiState.Ready).conversations,
        )
    }

    @Test
    fun success_sorts_by_latest_activity_and_search_filters_by_provider() = runTest {
        coEvery { useCase() } returns ConversationsOutcome.Success(
            listOf(
                sampleConversation(
                    id = "older",
                    providerName = "Juan",
                    updatedOn = 10L,
                ),
                sampleConversation(
                    id = "newer",
                    providerName = "Pedro",
                    updatedOn = 20L,
                ),
            ),
        )

        viewModel = MessagesListViewModel(useCase)
        advanceUntilIdle()
        assertEquals(
            listOf("newer", "older"),
            (viewModel.uiState.value as MessagesListUiState.Ready).conversations.map { it.id },
        )

        viewModel.onSearchQueryChange("Pedro")

        val filtered = viewModel.uiState.value as MessagesListUiState.Ready
        assertEquals(listOf("newer"), filtered.conversations.map { it.id })
        assertEquals(2, filtered.totalConversations)
        assertEquals("Pedro", filtered.searchQuery)
    }

    @Test
    fun failure_Network_transitions_to_Error_carrying_the_failure() = runTest {
        val cause = IOException("dns")
        coEvery { useCase() } returns ConversationsOutcome.Failure.Network(cause)

        viewModel = MessagesListViewModel(useCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "expected Error, was $state",
            state is MessagesListUiState.Error,
        )
        val failure = (state as MessagesListUiState.Error).failure
        assertTrue(failure is ConversationsOutcome.Failure.Network)
        assertTrue(
            (failure as ConversationsOutcome.Failure.Network).cause === cause,
        )
    }

    @Test
    fun failure_Server_transitions_to_Error_carrying_code_and_message() = runTest {
        coEvery { useCase() } returns
            ConversationsOutcome.Failure.Server(code = 503, message = "unavailable")

        viewModel = MessagesListViewModel(useCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MessagesListUiState.Error)
        val failure = (state as MessagesListUiState.Error).failure
        assertTrue(failure is ConversationsOutcome.Failure.Server)
        failure as ConversationsOutcome.Failure.Server
        assertEquals(503, failure.code)
        assertEquals("unavailable", failure.message)
    }

    @Test
    fun failure_Unauthorized_transitions_to_Error_carrying_message() = runTest {
        coEvery { useCase() } returns
            ConversationsOutcome.Failure.Unauthorized("token expired")

        viewModel = MessagesListViewModel(useCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MessagesListUiState.Error)
        val failure = (state as MessagesListUiState.Error).failure
        assertTrue(failure is ConversationsOutcome.Failure.Unauthorized)
        assertEquals(
            "token expired",
            (failure as ConversationsOutcome.Failure.Unauthorized).message,
        )
    }

    @Test
    fun load_re_fires_use_case_after_failure() = runTest {
        // First call fails, then the consumer hits retry (or pull-
        // to-refresh once that lands). The VM must dispatch a
        // fresh use-case invocation.
        coEvery { useCase() } returns
            ConversationsOutcome.Failure.Server(500, "boom") andThen
            ConversationsOutcome.Success(listOf(sampleConversation(id = "99")))

        viewModel = MessagesListViewModel(useCase)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is MessagesListUiState.Error)

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MessagesListUiState.Ready)
        assertEquals(
            listOf("99"),
            (state as MessagesListUiState.Ready).conversations.map { it.id },
        )
        coVerify(exactly = 2) { useCase() }
    }
}

package com.loresuelvo.consumer.ui.screens.home

import com.loresuelvo.consumer.domain.category.Category
import com.loresuelvo.consumer.domain.assistant.AiConversationSummary
import com.loresuelvo.consumer.domain.assistant.AiConversationListOutcome
import com.loresuelvo.consumer.domain.serviceproposal.ServiceProposal
import com.loresuelvo.consumer.domain.turno.Turno

sealed interface HomeUiState {

    val categories: CategoriesState
    val pendingServiceProposals: ServiceProposalsState
    val upcomingServiceProposals: ServiceProposalsState
    val awaitingPaymentTurnos: TurnosState
    val turnos: TurnosState
    val recentAiConversations: AiConversationsState

    data class Loading(
        override val categories: CategoriesState = CategoriesState.Loading,
        override val pendingServiceProposals: ServiceProposalsState = ServiceProposalsState.Loading,
        override val upcomingServiceProposals: ServiceProposalsState = ServiceProposalsState.Loading,
        override val awaitingPaymentTurnos: TurnosState = TurnosState.Loading,
        override val turnos: TurnosState = TurnosState.Loading,
        override val recentAiConversations: AiConversationsState = AiConversationsState.Loading,
    ) : HomeUiState

    data class Ready(
        override val categories: CategoriesState,
        override val pendingServiceProposals: ServiceProposalsState,
        override val upcomingServiceProposals: ServiceProposalsState,
        override val awaitingPaymentTurnos: TurnosState,
        override val turnos: TurnosState,
        override val recentAiConversations: AiConversationsState = AiConversationsState.Loading,
    ) : HomeUiState

    data class Error(
        override val categories: CategoriesState = CategoriesState.Error,
        val messageResId: Int,
        override val pendingServiceProposals: ServiceProposalsState = ServiceProposalsState.Error,
        override val upcomingServiceProposals: ServiceProposalsState = ServiceProposalsState.Error,
        override val awaitingPaymentTurnos: TurnosState = TurnosState.Error,
        override val turnos: TurnosState = TurnosState.Error,
        override val recentAiConversations: AiConversationsState = AiConversationsState.Loading,
    ) : HomeUiState
}

sealed interface CategoriesState {
    data object Loading : CategoriesState
    data class Ready(val items: List<Category>) : CategoriesState
    data object Error : CategoriesState
}

sealed interface ServiceProposalsState {
    data object Loading : ServiceProposalsState
    data class Ready(val items: List<ServiceProposal>) : ServiceProposalsState
    data object Error : ServiceProposalsState
}

sealed interface TurnosState {
    data object Loading : TurnosState
    data class Ready(val items: List<Turno>) : TurnosState
    data object Error : TurnosState
}

sealed interface AiConversationsState {
    data object Loading : AiConversationsState
    data class Ready(val items: List<AiConversationSummary>) : AiConversationsState
    data class Error(val failure: AiConversationListOutcome.Failure) : AiConversationsState
}

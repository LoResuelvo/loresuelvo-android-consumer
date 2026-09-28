package com.loresuelvo.consumer.ui.screens.providerprofile

import com.loresuelvo.consumer.domain.provider.ProviderProfile
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome

sealed interface ProviderProfileUiState {
    data object Loading : ProviderProfileUiState
    data class Ready(val profile: ProviderProfile) : ProviderProfileUiState
    data class Error(val failure: ProviderProfileOutcome.Failure) : ProviderProfileUiState
}

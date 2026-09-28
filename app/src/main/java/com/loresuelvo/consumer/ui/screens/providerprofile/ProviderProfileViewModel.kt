package com.loresuelvo.consumer.ui.screens.providerprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.provider.ProviderProfileOutcome
import com.loresuelvo.consumer.domain.usecase.provider.GetProviderProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProviderProfileViewModel @Inject constructor(
    private val getProviderProfile: GetProviderProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProviderProfileUiState>(ProviderProfileUiState.Loading)
    val uiState: StateFlow<ProviderProfileUiState> = _uiState.asStateFlow()

    fun load(providerId: Int) {
        viewModelScope.launch {
            _uiState.update { ProviderProfileUiState.Loading }
            _uiState.update {
                when (val outcome = getProviderProfile(providerId)) {
                    is ProviderProfileOutcome.Success -> ProviderProfileUiState.Ready(outcome.profile)
                    is ProviderProfileOutcome.Failure -> ProviderProfileUiState.Error(outcome)
                }
            }
        }
    }
}

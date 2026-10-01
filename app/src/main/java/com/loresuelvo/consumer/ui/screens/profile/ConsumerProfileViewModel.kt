package com.loresuelvo.consumer.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loresuelvo.consumer.domain.auth.CurrentUserOutcome
import com.loresuelvo.consumer.domain.usecase.auth.GetConsumerProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ConsumerProfileViewModel @Inject constructor(
    private val getConsumerProfile: GetConsumerProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConsumerProfileUiState>(ConsumerProfileUiState.Loading)
    val uiState: StateFlow<ConsumerProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ConsumerProfileUiState.Loading
            when (val outcome = getConsumerProfile()) {
                is CurrentUserOutcome.Success ->
                    _uiState.value = ConsumerProfileUiState.Ready(outcome.user)
                else ->
                    _uiState.value = ConsumerProfileUiState.Error(
                        outcome.toConsumerProfileFailure()!!,
                    )
            }
        }
    }
}

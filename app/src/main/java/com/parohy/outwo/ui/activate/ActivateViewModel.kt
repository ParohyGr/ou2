package com.parohy.outwo.ui.activate

import androidx.lifecycle.SavedStateHandle
import com.parohy.outwo.factory.CardSpecificViewModel
import com.parohy.outwo.scratch.MainScope
import com.parohy.outwo.scratch.core.*
import com.parohy.outwo.scratch.repo.CardsRepository
import com.parohy.outwo.scratch.repo.ScratchCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivateUiState(
  val card: State<Throwable, ScratchCard>? = null,
  val activate: State<Throwable, Unit>? = null
)

@HiltViewModel
class ActivateViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  cardsRepository: CardsRepository,
  @MainScope viewModelScope: CoroutineScope
): CardSpecificViewModel(savedStateHandle, cardsRepository, viewModelScope) {
  private val _uiState: MutableStateFlow<ActivateUiState> = MutableStateFlow(ActivateUiState())
  val uiState: StateFlow<ActivateUiState> = _uiState

  init {
    viewModelScope.launch {
      cardState.collect { card ->
        _uiState.value = _uiState.value.copy(card = card)
      }
    }

    viewModelScope.launch {
      cardsRepository.getData().collect {
        _uiState.value = _uiState.value.copy(activate = it.activation)
      }
    }
  }

  fun activateCard() = cardsRepository.activateCard(cardCode)

  fun clearActivationState() = cardsRepository.resetActivate()

  override fun onCleared() {
    clearActivationState()
  }
}
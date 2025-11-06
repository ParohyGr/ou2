package com.parohy.outwo.ui.scratch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.parohy.outwo.factory.CardSpecificViewModel
import com.parohy.outwo.scratch.core.*
import com.parohy.outwo.scratch.MainScope
import com.parohy.outwo.scratch.repo.CardsRepository
import com.parohy.outwo.scratch.repo.ScratchCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScratchUiState(
  val card: State<Throwable, ScratchCard>? = null,
  val scratch: State<Throwable, Unit>? = null
)

@HiltViewModel
class ScratchViewModel @Inject constructor(
  savedState: SavedStateHandle,
  cardsRepository: CardsRepository,
  @MainScope viewModelScope: CoroutineScope
): CardSpecificViewModel(savedState, cardsRepository, viewModelScope) {
  private val _uiState: MutableStateFlow<ScratchUiState> = MutableStateFlow(ScratchUiState())
  val uiState: StateFlow<ScratchUiState> = _uiState

  init {
    viewModelScope.launch {
      cardState.collect { card ->
        _uiState.value = _uiState.value.copy(card = card)
      }
    }
  }

  fun scratchCard() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(scratch = Loading)
      _uiState.value = try {
        cardsRepository.scratchCard(cardCode)
        _uiState.value.copy(scratch = SUCCESS)
      } catch (e: Exception) {
        _uiState.value.copy(scratch = Failure(e))
      }
    }
  }

  fun clearScratchState() {
    _uiState.value = _uiState.value.copy(scratch = null)
  }

  override fun onCleared() {
    clearScratchState()
  }
}

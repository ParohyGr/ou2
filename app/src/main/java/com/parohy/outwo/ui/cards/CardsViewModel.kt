package com.parohy.outwo.ui.cards

import androidx.lifecycle.*
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


data class CardsUiState(
  val cards: State<Throwable, List<ScratchCard>>? = null,
  val generate: State<Throwable, Unit>? = null
)

@HiltViewModel
class CardsViewModel @Inject constructor(
  private val savedState: SavedStateHandle,
  private val cardsRepository: CardsRepository,
  @MainScope viewModelScope: CoroutineScope
): ViewModel(viewModelScope) {
  private val _uiState: MutableStateFlow<CardsUiState> =
    MutableStateFlow(CardsUiState(cards = savedState.getCards()))
  val uiState: StateFlow<CardsUiState> = _uiState

  init {
    viewModelScope.launch {
      cardsRepository.getData().collect { data ->
        val cards = data.cards?.map { it.values.toList() }
        _uiState.value = _uiState.value.copy(cards = cards)
        cards.ifContent { value ->
          savedState["cards"] =
            value.joinToString(separator = "-") { "${it.code};${it.isScratched};${it.isActivated}" }
        }
      }
    }
  }

  fun loadCards() {
    viewModelScope.launch {
      cardsRepository.loadCards()
    }
  }

  fun generateCard() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(generate = Loading)
      _uiState.value = try {
        cardsRepository.generateCard()
        _uiState.value.copy(generate = SUCCESS)
      } catch (e: Throwable) {
        e.printStackTrace()
        _uiState.value.copy(generate = Failure(RuntimeException("Failed to generate card", e)))
      }
    }
  }

  fun clearGenerateState() {
    _uiState.value = _uiState.value.copy(generate = null)
  }

  fun removeCard(code: String) = cardsRepository.removeCard(code)
}

private fun SavedStateHandle.getCards(): State<Throwable, List<ScratchCard>>? {
  val savedCards = get<String>("cards")?.split("-")?.map {
    val (code, isScratched, isActive) = it.split(";")
    ScratchCard(code, isScratched.toBoolean(), isActive.toBoolean())
  }
  return savedCards?.let { Content(it) }
}
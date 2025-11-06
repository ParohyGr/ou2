package com.parohy.outwo.factory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.parohy.outwo.scratch.core.*
import com.parohy.outwo.scratch.repo.CardsRepository
import com.parohy.outwo.scratch.repo.ScratchCard
import com.parohy.outwo.ui.NAV_ARG_CARD_CODE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

open class CardSpecificViewModel(
  private val savedState: SavedStateHandle,
  protected val cardsRepository: CardsRepository,
  viewModelScope: CoroutineScope
): ViewModel(viewModelScope) {
  private val card: MutableStateFlow<State<Throwable, ScratchCard>?> = MutableStateFlow(savedState.getCard())
  val cardState: StateFlow<State<Throwable, ScratchCard>?> = card

  protected val cardCode = savedState.get<String>(NAV_ARG_CARD_CODE) ?: throw java.lang.IllegalStateException("NAV_ARG_CARD_CODE not found")

  init {
    viewModelScope.launch {
      cardsRepository.getData().map { state ->
        try {
          state.cards?.map { it.values.toList() }?.map { cards ->
            cards.isNotEmpty() || throw IllegalStateException("There are no cards")
            cards.find { it.code == cardCode } ?: throw IllegalArgumentException("Card $cardCode not found")
          } ?: throw IllegalStateException("Cards state not loaded")
        } catch (e: Exception) {
          Failure(e)
        }
      }.collectLatest { cardState ->
        card.value = cardState

        cardState.ifContent { value ->
          savedState["card"] = "${value.code};${value.isScratched}"
        }
      }
    }
  }

  private fun SavedStateHandle.getCard(): State<Throwable, ScratchCard>? {
    val savedCard = get<String>("card")?.let {
      val (code, isScratched) = it.split(";")
      ScratchCard(code, isScratched.toBoolean())
    }
    return savedCard?.let(::Content)
  }
}
package com.parohy.outwo.scratch.repo

import com.parohy.outwo.scratch.IoScope
import com.parohy.outwo.scratch.core.*
import com.parohy.outwo.scratch.database.CardsDao
import com.parohy.outwo.scratch.network.ApiService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import javax.inject.Inject

private const val MIN_RESULT_INT = 277028

internal class CardsRepositoryImpl @Inject internal constructor(
  private val db: CardsDao,
  private val apiService: ApiService,
  @IoScope private val coroutineScope: CoroutineScope
): CardsRepository {
  private val _data: MutableStateFlow<CardRepositoryState> = MutableStateFlow(CardRepositoryState())
  override fun getData(): Flow<CardRepositoryState> = _data

  private val cardsMap: Map<String, ScratchCard> get() = (_data.value.cards.valueOrNull ?: emptyMap())

  override suspend fun loadCards() {
    if (_data.value.cards.isLoading) return

    withContext(Dispatchers.IO) {
      if (!_data.value.cards.isContent) //here could handle refresh state
        _data.value = _data.value.copy(cards = Loading)

      delay(2000) //artificial delay...
      _data.value = try {
        _data.value.copy(cards = Content(db.loadCards().associateBy(ScratchCard::code)))
      } catch (e: Exception) {
        _data.value.copy(cards = Failure(RuntimeException("Failed to load cards", e)))
      }
    }
  }

  override suspend fun generateCard() {
    withContext(Dispatchers.IO) {
      delay(2000)
      val uuid = UUID.randomUUID().toString()
      val newCards = cardsMap + (uuid to ScratchCard(uuid))
      db.insertCards(*newCards.values.toTypedArray())
      _data.value = _data.value.copy(cards = Content(newCards))
    }
  }

  override suspend fun scratchCard(code: String) {
    withContext(Dispatchers.IO) {
      delay(2000)
      val cardToScratch = cardsMap[code]?.copy(isScratched = true) ?: throw IllegalArgumentException("Card $code not found")
      val newCards = cardsMap + (code to cardToScratch)
      db.insertCards(*newCards.values.toTypedArray())
      _data.value = _data.value.copy(cards = Content(newCards))
    }
  }

  override fun activateCard(code: String) {
    if (_data.value.activation.isLoading) return

    coroutineScope.launch {
      _data.value = _data.value.copy(activation = Loading)

      try {
        cardsMap.isNotEmpty() || throw IllegalStateException("Cards state not loaded")
        cardsMap.containsKey(code) || throw IllegalArgumentException("Card $code not found")

        val androidVersion = apiService.activateVersion(code)

        val result =
          if (androidVersion.version.toInt() > MIN_RESULT_INT)
            Result.success(Unit)
          else
            Result.failure(IllegalStateException("Failed to activate card!"))

        val activatedCard = cardsMap[code]?.copy(isActivated = result.isSuccess) ?: throw IllegalArgumentException("Card $code not found")
        val newCards = cardsMap + (code to activatedCard)

        db.insertCards(*newCards.values.toTypedArray())
        _data.value = _data.value.copy(activation = result.toState(), cards = Content(newCards))
      } catch (e: Exception) {
        _data.value = _data.value.copy(activation = Failure(e))
      }
    }
  }

  override fun resetActivate() {
    _data.value = _data.value.copy(activation = null)
  }

  override fun removeCard(code: String) {
    if (!cardsMap.containsKey(code))
      throw IllegalArgumentException("Card $code not found")

    val newCards = cardsMap - code
    coroutineScope.launch {
      db.insertCards(*newCards.values.toTypedArray())
    }
    _data.value = _data.value.copy(cards = Content(newCards))
  }
}
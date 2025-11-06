package com.parohy.outwo.scratch.repo

import kotlinx.coroutines.flow.Flow

interface CardsRepository {
  suspend fun loadCards()
  suspend fun generateCard()
  suspend fun scratchCard(code: String)
  fun activateCard(code: String)
  fun resetActivate()
  fun removeCard(code: String)
  fun getData(): Flow<CardRepositoryState>
}
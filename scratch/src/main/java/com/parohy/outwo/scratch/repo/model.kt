package com.parohy.outwo.scratch.repo

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.parohy.outwo.scratch.core.State

@Entity
data class ScratchCard(
  @PrimaryKey val code: String,
  val isScratched: Boolean = false,
  val isActivated: Boolean = false
)

data class CardRepositoryState(
  val cards: State<Throwable, Map<String, ScratchCard>>? = null,
  val activation: State<Throwable, Unit>? = null
)
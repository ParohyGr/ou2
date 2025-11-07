package com.parohy.outwo.scratch.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.parohy.outwo.scratch.repo.ScratchCard

@Dao
internal interface CardsDao {
  @Query("SELECT * FROM ScratchCard")
  suspend fun loadCards(): List<ScratchCard>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCards(vararg card: ScratchCard)

  @Delete
  suspend fun deleteCard(card: ScratchCard)
}
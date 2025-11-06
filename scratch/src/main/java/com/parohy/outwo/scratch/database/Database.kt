package com.parohy.outwo.scratch.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.parohy.outwo.scratch.repo.ScratchCard

@Database(
  entities = [ScratchCard::class],
  version = 1
)
abstract class Database: RoomDatabase() {
  abstract fun cardsDao(): CardsDao
}
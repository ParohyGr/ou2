package com.parohy.outwo.scratch.database

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): Database {
    return Room.databaseBuilder(
      context,
      Database::class.java,
      "scratch-database"
    ).build()
  }

  @Provides
  fun provideCardsDao(database: Database): CardsDao = database.cardsDao()
}
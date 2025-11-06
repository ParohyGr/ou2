package com.parohy.outwo.scratch

import com.parohy.outwo.scratch.repo.CardsRepository
import com.parohy.outwo.scratch.repo.CardsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ScratchModule {
  @Binds
  @Singleton
  abstract fun bindCardsRepository(cardsRepositoryImpl: CardsRepositoryImpl): CardsRepository
}
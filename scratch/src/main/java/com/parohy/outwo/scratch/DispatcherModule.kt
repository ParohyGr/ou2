package com.parohy.outwo.scratch

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoScope

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

  @Provides
  @MainScope
  fun provideMainCoroutineScope(): CoroutineScope = CoroutineScope( Dispatchers.Main + SupervisorJob())

  @Provides
  @IoScope
  fun provideIoCoroutineScope(): CoroutineScope = CoroutineScope( Dispatchers.IO + SupervisorJob())
}
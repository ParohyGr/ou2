package com.parohy.outwo.scratch.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private const val API_ENDPOINT = "https://api.o2.sk/"

@Module
@InstallIn(SingletonComponent::class)
internal class NetworkModule {
  // 1. Provides the Retrofit instance
  @Singleton
  @Provides
  internal fun provideRetrofit(): Retrofit {
    return Retrofit.Builder()
      .baseUrl(API_ENDPOINT)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
  }

  // 2. Provides the MyApiService implementation
  @Singleton
  @Provides
  internal fun provideApiService(retrofit: Retrofit): ApiService {
    // Hilt automatically injects the Retrofit instance provided above
    return retrofit.create(ApiService::class.java)
  }
}
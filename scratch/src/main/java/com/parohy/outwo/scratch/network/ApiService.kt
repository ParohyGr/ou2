package com.parohy.outwo.scratch.network

import retrofit2.http.GET
import retrofit2.http.Query

internal interface ApiService {
  @GET("/version")
  suspend fun activateVersion(@Query("code") code: String): AndroidVersion
}

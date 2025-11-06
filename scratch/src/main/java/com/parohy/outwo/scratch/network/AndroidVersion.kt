package com.parohy.outwo.scratch.network

import com.google.gson.annotations.SerializedName

internal data class AndroidVersion(
  @SerializedName("android") val version: String
)
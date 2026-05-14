package com.lena.kartoshka.network

import com.google.gson.annotations.SerializedName

data class ApiErrorBody(
    val error: ApiErrorDetails
)

data class ApiErrorDetails(
    val code: String,
    val message: String,
    @SerializedName("request_id") val requestId: String? = null,
    val detail: String? = null
)

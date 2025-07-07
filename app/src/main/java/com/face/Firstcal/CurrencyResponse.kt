package com.face.Firstcal

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

data class QuotaResponse(
    val result: String,
    val documentation: String,
    val terms_of_use: String,
    val plan_quota: Int,
    val requests_remaining: Int,
    val refresh_day_of_month: Int
)

data class CurrencyResponse(
    val result: String,
    val base_code: String,
    val conversion_rates: Map<String, Double>
)

interface CurrencyApiService {
    // Get quota info
    @GET("v6/{apiKey}/quota")
    suspend fun getQuota(@Path("apiKey") apiKey: String): Response<QuotaResponse>

    // Get latest exchange rates with all currencies
    @GET("v6/{apiKey}/latest/{base}")
    suspend fun getRates(
        @Path("apiKey") apiKey: String,
        @Path("base") base: String
    ): Response<CurrencyResponse>
}

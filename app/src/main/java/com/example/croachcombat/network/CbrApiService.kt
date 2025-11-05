package com.example.croachcombat.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface CbrApiService {
    @GET("scripts/XML_daily.asp")
    suspend fun getDailyRates(): ValCurs

    companion object {
        private const val BASE_URL = "https://www.cbr.ru/scripts/"

        fun create(): CbrApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()
                .create(CbrApiService::class.java)
        }
    }
}
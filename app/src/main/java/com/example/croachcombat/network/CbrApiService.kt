package com.example.croachcombat.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

interface CbrApiService {

    @GET("xml_metall.asp")
    suspend fun getMetalRates(
        @Query("date_req1") dateReq1: String,
        @Query("date_req2") dateReq2: String
    ): MetalRates

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

        fun getTodayDate(): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return dateFormat.format(Date())
        }

        fun getYesterdayDate(): String {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return dateFormat.format(calendar.time)
        }
    }
}
package com.example.finfit.health.repository

import com.example.finfit.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object HealthNewsRepository {

    private val api: AirtableApi
    private var cachedRecords: List<AirtableRecord>? = null

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.airtable.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(AirtableApi::class.java)
    }

    suspend fun getNewsRecords(forceRefresh: Boolean = false): List<AirtableRecord> {
        if (!forceRefresh && cachedRecords != null) {
            return cachedRecords!!
        }
        val response = api.getRecords(
            baseId = BuildConfig.AIRTABLE_BASE_ID,
            tableId = BuildConfig.AIRTABLE_TABLE_ID,
            authHeader = "Bearer ${BuildConfig.AIRTABLE_PAT}",
            filterByFormula = "{Status} = 'Published'"
        )
        cachedRecords = response.records
        return response.records
    }
}

package com.example.finfit.health.repository

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// --- Models ---

data class AirtableResponse(
    val records: List<AirtableRecord>
)

data class AirtableRecord(
    val id: String,
    val createdTime: String,
    val fields: AirtableFields
)

data class AirtableFields(
    @SerializedName("Title") val title: String? = null,
    @SerializedName("Content") val content: String? = null,
    @SerializedName("Media") val media: List<AirtableImage>? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("IsFeatured") val isFeatured: Boolean? = false,
    @SerializedName("Excerpt") val excerpt: String? = null,
    @SerializedName("Category") val category: String? = null
)

data class AirtableImage(
    val id: String,
    val url: String,
    val filename: String
)

// --- Retrofit API ---

interface AirtableApi {
    @GET("v0/{baseId}/{tableId}")
    suspend fun getRecords(
        @Path("baseId") baseId: String,
        @Path("tableId") tableId: String,
        @Header("Authorization") authHeader: String,
        @Query("filterByFormula") filterByFormula: String? = null
    ): AirtableResponse
}

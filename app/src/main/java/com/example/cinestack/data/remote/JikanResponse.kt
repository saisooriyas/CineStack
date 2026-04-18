package com.example.cinestack.data.remote

import com.google.gson.annotations.SerializedName

data class JikanResponse(
    val data: List<AnimeData>
)

data class AnimeData(
    @SerializedName("mal_id") val malId: String,
    val title: String,
    val images: AnimeImages,
    val score: Double?,
    val synopsis: String?,
    val episodes: Int?,
    val year: Int?
)

data class AnimeImages(
    val jpg: AnimeImageDetails
)

data class AnimeImageDetails(
    @SerializedName("large_image_url") val largeImageUrl: String
)

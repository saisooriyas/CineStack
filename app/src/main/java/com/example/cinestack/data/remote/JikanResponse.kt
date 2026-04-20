package com.example.cinestack.data.remote

import com.google.gson.annotations.SerializedName

// ── Search response (/v4/anime?q=...) ─────────────────────────────────────
data class JikanResponse(
    val data: List<AnimeData>
)

// ── Top/Ranking response (/v4/top/anime) — items are wrapped in "node" ────
// Jikan top anime returns: { data: [ { mal_id, title, images, score, ... }, ... ] }
// (Same flat structure as search — no node wrapper in Jikan v4 top endpoint)
// Seasonal response (/v4/seasons/now) also returns same flat structure.
// So we can reuse JikanResponse for all three. No changes needed for top/seasonal.

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
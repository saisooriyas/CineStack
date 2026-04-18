package com.example.cinestack.data.remote

import com.google.gson.annotations.SerializedName

data class PDBResponse(
    val results: List<TMDBMovie>
)

data class PDBMovie(
    val id: Int,
    val title: String?,
    val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    val overview: String,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?
)

data class PBCreditsResponse(
    val cast: List<PDBCast>
)

data class PDBCast(
    val id: Int,
    val name: String,
    val character: String,
    @SerializedName("profile_path") val profilePath: String?
)

data class PDBMovieDetailsResponse(
    val id: Int,
    val runtime: Int?,
    @SerializedName("production_companies") val productionCompanies: List<TMDBProductionCompany>?,
    @SerializedName("original_language") val originalLanguage: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?,
    val seasons: List<TMDBSeason>?
)

data class PDBProductionCompany(
    val id: Int,
    val name: String
)

data class PDBPersonDetails(
    val id: Int,
    val name: String,
    val biography: String,
    @SerializedName("profile_path") val profilePath: String?,
    val birthday: String?,
    val deathday: String?,
    @SerializedName("place_of_birth") val placeOfBirth: String?
)

data class PDBTrendingResponse(
    val results: List<TMDBMovie>
)

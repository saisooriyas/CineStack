package com.example.cinestack.data.remote

import com.google.gson.annotations.SerializedName

data class TMDBResponse(
    val results: List<TMDBMovie>
)

data class TMDBMovie(
    val id: String,
    val title: String?,
    val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    val overview: String,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?
)

data class TMDBCreditsResponse(
    val cast: List<TMDBCast>
)

data class TMDBCast(
    val id: String,
    val name: String,
    val character: String,
    @SerializedName("profile_path") val profilePath: String?
)

data class TMDBMovieDetailsResponse(
    val id: String,
    val runtime: Int?,
    @SerializedName("production_companies") val productionCompanies: List<TMDBProductionCompany>?,
    @SerializedName("original_language") val originalLanguage: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?,
    val seasons: List<TMDBSeason>?
)

data class TMDBSeason(
    val id: String,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("episode_count") val episodeCount: Int,
    val name: String
)

data class TMDBProductionCompany(
    val id: String,
    val name: String
)

data class TMDBPersonDetails(
    val id: String,
    val name: String,
    val biography: String,
    @SerializedName("profile_path") val profilePath: String?,
    val birthday: String?,
    val deathday: String?,
    @SerializedName("place_of_birth") val placeOfBirth: String?,
    @SerializedName("known_for_department") val knownForDepartment: String?,
    @SerializedName("combined_credits") val combinedCredits: TMDBPersonCreditsResponse?
)

data class TMDBPersonCreditsResponse(
    val cast: List<TMDBCombinedCredit>,
    val crew: List<TMDBCombinedCredit>
)

data class TMDBCombinedCredit(
    val id: String,
    val title: String?,
    val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    val character: String?,
    val job: String?,
    val department: String?,
    val overview: String?,
    @SerializedName("vote_average") val voteAverage: Double
)

data class TMDBTrendingResponse(
    val results: List<TMDBMovie>
)

package com.example.cinestack.data.remote

import retrofit2.http.*

interface ApiService {
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = true
    ): TMDBResponse

    @GET("search/tv")
    suspend fun searchTV(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = true
    ): TMDBResponse

    @GET("https://api.jikan.moe/v4/anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): JikanResponse

    @GET("{type}/{id}/credits")
    suspend fun getCredits(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("api_key") apiKey: String
    ): TMDBCreditsResponse

    @GET("{type}/{id}")
    suspend fun getDetails(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("api_key") apiKey: String
    ): TMDBMovieDetailsResponse

    @GET("person/{person_id}")
    suspend fun getPersonDetails(
        @Path("person_id") personId: String,
        @Query("api_key") apiKey: String,
        @Query("append_to_response") appendToResponse: String = "combined_credits"
    ): TMDBPersonDetails

    // ── Movie discovery ───────────────────────────────────────────────────
    @GET("trending/movie/day")
    suspend fun getTrendingMovies(
        @Query("api_key") apiKey: String
    ): TMDBResponse

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String
    ): TMDBResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("api_key") apiKey: String
    ): TMDBResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String
    ): TMDBResponse

    // ── TV discovery ──────────────────────────────────────────────────────
    @GET("trending/tv/day")
    suspend fun getTrendingTV(
        @Query("api_key") apiKey: String
    ): TMDBResponse

    @GET("tv/popular")
    suspend fun getPopularTV(
        @Query("api_key") apiKey: String
    ): TMDBResponse

    @GET("tv/top_rated")
    suspend fun getTopRatedTV(
        @Query("api_key") apiKey: String
    ): TMDBResponse

    @GET("tv/on_the_air")
    suspend fun getOnAirTV(
        @Query("api_key") apiKey: String
    ): TMDBResponse

    // ── Anime discovery via Jikan ─────────────────────────────────────────
    // Top airing anime
    @GET("https://api.jikan.moe/v4/top/anime")
    suspend fun getTopAnime(
        @Query("filter") filter: String = "airing", // airing | upcoming | bypopularity | favorite
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1
    ): JikanResponse

    // Seasonal anime (current season)
    @GET("https://api.jikan.moe/v4/seasons/now")
    suspend fun getCurrentSeasonAnime(
        @Query("limit") limit: Int = 20
    ): JikanResponse

    // ── Recommendations ───────────────────────────────────────────────────
    @GET("{type}/{id}/recommendations")
    suspend fun getRecommendations(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("api_key") apiKey: String
    ): TMDBResponse
}
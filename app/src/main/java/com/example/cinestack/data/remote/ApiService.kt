package com.example.cinestack.data.remote

import retrofit2.http.*

interface ApiService {
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = true
    ): TMDBResponse

    @GET("search/tv")
    suspend fun searchTV(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = true
    ): TMDBResponse

    @GET("https://api.jikan.moe/v4/anime")
    suspend fun searchAnime(
        @Query("q") query: String
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

    @GET("trending/movie/day")
    suspend fun getTrendingMovies(
        @Query("api_key") apiKey: String
    ): TMDBResponse

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("api_key") apiKey: String
    ): TMDBResponse
    @GET("{type}/{id}/recommendations")
    suspend fun getRecommendations(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("api_key") apiKey: String
    ): TMDBResponse
}

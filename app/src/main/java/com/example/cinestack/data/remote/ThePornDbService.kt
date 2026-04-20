package com.example.cinestack.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ThePornDbService {

    // Text search
    @GET("scenes")
    suspend fun searchScenes(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Recently released scenes — used for discovery/trending shelf
    @GET("scenes")
    suspend fun getRecentScenes(
        @Header("Authorization") auth: String,
        @Query("orderBy") orderBy: String = "recently_released",
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Recently released movies on TPDB
    @GET("movies")
    suspend fun getRecentMovies(
        @Header("Authorization") auth: String,
        @Query("orderBy") orderBy: String = "recently_released",
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Search movies
    @GET("movies")
    suspend fun searchPDBMovies(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Scenes filtered by performer using QueryMap
    @GET("scenes")
    suspend fun searchScenesByCast(
        @Header("Authorization") auth: String,
        @QueryMap performers: Map<String, String>,
        @Query("performer_and") performerAnd: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    @GET("movies")
    suspend fun searchMoviessByCast(
        @Header("Authorization") auth: String,
        @QueryMap performers: Map<String, String>,
        @Query("performer_and") performerAnd: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Dedicated performer scenes endpoint — most reliable for single performer
    @GET("performers/{identifier}/scenes")
    suspend fun getPerformerScenes(
        @Header("Authorization") auth: String,
        @Path("identifier") identifier: String,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    @GET("performers/{identifier}/movies")
    suspend fun getPerformerMovies(
        @Header("Authorization") auth: String,
        @Path("identifier") identifier: String,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    @GET("performers")
    suspend fun searchPerformers(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("per_page") limit: Int = 10
    ): PDBPerformerSearchResponse
}

// ── Response models ───────────────────────────────────────────────────────────

data class PDBRestResponse(
    val count: Int?,
    val data: List<PDBRestScene>
)

data class PDBPerformerSearchResponse(
    val data: List<PDBPerformerResult>
)

data class PDBPerformerResult(
    val id   : String,
    val name : String,
    val image: String?
)

data class PDBRestScene(
    val id: String,
    val title: String,
    val date: String?,
    val duration: Int?,
    val background: PDBBackground?,
    val posters: PDBPosters?,
    val site: PDBRestSite?,
    val performers: List<PDBRestPerformer>?,
    val tags: List<PDBRestTag>?
)

data class PDBBackground(
    val full: String?,
    val medium: String?
)

data class PDBPosters(
    val full: String?,
    val medium: String?
)

data class PDBRestSite(val name: String?)
data class PDBRestPerformer(val name: String?)
data class PDBRestTag(val name: String?)
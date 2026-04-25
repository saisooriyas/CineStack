package com.example.cinestack.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ThePornDbService {

    // Text search — Scenes
    @GET("scenes")
    suspend fun searchScenes(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Text search — Movies
    @GET("movies")
    suspend fun searchPDBMovies(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Recently released scenes
    @GET("scenes")
    suspend fun getRecentScenes(
        @Header("Authorization") auth: String,
        @Query("orderBy") orderBy: String = "recently_released",
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Recently released movies
    @GET("movies")
    suspend fun getRecentMovies(
        @Header("Authorization") auth: String,
        @Query("orderBy") orderBy: String = "recently_released",
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Single performer scenes — most reliable for one performer
    @GET("performers/{identifier}/scenes")
    suspend fun getPerformerScenes(
        @Header("Authorization") auth: String,
        @Path("identifier") identifier: String,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Single performer movies
    @GET("performers/{identifier}/movies")
    suspend fun getPerformerMovies(
        @Header("Authorization") auth: String,
        @Path("identifier") identifier: String,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Multi-performer scene search via QueryMap
    // TPDB expects: performers[ID1]=Name1&performers[ID2]=Name2
    // Retrofit QueryMap encodes keys as-is, so pass "performers[ID]" as the key
    @GET("scenes")
    suspend fun searchScenesByPerformers(
        @Header("Authorization") auth: String,
        @QueryMap(encoded = false) performers: Map<String, String>,
        @Query("performer_and") performerAnd: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Multi-performer movie search via QueryMap
    @GET("movies")
    suspend fun searchMoviesByPerformers(
        @Header("Authorization") auth: String,
        @QueryMap(encoded = false) performers: Map<String, String>,
        @Query("performer_and") performerAnd: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("per_page") limit: Int = 20
    ): PDBRestResponse

    // Performer search
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
    val id   : String,           // UUID — keep for starring
    @SerializedName("_id") val numericId: Int = 0,  // ← ADD THIS
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
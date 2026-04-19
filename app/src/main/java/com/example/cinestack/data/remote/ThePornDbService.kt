package com.example.cinestack.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ThePornDbService {

    // Text search — ?parse= matches against title
    @GET("scenes")
    suspend fun searchScenes(
        @Header("Authorization") auth: String,
        @Query("parse") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PDBRestResponse

    // Scenes filtered by one or more performer IDs
    @GET("scenes")
    suspend fun searchScenesByCast(
        @Header("Authorization") auth: String,
        @Query("cast[]") castIds: List<String>,   // API accepts multiple cast[] params
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PDBRestResponse

    @GET("performers")
    suspend fun searchPerformers(
        @Header("Authorization") auth: String,
        @Query("q") query: String
    ): PDBPerformerSearchResponse

}

// ── REST response models ──────────────────────────────────────────────────────

data class PDBRestResponse(
    val count: Int?,           // may be absent
    val data: List<PDBRestScene>  // root array is "data", not "results"
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
    val posters: PDBPosters?,     // it's an object, not a list
    val site: PDBRestSite?,
    val performers: List<PDBRestPerformer>?,  // nullable — some scenes have no performers
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
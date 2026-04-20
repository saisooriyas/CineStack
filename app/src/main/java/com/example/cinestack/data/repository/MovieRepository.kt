package com.example.cinestack.data.repository

import com.example.cinestack.BuildConfig
import com.example.cinestack.data.local.GroupEntity
import com.example.cinestack.data.local.GroupItemEntity
import com.example.cinestack.data.local.MovieDao
import com.example.cinestack.data.local.MovieEntity
import com.example.cinestack.data.model.Movie
import com.example.cinestack.data.remote.ApiService
import com.example.cinestack.data.remote.PDBPerformerResult
import com.example.cinestack.data.remote.PDBRestScene
import com.example.cinestack.data.remote.TMDBCast
import com.example.cinestack.data.remote.TMDBCombinedCredit
import com.example.cinestack.data.remote.TMDBMovie
import com.example.cinestack.data.remote.TMDBMovieDetailsResponse
import com.example.cinestack.data.remote.TMDBPersonDetails
import com.example.cinestack.data.remote.ThePornDbService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MovieRepository(private val movieDao: MovieDao) {

    // ── API keys ──────────────────────────────────────────────────────────────
    private var tmdbApiKey = BuildConfig.TMDB_API_KEY
    private val tpdbApiKey = BuildConfig.TPDB_API_KEY

    fun updateApiKey(newKey: String) {
        if (newKey.isNotBlank()) tmdbApiKey = newKey
    }

    // ── HTTP client (shared) ──────────────────────────────────────────────────
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // ── TMDB + Jikan Retrofit ─────────────────────────────────────────────────
    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // ── ThePornDB Retrofit ────────────────────────────────────────────────────
    private val pdbService: ThePornDbService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.theporndb.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ThePornDbService::class.java)
    }

    // ── Library ───────────────────────────────────────────────────────────────
    private val _library = MutableStateFlow<List<Movie>>(emptyList())
    val library: StateFlow<List<Movie>> = _library.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.getAllMovies().collect { entities ->
                _library.value = entities.map { it.toMovie() }
            }
        }
    }

    fun addToLibrary(movie: Movie, status: String = "", personalScore: Double = 0.0, season: Int = 0, episode: Int = 0) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertMovie(movie.toEntity(status, personalScore, season, episode))
        }
    }

    fun removeFromLibrary(movieId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            _library.value.find { it.id == movieId }?.toEntity()?.let {
                movieDao.deleteMovie(it)
            }
        }
    }

    fun isInLibrary(movieId: String): Boolean =
        _library.value.any { it.id == movieId }

    // ── Groups ────────────────────────────────────────────────────────────────
    private val _allGroups = MutableStateFlow<List<GroupEntity>>(emptyList())
    val allGroups: StateFlow<List<GroupEntity>> = _allGroups.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.getAllGroups().collect { _allGroups.value = it }
        }
    }

    suspend fun getGroupItems(groupId: Int) = movieDao.getGroupItems(groupId)
    suspend fun getSubGroups(parentId: Int)  = movieDao.getSubGroups(parentId)
    suspend fun getGroupById(groupId: Int)   = movieDao.getGroupById(groupId)

    fun createGroup(name: String, parentId: Int? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertGroup(GroupEntity(name = name, parentGroupId = parentId))
        }
    }

    fun deleteGroup(groupId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.getGroupById(groupId)?.let { movieDao.deleteGroup(it) }
        }
    }

    fun addItemToGroup(groupId: Int, movie: Movie) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertGroupItem(GroupItemEntity(
                groupId    = groupId,
                type       = "MOVIE",
                externalId = movie.id,
                title      = movie.title,
                imageUrl   = movie.posterUrl
            ))
        }
    }

    fun addPersonToGroup(groupId: Int, cast: TMDBCast) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertGroupItem(GroupItemEntity(
                groupId    = groupId,
                type       = "PERSON",
                externalId = cast.id,
                title      = cast.name,
                imageUrl   = "https://image.tmdb.org/t/p/w500${cast.profilePath}"
            ))
        }
    }

    fun removeGroupItem(item: GroupItemEntity) {
        CoroutineScope(Dispatchers.IO).launch { movieDao.deleteGroupItem(item) }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    suspend fun searchMovies(query: String, page: Int = 1): List<Movie> {
        val response = apiService.searchMovies(tmdbApiKey, query, page = page)
        return response.results.map { it.toMovie("movie") }
    }

    suspend fun searchTV(query: String, page: Int = 1): List<Movie> {
        val response = apiService.searchTV(tmdbApiKey, query, page = page)
        return response.results.map { it.toMovie("tv") }
    }

    suspend fun searchAnime(query: String, page: Int = 1): List<Movie> {
        val response = apiService.searchAnime(query, page = page)
        return response.data.map { anime ->
            Movie(
                id          = anime.malId,
                title       = anime.title,
                posterUrl   = anime.images.jpg.largeImageUrl,
                backdropUrl = anime.images.jpg.largeImageUrl,
                rating      = anime.score ?: 0.0,
                genre       = listOf("Anime"),
                duration    = "${anime.episodes ?: "?"} eps",
                releaseYear = anime.year ?: 0,
                synopsis    = anime.synopsis ?: "",
                mediaType   = "anime"
            )
        }
    }

    suspend fun searchPDBScenes(query: String, page: Int = 1): List<Movie> {
        return try {
            pdbService.searchScenes(
                auth  = "Bearer $tpdbApiKey",
                query = query,
                page  = page
            ).data.map { it.toMovie() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun searchPerformers(query: String): List<PDBPerformerResult> {
        return try {
            pdbService.searchPerformers(
                auth  = "Bearer $tpdbApiKey",
                query = query
            ).data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchPDBScenesByCast(castIds: List<String>, page: Int = 1): List<Movie> {
        return try {
            if (castIds.size == 1) {
                // Use dedicated performer endpoint for single performer — most reliable
                pdbService.getPerformerScenes(
                    auth       = "Bearer $tpdbApiKey",
                    identifier = castIds.first(),
                    page       = page
                ).data.map { it.toMovie() }
            } else {
                // For multiple performers, build performers QueryMap
                val performersMap = castIds.associate { id -> "performers[$id]" to id }
                pdbService.searchScenesByCast(
                    auth         = "Bearer $tpdbApiKey",
                    performers   = performersMap,
                    performerAnd = false,
                    page         = page
                ).data.map { it.toMovie() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun PDBRestScene.toMovie() = Movie(
        id          = id,
        title       = title,
        posterUrl   = background?.full ?: background?.medium ?: posters?.full ?: posters?.medium ?: "",
        backdropUrl = background?.full ?: "",
        rating      = 0.0,
        genre       = listOf(site?.name ?: "XXX"),
        duration    = duration?.let { "${it / 60} min" } ?: "N/A",
        releaseYear = date?.take(4)?.toIntOrNull() ?: 0,
        synopsis    = buildString {
            val names = performers?.mapNotNull { it.name }?.filter { it.isNotBlank() }
            if (!names.isNullOrEmpty()) append(names.joinToString(", "))
            site?.name?.let { if (isNotEmpty()) append(" · "); append(it) }
        },
        mediaType = "xxx"
    )

    // ── Discovery — Movie ─────────────────────────────────────────────────────
    suspend fun getTrendingMovies(): List<Movie> =
        try { apiService.getTrendingMovies(tmdbApiKey).results.map { it.toMovie("movie") } }
        catch (e: Exception) { emptyList() }

    suspend fun getPopularMovies(): List<Movie> =
        try { apiService.getPopularMovies(tmdbApiKey).results.map { it.toMovie("movie") } }
        catch (e: Exception) { emptyList() }

    suspend fun getNowPlayingMovies(): List<Movie> =
        try { apiService.getNowPlayingMovies(tmdbApiKey).results.map { it.toMovie("movie") } }
        catch (e: Exception) { emptyList() }

    suspend fun getTopRatedMovies(): List<Movie> =
        try { apiService.getTopRatedMovies(tmdbApiKey).results.map { it.toMovie("movie") } }
        catch (e: Exception) { emptyList() }

    // ── Discovery — TV ────────────────────────────────────────────────────────
    suspend fun getTrendingTV(): List<Movie> =
        try { apiService.getTrendingTV(tmdbApiKey).results.map { it.toMovie("tv") } }
        catch (e: Exception) { emptyList() }

    suspend fun getPopularTV(): List<Movie> =
        try { apiService.getPopularTV(tmdbApiKey).results.map { it.toMovie("tv") } }
        catch (e: Exception) { emptyList() }

    suspend fun getTopRatedTV(): List<Movie> =
        try { apiService.getTopRatedTV(tmdbApiKey).results.map { it.toMovie("tv") } }
        catch (e: Exception) { emptyList() }

    suspend fun getOnAirTV(): List<Movie> =
        try { apiService.getOnAirTV(tmdbApiKey).results.map { it.toMovie("tv") } }
        catch (e: Exception) { emptyList() }

    // ── Discovery — Anime ─────────────────────────────────────────────────────
    suspend fun getTopAiringAnime(): List<Movie> =
        try {
            apiService.getTopAnime(filter = "airing", limit = 20).data.map { anime ->
                Movie(
                    id          = anime.malId,
                    title       = anime.title,
                    posterUrl   = anime.images.jpg.largeImageUrl,
                    backdropUrl = anime.images.jpg.largeImageUrl,
                    rating      = anime.score ?: 0.0,
                    genre       = listOf("Anime"),
                    duration    = "${anime.episodes ?: "?"} eps",
                    releaseYear = anime.year ?: 0,
                    synopsis    = anime.synopsis ?: "",
                    mediaType   = "anime"
                )
            }
        } catch (e: Exception) { emptyList() }

    suspend fun getTopAnimeByPopularity(): List<Movie> =
        try {
            apiService.getTopAnime(filter = "bypopularity", limit = 20).data.map { anime ->
                Movie(
                    id          = anime.malId,
                    title       = anime.title,
                    posterUrl   = anime.images.jpg.largeImageUrl,
                    backdropUrl = anime.images.jpg.largeImageUrl,
                    rating      = anime.score ?: 0.0,
                    genre       = listOf("Anime"),
                    duration    = "${anime.episodes ?: "?"} eps",
                    releaseYear = anime.year ?: 0,
                    synopsis    = anime.synopsis ?: "",
                    mediaType   = "anime"
                )
            }
        } catch (e: Exception) { emptyList() }

    // ── Discovery — XXX ───────────────────────────────────────────────────────
    suspend fun getRecentXxxScenes(): List<Movie> =
        try {
            pdbService.getRecentScenes(
                auth    = "Bearer $tpdbApiKey",
                orderBy = "recently_released"
            ).data.map { it.toMovie() }
        } catch (e: Exception) { emptyList() }

    suspend fun getRecentXxxMovies(): List<Movie> =
        try {
            pdbService.getRecentMovies(
                auth    = "Bearer $tpdbApiKey",
                orderBy = "recently_released"
            ).data.map { it.toMovie() }
        } catch (e: Exception) { emptyList() }

    // ── Details / Cast / Recommendations ─────────────────────────────────────
    suspend fun getCast(movieId: String, type: String = "movie"): List<TMDBCast> {
        if (type == "anime" || type == "xxx") return emptyList()
        return try { apiService.getCredits(type, movieId, tmdbApiKey).cast }
        catch (e: Exception) { emptyList() }
    }

    suspend fun getMovieDetails(movieId: String, type: String = "movie"): TMDBMovieDetailsResponse? {
        if (type == "anime" || type == "xxx") return null
        return try { apiService.getDetails(type, movieId, tmdbApiKey) }
        catch (e: Exception) { null }
    }

    suspend fun getPersonDetails(personId: String): TMDBPersonDetails? =
        try { apiService.getPersonDetails(personId, tmdbApiKey) }
        catch (e: Exception) { null }

    suspend fun getRecommendations(movieId: String, type: String = "movie"): List<Movie> {
        if (type == "anime" || type == "xxx") return emptyList()
        return try { apiService.getRecommendations(type, movieId, tmdbApiKey).results.map { it.toMovie(type) } }
        catch (e: Exception) { emptyList() }
    }

    private fun TMDBCombinedCredit.toMovieModel() = Movie(
        id          = id,
        title       = title ?: name ?: "Unknown",
        posterUrl   = "https://image.tmdb.org/t/p/w500$posterPath",
        backdropUrl = "https://image.tmdb.org/t/p/original$backdropPath",
        rating      = voteAverage,
        genre       = listOf(if (mediaType == "tv") "TV Series" else "Movie"),
        duration    = "N/A",
        releaseYear = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull() ?: 0,
        synopsis    = overview ?: "",
        mediaType   = mediaType
    )

    fun mapCombinedCreditToMovie(credit: TMDBCombinedCredit) = credit.toMovieModel()

    // ── Mappers ───────────────────────────────────────────────────────────────
    private fun TMDBMovie.toMovie(type: String = "movie") = Movie(
        id          = id,
        title       = title ?: name ?: "Unknown",
        posterUrl   = "https://image.tmdb.org/t/p/w500$posterPath",
        backdropUrl = "https://image.tmdb.org/t/p/original$backdropPath",
        rating      = voteAverage,
        genre       = listOf(if (type == "tv") "TV Series" else "Movie"),
        duration    = "N/A",
        releaseYear = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull() ?: 0,
        synopsis    = overview,
        mediaType   = type
    )

    private fun MovieEntity.toMovie() = Movie(
        id             = id,
        title          = title,
        posterUrl      = posterUrl,
        backdropUrl    = backdropUrl,
        rating         = rating,
        genre          = genres.split(","),
        duration       = duration,
        releaseYear    = releaseYear,
        synopsis       = synopsis,
        mediaType      = mediaType,
        userStatus     = userStatus,
        userRating     = userRating,
        currentSeason  = currentSeason,
        currentEpisode = currentEpisode,
        totalEpisodes  = totalEpisodes
    )

    private fun Movie.toEntity(
        status: String = userStatus,
        score: Double = userRating,
        season: Int = currentSeason,
        episode: Int = currentEpisode
    ) = MovieEntity(
        id             = id,
        title          = title,
        posterUrl      = posterUrl,
        backdropUrl    = backdropUrl,
        rating         = rating,
        genres         = genre.joinToString(","),
        duration       = duration,
        releaseYear    = releaseYear,
        synopsis       = synopsis,
        mediaType      = mediaType,
        userStatus     = status,
        userRating     = score,
        currentSeason  = season,
        currentEpisode = episode,
        totalEpisodes  = totalEpisodes
    )
}
package com.example.cinestack.data.repository

import com.example.cinestack.BuildConfig
import com.example.cinestack.data.local.GroupEntity
import com.example.cinestack.data.local.GroupItemEntity
import com.example.cinestack.data.local.MovieDao
import com.example.cinestack.data.local.MovieEntity
import com.example.cinestack.data.local.StarredPerformerEntity
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

    private var tmdbApiKey = BuildConfig.TMDB_API_KEY
    private val tpdbApiKey = BuildConfig.TPDB_API_KEY

    fun updateApiKey(newKey: String) {
        if (newKey.isNotBlank()) tmdbApiKey = newKey
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder().addInterceptor(logging).build()
    }

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

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
            movieDao.getAllMovies().collect { _library.value = it.map { e -> e.toMovie() } }
        }
    }

    fun addToLibrary(movie: Movie, status: String = "", personalScore: Double = 0.0, season: Int = 0, episode: Int = 0) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertMovie(movie.toEntity(status, personalScore, season, episode))
        }
    }

    fun removeFromLibrary(movieId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            _library.value.find { it.id == movieId }?.toEntity()?.let { movieDao.deleteMovie(it) }
        }
    }

    fun isInLibrary(movieId: String): Boolean = _library.value.any { it.id == movieId }

    // ── Starred Performers ────────────────────────────────────────────────────
    private val _starredPerformers = MutableStateFlow<List<StarredPerformerEntity>>(emptyList())
    val starredPerformers: StateFlow<List<StarredPerformerEntity>> = _starredPerformers.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.getAllStarredPerformers().collect { _starredPerformers.value = it }
        }
    }

    fun starPerformer(performer: StarredPerformerEntity) {
        CoroutineScope(Dispatchers.IO).launch { movieDao.insertStarredPerformer(performer) }
    }

    fun unstarPerformer(performerId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            _starredPerformers.value.find { it.id == performerId }?.let {
                movieDao.deleteStarredPerformer(it)
            }
        }
    }

    fun isPerformerStarred(performerId: String): Boolean =
        _starredPerformers.value.any { it.id == performerId }

    // ── Groups ────────────────────────────────────────────────────────────────
    private val _allGroups = MutableStateFlow<List<GroupEntity>>(emptyList())
    val allGroups: StateFlow<List<GroupEntity>> = _allGroups.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.getAllGroups().collect { _allGroups.value = it }
        }
    }

    suspend fun getGroupItems(groupId: Int) = movieDao.getGroupItems(groupId)
    suspend fun getSubGroups(parentId: Int) = movieDao.getSubGroups(parentId)
    suspend fun getGroupById(groupId: Int) = movieDao.getGroupById(groupId)

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
            movieDao.insertGroupItem(GroupItemEntity(groupId = groupId, type = "MOVIE",
                externalId = movie.id, title = movie.title, imageUrl = movie.posterUrl))
        }
    }

    fun addPersonToGroup(groupId: Int, cast: TMDBCast) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertGroupItem(GroupItemEntity(groupId = groupId, type = "PERSON",
                externalId = cast.id, title = cast.name,
                imageUrl = "https://image.tmdb.org/t/p/w500${cast.profilePath}"))
        }
    }

    fun removeGroupItem(item: GroupItemEntity) {
        CoroutineScope(Dispatchers.IO).launch { movieDao.deleteGroupItem(item) }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    suspend fun searchMovies(query: String, page: Int = 1): List<Movie> =
        try { apiService.searchMovies(tmdbApiKey, query, page = page).results.map { it.toMovie("movie") } }
        catch (e: Exception) { emptyList() }

    suspend fun searchTV(query: String, page: Int = 1): List<Movie> =
        try { apiService.searchTV(tmdbApiKey, query, page = page).results.map { it.toMovie("tv") } }
        catch (e: Exception) { emptyList() }

    suspend fun searchAnime(query: String, page: Int = 1): List<Movie> =
        try {
            apiService.searchAnime(query, page = page).data.map { anime ->
                Movie(id = anime.malId, title = anime.title, posterUrl = anime.images.jpg.largeImageUrl,
                    backdropUrl = anime.images.jpg.largeImageUrl, rating = anime.score ?: 0.0,
                    genre = listOf("Anime"), duration = "${anime.episodes ?: "?"} eps",
                    releaseYear = anime.year ?: 0, synopsis = anime.synopsis ?: "", mediaType = "anime")
            }
        } catch (e: Exception) { emptyList() }

    suspend fun searchPDBScenes(query: String, page: Int = 1): List<Movie> =
        try { pdbService.searchScenes("Bearer $tpdbApiKey", query, page).data.map { it.toMovie("xxx") } }
        catch (e: Exception) { e.printStackTrace(); emptyList() }

    suspend fun searchPDBMoviesText(query: String, page: Int = 1): List<Movie> =
        try { pdbService.searchPDBMovies("Bearer $tpdbApiKey", query, page).data.map { it.toMovie("xxx_movie") } }
        catch (e: Exception) { e.printStackTrace(); emptyList() }

    suspend fun searchPerformers(query: String): List<PDBPerformerResult> =
        try { pdbService.searchPerformers("Bearer $tpdbApiKey", query).data }
        catch (e: Exception) { emptyList() }

    suspend fun searchPDBScenesByCast(castIds: List<String>, castNames: Map<String, String>, page: Int = 1): List<Movie> {
        return try {
            if (castIds.size == 1) {
                pdbService.getPerformerScenes("Bearer $tpdbApiKey", castIds.first(), page)
                    .data.map { it.toMovie("xxx") }
            } else {
                val map = castIds.associate { id -> "performers[$id]" to (castNames[id] ?: id) }
                pdbService.searchScenesByPerformers("Bearer $tpdbApiKey", map, false, page)
                    .data.map { it.toMovie("xxx") }
            }
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    suspend fun searchPDBMoviesByCast(castIds: List<String>, castNames: Map<String, String>, page: Int = 1): List<Movie> {
        return try {
            if (castIds.size == 1) {
                pdbService.getPerformerMovies("Bearer $tpdbApiKey", castIds.first(), page)
                    .data.map { it.toMovie("xxx_movie") }
            } else {
                val map = castIds.associate { id -> "performers[$id]" to (castNames[id] ?: id) }
                pdbService.searchMoviesByPerformers("Bearer $tpdbApiKey", map, false, page)
                    .data.map { it.toMovie("xxx_movie") }
            }
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // ── Discovery ─────────────────────────────────────────────────────────────
    suspend fun getTrendingMovies(): List<Movie> =
        try { apiService.getTrendingMovies(tmdbApiKey).results.map { it.toMovie("movie") } } catch (e: Exception) { emptyList() }

    suspend fun getPopularMovies(): List<Movie> =
        try { apiService.getPopularMovies(tmdbApiKey).results.map { it.toMovie("movie") } } catch (e: Exception) { emptyList() }

    suspend fun getNowPlayingMovies(): List<Movie> =
        try { apiService.getNowPlayingMovies(tmdbApiKey).results.map { it.toMovie("movie") } } catch (e: Exception) { emptyList() }

    suspend fun getTopRatedMovies(): List<Movie> =
        try { apiService.getTopRatedMovies(tmdbApiKey).results.map { it.toMovie("movie") } } catch (e: Exception) { emptyList() }

    suspend fun getTrendingTV(): List<Movie> =
        try { apiService.getTrendingTV(tmdbApiKey).results.map { it.toMovie("tv") } } catch (e: Exception) { emptyList() }

    suspend fun getPopularTV(): List<Movie> =
        try { apiService.getPopularTV(tmdbApiKey).results.map { it.toMovie("tv") } } catch (e: Exception) { emptyList() }

    suspend fun getTopRatedTV(): List<Movie> =
        try { apiService.getTopRatedTV(tmdbApiKey).results.map { it.toMovie("tv") } } catch (e: Exception) { emptyList() }

    suspend fun getOnAirTV(): List<Movie> =
        try { apiService.getOnAirTV(tmdbApiKey).results.map { it.toMovie("tv") } } catch (e: Exception) { emptyList() }

    suspend fun getTopAiringAnime(): List<Movie> =
        try {
            apiService.getTopAnime(filter = "airing", limit = 20).data.map { anime ->
                Movie(id = anime.malId, title = anime.title, posterUrl = anime.images.jpg.largeImageUrl,
                    backdropUrl = anime.images.jpg.largeImageUrl, rating = anime.score ?: 0.0,
                    genre = listOf("Anime"), duration = "${anime.episodes ?: "?"} eps",
                    releaseYear = anime.year ?: 0, synopsis = anime.synopsis ?: "", mediaType = "anime")
            }
        } catch (e: Exception) { emptyList() }

    suspend fun getTopAnimeByPopularity(): List<Movie> =
        try {
            apiService.getTopAnime(filter = "bypopularity", limit = 20).data.map { anime ->
                Movie(id = anime.malId, title = anime.title, posterUrl = anime.images.jpg.largeImageUrl,
                    backdropUrl = anime.images.jpg.largeImageUrl, rating = anime.score ?: 0.0,
                    genre = listOf("Anime"), duration = "${anime.episodes ?: "?"} eps",
                    releaseYear = anime.year ?: 0, synopsis = anime.synopsis ?: "", mediaType = "anime")
            }
        } catch (e: Exception) { emptyList() }

    suspend fun getRecentXxxScenes(): List<Movie> =
        try { pdbService.getRecentScenes("Bearer $tpdbApiKey", "recently_released").data.map { it.toMovie("xxx") } }
        catch (e: Exception) { emptyList() }

    suspend fun getRecentXxxMovies(): List<Movie> =
        try { pdbService.getRecentMovies("Bearer $tpdbApiKey", "recently_released").data.map { it.toMovie("xxx_movie") } }
        catch (e: Exception) { emptyList() }

    // ── Details ───────────────────────────────────────────────────────────────
    suspend fun getCast(movieId: String, type: String = "movie"): List<TMDBCast> {
        if (type == "anime" || type == "xxx" || type == "xxx_movie") return emptyList()
        return try { apiService.getCredits(type, movieId, tmdbApiKey).cast } catch (e: Exception) { emptyList() }
    }

    suspend fun getMovieDetails(movieId: String, type: String = "movie"): TMDBMovieDetailsResponse? {
        if (type == "anime" || type == "xxx" || type == "xxx_movie") return null
        return try { apiService.getDetails(type, movieId, tmdbApiKey) } catch (e: Exception) { null }
    }

    suspend fun getPersonDetails(personId: String): TMDBPersonDetails? =
        try { apiService.getPersonDetails(personId, tmdbApiKey) } catch (e: Exception) { null }

    suspend fun getRecommendations(movieId: String, type: String = "movie"): List<Movie> {
        if (type == "anime" || type == "xxx" || type == "xxx_movie") return emptyList()
        return try { apiService.getRecommendations(type, movieId, tmdbApiKey).results.map { it.toMovie(type) } }
        catch (e: Exception) { emptyList() }
    }

    fun mapCombinedCreditToMovie(credit: TMDBCombinedCredit) = Movie(
        id = credit.id, title = credit.title ?: credit.name ?: "Unknown",
        posterUrl = "https://image.tmdb.org/t/p/w500${credit.posterPath}",
        backdropUrl = "https://image.tmdb.org/t/p/original${credit.backdropPath}",
        rating = credit.voteAverage, genre = listOf(if (credit.mediaType == "tv") "TV Series" else "Movie"),
        duration = "N/A", releaseYear = (credit.releaseDate ?: credit.firstAirDate)?.take(4)?.toIntOrNull() ?: 0,
        synopsis = credit.overview ?: "", mediaType = credit.mediaType
    )

    // ── Mappers ───────────────────────────────────────────────────────────────
    private fun PDBRestScene.toMovie(type: String = "xxx"): Movie {
        val castList = performers?.mapNotNull { it.name }?.filter { it.isNotBlank() } ?: emptyList()
        return Movie(
            id = id, title = title,
            posterUrl = background?.full ?: background?.medium ?: posters?.full ?: posters?.medium ?: "",
            backdropUrl = background?.full ?: "", rating = 0.0,
            genre = listOf(site?.name ?: if (type == "xxx_movie") "XXX Movie" else "XXX"),
            duration = duration?.let { "${it / 60} min" } ?: "N/A",
            releaseYear = date?.take(4)?.toIntOrNull() ?: 0,
            synopsis = buildString {
                if (castList.isNotEmpty()) append(castList.joinToString(", "))
                site?.name?.let { if (isNotEmpty()) append(" · "); append(it) }
            },
            mediaType = type, castNames = castList
        )
    }

    private fun TMDBMovie.toMovie(type: String = "movie") = Movie(
        id = id, title = title ?: name ?: "Unknown",
        posterUrl = "https://image.tmdb.org/t/p/w500$posterPath",
        backdropUrl = "https://image.tmdb.org/t/p/original$backdropPath",
        rating = voteAverage, genre = listOf(if (type == "tv") "TV Series" else "Movie"),
        duration = "N/A", releaseYear = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull() ?: 0,
        synopsis = overview, mediaType = type
    )

    private fun MovieEntity.toMovie() = Movie(
        id = id, title = title, posterUrl = posterUrl, backdropUrl = backdropUrl,
        rating = rating, genre = genres.split(","), duration = duration,
        releaseYear = releaseYear, synopsis = synopsis, mediaType = mediaType,
        userStatus = userStatus, userRating = userRating,
        currentSeason = currentSeason, currentEpisode = currentEpisode, totalEpisodes = totalEpisodes,
        castNames = if (castNames.isBlank()) emptyList() else castNames.split(",")
    )

    private fun Movie.toEntity(
        status: String = userStatus, score: Double = userRating,
        season: Int = currentSeason, episode: Int = currentEpisode
    ) = MovieEntity(
        id = id, title = title, posterUrl = posterUrl, backdropUrl = backdropUrl,
        rating = rating, genres = genre.joinToString(","), duration = duration,
        releaseYear = releaseYear, synopsis = synopsis, mediaType = mediaType,
        userStatus = status, userRating = score, currentSeason = season,
        currentEpisode = episode, totalEpisodes = totalEpisodes,
        castNames = castNames.joinToString(",")
    )
}
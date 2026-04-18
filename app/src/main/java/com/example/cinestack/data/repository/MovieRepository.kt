package com.example.cinestack.data.repository

import com.example.cinestack.data.model.Movie
import com.example.cinestack.data.remote.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import com.example.cinestack.data.local.MovieDao
import com.example.cinestack.data.local.MovieEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieRepository(private val movieDao: MovieDao) {
    private var apiKey = "201b96b1a333e456ac48450aaf405103"

    fun updateApiKey(newKey: String) {
        if (newKey.isNotBlank()) {
            apiKey = newKey
        }
    }

    private val _library = MutableStateFlow<List<Movie>>(emptyList())
    val library: StateFlow<List<Movie>> = _library.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.getAllMovies().collect { entities ->
                _library.value = entities.map { it.toMovie() }
            }
        }
    }

    // New Group-related StateFlows
    private val _allGroups = MutableStateFlow<List<com.example.cinestack.data.local.GroupEntity>>(emptyList())
    val allGroups: StateFlow<List<com.example.cinestack.data.local.GroupEntity>> = _allGroups.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.getAllGroups().collect { groups ->
                _allGroups.value = groups
            }
        }
    }

    suspend fun getGroupItems(groupId: Int) = movieDao.getGroupItems(groupId)
    suspend fun getSubGroups(parentId: Int) = movieDao.getSubGroups(parentId)
    suspend fun getGroupById(groupId: Int) = movieDao.getGroupById(groupId)

    fun createGroup(name: String, parentId: Int? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertGroup(com.example.cinestack.data.local.GroupEntity(name = name, parentGroupId = parentId))
        }
    }

    fun addItemToGroup(groupId: Int, movie: Movie) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertGroupItem(com.example.cinestack.data.local.GroupItemEntity(
                groupId = groupId,
                type = "MOVIE",
                externalId = movie.id,
                title = movie.title,
                imageUrl = movie.posterUrl
            ))
        }
    }

    fun addPersonToGroup(groupId: Int, cast: com.example.cinestack.data.remote.TMDBCast) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertGroupItem(com.example.cinestack.data.local.GroupItemEntity(
                groupId = groupId,
                type = "PERSON",
                externalId = cast.id,
                title = cast.name,
                imageUrl = "https://image.tmdb.org/t/p/w500${cast.profilePath}"
            ))
        }
    }

    fun addToLibrary(movie: Movie, status: String = "", personalScore: Double = 0.0, season: Int = 0, episode: Int = 0) {
        CoroutineScope(Dispatchers.IO).launch {
            movieDao.insertMovie(movie.toEntity(status, personalScore, season, episode))
        }
    }

    fun removeFromLibrary(movieId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val entity = _library.value.find { it.id == movieId }?.toEntity()
            if (entity != null) {
                movieDao.deleteMovie(entity)
            }
        }
    }

    fun isInLibrary(movieId: Int): Boolean {
        return _library.value.any { it.id == movieId }
    }

    private fun MovieEntity.toMovie() = Movie(
        id = id,
        title = title,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        rating = rating,
        genre = genres.split(","),
        duration = duration,
        releaseYear = releaseYear,
        synopsis = synopsis,
        mediaType = mediaType,
        userStatus = userStatus,
        userRating = userRating,
        currentSeason = currentSeason,
        currentEpisode = currentEpisode,
        totalEpisodes = totalEpisodes
    )

    private fun Movie.toEntity(status: String = userStatus, score: Double = userRating, season: Int = currentSeason, episode: Int = currentEpisode) = MovieEntity(
        id = id,
        title = title,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        rating = rating,
        genres = genre.joinToString(","),
        duration = duration,
        releaseYear = releaseYear,
        synopsis = synopsis,
        mediaType = mediaType,
        userStatus = status,
        userRating = score,
        currentSeason = season,
        currentEpisode = episode,
        totalEpisodes = totalEpisodes
    )

    private val apiService: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    suspend fun searchMovies(query: String): List<Movie> {
        val response = apiService.searchMovies(apiKey, query)
        return response.results.map {
            Movie(
                id = it.id,
                title = it.title ?: it.name ?: "Unknown",
                posterUrl = "https://image.tmdb.org/t/p/w500${it.posterPath}",
                backdropUrl = "https://image.tmdb.org/t/p/original${it.backdropPath}",
                rating = it.voteAverage,
                genre = listOf("Movie"),
                duration = "N/A",
                releaseYear = it.releaseDate?.take(4)?.toIntOrNull() ?: 0,
                synopsis = it.overview,
                mediaType = "movie"
            )
        }
    }

    suspend fun searchTV(query: String): List<Movie> {
        val response = apiService.searchTV(apiKey, query)
        return response.results.map {
            Movie(
                id = it.id,
                title = it.title ?: it.name ?: "Unknown",
                posterUrl = "https://image.tmdb.org/t/p/w500${it.posterPath}",
                backdropUrl = "https://image.tmdb.org/t/p/original${it.backdropPath}",
                rating = it.voteAverage,
                genre = listOf("TV Series"),
                duration = "N/A",
                releaseYear = it.firstAirDate?.take(4)?.toIntOrNull() ?: 0,
                synopsis = it.overview,
                mediaType = "tv"
            )
        }
    }

    suspend fun searchAnime(query: String): List<Movie> {
        val response = apiService.searchAnime(query)
        return response.data.map {
            Movie(
                id = it.malId,
                title = it.title,
                posterUrl = it.images.jpg.largeImageUrl,
                backdropUrl = it.images.jpg.largeImageUrl,
                rating = it.score ?: 0.0,
                genre = listOf("Anime"),
                duration = "${it.episodes ?: "?"} eps",
                releaseYear = it.year ?: 0,
                synopsis = it.synopsis ?: "",
                mediaType = "anime"
            )
        }
    }

    suspend fun getCast(movieId: Int, type: String = "movie"): List<com.example.cinestack.data.remote.TMDBCast> {
        if (type == "anime") return emptyList()
        return try {
            apiService.getCredits(type, movieId, apiKey).cast
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMovieDetails(movieId: Int, type: String = "movie"): com.example.cinestack.data.remote.TMDBMovieDetailsResponse? {
        if (type == "anime") return null
        return try {
            apiService.getDetails(type, movieId, apiKey)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPersonDetails(personId: Int): com.example.cinestack.data.remote.TMDBPersonDetails? {
        return try {
            apiService.getPersonDetails(personId, apiKey)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTrendingMovies(): List<Movie> {
        return try {
            val response = apiService.getTrendingMovies(apiKey)
            response.results.map { it.toMovie() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPopularMovies(): List<Movie> {
        return try {
            val response = apiService.getPopularMovies(apiKey)
            response.results.map { it.toMovie() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRecommendations(movieId: Int, type: String = "movie"): List<Movie> {
        if (type == "anime") return emptyList()
        return try {
            val response = apiService.getRecommendations(type, movieId, apiKey)
            response.results.map { it.toMovie(type) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun com.example.cinestack.data.remote.TMDBMovie.toMovie(type: String = "movie") = Movie(
        id = id,
        title = title ?: name ?: "Unknown",
        posterUrl = "https://image.tmdb.org/t/p/w500$posterPath",
        backdropUrl = "https://image.tmdb.org/t/p/original$backdropPath",
        rating = voteAverage,
        genre = listOf(if (type == "movie") "Movie" else "TV Series"),
        duration = "N/A",
        releaseYear = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull() ?: 0,
        synopsis = overview,
        mediaType = type
    )
}

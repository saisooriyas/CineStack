package com.example.cinestack.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinestack.data.local.AppDatabase
import com.example.cinestack.data.model.Movie
import com.example.cinestack.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MovieRepository

    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val searchResults: StateFlow<List<Movie>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedType = MutableStateFlow("Movie")
    val selectedType: StateFlow<String> = _selectedType

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _cast = MutableStateFlow<List<com.example.cinestack.data.remote.TMDBCast>>(emptyList())
    val cast: StateFlow<List<com.example.cinestack.data.remote.TMDBCast>> = _cast

    private val _movieDetails = MutableStateFlow<com.example.cinestack.data.remote.TMDBMovieDetailsResponse?>(null)
    val movieDetails: StateFlow<com.example.cinestack.data.remote.TMDBMovieDetailsResponse?> = _movieDetails

    private val _personDetails = MutableStateFlow<com.example.cinestack.data.remote.TMDBPersonDetails?>(null)
    val personDetails: StateFlow<com.example.cinestack.data.remote.TMDBPersonDetails?> = _personDetails

    private val _trendingMovies = MutableStateFlow<List<Movie>>(emptyList())
    val trendingMovies: StateFlow<List<Movie>> = _trendingMovies

    private val _popularMovies = MutableStateFlow<List<Movie>>(emptyList())
    val popularMovies: StateFlow<List<Movie>> = _popularMovies

    val library: StateFlow<List<Movie>>

    private val movieCache = mutableMapOf<Int, Movie>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MovieRepository(database.movieDao())
        library = repository.library
        fetchDashboardData()
    }

    private fun updateCache(movies: List<Movie>) {
        movies.forEach { movieCache[it.id] = it }
    }

    fun getMovieFromCache(movieId: Int?): Movie? {
        if (movieId == null) return null
        return movieCache[movieId] ?: library.value.find { it.id == movieId }
    }

    private fun fetchDashboardData() {
        viewModelScope.launch {
            val trending = repository.getTrendingMovies()
            val popular = repository.getPopularMovies()
            updateCache(trending)
            updateCache(popular)
            _trendingMovies.value = trending
            _popularMovies.value = popular
        }
    }

    fun search(query: String, type: String = _selectedType.value) {
        _searchQuery.value = query
        _selectedType.value = type
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = when (type.lowercase()) {
                    "movie" -> repository.searchMovies(query)
                    "tv" -> repository.searchTV(query)
                    "anime" -> repository.searchAnime(query)
                    else -> emptyList()
                }
                updateCache(results)
                _searchResults.value = results
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onTypeSelected(type: String) {
        _selectedType.value = type
        if (_searchQuery.value.isNotBlank()) {
            search(_searchQuery.value, type)
        }
    }

    private val _recommendations = MutableStateFlow<List<Movie>>(emptyList())
    val recommendations: StateFlow<List<Movie>> = _recommendations

    fun fetchCast(movieId: Int, type: String = "movie") {
        viewModelScope.launch {
            _cast.value = repository.getCast(movieId, type)
            _movieDetails.value = repository.getMovieDetails(movieId, type)
            val recommendations = repository.getRecommendations(movieId, type)
            updateCache(recommendations)
            _recommendations.value = recommendations
        }
    }

    fun fetchPersonDetails(personId: Int) {
        viewModelScope.launch {
            _personDetails.value = repository.getPersonDetails(personId)
        }
    }

    fun clearPersonDetails() {
        _personDetails.value = null
    }

    fun addToLibrary(movie: Movie, status: String = "", rating: Double = 0.0, season: Int = 0, episode: Int = 0) {
        repository.addToLibrary(movie, status, rating, season, episode)
    }

    fun getLibraryMovie(movieId: Int): Movie? {
        return repository.library.value.find { it.id == movieId }
    }

    fun removeFromLibrary(movieId: Int) {
        repository.removeFromLibrary(movieId)
    }

    fun isInLibrary(movieId: Int): Boolean {
        return repository.isInLibrary(movieId)
    }

    fun updateApiKey(newKey: String) {
        repository.updateApiKey(newKey)
    }

    // Group methods
    val allGroups = repository.allGroups
    fun createGroup(name: String, parentId: Int? = null) = repository.createGroup(name, parentId)
    fun addItemToGroup(groupId: Int, movie: Movie) = repository.addItemToGroup(groupId, movie)
    fun addPersonToGroup(groupId: Int, cast: com.example.cinestack.data.remote.TMDBCast) = repository.addPersonToGroup(groupId, cast)
    suspend fun getGroupItems(groupId: Int) = repository.getGroupItems(groupId)
    suspend fun getSubGroups(parentId: Int) = repository.getSubGroups(parentId)
    suspend fun getGroupById(groupId: Int) = repository.getGroupById(groupId)
}

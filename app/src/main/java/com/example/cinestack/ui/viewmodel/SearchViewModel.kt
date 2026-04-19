package com.example.cinestack.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinestack.data.local.AppDatabase
import com.example.cinestack.data.model.Movie
import com.example.cinestack.data.remote.PDBPerformerResult
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

    private val movieCache = mutableMapOf<String, Movie>()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    private val _hasMoreResults = MutableStateFlow(false)
    val hasMoreResults: StateFlow<Boolean> = _hasMoreResults

    private val _performerSuggestions = MutableStateFlow<List<PDBPerformerResult>>(emptyList())
    val performerSuggestions: StateFlow<List<PDBPerformerResult>> = _performerSuggestions

    private val _selectedCastIds = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // id to name
    val selectedCastIds: StateFlow<List<Pair<String, String>>> = _selectedCastIds

    // Cache previous pages so back doesn't reload
    private val _pagedResults = mutableListOf<Movie>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MovieRepository(database.movieDao())
        library = repository.library
        fetchDashboardData()
    }

    private fun updateCache(movies: List<Movie>) {
        movies.forEach { movieCache[it.id] = it }
    }

    fun getMovieFromCache(movieId: String?): Movie? {
        if (movieId == null) return null
        return movieCache[movieId] ?: library.value.find { it.id == movieId }
    }

    private fun fetchDashboardData() {
        viewModelScope.launch {
            val trending = repository.getTrendingMovies().distinctBy { it.id }
            val popular = repository.getPopularMovies().distinctBy { it.id }
            updateCache(trending)
            updateCache(popular)
            _trendingMovies.value = trending
            _popularMovies.value = popular
        }
    }

    fun search(query: String, type: String = _selectedType.value, page: Int = 1) {
        _searchQuery.value  = query
        _selectedType.value = type
        _currentPage.value  = page
        if (query.isBlank()) { _searchResults.value = emptyList(); return }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rawResults = when (type.lowercase()) {
                    "movie"      -> repository.searchMovies(query, page)
                    "tv"         -> repository.searchTV(query, page)
                    "anime"      -> repository.searchAnime(query, page)
                    "xxx scenes" -> repository.searchPDBScenes(query)
                    else         -> emptyList()
                }
                val results = rawResults.distinctBy { it.id }
                if (page == 1) _pagedResults.clear()
                _pagedResults.addAll(results)
                _searchResults.value  = _pagedResults.toList()
                _hasMoreResults.value = results.size >= 20  // if full page returned, assume more exist
                updateCache(results)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNextPage() {
        search(_searchQuery.value, _selectedType.value, _currentPage.value + 1)
    }

    fun loadPreviousPage() {
        if (_currentPage.value > 1) {
            search(_searchQuery.value, _selectedType.value, _currentPage.value - 1)
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

    fun fetchCast(movieId: String, type: String = "movie") {
        viewModelScope.launch {
            _cast.value = repository.getCast(movieId, type)
            _movieDetails.value = repository.getMovieDetails(movieId, type)
            val recommendations = repository.getRecommendations(movieId, type)
            updateCache(recommendations)
            _recommendations.value = recommendations
        }
    }

    fun fetchPersonDetails(personId: String) {
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

    fun getLibraryMovie(movieId: String): Movie? {
        return repository.library.value.find { it.id == movieId }
    }

    fun removeFromLibrary(movieId: String) {
        repository.removeFromLibrary(movieId)
    }

    fun isInLibrary(movieId: String): Boolean {
        return repository.isInLibrary(movieId)
    }

    fun updateApiKey(newKey: String) {
        repository.updateApiKey(newKey)
    }

    fun mapCombinedCreditToMovie(credit: com.example.cinestack.data.remote.TMDBCombinedCredit): Movie {
        val movie = repository.mapCombinedCreditToMovie(credit)
        movieCache[movie.id] = movie
        return movie
    }

    fun searchPerformers(query: String) {
        viewModelScope.launch {
            if (query.length < 2) { _performerSuggestions.value = emptyList(); return@launch }
            _performerSuggestions.value = repository.searchPerformers(query)
        }
    }

    fun addCastFilter(id: String, name: String) {
        val current = _selectedCastIds.value.toMutableList()
        if (current.none { it.first == id }) current.add(id to name)
        _selectedCastIds.value = current
        _performerSuggestions.value = emptyList()
        searchByCast()
    }

    fun removeCastFilter(id: String) {
        _selectedCastIds.value = _selectedCastIds.value.filter { it.first != id }
        searchByCast()
    }

    fun clearCastFilters() {
        _selectedCastIds.value = emptyList()
    }

    private fun searchByCast() {
        val ids = _selectedCastIds.value.map { it.first }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _searchResults.value = repository.searchPDBScenesByCast(ids)
            _isLoading.value = false
        }
    }

    // Group methods
    val allGroups = repository.allGroups
    fun createGroup(name: String, parentId: Int? = null) = repository.createGroup(name, parentId)
    fun deleteGroup(groupId: Int) = repository.deleteGroup(groupId)
    fun addItemToGroup(groupId: Int, movie: Movie) = repository.addItemToGroup(groupId, movie)
    fun addPersonToGroup(groupId: Int, cast: com.example.cinestack.data.remote.TMDBCast) = repository.addPersonToGroup(groupId, cast)
    fun removeGroupItem(item: com.example.cinestack.data.local.GroupItemEntity) = repository.removeGroupItem(item)
    suspend fun getGroupItems(groupId: Int) = repository.getGroupItems(groupId)
    suspend fun getSubGroups(parentId: Int) = repository.getSubGroups(parentId)
    suspend fun getGroupById(groupId: Int) = repository.getGroupById(groupId)
}

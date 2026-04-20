package com.example.cinestack.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinestack.data.local.AppDatabase
import com.example.cinestack.data.local.StarredPerformerEntity
import com.example.cinestack.data.model.Movie
import com.example.cinestack.data.remote.PDBPerformerResult
import com.example.cinestack.data.repository.MovieRepository
import kotlinx.coroutines.Job
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

    private val _recommendations = MutableStateFlow<List<Movie>>(emptyList())
    val recommendations: StateFlow<List<Movie>> = _recommendations

    // ── Discovery shelves ─────────────────────────────────────────────────────
    private val _discoveryShelf1 = MutableStateFlow<List<Movie>>(emptyList())
    val discoveryShelf1: StateFlow<List<Movie>> = _discoveryShelf1

    private val _discoveryShelf2 = MutableStateFlow<List<Movie>>(emptyList())
    val discoveryShelf2: StateFlow<List<Movie>> = _discoveryShelf2

    private val _shelf1Label = MutableStateFlow("TRENDING TODAY")
    val shelf1Label: StateFlow<String> = _shelf1Label

    private val _shelf2Label = MutableStateFlow("POPULAR MOVIES")
    val shelf2Label: StateFlow<String> = _shelf2Label

    private val _isDiscoveryLoading = MutableStateFlow(false)
    val isDiscoveryLoading: StateFlow<Boolean> = _isDiscoveryLoading

    // Legacy aliases used by DashboardScreen (HomeScreen.kt)
    val trendingMovies: StateFlow<List<Movie>> = _discoveryShelf1
    val popularMovies: StateFlow<List<Movie>>  = _discoveryShelf2

    val library: StateFlow<List<Movie>>

    private val movieCache = mutableMapOf<String, Movie>()

    // ── Pagination ────────────────────────────────────────────────────────────
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    private val _hasMoreResults = MutableStateFlow(false)
    val hasMoreResults: StateFlow<Boolean> = _hasMoreResults

    // ── Performer / cast filter ───────────────────────────────────────────────
    private val _performerSuggestions = MutableStateFlow<List<PDBPerformerResult>>(emptyList())
    val performerSuggestions: StateFlow<List<PDBPerformerResult>> = _performerSuggestions

    private val _selectedCastIds = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val selectedCastIds: StateFlow<List<Pair<String, String>>> = _selectedCastIds

    // ── Starred performers ────────────────────────────────────────────────────
    val starredPerformers: StateFlow<List<StarredPerformerEntity>>
        get() = repository.starredPerformers

    // ── "See All" expanded shelf ──────────────────────────────────────────────
    private val _expandedShelf = MutableStateFlow<Pair<String, List<Movie>>?>(null)
    val expandedShelf: StateFlow<Pair<String, List<Movie>>?> = _expandedShelf

    // ── Now Playing / Top Rated for Dashboard dynamic section ─────────────────
    private val _nowPlayingMovies = MutableStateFlow<List<Movie>>(emptyList())
    val nowPlayingMovies: StateFlow<List<Movie>> = _nowPlayingMovies

    private val _topRatedMovies = MutableStateFlow<List<Movie>>(emptyList())
    val topRatedMovies: StateFlow<List<Movie>> = _topRatedMovies

    private var discoveryJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MovieRepository(database.movieDao())
        library = repository.library
        fetchDiscoveryForType("Movie")
        fetchDashboardExtras()
    }

    private fun fetchDashboardExtras() {
        viewModelScope.launch {
            try {
                val np = repository.getNowPlayingMovies()
                updateCache(np); _nowPlayingMovies.value = np
                val tr = repository.getTopRatedMovies()
                updateCache(tr); _topRatedMovies.value = tr
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateCache(movies: List<Movie>) {
        movies.forEach { movieCache[it.id] = it }
    }

    fun getMovieFromCache(movieId: String?): Movie? {
        if (movieId == null) return null
        return movieCache[movieId] ?: library.value.find { it.id == movieId }
    }

    fun expandShelf(label: String, movies: List<Movie>) { _expandedShelf.value = label to movies }
    fun closeExpandedShelf() { _expandedShelf.value = null }

    // ── Discovery ─────────────────────────────────────────────────────────────
    fun fetchDiscoveryForType(type: String) {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            _isDiscoveryLoading.value = true
            _discoveryShelf1.value = emptyList()
            _discoveryShelf2.value = emptyList()
            try {
                when (type) {
                    "Movie" -> {
                        _shelf1Label.value = "TRENDING TODAY"
                        _shelf2Label.value = "POPULAR MOVIES"
                        val s1 = repository.getTrendingMovies().distinctBy { it.id }
                        updateCache(s1); _discoveryShelf1.value = s1
                        val s2 = repository.getPopularMovies().distinctBy { it.id }
                        updateCache(s2); _discoveryShelf2.value = s2
                    }
                    "TV" -> {
                        _shelf1Label.value = "TRENDING TV TODAY"
                        _shelf2Label.value = "POPULAR SERIES"
                        val s1 = repository.getTrendingTV().distinctBy { it.id }
                        updateCache(s1); _discoveryShelf1.value = s1
                        val s2 = repository.getPopularTV().distinctBy { it.id }
                        updateCache(s2); _discoveryShelf2.value = s2
                    }
                    "Anime" -> {
                        _shelf1Label.value = "TOP AIRING ANIME"
                        _shelf2Label.value = "MOST POPULAR"
                        val s1 = repository.getTopAiringAnime().distinctBy { it.id }
                        updateCache(s1); _discoveryShelf1.value = s1
                        val s2 = repository.getTopAnimeByPopularity().distinctBy { it.id }
                        updateCache(s2); _discoveryShelf2.value = s2
                    }
                    "XXX Scenes" -> {
                        _shelf1Label.value = "RECENTLY RELEASED SCENES"
                        _shelf2Label.value = "RECENT XXX MOVIES"
                        val s1 = repository.getRecentXxxScenes().distinctBy { it.id }
                        updateCache(s1); _discoveryShelf1.value = s1
                        val s2 = repository.getRecentXxxMovies().distinctBy { it.id }
                        updateCache(s2); _discoveryShelf2.value = s2
                    }
                    "XXX Movies" -> {
                        _shelf1Label.value = "RECENT XXX MOVIES"
                        _shelf2Label.value = "RECENTLY RELEASED SCENES"
                        val s1 = repository.getRecentXxxMovies().distinctBy { it.id }
                        updateCache(s1); _discoveryShelf1.value = s1
                        val s2 = repository.getRecentXxxScenes().distinctBy { it.id }
                        updateCache(s2); _discoveryShelf2.value = s2
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { _isDiscoveryLoading.value = false }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    fun search(query: String, type: String = _selectedType.value, page: Int = 1) {
        _searchQuery.value  = query
        _selectedType.value = type
        _currentPage.value  = page
        if (query.isBlank()) { _searchResults.value = emptyList(); return }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = when (type.lowercase()) {
                    "movie"      -> repository.searchMovies(query, page)
                    "tv"         -> repository.searchTV(query, page)
                    "anime"      -> repository.searchAnime(query, page)
                    "xxx scenes" -> repository.searchPDBScenes(query, page)
                    "xxx movies" -> repository.searchPDBMoviesText(query, page)
                    else         -> emptyList()
                }.distinctBy { it.id }
                _searchResults.value  = results
                _hasMoreResults.value = results.size >= 20
                updateCache(results)
            } catch (e: Exception) { e.printStackTrace() }
            finally { _isLoading.value = false }
        }
    }

    fun loadNextPage() { search(_searchQuery.value, _selectedType.value, _currentPage.value + 1) }
    fun loadPreviousPage() {
        if (_currentPage.value > 1) search(_searchQuery.value, _selectedType.value, _currentPage.value - 1)
    }

    fun onTypeSelected(type: String) {
        _selectedType.value = type
        if (_searchQuery.value.isNotBlank()) search(_searchQuery.value, type, page = 1)
        else fetchDiscoveryForType(type)
    }

    fun fetchCast(movieId: String, type: String = "movie") {
        viewModelScope.launch {
            _cast.value = repository.getCast(movieId, type)
            _movieDetails.value = repository.getMovieDetails(movieId, type)
            val recs = repository.getRecommendations(movieId, type)
            updateCache(recs); _recommendations.value = recs
        }
    }

    fun fetchPersonDetails(personId: String) {
        viewModelScope.launch { _personDetails.value = repository.getPersonDetails(personId) }
    }

    fun clearPersonDetails() { _personDetails.value = null }

    fun addToLibrary(movie: Movie, status: String = "", rating: Double = 0.0, season: Int = 0, episode: Int = 0) {
        repository.addToLibrary(movie, status, rating, season, episode)
    }

    fun getLibraryMovie(movieId: String): Movie? = repository.library.value.find { it.id == movieId }
    fun removeFromLibrary(movieId: String) = repository.removeFromLibrary(movieId)
    fun isInLibrary(movieId: String): Boolean = repository.isInLibrary(movieId)
    fun updateApiKey(newKey: String) = repository.updateApiKey(newKey)

    fun mapCombinedCreditToMovie(credit: com.example.cinestack.data.remote.TMDBCombinedCredit): Movie {
        val movie = repository.mapCombinedCreditToMovie(credit)
        movieCache[movie.id] = movie
        return movie
    }

    // ── Performer search & cast filter ────────────────────────────────────────
    fun searchPerformers(query: String) {
        viewModelScope.launch {
            if (query.length < 2) { _performerSuggestions.value = emptyList(); return@launch }
            _performerSuggestions.value = repository.searchPerformers(query)
        }
    }

    fun clearPerformerSuggestions() { _performerSuggestions.value = emptyList() }

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

    fun clearCastFilters() { _selectedCastIds.value = emptyList() }

    private fun searchByCast() {
        val pairs = _selectedCastIds.value
        if (pairs.isEmpty()) { _searchResults.value = emptyList(); return }
        val ids   = pairs.map { it.first }
        val names = pairs.associate { it.first to it.second }
        val isMovieMode = _selectedType.value == "XXX Movies"
        viewModelScope.launch {
            _isLoading.value = true
            _searchResults.value = if (isMovieMode)
                repository.searchPDBMoviesByCast(ids, names)
            else
                repository.searchPDBScenesByCast(ids, names)
            _isLoading.value = false
        }
    }

    // ── Starred Performers ────────────────────────────────────────────────────
    fun starPerformer(id: String, name: String, imageUrl: String) {
        repository.starPerformer(StarredPerformerEntity(id = id, name = name, imageUrl = imageUrl))
    }

    fun unstarPerformer(id: String) = repository.unstarPerformer(id)

    fun isPerformerStarred(id: String): Boolean = repository.isPerformerStarred(id)

    // ── Groups ────────────────────────────────────────────────────────────────
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
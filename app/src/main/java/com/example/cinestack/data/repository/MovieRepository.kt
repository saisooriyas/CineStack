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
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
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

    // ── Raw TPDB request using HttpUrl.Builder ────────────────────────────────
    //
    // Retrofit's @QueryMap always URL-encodes keys, turning performers[id] into
    // performers%5Bid%5D which TPDB rejects. OkHttp's Request.Builder(url: String)
    // also re-parses and re-encodes. The only safe way to keep square brackets
    // raw is to build an HttpUrl with addEncodedQueryParameter() — that method
    // takes keys and values that are ALREADY encoded and passes them through
    // verbatim. Since square brackets are legal in query strings per RFC 3986
    // and TPDB expects them literally, we just pass the key as-is.
    //
    // This produces exactly the same URL as the TPDB website:
    //   /scenes?q=american+milf&performers[82895]=Brandi+Love&performer_and=1
    //
    private fun buildPdbHttpUrl(
        endpoint: String,          // "scenes" or "movies"
        query: String,             // text query, may be blank
        castIds: List<String>,     // performer ids (integers or UUIDs)
        castNames: Map<String, String>,
        performerAnd: Boolean,     // true = must feature ALL performers
        orderBy: String,           // e.g. "most_relevant"
        page: Int
    ): HttpUrl {
        val builder = HttpUrl.Builder()
            .scheme("https")
            .host("api.theporndb.net")
            .addPathSegment(endpoint)
            .addQueryParameter("page", page.toString())
            .addQueryParameter("per_page", "20")
            .addQueryParameter("orderBy", orderBy)
            .addQueryParameter("performer_and", if (performerAnd) "1" else "0")

        if (query.isNotBlank()) {
            builder.addQueryParameter("q", query)
        }

        // addEncodedQueryParameter passes the key verbatim — brackets stay raw.
        // We still encode the name value normally (spaces → %20 etc.)
        castIds.forEach { id ->
            val name = castNames[id] ?: ""
            // Key:   performers[id]  — brackets intentionally NOT encoded
            // Value: performer name  — encoded normally by addEncodedQueryParameter
            builder.addEncodedQueryParameter(
                "performers[$id]",
                name.trim().replace(" ", "%20")
            )
        }

        return builder.build()
    }

    private suspend fun rawPdbGet(url: HttpUrl): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)   // HttpUrl object — OkHttp uses it directly without re-encoding
            .addHeader("Authorization", "Bearer $tpdbApiKey")
            .build()
        okHttpClient.newCall(request).execute().use { resp ->
            resp.body?.string() ?: "{}"
        }
    }

    private fun parsePdbResponse(json: String, type: String): List<Movie> {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONArray("data") ?: return emptyList()
            (0 until data.length()).mapNotNull { i ->
                try {
                    val obj      = data.getJSONObject(i)
                    val id       = obj.optString("id", "").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val title    = obj.optString("title", "Unknown")
                    val date     = obj.optString("date", "")
                    val dur      = obj.optInt("duration", 0)

                    // Image resolution priority: background.full > background.medium > posters.full > posters.medium
                    val bg       = obj.optJSONObject("background")
                    val posters  = obj.optJSONObject("posters")
                    val imageUrl = bg?.optString("full")?.takeIf { it.isNotBlank() }
                        ?: bg?.optString("medium")?.takeIf { it.isNotBlank() }
                        ?: posters?.optString("full")?.takeIf { it.isNotBlank() }
                        ?: posters?.optString("medium")?.takeIf { it.isNotBlank() }
                        ?: obj.optString("image", "")

                    val site     = obj.optJSONObject("site")?.optString("name", "") ?: ""

                    val perfArr  = obj.optJSONArray("performers") ?: JSONArray()
                    val castList = (0 until perfArr.length()).mapNotNull { j ->
                        try {
                            // Performers in scene search are site-performer objects
                            // with nested "parent" that has the canonical name.
                            val p      = perfArr.getJSONObject(j)
                            val parent = p.optJSONObject("parent")
                            (parent?.optString("name") ?: p.optString("name", ""))
                                .takeIf { it.isNotBlank() }
                        } catch (e: Exception) { null }
                    }

                    Movie(
                        id          = id,
                        title       = title,
                        posterUrl   = imageUrl,
                        backdropUrl = imageUrl,
                        rating      = 0.0,
                        genre       = listOf(site.ifBlank { if (type == "xxx_movie") "XXX Movie" else "XXX" }),
                        duration    = if (dur > 0) "${dur / 60} min" else "N/A",
                        releaseYear = date.take(4).toIntOrNull() ?: 0,
                        synopsis    = buildString {
                            if (castList.isNotEmpty()) append(castList.joinToString(", "))
                            if (site.isNotBlank()) { if (isNotEmpty()) append(" · "); append(site) }
                        },
                        mediaType  = type,
                        castNames  = castList
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // ── Combined cast + text search ───────────────────────────────────────────
    //
    // Mirrors what the TPDB website does:
    //   /scenes?q=...&performers[id]=Name&performer_and=1&orderBy=most_relevant
    //
    // For a single performer with no text query we still use the dedicated
    // /performers/{id}/scenes endpoint which is faster and more reliable.
    // For everything else — multi-cast, or single-cast + text — we use the
    // combined endpoint so the server does the intersection properly.
    //
    suspend fun searchPDBScenesByCast(
        castIds: List<String>,
        castNames: Map<String, String>,
        query: String = "",
        page: Int = 1
    ): List<Movie> {
        return try {
            if (castIds.size == 1 && query.isBlank()) {
                // Fast path: single performer, no text query
                pdbService.getPerformerScenes("Bearer $tpdbApiKey", castIds.first(), page)
                    .data.map { it.toMovie("xxx") }
            } else {
                // Full combined search: text + one or more performers
                val url = buildPdbHttpUrl(
                    endpoint     = "scenes",
                    query        = query,
                    castIds      = castIds,
                    castNames    = castNames,
                    performerAnd = castIds.size > 1,  // AND only when multiple performers
                    orderBy      = if (query.isNotBlank()) "most_relevant" else "recently_released",
                    page         = page
                )
                parsePdbResponse(rawPdbGet(url), "xxx")
            }
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    suspend fun searchPDBMoviesByCast(
        castIds: List<String>,
        castNames: Map<String, String>,
        query: String = "",
        page: Int = 1
    ): List<Movie> {
        return try {
            if (castIds.size == 1 && query.isBlank()) {
                pdbService.getPerformerMovies("Bearer $tpdbApiKey", castIds.first(), page)
                    .data.map { it.toMovie("xxx_movie") }
            } else {
                val url = buildPdbHttpUrl(
                    endpoint     = "movies",
                    query        = query,
                    castIds      = castIds,
                    castNames    = castNames,
                    performerAnd = castIds.size > 1,
                    orderBy      = if (query.isNotBlank()) "most_relevant" else "recently_released",
                    page         = page
                )
                parsePdbResponse(rawPdbGet(url), "xxx_movie")
            }
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // ── Text-only search ──────────────────────────────────────────────────────
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
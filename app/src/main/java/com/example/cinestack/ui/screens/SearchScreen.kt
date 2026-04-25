package com.example.cinestack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.cinestack.data.model.Movie
import com.example.cinestack.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onMovieClick: (Movie) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val searchResults       by viewModel.searchResults.collectAsState()
    val isLoading           by viewModel.isLoading.collectAsState()
    val selectedType        by viewModel.selectedType.collectAsState()
    val searchQuery         by viewModel.searchQuery.collectAsState()
    val currentPage         by viewModel.currentPage.collectAsState()
    val hasMoreResults      by viewModel.hasMoreResults.collectAsState()
    val isDiscoveryLoading  by viewModel.isDiscoveryLoading.collectAsState()
    val shelf1              by viewModel.discoveryShelf1.collectAsState()
    val shelf2              by viewModel.discoveryShelf2.collectAsState()
    val shelf1Label         by viewModel.shelf1Label.collectAsState()
    val shelf2Label         by viewModel.shelf2Label.collectAsState()
    val expandedShelf       by viewModel.expandedShelf.collectAsState()

    var localQuery by rememberSaveable { mutableStateOf(searchQuery) }
    val isSearching = localQuery.isNotBlank()
    val isXxx = selectedType == "XXX Scenes" || selectedType == "XXX Movies"

    LaunchedEffect(localQuery) {
        if (localQuery != searchQuery) {
            delay(400L)
            viewModel.search(localQuery)
        }
    }

    // ── "See All" overlay ─────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = expandedShelf != null,
        enter   = fadeIn() + slideInVertically { it / 4 },
        exit    = fadeOut() + slideOutVertically { it / 4 }
    ) {
        expandedShelf?.let { (label, movies) ->
            SeeAllScreen(
                label    = label,
                movies   = movies,
                onBack   = { viewModel.closeExpandedShelf() },
                onMovieClick = {
                    viewModel.closeExpandedShelf()
                    onMovieClick(it)
                }
            )
        }
    }

    if (expandedShelf != null) return  // don't render the rest while expanded

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(Color.Black),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ── Search bar ─────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SearchInputBar(
                query         = localQuery,
                onQueryChange = { localQuery = it },
                onClear       = { localQuery = "" }
            )
        }

        // ── Category pill ──────────────────────────────────────────────────
        item {
            SegmentedTypePill(
                selectedType = selectedType,
                onSelect     = {
                    viewModel.onTypeSelected(it)
                    if (localQuery.isNotBlank()) viewModel.search(localQuery, it, 1)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ── XXX cast filter (search mode only) ────────────────────────────
        if (isSearching && isXxx) {
            item {
                XxxCastFilterBar(
                    viewModel   = viewModel,
                    isMovieMode = selectedType == "XXX Movies",
                    modifier    = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // DISCOVERY MODE
        // ══════════════════════════════════════════════════════════════════
        if (!isSearching) {

            item {
                Text(
                    "BROWSE BY CATEGORY",
                    style         = MaterialTheme.typography.labelSmall,
                    color         = Color.Gray,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categoryList) { cat ->
                        CategoryCard(
                            category = cat,
                            selected = selectedType == cat.type,
                            onClick  = { viewModel.onTypeSelected(cat.type) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isDiscoveryLoading) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                if (shelf1.isNotEmpty()) {
                    item {
                        ShelfHeader(
                            title   = shelf1Label,
                            onSeeAll = { viewModel.expandShelf(shelf1Label, shelf1) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding        = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(shelf1.take(15), key = { it.id }) { movie ->
                                DiscoveryCard(selectedType, movie) { onMovieClick(movie) }
                            }
                        }
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                if (shelf2.isNotEmpty()) {
                    item {
                        ShelfHeader(
                            title    = shelf2Label,
                            onSeeAll = { viewModel.expandShelf(shelf2Label, shelf2) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding        = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(shelf2.take(15), key = { it.id }) { movie ->
                                DiscoveryCard(selectedType, movie) { onMovieClick(movie) }
                            }
                        }
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                if (shelf1.isEmpty() && shelf2.isEmpty()) {
                    item {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(64.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Nothing to show. Check your API keys.", color = Color.Gray) }
                    }
                }

                // ── Dynamic stats bar replaces hardcoded Curated Collections ──
                item {
                    QuickStatsSection(selectedType = selectedType)
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // SEARCH RESULTS MODE
        // ══════════════════════════════════════════════════════════════════
        if (isSearching) {
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (searchResults.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(64.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No results for", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text("\"$localQuery\"", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                items(searchResults, key = { it.id }) { movie ->
                    when (selectedType) {
                        "Anime"                    -> AnimeListCard(movie = movie, onClick = { onMovieClick(movie) })
                        "XXX Scenes", "XXX Movies" -> XxxSceneCard(movie = movie, onClick = { onMovieClick(movie) })
                        else                       -> FeaturedMovieCard(movie = movie, onMovieClick = onMovieClick)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (!isLoading && searchResults.isNotEmpty()) {
                item {
                    PaginationBar(
                        currentPage = currentPage,
                        hasMore     = hasMoreResults,
                        onPrevious  = { viewModel.loadPreviousPage() },
                        onNext      = { viewModel.loadNextPage() }
                    )
                }
            }
        }
    }
}

// ── Discovery card dispatcher ─────────────────────────────────────────────────
@Composable
private fun DiscoveryCard(type: String, movie: Movie, onClick: () -> Unit) {
    when (type) {
        "Anime"      -> AnimeDiscoveryCard(movie = movie, onClick = onClick)
        "XXX Scenes",
        "XXX Movies" -> XxxDiscoveryCard(movie = movie, onClick = onClick)
        else         -> PosterCard(movie = movie, onClick = onClick)
    }
}

// ── "See All" full-screen grid ────────────────────────────────────────────────
@Composable
fun SeeAllScreen(label: String, movies: List<Movie>, onBack: () -> Unit, onMovieClick: (Movie) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier             = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text(
                label,
                style      = MaterialTheme.typography.titleMedium,
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        LazyVerticalGrid(
            columns                = GridCells.Fixed(3),
            contentPadding         = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement  = Arrangement.spacedBy(12.dp),
            verticalArrangement    = Arrangement.spacedBy(16.dp),
            modifier               = Modifier.fillMaxSize()
        ) {
            items(movies) { movie ->
                Column(modifier = Modifier.clickable { onMovieClick(movie) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1A1A1A))
                    ) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (movie.rating > 0) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                shape    = RoundedCornerShape(4.dp),
                                color    = Color.Black.copy(alpha = 0.75f)
                            ) {
                                Text(
                                    "★ ${String.format("%.1f", movie.rating)}",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = Color(0xFFFFD700),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(movie.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, lineHeight = 14.sp)
                    if (movie.releaseYear > 0) Text("${movie.releaseYear}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── Dynamic stats / info section (replaces hardcoded Curated Collections) ─────
@Composable
fun QuickStatsSection(selectedType: String) {
    val items = when (selectedType) {
        "Movie" -> listOf(
            Triple(Icons.Default.Whatshot,  "Trending",     "Top picks updated daily from TMDB"),
            Triple(Icons.Default.Star,       "Top Rated",    "Highest rated films of all time"),
            Triple(Icons.AutoMirrored.Filled.TrendingUp, "Now Playing",  "Currently in cinemas worldwide")
        )
        "TV" -> listOf(
            Triple(Icons.Default.Whatshot,  "Trending",    "Most-watched series right now"),
            Triple(Icons.Default.Star,       "Top Rated",   "Critically acclaimed TV shows"),
            Triple(Icons.AutoMirrored.Filled.TrendingUp, "On The Air",  "Currently airing episodes")
        )
        "Anime" -> listOf(
            Triple(Icons.Default.Whatshot,  "Airing",      "Currently airing this season"),
            Triple(Icons.Default.Star,       "All Time Top", "Highest scored on MyAnimeList"),
            Triple(Icons.AutoMirrored.Filled.TrendingUp, "By Popularity","Most users tracking these shows")
        )
        else -> listOf(
            Triple(Icons.Default.Whatshot,  "Recent",      "Freshly released content"),
            Triple(Icons.Default.Star,       "Most Viewed", "Trending performers & studios"),
            Triple(Icons.AutoMirrored.Filled.TrendingUp, "New Scenes",  "Latest added to ThePornDB")
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "ABOUT THIS SECTION",
            style         = MaterialTheme.typography.labelSmall,
            color         = Color.Gray,
            letterSpacing = 1.sp,
            modifier      = Modifier.padding(bottom = 12.dp)
        )
        items.forEach { (icon, title, desc) ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape    = RoundedCornerShape(12.dp),
                color    = Color(0xFF121212)
            ) {
                Row(
                    modifier          = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column {
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(desc, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// ── Search bar ─────────────────────────────────────────────────────────────────

@Composable
fun SearchInputBar(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp),
        shape    = RoundedCornerShape(28.dp),
        color    = Color(0xFF1A1A1A),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        TextField(
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier.fillMaxSize(),
            placeholder   = { Text("Search films, series, anime…", color = Color.Gray, style = MaterialTheme.typography.bodyMedium) },
            leadingIcon   = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
            trailingIcon  = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) { Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                cursorColor             = MaterialTheme.colorScheme.primary,
                focusedTextColor        = Color.White, unfocusedTextColor = Color.White
            ),
            singleLine = true
        )
    }
}

// ── Segmented pill — now includes XXX Movies ──────────────────────────────────

private val segmentLabels = listOf(
    "Movie"      to "Movie",
    "TV"         to "TV",
    "Anime"      to "Anime",
    "Scenes"     to "XXX Scenes",
    "Movies"     to "XXX Movies"
)

@Composable
fun SegmentedTypePill(selectedType: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().height(38.dp),
        shape    = RoundedCornerShape(50),
        color    = Color(0xFF1A1A1A)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(3.dp)) {
            segmentLabels.forEach { (label, typeValue) ->
                val selected = selectedType == typeValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(typeValue) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (selected) Color.Black else Color.Gray,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 10.sp
                    )
                }
            }
        }
    }
}

// ── Pagination ─────────────────────────────────────────────────────────────────

@Composable
fun PaginationBar(currentPage: Int, hasMore: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Surface(
            onClick  = onPrevious, enabled = currentPage > 1,
            shape    = RoundedCornerShape(12.dp),
            color    = if (currentPage > 1) Color(0xFF1A1A1A) else Color(0xFF111111),
            border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = if (currentPage > 1) 0.1f else 0.03f))
        ) {
            Text("← PREV", modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, color = if (currentPage > 1) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
        }
        Text("PAGE $currentPage", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
        Surface(
            onClick  = onNext, enabled = hasMore,
            shape    = RoundedCornerShape(12.dp),
            color    = if (hasMore) MaterialTheme.colorScheme.primary else Color(0xFF111111),
            border   = if (!hasMore) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)) else null
        ) {
            Text("NEXT →", modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), style = MaterialTheme.typography.labelSmall, color = if (hasMore) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Category cards ─────────────────────────────────────────────────────────────

data class CategoryItem(val type: String, val emoji: String, val label: String, val color: Color, val subLabel: String)

private val categoryList = listOf(
    CategoryItem("Movie",      "🎬", "Movies",     Color(0xFF1565C0), "TMDB"),
    CategoryItem("TV",         "📺", "TV Series",  Color(0xFF6A1B9A), "TMDB"),
    CategoryItem("Anime",      "⛩",  "Anime",      Color(0xFFB71C1C), "MyAnimeList"),
    CategoryItem("XXX Scenes", "🎭", "XXX Scenes", Color(0xFF4A0010), "ThePornDB"),
    CategoryItem("XXX Movies", "📽", "XXX Movies", Color(0xFF3E0000), "ThePornDB")
)

@Composable
fun CategoryCard(category: CategoryItem, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(20.dp),
        color    = if (selected) category.color else Color(0xFF141414),
        border   = androidx.compose.foundation.BorderStroke(1.dp, if (selected) category.color.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.width(110.dp).height(90.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(category.emoji, fontSize = 26.sp)
            Column {
                Text(category.label, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                Text(category.subLabel, style = MaterialTheme.typography.labelSmall, color = if (selected) Color.White.copy(alpha = 0.7f) else Color.Gray, fontSize = 9.sp)
            }
        }
    }
}

// ── Shelf header with working See All ─────────────────────────────────────────

@Composable
fun ShelfHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        if (onSeeAll != null) {
            Text(
                "SEE ALL",
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp,
                modifier = Modifier.clickable { onSeeAll() }.padding(4.dp)
            )
        } else {
            Text("SEE ALL", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 0.5.sp)
        }
    }
}

// ── Poster card ────────────────────────────────────────────────────────────────

@Composable
fun PosterCard(movie: Movie, onClick: () -> Unit) {
    Column(modifier = Modifier.width(110.dp).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp))) {
            AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), startY = 80f)))
            if (movie.rating > 0) {
                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), shape = RoundedCornerShape(6.dp), color = Color.Black.copy(alpha = 0.75f)) {
                    Text("★ ${String.format("%.1f", movie.rating)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 9.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(movie.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, fontWeight = FontWeight.Medium, lineHeight = 15.sp)
        if (movie.releaseYear > 0) Text("${movie.releaseYear}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun AnimeDiscoveryCard(movie: Movie, onClick: () -> Unit) {
    Column(modifier = Modifier.width(110.dp).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp))) {
            AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), startY = 80f)))
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(6.dp), shape = RoundedCornerShape(4.dp), color = Color(0xFFB71C1C).copy(alpha = 0.9f)) {
                Text("ANIME", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            if (movie.rating > 0) {
                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), shape = RoundedCornerShape(6.dp), color = Color.Black.copy(alpha = 0.75f)) {
                    Text("★ ${String.format("%.1f", movie.rating)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 9.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(movie.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, fontWeight = FontWeight.Medium, lineHeight = 15.sp)
        if (movie.releaseYear > 0) Text("${movie.releaseYear}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun XxxDiscoveryCard(movie: Movie, onClick: () -> Unit) {
    Column(modifier = Modifier.width(180.dp).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(12.dp))) {
            AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)), startY = 40f)))
            val isMovie = movie.mediaType == "xxx_movie"
            Surface(modifier = Modifier.align(Alignment.TopStart), shape = RoundedCornerShape(bottomEnd = 6.dp), color = if (isMovie) Color(0xFF880000) else Color(0xFFAA0000)) {
                Text(if (isMovie) "MOVIE" else "SCENE", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            if (movie.genre.firstOrNull()?.isNotBlank() == true) {
                Text(movie.genre.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(movie.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, fontWeight = FontWeight.Medium, lineHeight = 15.sp)
        if (movie.synopsis.isNotBlank()) Text(movie.synopsis, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1, fontSize = 9.sp)
    }
}

// ── Search result cards ────────────────────────────────────────────────────────

@Composable
fun FeaturedMovieCard(movie: Movie, onMovieClick: (Movie) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp)).clickable { onMovieClick(movie) }
    ) {
        AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)), startY = 300f)))
        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(0f, 1000f), radius = 800f)))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {
                Text("RESULT", style = MaterialTheme.typography.labelSmall, color = Color.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = movie.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(text = "${movie.releaseYear}  •  ${movie.genre.firstOrNull()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun AnimeListCard(movie: Movie, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() },
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).height(100.dp)) {
            AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.width(70.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.DarkGray, shape = RoundedCornerShape(4.dp)) {
                        Text("ANIME", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${movie.rating} Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(movie.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                Text(movie.synopsis, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2)
            }
        }
    }
}

@Composable
fun XxxSceneCard(movie: Movie, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() },
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).height(110.dp)) {
            Box(modifier = Modifier.width(160.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A))) {
                AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                val isMovie = movie.mediaType == "xxx_movie"
                Surface(color = if (isMovie) Color(0xFF880000) else Color(0xFFAA0000), shape = RoundedCornerShape(bottomEnd = 6.dp), modifier = Modifier.align(Alignment.TopStart)) {
                    Text(if (isMovie) "MOVIE" else "SCENE", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(movie.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(movie.genre.firstOrNull() ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    if (movie.synopsis.isNotBlank()) Text(movie.synopsis, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (movie.releaseYear > 0) Text("${movie.releaseYear}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        if (movie.duration != "N/A") Text("⏱ ${movie.duration}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ── XXX Cast Filter Bar — updated with movie mode toggle ──────────────────────

@Composable
fun XxxCastFilterBar(viewModel: SearchViewModel, isMovieMode: Boolean, modifier: Modifier = Modifier) {
    val suggestions  by viewModel.performerSuggestions.collectAsState()
    val selectedCast by viewModel.selectedCastIds.collectAsState()
    var castQuery    by remember { mutableStateOf("") }

    LaunchedEffect(castQuery) {
        delay(300L)
        if (castQuery.length >= 2) viewModel.searchPerformers(castQuery)
        else viewModel.clearPerformerSuggestions()
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "FILTER BY PERFORMER",
                style         = MaterialTheme.typography.labelSmall,
                color         = Color.Gray,
                letterSpacing = 1.sp
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isMovieMode) Color(0xFF880000) else Color(0xFFAA0000)
            ) {
                Text(
                    if (isMovieMode) "MOVIES" else "SCENES",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedCast.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                items(selectedCast) { (id, name) ->
                    InputChip(
                        selected     = true,
                        onClick      = { viewModel.removeCastFilter(id) },
                        label        = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor     = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Surface(
            shape    = RoundedCornerShape(12.dp),
            color    = Color(0xFF1A1A1A),
            border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            TextField(
                value         = castQuery,
                onValueChange = { castQuery = it },
                placeholder   = { Text("Search performer name…", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
                leadingIcon   = { Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                trailingIcon  = {
                    if (castQuery.isNotEmpty()) {
                        IconButton(onClick = { castQuery = ""; viewModel.clearPerformerSuggestions() }) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                colors     = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor        = Color.White, unfocusedTextColor = Color.White,
                    cursorColor             = MaterialTheme.colorScheme.primary
                ),
                singleLine = true,
                textStyle  = MaterialTheme.typography.bodySmall
            )
        }

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E1E1E), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
                Column {
                    suggestions.take(5).forEachIndexed { index, performer ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    viewModel.addCastFilter(
                                        performer.id,
                                        performer.numericId.toString(),
                                        performer.name
                                    )
                                    castQuery = ""
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(model = performer.image, contentDescription = null, modifier = Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Text(performer.name, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        if (index < suggestions.take(5).lastIndex)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}
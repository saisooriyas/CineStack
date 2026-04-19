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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val selectedType  by viewModel.selectedType.collectAsState()
    val trendingMovies by viewModel.trendingMovies.collectAsState()
    val popularMovies  by viewModel.popularMovies.collectAsState()
    val searchQuery    by viewModel.searchQuery.collectAsState()
    val currentPage    by viewModel.currentPage.collectAsState()
    val hasMoreResults by viewModel.hasMoreResults.collectAsState()

    var localQuery by rememberSaveable { mutableStateOf(searchQuery) }
    val isSearching = localQuery.isNotBlank()

    // Debounce search
    LaunchedEffect(localQuery) {
        // Only trigger search if localQuery is different from what's already in VM
        // to avoid clearing results or re-fetching on back navigation
        if (localQuery != searchQuery) {
            delay(400L)
            viewModel.search(localQuery)
        }
    }

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

        // ── Segmented pill — only visible when searching ───────────────────
        item {
            AnimatedVisibility(
                visible = isSearching,
                enter   = fadeIn() + slideInVertically { -it },
                exit    = fadeOut() + slideOutVertically { -it }
            ) {
                SegmentedTypePill(
                    selectedType = selectedType,
                    onSelect     = { viewModel.onTypeSelected(it) },
                    modifier     = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (!isSearching) Spacer(modifier = Modifier.height(8.dp))
        }

        // ══════════════════════════════════════════════════════════════════
        // DISCOVERY MODE — shown when search bar is empty
        // ══════════════════════════════════════════════════════════════════
        if (!isSearching) {

            // Category cards row
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
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Trending shelf
            if (trendingMovies.isNotEmpty()) {
                item {
                    ShelfHeader(title = "TRENDING NOW")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trendingMovies.take(10), key = { it.id }) { movie ->
                            PosterCard(movie = movie, onClick = { onMovieClick(movie) })
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // Popular shelf
            if (popularMovies.isNotEmpty()) {
                item {
                    ShelfHeader(title = "POPULAR MOVIES")
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(popularMovies.take(10), key = { it.id }) { movie ->
                            PosterCard(movie = movie, onClick = { onMovieClick(movie) })
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // Loading shimmer while discovery data loads
            if (trendingMovies.isEmpty() && popularMovies.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // SEARCH RESULTS MODE
        // ══════════════════════════════════════════════════════════════════
        if (isSearching) {
            if (isLoading) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (searchResults.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No results for", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "\"$localQuery\"",
                                color      = Color.White,
                                fontWeight = FontWeight.Bold,
                                style      = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            } else {
                items(searchResults, key = { it.id }) { movie ->
                    when (selectedType) {
                        "Anime"      -> AnimeListCard(movie = movie, onClick = { onMovieClick(movie) })
                        "XXX Scenes" -> XxxSceneCard(movie = movie, onClick = { onMovieClick(movie) })
                        else         -> FeaturedMovieCard(movie = movie, onMovieClick = onMovieClick)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if(isSearching && !isLoading && searchResults.isNotEmpty()) {
            item {
                PaginationBar(
                    currentPage    = currentPage,
                    hasMore        = hasMoreResults,
                    onPrevious     = { viewModel.loadPreviousPage() },
                    onNext         = { viewModel.loadNextPage() }
                )
            }
        }

        // Only show when XXX Scenes is active
        if(isSearching) {
            if (selectedType == "XXX Scenes") {
                item {
                    XxxCastFilterBar(
                        viewModel = viewModel,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ── Search bar ─────────────────────────────────────────────────────────────

@Composable
fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(52.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1A1A1A),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, Color.White.copy(alpha = 0.08f)
        )
    ) {
        TextField(
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier.fillMaxSize(),
            placeholder   = {
                Text(
                    "Search films, series, anime…",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon  = {
                Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor             = MaterialTheme.colorScheme.primary,
                focusedTextColor        = Color.White,
                unfocusedTextColor      = Color.White
            ),
            singleLine = true
        )
    }
}

// ── Segmented pill ─────────────────────────────────────────────────────────

private val segmentLabels = listOf(
    "Movie" to "Movie",
    "TV"    to "TV",
    "Anime" to "Anime",
    "XXX"   to "XXX Scenes"
)

@Composable
fun SegmentedTypePill(
    selectedType : String,
    onSelect     : (String) -> Unit,
    modifier     : Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        shape = RoundedCornerShape(50),
        color = Color(0xFF1A1A1A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            segmentLabels.forEach { (label, typeValue) ->
                val selected = selectedType == typeValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable { onSelect(typeValue) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (selected) Color.Black else Color.Gray,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// -- Pagination -------------------

@Composable
fun PaginationBar(
    currentPage : Int,
    hasMore     : Boolean,
    onPrevious  : () -> Unit,
    onNext      : () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Previous
        Surface(
            onClick  = onPrevious,
            enabled  = currentPage > 1,
            shape    = RoundedCornerShape(12.dp),
            color    = if (currentPage > 1) Color(0xFF1A1A1A) else Color(0xFF111111),
            border   = androidx.compose.foundation.BorderStroke(
                1.dp, Color.White.copy(alpha = if (currentPage > 1) 0.1f else 0.03f)
            )
        ) {
            Text(
                "← PREV",
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style      = MaterialTheme.typography.labelSmall,
                color      = if (currentPage > 1) Color.White else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "PAGE $currentPage",
            style         = MaterialTheme.typography.labelSmall,
            color         = Color.Gray,
            letterSpacing = 1.sp
        )

        // Next
        Surface(
            onClick  = onNext,
            enabled  = hasMore,
            shape    = RoundedCornerShape(12.dp),
            color    = if (hasMore) MaterialTheme.colorScheme.primary else Color(0xFF111111),
            border   = if (!hasMore) androidx.compose.foundation.BorderStroke(
                1.dp, Color.White.copy(alpha = 0.03f)
            ) else null
        ) {
            Text(
                "NEXT →",
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style      = MaterialTheme.typography.labelSmall,
                color      = if (hasMore) Color.Black else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Category cards ─────────────────────────────────────────────────────────

data class CategoryItem(
    val type    : String,
    val emoji   : String,
    val label   : String,
    val color   : Color,
    val subLabel: String
)

private val categoryList = listOf(
    CategoryItem("Movie",      "🎬", "Movies",   Color(0xFF1565C0), "TMDB"),
    CategoryItem("TV",         "📺", "TV Series",Color(0xFF6A1B9A), "TMDB"),
    CategoryItem("Anime",      "⛩", "Anime",    Color(0xFFB71C1C), "MyAnimeList"),
    CategoryItem("XXX Scenes", "🔞", "Scenes",   Color(0xFF4A0010), "ThePornDB")
)

@Composable
fun CategoryCard(
    category : CategoryItem,
    selected : Boolean,
    onClick  : () -> Unit
) {
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(20.dp),
        color    = if (selected) category.color else Color(0xFF141414),
        border   = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) category.color.copy(alpha = 0.6f)
            else Color.White.copy(alpha = 0.06f)
        ),
        modifier = Modifier.width(110.dp).height(90.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category.emoji, fontSize = 26.sp)
            Column {
                Text(
                    category.label,
                    style      = MaterialTheme.typography.labelMedium,
                    color      = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    category.subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ── Shelf header ───────────────────────────────────────────────────────────

@Composable
fun ShelfHeader(title: String) {
    Row(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment   = Alignment.CenterVertically
    ) {
        Text(
            title,
            style         = MaterialTheme.typography.labelSmall,
            color         = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            fontWeight    = FontWeight.Bold
        )
        Text(
            "SEE ALL",
            style         = MaterialTheme.typography.labelSmall,
            color         = Color.Gray,
            letterSpacing = 0.5.sp
        )
    }
}

// ── Poster card for discovery shelves ─────────────────────────────────────

@Composable
fun PosterCard(movie: Movie, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model            = movie.posterUrl,
                contentDescription = null,
                modifier         = Modifier.fillMaxSize(),
                contentScale     = ContentScale.Crop
            )
            // Subtle gradient at bottom for text legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors  = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY  = 80f
                        )
                    )
            )
            // Rating badge
            if (movie.rating > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Text(
                        text     = "★ ${String.format("%.1f", movie.rating)}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color(0xFFFFD700),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        fontSize = 9.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text      = movie.title,
            style     = MaterialTheme.typography.labelSmall,
            color     = Color.White,
            maxLines  = 2,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp
        )
        if (movie.releaseYear > 0) {
            Text(
                text  = "${movie.releaseYear}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
@Composable
fun FeaturedMovieCard(movie: Movie, onMovieClick: (Movie) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onMovieClick(movie) }
    ) {
        AsyncImage(
            model = movie.posterUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        startY = 300f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(0f, 1000f),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "RESULT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = "${movie.releaseYear}  •  ${movie.genre.firstOrNull()}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AnimeListCard(movie: Movie, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(100.dp)
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = null,
                modifier = Modifier
                    .width(70.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.DarkGray,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ANIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${movie.rating} Rating",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = movie.synopsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun XxxSceneCard(movie: Movie, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(110.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A1A))
            ) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    color = Color(0xFFAA0000),
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "XXX",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = movie.genre.firstOrNull() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    if (movie.synopsis.isNotBlank()) {
                        Text(
                            text = movie.synopsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (movie.releaseYear > 0) {
                            Text(
                                text = "${movie.releaseYear}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        if (movie.duration != "N/A") {
                            Text(
                                text = "⏱ ${movie.duration}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun XxxCastFilterBar(
    viewModel: SearchViewModel,
    modifier : Modifier = Modifier
) {
    val suggestions   by viewModel.performerSuggestions.collectAsState()
    val selectedCast  by viewModel.selectedCastIds.collectAsState()
    var castQuery     by remember { mutableStateOf("") }

    LaunchedEffect(castQuery) {
        delay(300L)
        viewModel.searchPerformers(castQuery)
    }

    Column(modifier = modifier) {

        // Selected cast chips
        if (selectedCast.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedCast) { (id, name) ->
                    InputChip(
                        selected = true,
                        onClick  = { viewModel.removeCastFilter(id) },
                        label    = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor     = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Performer search input
        Surface(
            shape  = RoundedCornerShape(12.dp),
            color  = Color(0xFF1A1A1A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            TextField(
                value         = castQuery,
                onValueChange = { castQuery = it },
                placeholder   = { Text("Filter by performer…", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
                leadingIcon   = { Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White,
                    cursorColor             = MaterialTheme.colorScheme.primary
                ),
                singleLine = true,
                textStyle  = MaterialTheme.typography.bodySmall
            )
        }

        // Autocomplete dropdown
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape  = RoundedCornerShape(12.dp),
                color  = Color(0xFF1E1E1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column {
                    suggestions.take(5).forEach { performer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addCastFilter(performer.id, performer.name)
                                    castQuery = ""
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model            = performer.image,
                                contentDescription = null,
                                modifier         = Modifier.size(32.dp).clip(CircleShape),
                                contentScale     = ContentScale.Crop
                            )
                            Text(
                                performer.name,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (performer != suggestions.last()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }
}
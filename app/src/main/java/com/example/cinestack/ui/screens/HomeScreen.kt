package com.example.cinestack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cinestack.data.model.Movie
import com.example.cinestack.ui.viewmodel.SearchViewModel

@Composable
fun DashboardScreen(
    viewModel: SearchViewModel,
    onMovieClick: (Movie) -> Unit
) {
    val trendingMovies  by viewModel.trendingMovies.collectAsState()
    val popularMovies   by viewModel.popularMovies.collectAsState()
    val nowPlaying      by viewModel.nowPlayingMovies.collectAsState()
    val topRated        by viewModel.topRatedMovies.collectAsState()
    val library         by viewModel.library.collectAsState()

    val watchingList  = library.filter { it.userStatus == "Watching" }
    val recentlyAdded = library.takeLast(6).reversed()
    val topUserPicks  = library.filter { it.userRating >= 8.0 }.sortedByDescending { it.userRating }.take(10)

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(Color.Black),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {

        // ── Currently Watching ─────────────────────────────────────────────
        if (watchingList.isNotEmpty()) {
            item {
                DashSectionHeader(
                    title    = "Continue Watching",
                    subtitle = "${watchingList.size} active"
                )
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(watchingList) { movie ->
                        WatchingCard(movie = movie, onClick = { onMovieClick(movie) })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Trending Hero ─────────────────────────────────────────────────
        if (trendingMovies.isNotEmpty()) {
            item {
                TrendingHeroCard(movie = trendingMovies.first(), onMovieClick = onMovieClick)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Now Playing in Cinemas ─────────────────────────────────────────
        if (nowPlaying.isNotEmpty()) {
            item {
                DashSectionHeader(title = "Now in Cinemas", subtitle = "Currently showing worldwide")
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(nowPlaying.take(15), key = { it.id }) { movie ->
                        DashPosterCard(movie = movie, onClick = { onMovieClick(movie) })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Popular Hits grid ─────────────────────────────────────────────
        if (popularMovies.isNotEmpty()) {
            item {
                DashSectionHeader(title = "Popular Hits", subtitle = "Trending on TMDB right now")
                popularMovies.take(4).chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { movie ->
                            PopularGridCard(movie = movie, onClick = { onMovieClick(movie) }, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Your Top Picks (from library, rating ≥ 8) ────────────────────
        if (topUserPicks.isNotEmpty()) {
            item {
                DashSectionHeader(title = "Your Top Picks", subtitle = "Items you rated 8+ stars")
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(topUserPicks, key = { it.id }) { movie ->
                        TopPickCard(movie = movie, onClick = { onMovieClick(movie) })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Top Rated by TMDB ─────────────────────────────────────────────
        if (topRated.isNotEmpty()) {
            item {
                DashSectionHeader(title = "All-Time Classics", subtitle = "Highest rated on TMDB")
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(topRated.take(15), key = { it.id }) { movie ->
                        DashPosterCard(movie = movie, onClick = { onMovieClick(movie) }, showRank = true)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Recently Added to Library ─────────────────────────────────────
        if (recentlyAdded.isNotEmpty()) {
            item {
                DashSectionHeader(title = "Recently Added", subtitle = "Last items you saved")
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentlyAdded, key = { it.id }) { movie ->
                        DashPosterCard(movie = movie, onClick = { onMovieClick(movie) })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Library stats quick-view ──────────────────────────────────────
        item {
            LibraryQuickStats(library = library)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun DashSectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

// ── Trending hero ─────────────────────────────────────────────────────────────

@Composable
fun TrendingHeroCard(movie: Movie, onMovieClick: (Movie) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .aspectRatio(0.75f).clip(RoundedCornerShape(20.dp))
            .clickable { onMovieClick(movie) }
    ) {
        AsyncImage(
            model = movie.posterUrl, contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
        // Dark vignette
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, Color.Black.copy(alpha = 0.92f)))
        ))
        // Accent radial glow
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(0f, Float.MAX_VALUE), radius = 900f
            )
        ))

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(4.dp)) {
                Text("TRENDING TODAY", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                movie.title.uppercase(), style = MaterialTheme.typography.headlineMedium,
                color = Color.White, fontWeight = FontWeight.Black, maxLines = 2,
                overflow = TextOverflow.Ellipsis, lineHeight = 30.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                movie.synopsis, color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onMovieClick(movie) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("View Details", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
                Surface(
                    color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                }
                Surface(
                    color = Color.Transparent, shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(String.format("%.1f", movie.rating), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Poster card (reusable) ────────────────────────────────────────────────────

@Composable
fun DashPosterCard(movie: Movie, onClick: () -> Unit, showRank: Boolean = false) {
    Column(modifier = Modifier.width(110.dp).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(12.dp))) {
            AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)), startY = 70f)))
            if (movie.rating > 0 && !showRank) {
                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp), shape = RoundedCornerShape(5.dp), color = Color.Black.copy(alpha = 0.75f)) {
                    Text("★ ${String.format("%.1f", movie.rating)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 9.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(movie.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
        if (movie.releaseYear > 0) Text("${movie.releaseYear}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
    }
}

// ── Popular hits grid card ────────────────────────────────────────────────────

@Composable
fun PopularGridCard(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.aspectRatio(0.72f).clip(RoundedCornerShape(14.dp)).clickable { onClick() }) {
        AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)), startY = 180f)))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(String.format("%.1f ★", movie.rating), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Top user pick card ────────────────────────────────────────────────────────

@Composable
fun TopPickCard(movie: Movie, onClick: () -> Unit) {
    Column(modifier = Modifier.width(100.dp).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))) {
            AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            // Personal rating badge
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(String.format("%.1f", movie.userRating), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(movie.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, lineHeight = 13.sp)
    }
}

// ── Watching card ─────────────────────────────────────────────────────────────

@Composable
fun WatchingCard(movie: Movie, onClick: () -> Unit) {
    Column(modifier = Modifier.width(220.dp).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp))) {
            AsyncImage(
                model = movie.backdropUrl.ifEmpty { movie.posterUrl },
                contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                if (movie.currentEpisode > 0)
                    Text("EP ${movie.currentEpisode}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            // Play overlay
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.padding(6.dp))
                }
            }
        }
    }
}

// ── Library quick-stats bar ───────────────────────────────────────────────────

@Composable
fun LibraryQuickStats(library: List<Movie>) {
    if (library.isEmpty()) return
    val watching  = library.count { it.userStatus == "Watching" }
    val completed = library.count { it.userStatus == "Completed" }
    val planned   = library.count { it.userStatus == "Plan" }
    val dropped   = library.count { it.userStatus == "Dropped" }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape    = RoundedCornerShape(16.dp),
        color    = Color(0xFF111111)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("LIBRARY SNAPSHOT", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("▶ WATCHING",  watching,  Color(0xFFFF5722), Modifier.weight(1f))
                StatChip("✓ DONE",      completed, Color(0xFF46EEDD), Modifier.weight(1f))
                StatChip("◷ PLANNED",  planned,   Color(0xFF7C4DFF), Modifier.weight(1f))
                StatChip("✗ DROPPED",  dropped,   Color(0xFFFF1744), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f)) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), fontSize = 8.sp, maxLines = 1)
        }
    }
}
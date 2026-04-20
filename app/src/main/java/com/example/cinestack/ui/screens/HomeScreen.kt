package com.example.cinestack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    val trendingMovies by viewModel.trendingMovies.collectAsState()
    val popularMovies by viewModel.popularMovies.collectAsState()
    val library by viewModel.library.collectAsState()
    
    val watchingList = library.filter { it.userStatus == "Watching" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        if (watchingList.isNotEmpty()) {
            item {
                CurrentlyWatchingSection(watchingList, onMovieClick)
            }
        }

        if (trendingMovies.isNotEmpty()) {
            item {
                TrendingNowSection(trendingMovies.first(), onMovieClick)
            }
        }

        if (popularMovies.isNotEmpty()) {
            item {
                QuickGridSection(popularMovies.take(4), onMovieClick)
            }
        }

        item {
            CuratedCollectionsSection()
        }
    }
}

@Composable
fun CurrentlyWatchingSection(movies: List<Movie>, onMovieClick: (Movie) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Currently\nWatching",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )
            Text(
                text = "${movies.size} ACTIVE\nSESSIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(movies) { movie ->
                WatchingCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

@Composable
fun WatchingCard(movie: Movie, onClick: () -> Unit) {
    Column(modifier = Modifier.width(240.dp).clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = movie.backdropUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("RESUME WATCHING", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("CONTINUE", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TrendingNowSection(movie: Movie, onMovieClick: (Movie) -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "Trending Now",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(16.dp))
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
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 300f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "TRENDING TODAY",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    movie.title.uppercase(),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    maxLines = 2
                )
                Text(
                    movie.synopsis,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onMovieClick(movie) },
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View Details", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickGridSection(movies: List<Movie>, onMovieClick: (Movie) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Popular Hits",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        movies.chunked(2).forEach { rowMovies ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowMovies.forEach { movie ->
                    GridCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowMovies.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GridCard(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
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
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 200f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(String.format("%.1f TMDB", movie.rating), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CuratedCollectionsSection() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "Curated Collections",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        CollectionItem(
            title = "Director's Cut",
            description = "Exclusive commentaries and behind-the-scenes masterclasses.",
            icon = Icons.Default.MovieFilter,
            iconColor = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        CollectionItem(
            title = "A.I. Narratives",
            description = "Exploring the boundaries of technology and human storytelling.",
            icon = Icons.Default.AutoAwesome,
            iconColor = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        CollectionItem(
            title = "Color Theory",
            description = "Cinematography focused on the psychology of visual palettes.",
            icon = Icons.Default.Palette,
            iconColor = Color(0xFFB39DDB)
        )
    }
}

@Composable
fun CollectionItem(title: String, description: String, icon: ImageVector, iconColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF121212)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

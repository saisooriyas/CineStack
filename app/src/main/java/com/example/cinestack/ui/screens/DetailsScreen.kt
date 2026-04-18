package com.example.cinestack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    movie: Movie,
    onBackClick: () -> Unit,
    viewModel: SearchViewModel,
    onPersonClick: (String) -> Unit,
    onMovieClick: (Movie) -> Unit = {}
) {
    val cast by viewModel.cast.collectAsState()
    val movieDetails by viewModel.movieDetails.collectAsState()
    val personDetails by viewModel.personDetails.collectAsState()

    // Check if the current details belong to the movie we are viewing
    val isCorrectDetails = movieDetails?.id == movie.id

    // Capture the movie in a stable state for this screen instance
    val stableMovie = remember(movie.id) { movie }

    var isInLibrary by remember(stableMovie.id) { mutableStateOf(viewModel.isInLibrary(stableMovie.id)) }
    val libraryMovie = remember(stableMovie.id) { viewModel.getLibraryMovie(stableMovie.id) }

    var personalScore by remember(stableMovie.id) { mutableStateOf(libraryMovie?.userRating?.toFloat() ?: 0f) }
    var status by remember(stableMovie.id) { mutableStateOf(libraryMovie?.userStatus ?: "") }
    var season by remember(stableMovie.id) { mutableStateOf(libraryMovie?.currentSeason ?: 1) }
    var episode by remember(stableMovie.id) { mutableStateOf(libraryMovie?.currentEpisode ?: 1) }

    val scrollState = rememberScrollState()

    var showPersonDialog by remember { mutableStateOf(false) }
    var showAddToGroupDialog by remember { mutableStateOf(false) }
    val allGroups by viewModel.allGroups.collectAsState()

    LaunchedEffect(stableMovie.id) {
        viewModel.fetchCast(stableMovie.id, stableMovie.mediaType)
        scrollState.scrollTo(0)
    }

    if (showPersonDialog && personDetails != null) {
        AlertDialog(
            onDismissRequest = {
                showPersonDialog = false
                viewModel.clearPersonDetails()
            },
            confirmButton = {
                TextButton(onClick = {
                    onPersonClick(personDetails?.id ?: "")
                    showPersonDialog = false
                    viewModel.clearPersonDetails()
                }) {
                    Text("SEE FULL PROFILE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPersonDialog = false
                    viewModel.clearPersonDetails()
                }) {
                    Text("CLOSE", color = Color.Gray)
                }
            },
            title = {
                Text(personDetails?.name ?: "", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = "https://image.tmdb.org/t/p/w200${personDetails?.profilePath}",
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            if (!personDetails?.birthday.isNullOrEmpty()) {
                                Text("Born: ${personDetails?.birthday}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            if (!personDetails?.placeOfBirth.isNullOrEmpty()) {
                                Text("Place: ${personDetails?.placeOfBirth}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        personDetails?.biography ?: "No biography available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            },
            containerColor = Color(0xFF1A1A1A),
            textContentColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Hero Image Section with tall aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
            ) {
                AsyncImage(
                    model = stableMovie.backdropUrl.ifEmpty { stableMovie.posterUrl },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Gradient Overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.7f),
                                0.4f to Color.Transparent,
                                1f to Color.Black
                            )
                        )
                )

                // Top Header (CineStack logo and icons as seen in image)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CineStack",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showAddToGroupDialog = true }) {
                                Icon(Icons.Default.LibraryAdd, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color.Gray) {
                                Icon(Icons.Default.Person, null, tint = Color.White)
                            }
                        }
                    }

                // Title and Tags Section
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (status.isNotEmpty()) {
                            StatusTag(status.uppercase(), Color(0xFFFF8A65).copy(alpha = 0.2f), Color(0xFFFF8A65))
                        }
                        StatusTag("SCI-FI NOIR", Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.7f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stableMovie.title.uppercase().split(" ").firstOrNull() ?: "",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 44.sp
                    )
                    Text(
                        text = stableMovie.title.uppercase().split(" ").drop(1).joinToString(" "),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 44.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        val studio = if (isCorrectDetails) (movieDetails?.productionCompanies?.firstOrNull()?.name ?: "N/A") else "..."
                        val releaseYear = if (isCorrectDetails) (movieDetails?.releaseDate?.split("-")?.firstOrNull() ?: stableMovie.releaseYear.toString()) else stableMovie.releaseYear.toString()

                        StatItem("STUDIO", studio)
                        StatItem("RELEASE", releaseYear)
                        StatItem("SCORE", String.format("%.1f", stableMovie.rating), isHighlight = true)
                    }
                }
            }

            // Interactive Controls Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF121212),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        if (status.isEmpty()) "STATUS" else "UPDATE STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusButton("PLAN TO WATCH", Icons.Default.CalendarToday, status == "Plan", Modifier.weight(1f)) { status = "Plan" }
                            StatusButton("WATCHING", Icons.Default.PlayCircle, status == "Watching", Modifier.weight(1f), activeColor = Color(0xFFFF5722)) { status = "Watching" }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusButton("COMPLETED", Icons.Default.CheckCircle, status == "Completed", Modifier.weight(1f)) { status = "Completed" }
                            StatusButton("DROPPED", Icons.Default.Cancel, status == "Dropped", Modifier.weight(1f)) { status = "Dropped" }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PERSONAL SCORE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = buildString {
                                append(String.format("%.1f", personalScore))
                                append(" / 10")
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Slider(
                        value = personalScore,
                        onValueChange = {
                            personalScore = Math.round(it * 10f) / 10f
                        },
                        valueRange = 1f..10f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1.0", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("5.0", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("10.0", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            viewModel.addToLibrary(stableMovie, status, personalScore.toDouble(), season, episode)
                            isInLibrary = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (isInLibrary) "UPDATE PROGRESS" else "SAVE PROGRESS", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            // Season and Episode Tracker (Only for TV/Anime when Watching)
            if ((stableMovie.mediaType == "tv" || stableMovie.mediaType == "anime") && status == "Watching") {
                val maxSeasons = movieDetails?.numberOfSeasons ?: 1
                val currentSeasonData = movieDetails?.seasons?.find { it.seasonNumber == season }
                val maxEpisodesInSeason = currentSeasonData?.episodeCount ?: movieDetails?.numberOfEpisodes ?: 999

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF121212),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SEASON", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (season > 1) {
                                        season--
                                        episode = 1
                                    }
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Remove, null, tint = Color.White)
                                }
                                Text(
                                    season.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = {
                                    if (season < maxSeasons) {
                                        season++
                                        episode = 1
                                    }
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, null, tint = Color.White)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("EPISODE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (episode > 1) episode-- }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Remove, null, tint = Color.White)
                                }
                                Text(
                                    episode.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = {
                                    if (episode < maxEpisodesInSeason) {
                                        episode++
                                    }
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Info Grid (Duration / Original)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val durationText = if (isCorrectDetails && movieDetails?.runtime != null) "${movieDetails?.runtime} min" else stableMovie.duration
                val lang = if (isCorrectDetails) (movieDetails?.originalLanguage?.uppercase() ?: "N/A") else "N/A"

                InfoBox(Icons.Default.Schedule, "DURATION", durationText, Modifier.weight(1f))
                InfoBox(Icons.Default.Language, "ORIGINAL", lang, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Synopsis
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    "SYNOPSIS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stableMovie.synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cast
            if (cast.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "MAIN CAST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    cast.take(10).forEach { member ->
                        CastMemberItem(member) {
                            viewModel.fetchPersonDetails(member.id)
                            showPersonDialog = true
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Gallery
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    "GALLERY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GalleryItem(stableMovie.backdropUrl, Modifier.weight(1f))
                    GalleryItem(stableMovie.posterUrl, Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recommendations / Suggestions
            val recommendations by viewModel.recommendations.collectAsState()
            if (recommendations.isNotEmpty()) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        "SUGGESTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recommendations) { suggestion ->
                            SuggestionCard(suggestion) {
                                onMovieClick(suggestion)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        // Floating Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(top = 48.dp, start = 8.dp)
                .size(40.dp)
        ) {
            Icon(Icons.Default.ChevronLeft, "Back", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        if (showAddToGroupDialog) {
            AlertDialog(
                onDismissRequest = { showAddToGroupDialog = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Add to Group", color = Color.White) },
                text = {
                    Column {
                        if (allGroups.isEmpty()) {
                            Text("No groups created yet. Create one in Library > Groups", color = Color.Gray)
                        } else {
                            allGroups.forEach { group ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            viewModel.addItemToGroup(group.id, stableMovie)
                                            showAddToGroupDialog = false
                                        },
                                    color = Color.Transparent
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(group.name, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddToGroupDialog = false }) { Text("CANCEL") }
                }
            )
        }
    }
}

@Composable
fun StatusTag(text: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun StatItem(label: String, value: String, isHighlight: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor else Color.White.copy(alpha = 0.05f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (isSelected) Color.Black else Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else Color.White
            )
        }
    }
}

@Composable
fun InfoBox(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF121212),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun CastMemberItem(member: com.example.cinestack.data.remote.TMDBCast, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF121212),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w200${member.profilePath}",
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text(member.character, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SuggestionCard(movie: Movie, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = movie.posterUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 2,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GalleryItem(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier.height(100.dp).clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop
    )
}

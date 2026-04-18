package com.example.cinestack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cinestack.data.model.Movie
import com.example.cinestack.data.remote.TMDBCombinedCredit
import com.example.cinestack.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailsScreen(
    personId: String,
    onBackClick: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    viewModel: SearchViewModel
) {
    val person by viewModel.personDetails.collectAsState()
    var selectedTab by remember { mutableStateOf("Movies") }

    LaunchedEffect(personId) {
        viewModel.fetchPersonDetails(personId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (person != null) {
            val credits = person?.combinedCredits
            val movies = credits?.cast?.filter { it.mediaType == "movie" }?.sortedByDescending { it.releaseDate } ?: emptyList()
            val tvShows = credits?.cast?.filter { it.mediaType == "tv" }?.sortedByDescending { it.firstAirDate } ?: emptyList()
            val directing = credits?.crew?.filter { it.job == "Director" || it.job == "Series Director" }?.sortedByDescending { it.releaseDate ?: it.firstAirDate } ?: emptyList()

            val tabs = mutableListOf<String>()
            if (movies.isNotEmpty()) tabs.add("Movies")
            if (tvShows.isNotEmpty()) tabs.add("TV Shows")
            if (directing.isNotEmpty()) tabs.add("Directing")

            if (!tabs.contains(selectedTab) && tabs.isNotEmpty()) {
                selectedTab = tabs.first()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Image Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(550.dp)
                ) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/original${person?.profilePath}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.5f to Color.Black.copy(alpha = 0.3f),
                                    0.8f to Color.Black.copy(alpha = 0.8f),
                                    1f to Color.Black
                                )
                            )
                    )

                    // Profile Content
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        // Small overlay profile pic
                        Surface(
                            modifier = Modifier
                                .size(100.dp, 140.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            AsyncImage(
                                model = "https://image.tmdb.org/t/p/w342${person?.profilePath}",
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = person?.name ?: "",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = person?.knownForDepartment?.uppercase() ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoBox("BORN", person?.birthday?.let { formatDate(it) } ?: "N/A")
                            InfoBox("AGE", person?.birthday?.let { calculateAge(it) } ?: "N/A")
                            InfoBox("BIRTHPLACE", person?.placeOfBirth ?: "N/A")
                        }
                    }
                }

                // Biography Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(24.dp).background(MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Biography",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (person?.biography.isNullOrEmpty()) "No biography available." else person?.biography!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Tabs Section
                if (tabs.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            if (tabs.indexOf(selectedTab) < tabPositions.size && tabs.indexOf(selectedTab) != -1) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        },
                        edgePadding = 24.dp
                    ) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tab,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (tab == "TV Shows") {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                color = Color(0xFFFF5722),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "HOT",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val currentList = when (selectedTab) {
                        "Movies" -> movies
                        "TV Shows" -> tvShows
                        "Directing" -> directing
                        else -> emptyList()
                    }

                    Box(modifier = Modifier.height(600.dp).padding(horizontal = 24.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentList) { credit ->
                                CreditCard(credit = credit, onClick = { 
                                    onMovieClick(viewModel.mapCombinedCreditToMovie(credit)) 
                                })
                            }
                        }
                    }
                }

                // Official Links / Footer
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Official Website", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Access portfolio and contact agency", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) { Icon(Icons.Default.Public, null, tint = Color.Gray) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Language, null, tint = Color.Gray) }
                        IconButton(onClick = {}) { Icon(Icons.Default.PlayCircleOutline, null, tint = Color.Gray) }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
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
    }
}

@Composable
private fun InfoBox(label: String, value: String) {
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(100.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CreditCard(credit: TMDBCombinedCredit, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w342${credit.posterPath}",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Year badge
            val year = (credit.releaseDate ?: credit.firstAirDate)?.take(4)
            if (year != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = year,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = credit.title ?: credit.name ?: "Unknown",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (!credit.character.isNullOrEmpty()) "Voice: ${credit.character}" else credit.job ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatDate(date: String): String {
    // Simple mapper for display
    return try {
        val parts = date.split("-")
        if (parts.size == 3) {
            val months = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            "${months[parts[1].toInt()]} ${parts[2]}, ${parts[0]}"
        } else date
    } catch (e: Exception) {
        date
    }
}

fun calculateAge(birthday: String): String {
    return try {
        val year = birthday.split("-")[0].toInt()
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        "${currentYear - year} Years"
    } catch (e: Exception) {
        "N/A"
    }
}

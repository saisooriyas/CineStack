package com.example.cinestack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cinestack.data.model.Movie
import com.example.cinestack.ui.viewmodel.SearchViewModel

@Composable
fun LibraryScreen(
    viewModel: SearchViewModel,
    onMovieClick: (Movie) -> Unit,
    onGroupClick: (Int) -> Unit
) {
    val library by viewModel.library.collectAsState()
    val allGroups by viewModel.allGroups.collectAsState()

    var selectedTab by remember { mutableStateOf("ALL MEDIA") }
    val tabs = listOf("ALL MEDIA", "MOVIE", "TV SERIES", "Anime", "GROUPS")

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    val filteredLibrary = when (selectedTab) {
        "MOVIE" -> library.filter { it.mediaType == "movie" }
        "TV SERIES" -> library.filter { it.mediaType == "tv" }
        "Anime" -> library.filter { it.mediaType == "anime" }
        else -> library
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            CuratorInsightsCard(library = library)
        }

        item {
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 24.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                tab,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == tab) Color.White else Color.Gray
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (selectedTab == "GROUPS") {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeaderWithLine("MY GROUPS", MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val rootGroups = allGroups.filter { it.parentGroupId == null }
            if (rootGroups.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No groups created yet.", color = Color.Gray)
                    }
                }
            } else {
                items(rootGroups) { group ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clickable { onGroupClick(group.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A1A1A)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(group.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showCreateGroupDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CREATE NEW GROUP", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        } else if (filteredLibrary.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Your library is empty.\nSearch and add content!",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            item {
                SectionHeaderWithLine("MY COLLECTION", MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
            }

            val chunks = filteredLibrary.chunked(2)
            items(chunks) { chunk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    chunk.forEach { movie ->
                        LibraryMediaCard(
                            title = movie.title,
                            subtitle = "${movie.releaseYear} • ${String.format("%.1f", movie.userRating)}/10",
                            imageUrl = movie.posterUrl,
                            badge = if (movie.userStatus == "Watching") "EP ${movie.currentEpisode}" else if (movie.userStatus.isNotEmpty()) movie.userStatus.uppercase() else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onMovieClick(movie) }
                        )
                    }
                    if (chunk.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("New Group", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    placeholder = { Text("Group Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newGroupName.isNotBlank()) {
                        viewModel.createGroup(newGroupName)
                        newGroupName = ""
                        showCreateGroupDialog = false
                    }
                }) { Text("CREATE", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }
}

@Composable
fun CuratorInsightsCard(library: List<Movie>) {
    val librarySize = library.size
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF151515)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Curator\nInsights",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "LIBRARY ANALYTICS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$librarySize",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "ITEMS IN COLLECTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
            LinearProgressIndicator(
                progress = { (librarySize / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.DarkGray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Progress towards level 1 curator",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Genre\nHeatmap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val movieCount = library.count { it.mediaType == "movie" }
            val tvCount = library.count { it.mediaType == "tv" }
            val animeCount = library.count { it.mediaType == "anime" }
            val total = if (librarySize > 0) librarySize.toFloat() else 1f

            val moviePercent = (movieCount / total * 100).toInt()
            val tvPercent = (tvCount / total * 100).toInt()
            val animePercent = (animeCount / total * 100).toInt()
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeatmapTile("MOVIE", "$moviePercent%", MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                HeatmapTile("TV/ANIME", "${tvPercent + animePercent}%", MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun HeatmapTile(label: String, value: String, bgColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (bgColor == Color.DarkGray) Color.Gray else Color.Black.copy(alpha = 0.6f))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (bgColor == Color.DarkGray) Color.White else Color.Black)
        }
    }
}

@Composable
fun SectionHeaderWithLine(title: String, lineColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(4.dp).background(lineColor, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun LibraryMediaCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    imageUrl: String,
    progress: Float? = null,
    badge: String? = null
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            
            if (badge != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

package com.example.cinestack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
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
import com.example.cinestack.data.local.StarredPerformerEntity
import com.example.cinestack.data.model.Movie
import com.example.cinestack.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.delay

// ── Tab definitions ───────────────────────────────────────────────────────────
private val LIBRARY_TABS = listOf("ALL", "MOVIES", "TV", "ANIME", "XXX")
private val STATUS_ORDER  = listOf("Watching", "Completed", "Plan", "Dropped")

@Composable
fun LibraryScreen(
    viewModel: SearchViewModel,
    onMovieClick: (Movie) -> Unit,
    onGroupClick: (Int) -> Unit
) {
    val library          by viewModel.library.collectAsState()
    val allGroups        by viewModel.allGroups.collectAsState()
    val starredPerformers by viewModel.starredPerformers.collectAsState()

    var selectedTab by remember { mutableStateOf("ALL") }
    var showCreateGroupDialog   by remember { mutableStateOf(false) }
    var showManagePerformers    by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<com.example.cinestack.data.local.GroupEntity?>(null) }
    var newGroupName  by remember { mutableStateOf("") }

    // Build content per tab
    val tabContent: List<Movie> = when (selectedTab) {
        "MOVIES" -> library.filter { it.mediaType == "movie" }
        "TV"     -> library.filter { it.mediaType == "tv" }
        "ANIME"  -> library.filter { it.mediaType == "anime" }
        "XXX"    -> library.filter { it.mediaType == "xxx" || it.mediaType == "xxx_movie" }
        else     -> library.filter { it.mediaType != "xxx" && it.mediaType != "xxx_movie" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Curator Insights card ─────────────────────────────────────────────
        CuratorInsightsCard(library = library)

        // ── Tab row ───────────────────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = LIBRARY_TABS.indexOf(selectedTab),
            containerColor   = Color.Transparent,
            contentColor     = MaterialTheme.colorScheme.primary,
            edgePadding      = 16.dp,
            divider          = {},
            indicator        = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[LIBRARY_TABS.indexOf(selectedTab)]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            LIBRARY_TABS.forEach { tab ->
                val count = when (tab) {
                    "MOVIES" -> library.count { it.mediaType == "movie" }
                    "TV"     -> library.count { it.mediaType == "tv" }
                    "ANIME"  -> library.count { it.mediaType == "anime" }
                    "XXX"    -> library.count { it.mediaType == "xxx" || it.mediaType == "xxx_movie" }
                    else     -> library.count { it.mediaType != "xxx" && it.mediaType != "xxx_movie" }
                }
                Tab(
                    selected = selectedTab == tab,
                    onClick  = { selectedTab = tab },
                    text     = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tab,
                                style      = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color      = if (selectedTab == tab) Color.White else Color.Gray
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedTab == tab) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "$count",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = if (selectedTab == tab) Color.Black else Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Body ──────────────────────────────────────────────────────────────
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            when (selectedTab) {
                "XXX" -> {
                    // Starred performer groups + "Other" bucket
                    item {
                        XxxLibraryHeader(
                            starredPerformers = starredPerformers,
                            onManageClick     = { showManagePerformers = true }
                        )
                    }

                    val xxxItems = library.filter { it.mediaType == "xxx" || it.mediaType == "xxx_movie" }

                    if (xxxItems.isEmpty()) {
                        item { EmptyTabMessage("No XXX content saved yet.") }
                    } else {
                        // One section per starred performer
                        starredPerformers.forEach { performer ->
                            val matchedItems = xxxItems.filter { movie ->
                                movie.castNames.any { castName ->
                                    castName.trim().equals(performer.name.trim(), ignoreCase = true)
                                }
                            }
                            if (matchedItems.isNotEmpty()) {
                                item {
                                    PerformerSection(
                                        performer = performer,
                                        items     = matchedItems,
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        }

                        // "Other" — content with no matching starred performer
                        val otherItems = xxxItems.filter { movie ->
                            starredPerformers.none { performer ->
                                movie.castNames.any { castName ->
                                    castName.trim().equals(performer.name.trim(), ignoreCase = true)
                                }
                            }
                        }
                        if (otherItems.isNotEmpty()) {
                            item {
                                CollapsibleStatusSection(
                                    title  = "OTHER",
                                    color  = Color.Gray,
                                    items  = otherItems,
                                    onMovieClick = onMovieClick
                                )
                            }
                        }
                    }
                }

                "GROUPS" -> {
                    // Groups tab
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeaderWithLine("MY GROUPS", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val rootGroups = allGroups.filter { it.parentGroupId == null }
                    if (rootGroups.isEmpty()) {
                        item { EmptyTabMessage("No groups created yet.") }
                    } else {
                        items(rootGroups) { group ->
                            Surface(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                                    .clickable { onGroupClick(group.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1A1A1A)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(group.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { groupToDelete = group }) {
                                        Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick  = { showCreateGroupDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CREATE NEW GROUP", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                else -> {
                    // ALL / MOVIES / TV / ANIME — status-bucketed grid
                    if (tabContent.isEmpty()) {
                        item { EmptyTabMessage("Nothing here yet.\nSearch and add content!") }
                    } else {
                        STATUS_ORDER.forEach { status ->
                            val bucket = tabContent.filter { it.userStatus == status }
                            if (bucket.isNotEmpty()) {
                                item {
                                    CollapsibleStatusSection(
                                        title        = status.uppercase(),
                                        color        = statusColor(status),
                                        items        = bucket,
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        }
                        // Items with no recognised status
                        val other = tabContent.filter { it.userStatus !in STATUS_ORDER }
                        if (other.isNotEmpty()) {
                            item {
                                CollapsibleStatusSection(
                                    title        = "UNSORTED",
                                    color        = Color.Gray,
                                    items        = other,
                                    onMovieClick = onMovieClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Manage Performers bottom-sheet style dialog ───────────────────────────
    if (showManagePerformers) {
        ManagePerformersDialog(
            viewModel  = viewModel,
            onDismiss  = { showManagePerformers = false }
        )
    }

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            containerColor   = Color(0xFF1A1A1A),
            title            = { Text("Delete Group", color = Color.White) },
            text             = { Text("Are you sure you want to delete '${groupToDelete?.name}'?", color = Color.Gray) },
            confirmButton    = {
                TextButton(onClick = { groupToDelete?.let { viewModel.deleteGroup(it.id) }; groupToDelete = null }) {
                    Text("DELETE", color = Color.Red)
                }
            },
            dismissButton    = { TextButton(onClick = { groupToDelete = null }) { Text("CANCEL", color = Color.White) } }
        )
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            containerColor   = Color(0xFF1A1A1A),
            title            = { Text("New Group", color = Color.White) },
            text             = {
                OutlinedTextField(
                    value         = newGroupName,
                    onValueChange = { newGroupName = it },
                    placeholder   = { Text("Group Name") },
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newGroupName.isNotBlank()) {
                        viewModel.createGroup(newGroupName); newGroupName = ""; showCreateGroupDialog = false
                    }
                }) { Text("CREATE", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }
}

// ── Manage Performers dialog ──────────────────────────────────────────────────

@Composable
fun ManagePerformersDialog(viewModel: SearchViewModel, onDismiss: () -> Unit) {
    val starred     by viewModel.starredPerformers.collectAsState()
    val suggestions by viewModel.performerSuggestions.collectAsState()
    var query       by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        delay(300L)
        if (query.length >= 2) viewModel.searchPerformers(query)
        else viewModel.clearPerformerSuggestions()
    }

    AlertDialog(
        onDismissRequest = { onDismiss(); viewModel.clearPerformerSuggestions() },
        containerColor   = Color(0xFF1A1A1A),
        title            = { Text("Starred Performers", color = Color.White, fontWeight = FontWeight.Bold) },
        text             = {
            Column {
                // Search field
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("Search performer…", color = Color.Gray) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    trailingIcon  = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = ""; viewModel.clearPerformerSuggestions() }) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    },
                    colors  = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor     = Color.White, unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth()
                )

                // Suggestions
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF252525)) {
                        Column {
                            suggestions.take(5).forEach { p ->
                                val isStarred = viewModel.isPerformerStarred(p.id)
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            if (isStarred) viewModel.unstarPerformer(p.id)
                                            else viewModel.starPerformer(p.id, p.name, p.image ?: "")
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = p.image, contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(p.name, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Icon(
                                        if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                        null,
                                        tint = if (isStarred) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Current starred list
                if (starred.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("STARRED", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    starred.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = p.imageUrl, contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(p.name, color = Color.White, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.unstarPerformer(p.id) }) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else if (suggestions.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Search for performers above to star them.\nStarred performers get their own shelf in your XXX library.",
                        color = Color.Gray, style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); viewModel.clearPerformerSuggestions() }) {
                Text("DONE", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

// ── XXX library header ────────────────────────────────────────────────────────

@Composable
fun XxxLibraryHeader(
    starredPerformers: List<StarredPerformerEntity>,
    onManageClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("XXX Library", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                "${starredPerformers.size} starred performer${if (starredPerformers.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall, color = Color.Gray
            )
        }
        OutlinedButton(
            onClick = onManageClick,
            shape   = RoundedCornerShape(50),
            border  = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Manage", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ── Performer section (starred) ───────────────────────────────────────────────

@Composable
fun PerformerSection(
    performer: StarredPerformerEntity,
    items: List<Movie>,
    onMovieClick: (Movie) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = performer.imageUrl, contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(performer.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("${items.size} item${if (items.size != 1) "s" else ""}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = Color.Gray
            )
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.padding(bottom = 16.dp)
            ) {
                items(items) { movie ->
                    XxxLibraryCard(movie = movie, onClick = { onMovieClick(movie) })
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 24.dp))
    }
}

// ── Collapsible status section (for ALL/MOVIES/TV/ANIME and XXX "Other") ──────

@Composable
fun CollapsibleStatusSection(
    title: String,
    color: Color,
    items: List<Movie>,
    onMovieClick: (Movie) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        // Clickable header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                title, style = MaterialTheme.typography.labelLarge, color = Color.White,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f)
            )
            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
                Text(
                    "${items.size}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                items.chunked(2).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        chunk.forEach { movie ->
                            LibraryMediaCard(
                                title    = movie.title,
                                subtitle = buildSubtitle(movie),
                                imageUrl = movie.posterUrl,
                                badge    = if (movie.userStatus == "Watching") "EP ${movie.currentEpisode}" else null,
                                modifier = Modifier.weight(1f).clickable { onMovieClick(movie) }
                            )
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 24.dp))
    }
}

// ── XXX horizontal card (used inside performer rows) ─────────────────────────

@Composable
fun XxxLibraryCard(movie: Movie, onClick: () -> Unit) {
    Column(modifier = Modifier.width(140.dp).clickable { onClick() }) {
        Box(
            modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            val isMovie = movie.mediaType == "xxx_movie"
            Surface(
                color = if (isMovie) Color(0xFF880000) else Color(0xFFAA0000),
                shape = RoundedCornerShape(bottomEnd = 6.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(if (isMovie) "MOVIE" else "SCENE", style = MaterialTheme.typography.labelSmall, color = Color.White,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 8.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(movie.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
        if (movie.releaseYear > 0) Text("${movie.releaseYear}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildSubtitle(movie: Movie): String {
    val year = if (movie.releaseYear > 0) "${movie.releaseYear}" else ""
    val rating = if (movie.userRating > 0) "★ ${String.format("%.1f", movie.userRating)}" else ""
    return listOf(year, rating).filter { it.isNotEmpty() }.joinToString(" · ")
}

private fun statusColor(status: String) = when (status) {
    "Watching"  -> Color(0xFFFF5722)
    "Completed" -> Color(0xFF46EEDD)
    "Plan"      -> Color(0xFF7C4DFF)
    "Dropped"   -> Color(0xFFFF1744)
    else        -> Color.Gray
}

@Composable
fun EmptyTabMessage(message: String) {
    Box(
        modifier         = Modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message, color = Color.Gray, style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ── CuratorInsightsCard ────────────────────────────────────────────────────────

@Composable
fun CuratorInsightsCard(library: List<Movie>) {
    val librarySize  = library.size
    val movieCount   = library.count { it.mediaType == "movie" }
    val tvCount      = library.count { it.mediaType == "tv" }
    val animeCount   = library.count { it.mediaType == "anime" }
    val xxxCount     = library.count { it.mediaType == "xxx" || it.mediaType == "xxx_movie" }
    val total        = if (librarySize > 0) librarySize.toFloat() else 1f
    val avgRating    = library.filter { it.userRating > 0 }.map { it.userRating }.average()

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape    = RoundedCornerShape(24.dp),
        color    = Color(0xFF111111)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Curator\nInsights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("LIBRARY ANALYTICS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$librarySize", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text("ITEMS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress    = { (librarySize / 100f).coerceIn(0f, 1f) },
                modifier    = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color       = MaterialTheme.colorScheme.primary,
                trackColor  = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Progress to Level 1 Curator", style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.6f))

            Spacer(modifier = Modifier.height(20.dp))

            // 4-tile grid
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsightTile("MOVIES",  movieCount, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                InsightTile("TV",      tvCount,    Color(0xFF7C4DFF),                 Modifier.weight(1f))
                InsightTile("ANIME",   animeCount, Color(0xFFFF5722),                 Modifier.weight(1f))
                InsightTile("XXX",     xxxCount,   Color(0xFF880000),                 Modifier.weight(1f))
            }

            if (avgRating.isFinite() && avgRating > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Avg personal rating: ${String.format("%.1f", avgRating)}/10", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun InsightTile(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(60.dp), shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), fontSize = 9.sp)
        }
    }
}

// ── SectionHeaderWithLine (shared) ────────────────────────────────────────────

@Composable
fun SectionHeaderWithLine(title: String, lineColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(4.dp).background(lineColor, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)))
        )
    }
}

// ── LibraryMediaCard (shared — used in status sections) ───────────────────────

@Composable
fun LibraryMediaCard(
    modifier:  Modifier = Modifier,
    title:     String,
    subtitle:  String,
    imageUrl:  String,
    badge:     String? = null
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp))) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
            if (badge != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                ) {
                    Text(badge, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                if (subtitle.isNotEmpty()) Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
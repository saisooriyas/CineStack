package com.example.cinestack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cinestack.data.model.sampleMovies
import com.example.cinestack.ui.screens.DashboardScreen
import com.example.cinestack.ui.screens.DetailsScreen
import com.example.cinestack.ui.screens.HomeScreen
import com.example.cinestack.ui.screens.LibraryScreen
import com.example.cinestack.ui.screens.PersonDetailsScreen
import com.example.cinestack.ui.screens.ProfileScreen
import com.example.cinestack.ui.theme.CineStackTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.example.cinestack.data.model.Movie
import com.example.cinestack.ui.viewmodel.SearchViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CineStackTheme {
                CineStackApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "DASHBOARD", Icons.Default.GridView)
    object Search : Screen("search", "SEARCH", Icons.Default.Search)
    object Library : Screen("library", "LIBRARY", Icons.Default.ViewCarousel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineStackApp() {
    val navController = rememberNavController()
    val searchViewModel: SearchViewModel = viewModel()
    val items = listOf(
        Screen.Dashboard,
        Screen.Search,
        Screen.Library,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevel = items.any { it.route == currentDestination?.route }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            if (isTopLevel) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "CINESTACK",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate("profile") }) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Glassmorphic background
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = CircleShape,
                        color = Color(0xFF1A1A1A).copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)
                            )
                        ),
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                val contentColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            screen.icon,
                                            contentDescription = null,
                                            tint = contentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        if (selected) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .size(4.dp)
                                                    .background(contentColor, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Search.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = searchViewModel,
                    onMovieClick = { movie ->
                        navController.navigate("details/${movie.id}/${movie.mediaType}")
                    }
                )
            }
            composable(Screen.Search.route) {
                HomeScreen(
                    onMovieClick = { movie ->
                        navController.navigate("details/${movie.id}/${movie.mediaType}")
                    },
                    viewModel = searchViewModel
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel = searchViewModel,
                    onMovieClick = { movie ->
                        navController.navigate("details/${movie.id}/${movie.mediaType}")
                    },
                    onGroupClick = { groupId ->
                        navController.navigate("group/$groupId")
                    }
                )
            }
            composable("group/{groupId}") { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull() ?: 0
                com.example.cinestack.ui.screens.GroupDetailsScreen(
                    groupId = groupId,
                    viewModel = searchViewModel,
                    onBackClick = { navController.popBackStack() },
                    onMovieClick = { movie ->
                        navController.navigate("details/${movie.id}/${movie.mediaType}")
                    },
                    onSubGroupClick = { subId ->
                        navController.navigate("group/$subId")
                    }
                )
            }
            composable("details/{movieId}/{mediaType}") { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId")?.toIntOrNull()
                val library by searchViewModel.library.collectAsState()
                
                // Using a derived state to find the movie from cache or library
                val movie = remember(movieId, library) {
                    searchViewModel.getMovieFromCache(movieId) ?: sampleMovies.find { it.id == movieId }
                }
                
                if (movie != null) {
                    DetailsScreen(
                        movie = movie,
                        onBackClick = { navController.popBackStack() },
                        viewModel = searchViewModel,
                        onPersonClick = { personId ->
                            navController.navigate("person/$personId")
                        },
                        onMovieClick = { selectedMovie ->
                            navController.navigate("details/${selectedMovie.id}/${selectedMovie.mediaType}")
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            composable("person/{personId}") { backStackEntry ->
                val personId = backStackEntry.arguments?.getString("personId")?.toIntOrNull() ?: 0
                PersonDetailsScreen(
                    personId = personId,
                    onBackClick = { navController.popBackStack() },
                    viewModel = searchViewModel
                )
            }
            composable("profile") {
                ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    viewModel = searchViewModel
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
    }
}

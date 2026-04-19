package com.example.cinestack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cinestack.data.model.sampleMovies
import com.example.cinestack.ui.screens.DashboardScreen
import com.example.cinestack.ui.screens.DetailsScreen
import com.example.cinestack.ui.screens.GroupDetailsScreen
import com.example.cinestack.ui.screens.HomeScreen
import com.example.cinestack.ui.screens.LibraryScreen
import com.example.cinestack.ui.screens.PersonDetailsScreen
import com.example.cinestack.ui.screens.ProfileScreen
import com.example.cinestack.ui.theme.CineStackTheme
import com.example.cinestack.ui.viewmodel.SearchViewModel

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
    object Search   : Screen("search",    "SEARCH",    Icons.Default.Search)
    object Library  : Screen("library",   "LIBRARY",   Icons.Default.ViewCarousel)
}

// Transition durations
private const val NAV_DURATION   = 280
private const val SLIDE_DURATION = 320

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineStackApp() {
    val navController   = rememberNavController()
    val searchViewModel : SearchViewModel = viewModel()

    val topLevelRoutes = listOf(Screen.Dashboard, Screen.Search, Screen.Library)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevel = topLevelRoutes.any { it.route == currentDestination?.route }

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
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color.White.copy(alpha = 0.1f)
                                )
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
                            Icon(Icons.Default.Notifications, null, tint = Color.White)
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
                BottomNavBar(
                    items        = topLevelRoutes,
                    currentDest  = currentDestination,
                    onItemClick  = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Search.route,
            modifier         = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Default enter/exit for all routes (fast fade)
            enterTransition  = { fadeIn(tween(NAV_DURATION)) },
            exitTransition   = { fadeOut(tween(NAV_DURATION)) },
            popEnterTransition  = { fadeIn(tween(NAV_DURATION)) },
            popExitTransition   = { fadeOut(tween(NAV_DURATION)) }
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel   = searchViewModel,
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
                    viewModel    = searchViewModel,
                    onMovieClick = { movie ->
                        navController.navigate("details/${movie.id}/${movie.mediaType}")
                    },
                    onGroupClick = { groupId ->
                        navController.navigate("group/$groupId")
                    }
                )
            }

            // Detail screen — slides up from bottom
            composable(
                route = "details/{movieId}/{mediaType}",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec  = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeIn(tween(SLIDE_DURATION))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeOut(tween(SLIDE_DURATION))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec  = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeIn(tween(SLIDE_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeOut(tween(SLIDE_DURATION))
                }
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId")
                val library by searchViewModel.library.collectAsState()
                val movie = remember(movieId, library) {
                    searchViewModel.getMovieFromCache(movieId)
                        ?: sampleMovies.find { it.id == movieId }
                }
                if (movie != null) {
                    DetailsScreen(
                        movie        = movie,
                        onBackClick  = { navController.popBackStack() },
                        viewModel    = searchViewModel,
                        onPersonClick = { personId ->
                            navController.navigate("person/$personId")
                        },
                        onMovieClick = { selectedMovie ->
                            navController.navigate("details/${selectedMovie.id}/${selectedMovie.mediaType}")
                        }
                    )
                } else {
                    Box(
                        modifier            = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment    = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Group, Person, Profile — slide in from right
            composable(
                route = "group/{groupId}",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec  = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeIn(tween(SLIDE_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeOut(tween(SLIDE_DURATION))
                }
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull() ?: 0
                GroupDetailsScreen(
                    groupId      = groupId,
                    viewModel    = searchViewModel,
                    onBackClick  = { navController.popBackStack() },
                    onMovieClick = { movie ->
                        navController.navigate("details/${movie.id}/${movie.mediaType}")
                    },
                    onSubGroupClick = { subId ->
                        navController.navigate("group/$subId")
                    }
                )
            }

            composable(
                route = "person/{personId}",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec  = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeIn(tween(SLIDE_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeOut(tween(SLIDE_DURATION))
                }
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getString("personId") ?: ""
                PersonDetailsScreen(
                    personId     = personId,
                    onBackClick  = { navController.popBackStack() },
                    onMovieClick = { movie ->
                        navController.navigate("details/${movie.id}/${movie.mediaType}")
                    },
                    viewModel = searchViewModel
                )
            }

            composable(
                route = "profile",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec  = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeIn(tween(SLIDE_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeOut(tween(SLIDE_DURATION))
                }
            ) {
                ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    viewModel   = searchViewModel
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(
    items       : List<Screen>,
    currentDest : androidx.navigation.NavDestination?,
    onItemClick : (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Surface(
            modifier      = Modifier.fillMaxWidth().height(64.dp),
            shape         = CircleShape,
            color         = Color(0xFF1A1A1A).copy(alpha = 0.95f),
            border        = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent))
            ),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier                = Modifier.fillMaxSize(),
                horizontalArrangement   = Arrangement.SpaceEvenly,
                verticalAlignment       = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val selected     = currentDest?.hierarchy?.any { it.route == screen.route } == true
                    val contentColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                    // Ripple-free interaction — feels snappier than default ripple on pill nav
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier         = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication        = null  // no ripple lag
                            ) { onItemClick(screen) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                screen.icon,
                                contentDescription = screen.label,
                                tint               = contentColor,
                                modifier           = Modifier.size(22.dp)
                            )
                            // Animated indicator dot
                            androidx.compose.animation.AnimatedVisibility(visible = selected) {
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
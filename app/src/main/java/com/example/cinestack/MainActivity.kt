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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.cinestack.ui.screens.*
import com.example.cinestack.ui.theme.CineStackTheme
import com.example.cinestack.ui.viewmodel.SearchViewModel
import java.net.URLDecoder
import java.net.URLEncoder

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
            enterTransition  = { fadeIn(tween(NAV_DURATION)) },
            exitTransition   = { fadeOut(tween(NAV_DURATION)) },
            popEnterTransition  = { fadeIn(tween(NAV_DURATION)) },
            popExitTransition   = { fadeOut(tween(NAV_DURATION)) }
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel   = searchViewModel,
                    onMovieClick = { movie ->
                        val encodedId = URLEncoder.encode(movie.id, "UTF-8")
                        navController.navigate("details/$encodedId/${movie.mediaType}")
                    }
                )
            }

            composable(Screen.Search.route) {
                HomeScreen(
                    onMovieClick = { movie ->
                        val encodedId = URLEncoder.encode(movie.id, "UTF-8")
                        navController.navigate("details/$encodedId/${movie.mediaType}")
                    },
                    viewModel = searchViewModel
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel    = searchViewModel,
                    onMovieClick = { movie ->
                        val encodedId = URLEncoder.encode(movie.id, "UTF-8")
                        navController.navigate("details/$encodedId/${movie.mediaType}")
                    },
                    onGroupClick = { groupId ->
                        navController.navigate("group/$groupId")
                    }
                )
            }

            // Detail screen
            composable(
                route = "details/{movieId}/{mediaType}",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec  = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeIn(tween(SLIDE_DURATION))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeOut(tween(SLIDE_DURATION))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec  = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeIn(tween(SLIDE_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)
                    ) + fadeOut(tween(SLIDE_DURATION))
                }
            ) { backStackEntry ->
                val rawId   = backStackEntry.arguments?.getString("movieId") ?: ""
                val movieId = try { URLDecoder.decode(rawId, "UTF-8") } catch (e: Exception) { rawId }
                val library by searchViewModel.library.collectAsState()

                val movie = remember(movieId, library) {
                    searchViewModel.getMovieFromCache(movieId)
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
                            val encodedId = URLEncoder.encode(selectedMovie.id, "UTF-8")
                            navController.navigate("details/$encodedId/${selectedMovie.mediaType}")
                        }
                    )
                } else {
                    // Movie not in cache — show spinner briefly, then pop back
                    // This only happens if the user somehow navigates to a stale URL
                    LaunchedEffect(movieId) {
                        kotlinx.coroutines.delay(3000)
                        navController.popBackStack()
                    }
                    Box(
                        modifier            = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment    = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading…", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            composable(
                route = "group/{groupId}",
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)) + fadeIn(tween(SLIDE_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)) + fadeOut(tween(SLIDE_DURATION))
                }
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")?.toIntOrNull() ?: 0
                GroupDetailsScreen(
                    groupId      = groupId,
                    viewModel    = searchViewModel,
                    onBackClick  = { navController.popBackStack() },
                    onMovieClick = { movie ->
                        val encodedId = URLEncoder.encode(movie.id, "UTF-8")
                        navController.navigate("details/$encodedId/${movie.mediaType}")
                    },
                    onSubGroupClick = { subId ->
                        navController.navigate("group/$subId")
                    }
                )
            }

            composable(
                route = "person/{personId}",
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)) + fadeIn(tween(SLIDE_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)) + fadeOut(tween(SLIDE_DURATION))
                }
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getString("personId") ?: ""
                PersonDetailsScreen(
                    personId     = personId,
                    onBackClick  = { navController.popBackStack() },
                    onMovieClick = { movie ->
                        val encodedId = URLEncoder.encode(movie.id, "UTF-8")
                        navController.navigate("details/$encodedId/${movie.mediaType}")
                    },
                    viewModel = searchViewModel
                )
            }

            composable(
                route = "profile",
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)) + fadeIn(tween(SLIDE_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(SLIDE_DURATION, easing = EaseInOut)) + fadeOut(tween(SLIDE_DURATION))
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
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier         = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication        = null
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
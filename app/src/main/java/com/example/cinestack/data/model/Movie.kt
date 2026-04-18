package com.example.cinestack.data.model

data class Movie(
    val id: Int,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val rating: Double,
    val genre: List<String>,
    val duration: String,
    val releaseYear: Int,
    val synopsis: String,
    val mediaType: String = "movie",
    val userStatus: String = "",
    val userRating: Double = 0.0,
    val currentSeason: Int = 0,
    val currentEpisode: Int = 0,
    val totalEpisodes: Int = 0
)

val sampleMovies = listOf(
    Movie(
        id = 1,
        title = "Inception",
        posterUrl = "https://image.tmdb.org/t/p/w500/9gk7Fn9sSAsS9699S1Z3C3D9Sff.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/original/8Z9R7v8Z9R7v8Z9R7v8Z9R7v8Z9.jpg",
        rating = 8.8,
        genre = listOf("Sci-Fi", "Action", "Adventure"),
        duration = "2h 28m",
        releaseYear = 2010,
        synopsis = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O."
    ),
    Movie(
        id = 2,
        title = "The Dark Knight",
        posterUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDp9s1vmsnZ9KqYpD1S.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/original/nMKdUU7p78Oig0S-vI16uY39R67.jpg",
        rating = 9.0,
        genre = listOf("Action", "Crime", "Drama"),
        duration = "2h 32m",
        releaseYear = 2008,
        synopsis = "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice."
    ),
    Movie(
        id = 3,
        title = "Interstellar",
        posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6EwfVDxCzs25vubp2FA.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/original/xJHbtvMubBrRXIv9u74v37UrIbh.jpg",
        rating = 8.7,
        genre = listOf("Adventure", "Drama", "Sci-Fi"),
        duration = "2h 49m",
        releaseYear = 2014,
        synopsis = "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival."
    )
)

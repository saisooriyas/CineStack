package com.example.cinestack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val rating: Double,
    val genres: String,
    val duration: String,
    val releaseYear: Int,
    val synopsis: String,
    val mediaType: String = "movie",
    val userStatus: String,
    val userRating: Double,
    val currentSeason: Int = 0,
    val currentEpisode: Int = 0,
    val totalEpisodes: Int = 0
)

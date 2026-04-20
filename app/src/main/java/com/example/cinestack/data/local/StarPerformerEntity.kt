package com.example.cinestack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "starred_performers")
data class StarredPerformerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String
)
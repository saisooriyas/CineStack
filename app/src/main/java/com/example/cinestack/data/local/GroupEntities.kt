package com.example.cinestack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val parentGroupId: Int? = null
)

@Entity(tableName = "group_items")
data class GroupItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val type: String, // "MOVIE", "PERSON"
    val externalId: Int,
    val title: String, // Cached for display
    val imageUrl: String // Cached for display
)

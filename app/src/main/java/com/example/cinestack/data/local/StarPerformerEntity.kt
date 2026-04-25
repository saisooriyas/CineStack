package com.example.cinestack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "starred_performers")
data class StarredPerformerEntity(
    @PrimaryKey val id: String,   // UUID
    val numericId: String = "",   // ← ADD: integer id as string for TPDB filtering
    val name: String,
    val imageUrl: String
)
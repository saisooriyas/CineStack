package com.example.cinestack.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Delete
    suspend fun deleteMovie(movie: MovieEntity)

    @Query("SELECT EXISTS(SELECT * FROM movies WHERE id = :id)")
    suspend fun exists(id: Int): Boolean

    // Group methods
    @Query("SELECT * FROM user_groups")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM user_groups WHERE parentGroupId IS NULL")
    fun getRootGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM user_groups WHERE parentGroupId = :parentId")
    fun getSubGroups(parentId: Int): Flow<List<GroupEntity>>

    @Query("SELECT * FROM user_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Int): GroupEntity?

    @Insert
    suspend fun insertGroup(group: GroupEntity): Long

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("SELECT * FROM group_items WHERE groupId = :groupId")
    fun getGroupItems(groupId: Int): Flow<List<GroupItemEntity>>

    @Insert
    suspend fun insertGroupItem(item: GroupItemEntity)

    @Delete
    suspend fun deleteGroupItem(item: GroupItemEntity)
}

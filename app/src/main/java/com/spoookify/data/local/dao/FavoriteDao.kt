package com.spoookify.data.local.dao

import androidx.room.*
import com.spoookify.data.local.entity.FavoriteTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_tracks WHERE interactionLevel > 0 ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteTrack>>

    @Query("SELECT * FROM favorite_tracks WHERE interactionLevel > 0 ORDER BY addedAt DESC")
    suspend fun getFavoritesList(): List<FavoriteTrack>

    @Query("SELECT * FROM favorite_tracks WHERE interactionLevel < 0")
    fun getNegativeTracks(): Flow<List<FavoriteTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(track: FavoriteTrack)

    @Delete
    suspend fun deleteFavorite(track: FavoriteTrack)

    @Query("SELECT EXISTS(SELECT * FROM favorite_tracks WHERE id = :id AND interactionLevel > 0)")
    fun isFavorite(id: String): Flow<Boolean>

    @Query("SELECT * FROM favorite_tracks WHERE id = :id")
    suspend fun getFavoriteById(id: String): FavoriteTrack?
}

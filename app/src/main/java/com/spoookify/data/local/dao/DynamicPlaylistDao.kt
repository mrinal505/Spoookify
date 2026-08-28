package com.spoookify.data.local.dao

import androidx.room.*
import com.spoookify.data.local.entity.DynamicPlaylist
import kotlinx.coroutines.flow.Flow

@Dao
interface DynamicPlaylistDao {
    @Query("SELECT * FROM dynamic_playlists")
    fun getAllDynamicPlaylists(): Flow<List<DynamicPlaylist>>

    @Query("SELECT * FROM dynamic_playlists")
    suspend fun getPlaylistsList(): List<DynamicPlaylist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: DynamicPlaylist)

    @Delete
    suspend fun deletePlaylist(playlist: DynamicPlaylist)
}
